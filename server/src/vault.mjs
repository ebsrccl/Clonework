import {createCipheriv, createDecipheriv, randomBytes, createHash, timingSafeEqual} from 'node:crypto';
import {mkdir, readFile, writeFile, rename} from 'node:fs/promises';
import {dirname} from 'node:path';

const AAD = Buffer.from('mikrotik-agent-vault:1');
export const digest = value => createHash('sha256').update(value).digest('hex');
export function sameSecret(a, b) {
  return typeof a === 'string' && typeof b === 'string' &&
    timingSafeEqual(Buffer.from(digest(a), 'hex'), Buffer.from(digest(b), 'hex'));
}
export class Vault {
  constructor(path, base64Key) {
    this.path = path;
    this.key = Buffer.from(base64Key || '', 'base64');
    if (this.key.length !== 32) throw new Error('VAULT_KEY_BASE64 harus berisi 32 byte acak dalam base64.');
  }
  async read() {
    let text;
    try { text = await readFile(this.path, 'utf8'); }
    catch (error) { if (error.code === 'ENOENT') return null; throw error; }
    const data = JSON.parse(text);
    if (data.version !== 1) throw new Error('Versi vault tidak didukung.');
    const cipher = createDecipheriv('aes-256-gcm', this.key, Buffer.from(data.iv, 'base64'));
    cipher.setAAD(AAD);
    cipher.setAuthTag(Buffer.from(data.tag, 'base64'));
    return JSON.parse(Buffer.concat([cipher.update(Buffer.from(data.body, 'base64')), cipher.final()]).toString());
  }
  async write(value) {
    await mkdir(dirname(this.path), {recursive: true, mode: 0o700});
    const iv = randomBytes(12);
    const cipher = createCipheriv('aes-256-gcm', this.key, iv);
    cipher.setAAD(AAD);
    const body = Buffer.concat([cipher.update(JSON.stringify(value), 'utf8'), cipher.final()]);
    const envelope = {version: 1, iv: iv.toString('base64'), tag: cipher.getAuthTag().toString('base64'), body: body.toString('base64')};
    const temp = this.path + '.tmp-' + randomBytes(8).toString('hex');
    await writeFile(temp, JSON.stringify(envelope), {mode: 0o600, flag: 'wx'});
    await rename(temp, this.path);
  }
}
