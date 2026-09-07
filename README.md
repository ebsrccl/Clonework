# MikroTik Agent Lokal — 0.2

Agen Android yang berjalan di HP, tanpa VPS, server pendamping, localhost gateway, atau kode pemasangan. APK mengakses MikroTik secara langsung melalui RouterOS API. Pesan, hasil pembacaan router yang diperlukan, dan instruksi fungsi dikirim ke OpenAI melalui HTTPS; fungsi dan perubahan router dieksekusi di HP.

**Build lokal berhasil:** [GitHub Actions 34040031683](https://github.com/ebsrccl/Clonework/actions/runs/34040031683), 15 tes Android lulus, lint tanpa error, tanda tangan APK terverifikasi. Pengujian pada HP dan akun router/OpenAI pengguna belum dilakukan.

## Mulai

1. Pasang **Mikrotik-Agent-Lokal-0.2.apk**, lalu buka **MikroTik Agent Lokal**.
2. Isi nama router, IP/hostname, port API, jenis koneksi, username, dan password MikroTik.
3. Isi API key OpenAI milik sendiri serta model yang tersedia pada akun API, lalu tekan **Uji koneksi & simpan di HP**.
4. Minta "cek status router" atau "tampilkan simple queue". Perubahan bandwidth disiapkan sebagai usulan di **Aktivitas** dan diterapkan melalui tombol pengguna.

Tidak ada kolom server agen. Profil dan riwayat disimpan terenkripsi dengan AES-GCM dan Android Keystore, digunakan kembali saat aplikasi dibuka. APK tidak memuat key/password bawaan. Mode demo memakai data contoh dan tidak memanggil jaringan.

[Petunjuk pemasangan dan koneksi](docs/CARA_PAKAI_APK.md) · [Spesifikasi lokal](docs/LOKAL_v0.2.md) · [Status validasi](docs/VALIDATION.md)

## Koneksi langsung

HP harus dapat menjangkau alamat/port API MikroTik lewat Wi-Fi lokal, VPN yang sudah tersedia, atau alamat remote API-SSL. Alamat Winbox/web saja belum tentu menyediakan API pada port yang sama.

- **API-SSL:** port standar 8729, TLS 1.2/1.3 dengan sertifikat yang dipercaya Android dan cocok dengan hostname. Untuk sertifikat sendiri, pin SHA-256 sertifikat dapat dimasukkan dari sumber router yang tepercaya. APK tidak mematikan validasi sertifikat dan tidak mendukung TLS anonim tanpa sertifikat.
- **API biasa:** port standar 8728, dipilih dengan menonaktifkan checkbox API-SSL. Password lewat koneksi ini tidak terenkripsi; pilihan ini dibatasi oleh aplikasi ke alamat LAN/VPN. Alamat publik harus menggunakan API-SSL. Tidak ada fallback otomatis dari TLS ke API biasa.
- **OpenAI:** hanya `https://api.openai.com/v1/responses`. API key dikirim sebagai header autentikasi OpenAI. Profil/router password tidak dimasukkan dalam payload model. Field router yang dikembalikan dibatasi. Uji koneksi OpenAI memakai kuota API.

## Kemampuan versi lokal

- Membaca identitas/sumber daya, interface, profil hotspot, dan simple queue.
- Membaca ringkasan langsung tanpa OpenAI melalui tab Skill setelah profil tersimpan.
- Menyiapkan dan menerapkan perubahan `max-limit` pada satu simple queue statis, memeriksa keadaan sebelum menulis, dan memverifikasi baca ulang.
- Menyimpan status sebelum mutasi; perubahan berstatus `applying`/`unknown` tidak dikirim ulang otomatis setelah gangguan atau restart.
- Chat dan eksekusi fungsi lokal; maksimum enam putaran model, 16 fungsi, lima percakapan model, 30 pesan tampilan, dan 100 usulan tersimpan.

Mikhmon tidak dipasang dan konektor operasinya belum tersedia. Voucher, PPPoE, firewall/NAT, backup, dan tugas terjadwal belum diimplementasikan. Tugas berjalan di proses aplikasi; belum ada layanan latar belakang yang menjamin tugas terus berjalan ketika Android menghentikan aplikasi.

## Build dan pengujian

```bash
cd android
gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

JDK 17, Gradle 8.13, AGP 8.13.2, Android SDK 36; minimum Android 8/API 26. Source aplikasi memakai Java dan komponen Android native tanpa dependensi runtime eksternal. JUnit dan org.json hanya dependensi pengujian JVM.

Workflow GitHub Actions membangun **Mikrotik-Agent-Lokal-APK**, memeriksa tanda tangan, dan menyertakan `SHA256SUMS.txt`. APK debug ini memakai ID `id.mas.agent.local`, sehingga dapat dipasang berdampingan dengan APK gateway 0.1 tanpa konflik tanda tangan. Data versi lama tidak dimigrasikan otomatis. Penandatanganan rilis tetap belum disiapkan; build debug berikutnya dapat memerlukan pemasangan ulang jika kunci debug berubah.

Folder `server/` adalah implementasi gateway lama yang dipertahankan sebagai arsip kode. APK lokal 0.2 tidak menjalankan atau menghubunginya. `docs/SPESIFIKASI_v0.2.md` mencatat rancangan server sebelumnya; arsitektur aktif dijelaskan pada `docs/LOKAL_v0.2.md`.

## Referensi

- [OpenAI function calling](https://developers.openai.com/api/docs/guides/function-calling)
- [Reasoning dan kelanjutan respons](https://developers.openai.com/api/docs/guides/reasoning)
- [RouterOS API dan port layanan](https://help.mikrotik.com/docs/spaces/ROS/pages/47579160/API)
