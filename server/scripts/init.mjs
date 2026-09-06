import {writeFile, chmod} from 'node:fs/promises';
import {randomBytes} from 'node:crypto';
const text = 'VAULT_KEY_BASE64=' + randomBytes(32).toString('base64') + '\nSETUP_TOKEN=' + randomBytes(32).toString('base64url') +
  '\nVAULT_FILE=./data/vault.json\nBIND_HOST=127.0.0.1\nPORT=8787\n';
try {
  await writeFile('.env', text, {mode: 0o600, flag: 'wx'}); await chmod('.env', 0o600);
  process.stdout.write('Berkas .env dibuat. Simpan kuncinya; jangan masukkan .env ke arsip atau git.\n');
} catch { process.stderr.write('Berkas .env tidak dibuat. Periksa apakah sudah ada; tidak menimpa kunci lama.\n'); process.exitCode = 1; }
