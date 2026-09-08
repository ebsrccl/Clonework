# Versi 0.4 — preview Android ARM64

Status 8 September 2026: APK berhasil dibangun dan ditandatangani debug. Sebanyak 31 pengujian JVM lolos, lint 0 error / 5 warning. Emulator Android 15 berhasil menguji startup runtime Codex, awal login resmi, pembatalan, restart, penyimpanan banyak router, serta backup terenkripsi. Ini bukan pengujian login akun nyata atau koneksi ke MikroTik fisik.

Build: https://github.com/ebsrccl/Clonework/actions/runs/34195949390
Commit aplikasi: `2dd8f04e67d30c84dd3fb8f07eaab96ed286ac75`
APK: `Mikrotik-Agent-ChatGPT-0.4-ARM64.apk` (84.371.695 byte)
SHA-256: `f877a2d072e12b2d4a8a65cb20820116a9633ac41747d02068c56c01e6d23cef`

## Mulai memakai

1. Instal APK 0.4. Aplikasi terpasang berdampingan dengan 0.3; login dan data router tidak otomatis berpindah.
2. Geser bar menu ke kiri untuk menemukan Pengaturan, kemudian masuk ChatGPT lewat Codex.
3. Buka Router > Tambah router. Isi alamat yang dapat dijangkau HP, port API, username/password, serta TLS/pin sesuai router.
4. Simpan profil setelah diagnostik API berhasil. Pilih router aktif sebelum memakai Chat atau Hotspot.
5. Periksa usulan di Aktivitas dan konfirmasikan sebelum perubahan diterapkan. Mode baca saja dapat diatur per router.
6. Hotspot menyediakan voucher dan cetak/PDF; Pengaturan menyediakan backup terenkripsi dengan frasa sandi.

Tidak memerlukan VPS atau API key OpenAI. Host RouterOS berjalan di HP; permintaan AI menggunakan sesi Codex/ChatGPT dan memerlukan internet.

## Implementasi dalam kode

- Host fungsi RouterOS langsung di Android dengan sumber daya/field yang terdaftar. Agen menyiapkan usulan; pengguna menerapkan, lalu aplikasi membaca ulang hasil.
- Sampai 50 profil router: tambah, edit, pilih, hapus. Kredensial, pesan, usulan, dan kartu akses terpisah per ID router, memakai AES-GCM dan Android Keystore.
- Migrasi format data lama jika berada dalam penyimpanan aplikasi yang sama. Perubahan koneksi menonaktifkan usulan lama.
- Diagnostik login API, identitas, TLS, dan inspeksi policy akun jika diizinkan router. Login/baca berhasil tidak dianggap bukti izin write; aplikasi tidak menaikkan izin.
- Hotspot: pengguna, profil, server, binding, host, sesi aktif, dan usulan pemutusan sesi.
- Batch 1–50 voucher: nama/password acak lokal, batas uptime, harga lokal, tanda terjual manual, cetak/PDF, ekspor CSV. Kredensial tidak menjadi hasil fungsi AI.
- PPP, queue, interface, alamat IP, DHCP, DNS statis, rute, filter firewall, NAT, address list, dan log melalui subset field yang ditentukan.
- Validasi usulan stale/kedaluwarsa, mode baca saja, penyimpanan sebelum menulis, verifikasi hasil, status tidak pasti/parsial, dan pencegahan pengulangan batch terputus.
- Pengaturan per router: TLS/pin sertifikat, izin menulis aplikasi, masa usulan 1–60 menit, URL Mikhmon opsional.
- Backup profil terenkripsi PBKDF2-HMAC-SHA256/AES-GCM dan pemulihan melalui pemilih berkas Android; sesi ChatGPT tidak termasuk cadangan.

## Batas yang masih berlaku

Mikhmon PHP asli belum dibundel atau di-host di APK. Hotspot/voucher di atas merupakan implementasi langsung API RouterOS. URL opsional membuka instalasi Mikhmon pengguna di browser; bukan otomatisasi seluruh fungsi Mikhmon.

Shell umum, skrip RouterOS bebas, perubahan policy pengguna, reset/reboot, backup biner router, penjadwalan, pembayaran, laporan penjualan Mikhmon berbasis skrip, dan kedaluwarsa voucher kalender belum diimplementasikan. Limit uptime adalah waktu pemakaian kumulatif.

Belum ada akses ke MikroTik fisik pengguna. Izin write efektif, parameter, dan dukungan versi/perangkat tetap memerlukan verifikasi nyata. Password akun yang dibuat tidak dibaca kembali dari router untuk verifikasi.

Identitas APK 0.4 adalah `id.mas.agent.chatgpt.v4`, berdampingan dengan 0.3 karena kunci debug CI berubah antar-build. Data dan sesi 0.3 tidak otomatis berpindah; aplikasi baru memerlukan login dan profil. Strategi tanda tangan update tetap perlu diselesaikan untuk rilis lanjutan.

## Pengujian yang dijalankan

`RouterHostTest` mencakup migrasi/isolasi, perubahan profil dan worker stale, kegagalan penyimpanan, backup/password/tamper, batch voucher sukses/parsial, mode baca saja, router mismatch, field skrip terlarang, verifikasi perubahan profil, dan inspeksi izin. Pengujian 0.3 dipertahankan dan lolos. `SmokeProbe` diperluas untuk penyimpanan banyak router dan backup di Android.

Build dan pengujian dijalankan melalui GitHub Actions setelah persetujuan publikasi. Runtime Codex native tetap memakai build yang telah berhasil digunakan APK 0.3.

## Referensi

- [RouterOS API](https://help.mikrotik.com/docs/spaces/ROS/pages/47579160/API)
- [Policy pengguna](https://help.mikrotik.com/docs/spaces/ROS/pages/8978504/User)
- [Hotspot](https://help.mikrotik.com/docs/spaces/ROS/pages/56459266/HotSpot+-+Captive+portal)
- [Mikhmon V3](https://github.com/laksa19/mikhmonv3)
