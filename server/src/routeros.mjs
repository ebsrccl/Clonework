import tls from 'node:tls';
import {once} from 'node:events';
import {isIP} from 'node:net';

const MAX_WORD = 1024 * 1024;
export function encodeLength(n) {
  if (!Number.isSafeInteger(n) || n < 0 || n > 0xffffffff) throw new Error('Panjang kata tidak valid.');
  if (n < 0x80) return Buffer.from([n]);
  if (n < 0x4000) return Buffer.from([(n >> 8) | 0x80, n & 255]);
  if (n < 0x200000) return Buffer.from([(n >> 16) | 0xc0, (n >> 8) & 255, n & 255]);
  if (n < 0x10000000) return Buffer.from([(n >> 24) | 0xe0, (n >> 16) & 255, (n >> 8) & 255, n & 255]);
  const data = Buffer.alloc(5); data[0] = 0xf0; data.writeUInt32BE(n, 1); return data;
}
export function decodeLength(data) {
  if (!data.length) return null;
  const first = data[0];
  const count = first < 0x80 ? 1 : first < 0xc0 ? 2 : first < 0xe0 ? 3 : first < 0xf0 ? 4 : first === 0xf0 ? 5 : 0;
  if (!count) throw new Error('Prefix protokol RouterOS tidak didukung.');
  if (data.length < count) return null;
  if (count === 5) return {length: data.readUInt32BE(1), count};
  let n = first & [0, 0x7f, 0x3f, 0x1f, 0x0f][count];
  for (let i = 1; i < count; i++) n = n * 256 + data[i];
  return {length: n, count};
}
export function encodeSentence(words) {
  return Buffer.concat([...words.flatMap(word => {
    const data = Buffer.from(word, 'utf8');
    if (data.length > MAX_WORD) throw new Error('Kata API terlalu panjang.');
    return [encodeLength(data.length), data];
  }), Buffer.from([0])]);
}
export class SentenceParser {
  constructor() { this.buffer = Buffer.alloc(0); this.words = []; this.bytes = 0; }
  feed(chunk) {
    this.buffer = Buffer.concat([this.buffer, chunk]);
    const result = [];
    while (this.buffer.length) {
      const head = decodeLength(this.buffer);
      if (!head) break;
      if (head.length > MAX_WORD) throw new Error('Balasan API terlalu besar.');
      if (this.buffer.length < head.count + head.length) break;
      const word = this.buffer.subarray(head.count, head.count + head.length).toString('utf8');
      this.buffer = this.buffer.subarray(head.count + head.length);
      if (head.length === 0) {
        result.push(this.words); this.words = []; this.bytes = 0;
      } else {
        this.bytes += head.length;
        if (this.bytes > 2 * MAX_WORD || this.words.length > 4096) throw new Error('Kalimat API terlalu besar.');
        this.words.push(word);
      }
    }
    return result;
  }
}
export class RouterOS {
  constructor(config, ca) { this.config = config; this.ca = ca; }
  async session(work) {
    const {host, port, username, password} = this.config;
    const socket = tls.connect({host, port, servername: isIP(host) ? undefined : host,
      rejectUnauthorized: true, minVersion: 'TLSv1.2', ...(this.ca ? {ca: this.ca} : {})});
    socket.setTimeout(12000, () => socket.destroy(new Error('Koneksi router melewati batas waktu.')));
    // Store errors before awaiting to avoid losing errors between two commands.
    let failure, waiter;
    const queue = [], parser = new SentenceParser();
    const fail = error => { failure = error; if (waiter) { const w = waiter; waiter = null; w.reject(error); } };
    socket.on('error', fail);
    socket.on('end', () => fail(new Error('Router menutup koneksi.')));
    socket.on('data', data => {
      try {
        queue.push(...parser.feed(data));
        if (waiter && queue.length) { const w = waiter; waiter = null; w.resolve(queue.shift()); }
      } catch (error) { fail(error); socket.destroy(); }
    });
    const next = () => failure ? Promise.reject(failure) : queue.length ? Promise.resolve(queue.shift()) :
      new Promise((resolve, reject) => { waiter = {resolve, reject}; });
    const command = async words => {
      if (failure) throw failure;
      socket.write(encodeSentence(words));
      const rows = [];
      for (let i = 0; i < 20000; i++) {
        const sentence = await next();
        const record = {};
        for (const word of sentence.slice(1)) {
          if (!word.startsWith('=')) continue;
          const split = word.indexOf('=', 1);
          if (split > 1) record[word.slice(1, split)] = word.slice(split + 1);
        }
        if (sentence[0] === '!trap' || sentence[0] === '!fatal') {
          // Raw RouterOS error strings can contain secrets or attacker-controlled text.
          throw new Error('Router menolak perintah. Periksa hak akses dan parameter.');
        }
        if (sentence[0] === '!done') return rows;
        if (sentence[0] === '!re') rows.push(record);
      }
      throw new Error('Jumlah balasan router melebihi batas.');
    };
    try {
      await once(socket, 'secureConnect');
      await command(['/login', '=name=' + username, '=password=' + password]);
      return await work(command);
    } finally { socket.destroy(); }
  }
  async read(path, properties) {
    const rows = await this.session(command => command([path + '/print', '=.proplist=' + properties.join(',')]));
    return rows.map(row => Object.fromEntries(properties.filter(key => Object.hasOwn(row, key)).map(key => [key, row[key]])));
  }
  async setQueue(id, limit) {
    return this.session(command => command(['/queue/simple/set', '=.id=' + id, '=max-limit=' + limit]));
  }
}
