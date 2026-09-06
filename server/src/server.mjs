import http from 'node:http';
import {randomBytes} from 'node:crypto';
import {readFile} from 'node:fs/promises';
import {pathToFileURL} from 'node:url';
import {Vault, sameSecret, digest} from './vault.mjs';
import {RouterOS} from './routeros.mjs';
import {Toolbox, runAgent, callOpenAI} from './agent.mjs';

function parseSetup(body) {
  const allowed = ['router', 'openai_key', 'model'];
  if (!body || Object.keys(body).some(k => !allowed.includes(k)) || !body.router) throw new Error('Isian pengaturan tidak sesuai.');
  const r = body.router;
  if (Object.keys(r).some(k => !['host', 'port', 'username', 'password', 'name'].includes(k))) throw new Error('Isian router tidak sesuai.');
  const text = (value, max) => typeof value === 'string' && value.length > 0 && value.length <= max && !/[\x00-\x1f]/.test(value);
  if (!text(r.host, 253) || !/^[a-zA-Z0-9.:_-]+$/.test(r.host) || !Number.isInteger(r.port) || r.port < 1 || r.port > 65535 ||
      !text(r.username, 128) || !text(r.password, 1024) || !text(r.name, 100) || !text(body.openai_key, 1024) ||
      !text(body.model, 100) || !/^[a-zA-Z0-9._:-]+$/.test(body.model)) throw new Error('Lengkapi alamat, port, akun router, API key, dan model.');
  return body;
}
async function readJson(request) {
  if (!(request.headers['content-type'] || '').startsWith('application/json')) throw new Error('Gunakan JSON.');
  const chunks = []; let size = 0;
  for await (const data of request) {
    size += data.length;
    if (size > 24000) throw new Error('Permintaan terlalu besar.');
    chunks.push(data);
  }
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}
function send(response, status, data) {
  response.writeHead(status, {'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff', 'Referrer-Policy': 'no-referrer'});
  response.end(JSON.stringify(data));
}
export async function createGateway({vault, setupToken, routerFactory = config => new RouterOS(config), responder = callOpenAI}) {
  if (typeof setupToken !== 'string' || setupToken.length < 32) throw new Error('Kode pemasangan harus minimal 32 karakter acak.');
  let state = await vault.read();
  let busy = false;
  const persist = () => vault.write(state);
  const authenticated = token => state?.deviceHash && sameSecret(digest(token), state.deviceHash);
  const toolbox = () => new Toolbox(routerFactory(state.config.router), state.plans, persist);
  const publicState = () => ({router_name: state.config.router.name, model: state.config.model,
    mikhmon: 'not_installed', plans: Object.values(state.plans).slice(-20), messages: state.messages.slice(-30)});
  return http.createServer(async (request, response) => {
    const pathname = (request.url || '').split('?')[0];
    const bearer = /^Bearer (.+)$/.exec(request.headers.authorization || '')?.[1] || '';
    const isSetup = request.method === 'POST' && pathname === '/v1/setup';
    if (request.method === 'GET' && pathname === '/health') return send(response, 200, {ok: true});
    if (isSetup ? !sameSecret(bearer, setupToken) || Boolean(state?.deviceHash) : !authenticated(bearer)) {
      return send(response, 401, {error: 'Akses tidak valid. Periksa kode pemasangan atau sesi perangkat.'});
    }
    if (request.method === 'GET' && pathname === '/v1/session') return send(response, 200, publicState());
    if (request.method !== 'POST') return send(response, 404, {error: 'Endpoint tidak tersedia.'});
    if (busy) return send(response, 409, {error: 'Ada tugas berjalan. Tunggu hingga selesai.'});
    busy = true;
    try {
      const body = await readJson(request);
      if (isSetup) {
        const config = parseSetup(body);
        const router = routerFactory(config.router);
        await router.read('/system/identity', ['name']);
        const check = await responder(config.openai_key, {model: config.model, store: false,
          input: 'Balas dengan kata siap.', max_output_tokens: 200});
        if (!Array.isArray(check.output)) throw new Error('OpenAI tidak mengembalikan respons yang sesuai.');
        const token = randomBytes(32).toString('base64url');
        state = {config, deviceHash: digest(token), plans: {}, turns: [], messages: []};
        try { await persist(); } catch { state = null; throw new Error('Tidak dapat menyimpan pengaturan.'); }
        return send(response, 200, {token, ...publicState()});
      }
      if (pathname === '/v1/chat') {
        if (typeof body.message !== 'string' || !body.message.trim() || body.message.length > 3000 || Object.keys(body).length !== 1) {
          return send(response, 400, {error: 'Pesan harus berisi 1–3000 karakter.'});
        }
        const result = await runAgent({message: body.message, model: state.config.model,
          key: state.config.openai_key, turns: state.turns, toolbox: toolbox(), responder});
        state.turns = [...state.turns, result.turn].slice(-5);
        state.messages.push({role: 'user', text: body.message}, {role: 'assistant', text: result.answer});
        state.messages = state.messages.slice(-30);
        await persist();
        return send(response, 200, {answer: result.answer, events: result.events, plans: Object.values(state.plans).slice(-20)});
      }
      const match = /^\/v1\/changes\/([a-f0-9-]{36})\/apply$/.exec(pathname);
      if (match) {
        const result = await toolbox().apply(match[1]);
        // Applying a plan changes the world. Drop old model context rather than retaining stale claims.
        state.turns = [];
        state.messages.push({role: 'assistant', text: 'Hasil perubahan bandwidth: ' + result.status + '.'});
        await persist();
        return send(response, 200, {plan: result});
      }
      if (pathname === '/v1/logout') {
        state.deviceHash = null; state.turns = [];
        await persist();
        return send(response, 200, {ok: true});
      }
      send(response, 404, {error: 'Endpoint tidak tersedia.'});
    } catch {
      // No request bodies, model output, raw network errors, or secrets in HTTP errors/logs.
      send(response, 400, {error: 'Tugas belum berhasil. Periksa koneksi, sertifikat API-SSL, akses router, model OpenAI, dan kuota API. Perubahan yang hasilnya belum pasti tidak diulang otomatis.'});
    } finally { busy = false; }
  });
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    const ca = process.env.ROUTER_CA_FILE ? await readFile(process.env.ROUTER_CA_FILE) : undefined;
    const server = await createGateway({vault: new Vault(process.env.VAULT_FILE || './data/vault.json', process.env.VAULT_KEY_BASE64),
      setupToken: process.env.SETUP_TOKEN, routerFactory: config => new RouterOS(config, ca)});
    server.requestTimeout = 300000;
    server.headersTimeout = 15000;
    server.listen(Number(process.env.PORT || 8787), process.env.BIND_HOST || '127.0.0.1', () => {
      process.stdout.write('Server agen aktif. Gunakan HTTPS reverse proxy untuk perangkat jarak jauh.\n');
    });
  } catch { process.stderr.write('Server gagal dimulai. Periksa konfigurasi rahasia dan berkas vault.\n'); process.exitCode = 1; }
}
