# Validasi APK ChatGPT 0.3

Tanggal pengujian: 7 September 2026 (UTC).

## Runtime Android

- Codex 0.153.4, sumber `3d2ee51ca2d5db578f328aa75e20aa22c0197c9a`.
- Workflow runtime: [34154458125](https://github.com/ebsrccl/Clonework/actions/runs/34154458125), commit build `8e7348fcf626bf82e748a0d6fe916d862e9d88d3`.
- ARM64 dan x86_64: kompilasi berhasil. Interpreter Android `/system/bin/linker64`; segmen ELF selaras 16 KiB; dependensi dinamis `libc.so`, `libm.so`, `libdl.so`.
- Probe Android: proses native berhasil melakukan `initialize` dan `account/read` dalam APK target SDK 36 pada emulator Android 15.
- Perbaikan portabilitas: OpenSSL vendored, dukungan `flock` Android pada Rust std, dan tautan pustaka builtins NDK. Autentikasi/kebijakan Codex tidak diubah. Binary Linux yang sebelumnya gagal tidak dipakai.

## APK dan batas tindakan agen

Build final [34155581557](https://github.com/ebsrccl/Clonework/actions/runs/34155581557), commit `858a429d2e211f7e2209be31752cdb30ad7f2787`, lulus:

- 20 pengujian JVM: LocalAgentTest 15, CodexRpcTest 3, CodexConversationTest 2; nol gagal atau dilewati.
- Pengujian meliputi pemisahan request/response RPC, batas paket dan putus proses, penyamaran password, validasi fungsi, proposal bandwidth, penolakan apply/shell dari model, konfigurasi stale, kedaluwarsa, replay, kegagalan penyimpanan, pemeriksaan ulang hasil, dan protokol RouterOS TCP tiruan.
- Percakapan tiruan memakai inti agen sebenarnya: model membuat proposal tanpa menulis ke router; tindakan aplikasi melalui tombol menerapkan satu perubahan dan memverifikasi hasil.
- `environments: []` dikirim saat thread dan turn dimulai; fungsi dinamis dibatasi daftar MikroTik. Permintaan server di luar handler ditolak.
- Android Lint: 0 error, 4 warning (target API, versi dependensi pengujian, pemeriksaan sertifikat pin khusus, dan teks UI literal). Pemeriksaan sertifikat khusus tetap membandingkan sidik jari yang ditentukan pengguna; verifikasi TLS tidak dinonaktifkan.
- Instrumentasi menggunakan CodexBrain produksi: akun awal kosong, login ChatGPT mengembalikan URL HTTPS resmi, login dibatalkan, akun tetap kosong, runtime dimulai ulang. Tidak mencatat URL/state/token login.
- APK terpasang di emulator; halaman akun ChatGPT terlihat tanpa kolom API key.
- Tanda tangan APK v2 diverifikasi oleh `apksigner`.

Pengujian OAuth sampai masuk menggunakan akun nyata, jawaban dari model OpenAI, koneksi ke MikroTik fisik, dan eksekusi di HP ARM64 belum dilakukan. Emulator menguji runtime x86_64; ARM64 diperiksa saat kompilasi dan inspeksi ELF. Ini rilis preview, bukan klaim pengujian ujung ke ujung pada perangkat pengguna.

## Berkas yang disediakan

- `Mikrotik-Agent-ChatGPT-0.3-ARM64.apk`
- Ukuran: 84341735 byte.
- SHA-256: `31b6be85372ade80143b342ecc3658de2ff27ac0e7877167d86405be38b98124`.
- Checksum ZIP artifact dan APK hasil ekstraksi cocok dengan catatan GitHub Actions.
- DEX berisi alur `account/login/start`, tanpa field `openai_key` atau transport Responses API versi lama.
- Tangkapan layar final memperlihatkan status siap dan tombol Login ChatGPT / Periksa login.
