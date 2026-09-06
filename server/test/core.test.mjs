import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtemp, readFile, rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {randomBytes} from 'node:crypto';
import {once} from 'node:events';
import {encodeLength, decodeLength, encodeSentence, SentenceParser, RouterOS} from '../src/routeros.mjs';
import {Vault} from '../src/vault.mjs';
import {Toolbox, runAgent, normalizeLimit} from '../src/agent.mjs';
import {createGateway} from '../src/server.mjs';

class FakeRouter {
  constructor() {
    this.queue = {'.id': '*A', name: 'Pelanggan contoh', target: '192.0.2.10/32', 'max-limit': '2M/3M', disabled: 'false', dynamic: 'false'};
    this.writes = 0;
  }
  async read(path) {
    if (path === '/queue/simple') return [{...this.queue}];
    if (path === '/system/identity') return [{name: 'Router uji'}];
    if (path === '/system/resource') return [{'cpu-load': '7', version: '7-test'}];
    return [];
  }
  async setQueue(id, limit) { assert.equal(id, this.queue['.id']); this.writes++; this.queue['max-limit'] = limit; }
}
const args = {queue_id: '*A', upload_mbps: 5, download_mbps: 10};
const outputText = text => ({status: 'completed', output: [{type: 'message', role: 'assistant', content: [{type: 'output_text', text}]}]});

test('RouterOS lengths match protocol boundary byte vectors', () => {
  const vectors = [[0, '00'], [127, '7f'], [128, '8080'], [16383, 'bfff'], [16384, 'c04000'],
    [2097151, 'dfffff'], [2097152, 'e0200000'], [268435455, 'efffffff'], [268435456, 'f010000000']];
  for (const [length, hex] of vectors) {
    assert.equal(encodeLength(length).toString('hex'), hex);
    assert.equal(decodeLength(Buffer.from(hex, 'hex')).length, length);
  }
  assert.equal(decodeLength(Buffer.from('c0', 'hex')), null);
  assert.throws(() => decodeLength(Buffer.from([0xf1])));
});
test('Parser handles fragmented UTF-8 and several sentences in one stream', () => {
  const parser = new SentenceParser(), found = [];
  const bytes = Buffer.concat([encodeSentence(['!re', '=name=Router ✓']), encodeSentence(['!done'])]);
  for (const byte of bytes) found.push(...parser.feed(Buffer.from([byte])));
  assert.deepEqual(found, [['!re', '=name=Router ✓'], ['!done']]);
  assert.throws(() => new SentenceParser().feed(encodeLength(2 ** 22)));
});
test('Router reader drops fields that were not requested, including secrets', async () => {
  const router = new RouterOS({});
  router.session = async () => [{name: 'Router', password: 'DO-NOT-RETURN', privateKey: 'DO-NOT-RETURN'}];
  assert.deepEqual(await router.read('/system/identity', ['name']), [{name: 'Router'}]);
});
test('Vault survives restart, hides plaintext, and rejects the wrong key', async () => {
  const dir = await mkdtemp(join(tmpdir(), 'mas-vault-'));
  try {
    const key = randomBytes(32).toString('base64'), path = join(dir, 'vault');
    const value = {config: {openai_key: 'SECRET-OPENAI', router: {password: 'SECRET-ROUTER'}}, messages: ['hello']};
    await new Vault(path, key).write(value);
    const bytes = await readFile(path, 'utf8');
    assert.ok(!bytes.includes('SECRET')); assert.ok(!bytes.includes('hello'));
    assert.deepEqual(await new Vault(path, key).read(), value);
    await assert.rejects(new Vault(path, randomBytes(32).toString('base64')).read());
  } finally { await rm(dir, {recursive: true, force: true}); }
});
test('Unregistered tools and extra arguments cannot execute writes', async () => {
  const router = new FakeRouter(), box = new Toolbox(router);
  await assert.rejects(box.execute('run_shell', {command: 'anything'}));
  await assert.rejects(box.execute('prepare_queue_limit', {...args, command: '/system/reset-configuration'}));
  await assert.rejects(box.execute('prepare_queue_limit', {...args, upload_mbps: -1}));
  assert.equal(router.writes, 0);
});
test('A proposal writes nothing; applying verifies readback and prevents duplicates', async () => {
  const router = new FakeRouter(), box = new Toolbox(router);
  const result = await box.execute('prepare_queue_limit', args);
  assert.equal(router.writes, 0); assert.equal(result.plan.status, 'pending');
  assert.equal((await box.apply(result.plan.id)).status, 'verified');
  await box.apply(result.plan.id); assert.equal(router.writes, 1);
  assert.equal(normalizeLimit('5M/10M'), '5000000/10000000');
});
test('Stale or expired queue proposals do not change the router', async () => {
  const router = new FakeRouter(), box = new Toolbox(router);
  const first = await box.execute('prepare_queue_limit', args);
  router.queue.target = '192.0.2.11/32';
  assert.equal((await box.apply(first.plan.id)).status, 'stale');
  const second = await box.execute('prepare_queue_limit', args);
  second.plan.expires_at = 0;
  assert.equal((await box.apply(second.plan.id)).status, 'expired'); assert.equal(router.writes, 0);
});
test('Uncertain write is persisted and never retried after an interrupted connection', async () => {
  const router = new FakeRouter();
  router.setQueue = async () => { router.writes++; throw new Error('Disconnected after sending'); };
  const persisted = []; const plans = {};
  const box = new Toolbox(router, plans, async () => persisted.push(structuredClone(plans)));
  const {plan} = await box.execute('prepare_queue_limit', args);
  assert.equal((await box.apply(plan.id)).status, 'unknown');
  const restored = new Toolbox(router, structuredClone(plans));
  await restored.apply(plan.id); assert.equal(router.writes, 1);
  assert.ok(persisted.some(state => state[plan.id].status === 'applying'));
});
test('Successful command with mismatched readback is not reported verified', async () => {
  const router = new FakeRouter(); router.setQueue = async () => { router.writes++; };
  const box = new Toolbox(router); const {plan} = await box.execute('prepare_queue_limit', args);
  assert.equal((await box.apply(plan.id)).status, 'unverified');
});
test('Changed queue target during a write prevents a verified result', async () => {
  const router = new FakeRouter(); router.setQueue = async (id, limit) => {
    router.queue['max-limit'] = limit; router.queue.target = '192.0.2.99/32'; router.writes++;
  };
  const box = new Toolbox(router); const {plan} = await box.execute('prepare_queue_limit', args);
  assert.equal((await box.apply(plan.id)).status, 'unverified');
});
test('Agent passes tool results and reasoning items back without router credentials', async () => {
  const router = new FakeRouter(); let calls = 0;
  const result = await runAgent({message: 'Cek status router', model: 'test-model', key: 'API-SECRET', toolbox: new Toolbox(router),
    responder: async (key, payload) => {
      calls++; assert.equal(key, 'API-SECRET'); assert.ok(!JSON.stringify(payload).includes('API-SECRET'));
      assert.equal(payload.store, false);
      if (calls === 1) return {status: 'completed', output: [
        {type: 'reasoning', id: 'r-test', summary: [], encrypted_content: 'encrypted-test'},
        {type: 'function_call', name: 'router_summary', call_id: 'call-test', arguments: '{}'}]};
      assert.ok(payload.input.some(x => x.type === 'reasoning'));
      const output = payload.input.find(x => x.type === 'function_call_output');
      assert.equal(output.call_id, 'call-test'); assert.ok(output.output.includes('Router uji'));
      return outputText('CPU router uji 7%.');
    }});
  assert.equal(calls, 2); assert.equal(result.answer, 'CPU router uji 7%.'); assert.equal(router.writes, 0);
});
test('Gateway authenticates setup, persists sessions, redacts responses, and revokes tokens', async () => {
  const dir = await mkdtemp(join(tmpdir(), 'mas-http-'));
  const key = randomBytes(32).toString('base64'), setupToken = 'test-only-setup-code-12345678901234567890';
  const vault = new Vault(join(dir, 'vault'), key), router = new FakeRouter();
  let server;
  const start = async () => {
    server = await createGateway({vault, setupToken, routerFactory: () => router, responder: async () => outputText('siap')});
    server.listen(0, '127.0.0.1'); await once(server, 'listening'); return 'http://127.0.0.1:' + server.address().port;
  };
  const stop = async () => { const done = once(server, 'close'); server.close(); server.closeAllConnections(); await done; };
  const request = (base, path, token, body) => fetch(base + path, {method: body ? 'POST' : 'GET',
    headers: {Authorization: 'Bearer ' + token, 'Content-Type': 'application/json'}, ...(body ? {body: JSON.stringify(body)} : {})});
  try {
    let base = await start();
    const data = {router: {name: 'Uji', host: '192.0.2.1', port: 8729, username: 'test-user', password: 'ROUTER-SECRET'}, openai_key: 'OPENAI-SECRET', model: 'test-model'};
    assert.equal((await request(base, '/v1/setup', 'wrong', data)).status, 401);
    const created = await request(base, '/v1/setup', setupToken, data); assert.equal(created.status, 200);
    const enrolled = await created.json(); assert.ok(enrolled.token); assert.ok(!JSON.stringify(enrolled).includes('SECRET'));
    assert.equal((await request(base, '/v1/setup', setupToken, data)).status, 401);
    assert.equal((await request(base, '/v1/session', 'wrong')).status, 401);
    await stop(); base = await start();
    assert.equal((await request(base, '/v1/session', enrolled.token)).status, 200);
    const chat = await request(base, '/v1/chat', enrolled.token, {message: 'Cek router'});
    assert.equal(chat.status, 200); assert.equal((await chat.json()).answer, 'siap');
    assert.equal((await request(base, '/v1/logout', enrolled.token, {})).status, 200);
    assert.equal((await request(base, '/v1/session', enrolled.token)).status, 401);
  } finally { if (server?.listening) await stop(); await rm(dir, {recursive: true, force: true}); }
});
