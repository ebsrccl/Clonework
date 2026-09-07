# Uji kelayakan login ChatGPT lokal

Modul ini adalah pengujian pengembang, bukan APK MikroTik untuk pengguna. Aplikasi utama versi 0.2 tidak diubah oleh modul ini.

Tujuan: memeriksa apakah binary resmi Codex App Server dapat berjalan di dalam sandbox aplikasi Android tanpa root, Termux, komputer pendamping, atau VPS. Build berhasil belum membuktikan runtime maupun login bekerja.

## Sumber dan kontrak

- Runtime dipatok ke [openai/codex rust-v0.153.4](https://github.com/openai/codex/releases/tag/rust-v0.153.4), arsip Linux musl x86_64 untuk emulator. SHA-256 arsip diperiksa sebelum pengemasan.
- [Dokumentasi App Server](https://learn.chatgpt.com/docs/app-server) menyediakan login ChatGPT yang dikelola Codex melalui `account/login/start`, `type: chatgpt` atau `chatgptDeviceCode`. Token tidak diperoleh dengan menyalin cookie browser atau membuat ulang endpoint OAuth.
- Android tidak tercantum sebagai target binary pada rilis ini. Memasukkan binary Linux ke APK adalah eksperimen kompatibilitas, bukan klaim dukungan resmi Android.

## Gate saat ini

1. Bangun APK dengan executable dari arsip resmi di `jniLibs/x86_64/libcodex.so`; Android mengekstraknya sebagai bagian instalasi APK.
2. Instal APK di emulator Android 15 dengan target SDK 36.
3. Dari proses aplikasi, jalankan binary melalui `ProcessBuilder` dan protokol stdio resmi.
4. Wajib memperoleh respons `initialize` dan `account/read` yang menunjukkan belum ada akun. Hanya keberhasilan keduanya menghasilkan `CODEX_ANDROID_PROBE_PASS`.

Uji ini tidak login ke akun, tidak membuat permintaan model, dan tidak mengakses MikroTik. Tidak ada kredensial dalam pengujian. Tidak ada perubahan kebijakan keamanan Android, root aplikasi, atau penurunan target SDK untuk meloloskan runtime.

## Pekerjaan bersyarat setelah gate berhasil

- Verifikasi kompatibilitas jaringan, DNS Android, sertifikat TLS, dan login resmi di browser. Jangan menganggap respons `initialize` membuktikan koneksi internet.
- Bangun dan periksa varian ARM64 untuk HP; hasil emulator x86_64 tidak membuktikan ARM64 bekerja.
- Gunakan `thread/start` dengan `environments: []` untuk menonaktifkan akses lingkungan terminal/filesystem. Nonaktifkan konektor, plugin, browser, web, subagen, dan fitur lain di luar fungsi MikroTik. Tolak permintaan tambahan yang tidak didukung.
- Daftarkan `dynamicTools` yang memetakan hanya ke allowlist `LocalAgent.execute`. `prepare_queue_limit` hanya menghasilkan usulan. Jangan mengekspos `apply` sebagai tool model.
- Tampilkan login ChatGPT asli, status akun dan keluar akun. Pemakaian tunduk pada akses dan kuota Codex akun tersebut.
- Uji fungsi dinamis, penyimpanan sesi, keluar/masuk lagi, dan penolakan operasi di luar allowlist sebelum menyediakan APK pengguna.

## Menjalankan ulang

Workflow `.github/workflows/chatgpt-android-probe.yml` berjalan pada perubahan modul ini di branch `chatgpt-local`. Log `CodexProbe` dan stderr proses native disediakan untuk membedakan kesalahan instalasi, crash native, dan kegagalan JSON-RPC.
