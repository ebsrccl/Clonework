import {Vault} from '../src/vault.mjs';
// Run only by the operator when recovering a lost/reinstalled device.
try {
  const vault = new Vault(process.env.VAULT_FILE || './data/vault.json', process.env.VAULT_KEY_BASE64);
  const state = await vault.read();
  if (state) { state.deviceHash = null; state.turns = []; await vault.write(state); }
  process.stdout.write('Token perangkat dicabut. Hentikan server saat menjalankan pemulihan ini, lalu mulai ulang.\n');
} catch { process.stderr.write('Pemulihan sesi gagal; konfigurasi tidak dapat dibaca.\n'); process.exitCode = 1; }
