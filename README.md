# Mikrotik Automation System — Fondasi 0.1

Fondasi source untuk aplikasi Android agen MikroTik. Pengguna belum mempunyai Mikhmon; pemasangan Mikhmon baru menjadi bagian tahap integrasi proyek.

**Status:** source Android dan server agen tersedia. Belum ada APK terkompilasi, server yang dipublikasikan, Mikhmon terpasang, atau pengujian router/OpenAI nyata. Antarmuka Android belum dijalankan atau diperiksa pada emulator/perangkat. Jangan menganggap paket ini sebagai aplikasi produksi yang sudah siap dipasang.

**Mulai memakai APK:** ikuti [cara build dan pemasangan](docs/CARA_PAKAI_APK.md). Paket kini menyertakan workflow **Build APK MikroTik** yang dapat dijalankan manual melalui GitHub Actions setelah source berada di repository. Konfigurasi workflow sudah diperiksa secara statis; build jarak jauh belum dijalankan.

## Isi paket

- `android/`: aplikasi native Java, Android 8+, compile/target SDK 36. Layar pengaturan sekali masuk, chat, skill, aktivitas perubahan, dan demo lokal dengan label data contoh.
- `server/`: Node.js 22+ tanpa dependensi npm eksternal. HTTP gateway pribadi, vault AES-256-GCM, sesi perangkat, Responses API, serta protokol RouterOS API-SSL.
- `docs/`: spesifikasi produk, status validasi, dan pekerjaan integrasi Mikhmon berikutnya.

## Kemampuan yang sudah ditulis

1. Menguji koneksi router dan akses model OpenAI, lalu menyimpan profil terenkripsi pada server.
2. Menyimpan token perangkat menggunakan enkripsi dengan Android Keystore. API key dan password router tidak ditanam ke source/APK.
3. Menggunakan OpenAI function calling dengan daftar fungsi khusus MikroTik. Tidak ada fungsi shell atau eksekusi perintah umum.
4. Membaca identitas router, sumber daya, interface, profil hotspot, dan simple queue.
5. Membuat usulan perubahan bandwidth satu simple queue. Pengguna menekan **Terapkan** pada objek yang ditinjau; server membaca keadaan terbaru, menolak usulan usang, menulis, kemudian memverifikasi target dan nilai.
6. Menyimpan hasil tidak pasti dan mencegah pengulangan buta setelah perubahan atau sambungan terputus.

Mikhmon, pembuatan voucher, PPPoE, firewall/NAT, backup/restore, penjadwalan, notifikasi, operasi multi-router, dan pemulihan otomatis belum diimplementasikan. Status ini juga tampil dalam aplikasi. Demo merupakan respons tetap dengan data contoh, bukan model OpenAI lokal.

## Menjalankan pengujian inti

Prasyarat: Node.js 22 atau lebih baru. Tidak memerlukan API key, router, SDK Android, atau unduhan dependensi.

```bash
cd server
node --test test/core.test.mjs
```

Pengujian mencakup protokol data, vault, sesi HTTP, pembatasan fungsi, verifikasi perubahan, dan penanganan kondisi tidak pasti. Router dan OpenAI ditirukan, sementara vault dan server HTTP diuji secara nyata di localhost.

## Menyiapkan server pengembangan

```bash
cd server
node scripts/init.mjs
node --env-file=.env src/server.mjs
```

`init.mjs` membuat `.env` berisi kunci enkripsi baru dan `SETUP_TOKEN` acak dengan izin berkas terbatas. Tidak menimpa konfigurasi yang sudah ada. Pertahankan kunci enkripsi agar vault lama tetap dapat dibaca. Berkas ini dan direktori `data/` tidak boleh dimasukkan ke source ZIP/git.

Server mendengarkan di `127.0.0.1:8787`. Untuk HP jarak jauh, operator perlu menyiapkan HTTPS reverse proxy pada server yang dikelola sendiri. Server harus dapat menjangkau alamat remote dan port API-SSL router. Jangan meneruskan password/API key melalui HTTP publik. Belum ada reverse proxy, VPS, domain, atau sertifikat yang disiapkan oleh paket ini.

API-SSL router harus sudah aktif dengan sertifikat yang dapat diverifikasi. Default port 8729; port remote dapat berbeda. Untuk CA pribadi, atur `ROUTER_CA_FILE` ke berkas CA PEM yang dipercaya pada server. Tidak ada opsi mematikan verifikasi sertifikat.

### Pengaturan sekali pada aplikasi

1. Alamat HTTPS server agen dan kode pemasangan `SETUP_TOKEN` dari operator server.
2. Nama router, host remote, port API-SSL, username, dan password router.
3. API key OpenAI serta ID model yang dapat dipakai akun. Isian awal model `gpt-5-mini` dapat diubah; ketersediaannya harus diuji dengan akun pengguna.

Seluruhnya berada pada satu formulir. Kode pemasangan mengikat satu perangkat aktif dan tidak dapat dipakai menimpa sesi yang masih aktif. Koneksi yang tersimpan dipakai pada pembukaan berikutnya. Tidak ada formulir kredensial Mikhmon lama karena Mikhmon belum ada.

Uji pengaturan membuat satu permintaan OpenAI nyata ketika dijalankan dengan key sendiri, sehingga termasuk penggunaan API. Tidak ada permintaan tersebut yang dijalankan dalam pengembangan paket ini.

Android debug hanya mengizinkan HTTP untuk `10.0.2.2`, `127.0.0.1`, dan `localhost`. Emulator Android biasa dapat memakai `http://10.0.2.2:8787` untuk mengakses server pada host yang sama. Release membutuhkan HTTPS.

Jika perangkat hilang atau aplikasi dipasang ulang, hentikan server, lalu jalankan dari direktori `server`:

```bash
node --env-file=.env scripts/reset-device.mjs
```

Mulai ulang server setelah itu. Token lama dicabut. Pengaturan baru dapat dilakukan dengan kode pemasangan. Perintah ini mempertahankan berkas vault; pengaturan baru yang berhasil akan mengganti profil pemilik tunggal sebelumnya. Jangan jalankan pemulihan ini bersama server yang masih aktif.

## Membangun APK debug

Memerlukan JDK 17, Gradle 8.13, Android SDK Platform 36, dan Android SDK Build Tools yang sesuai. Plugin Android yang dipakai adalah 8.13.2. Build belum dilakukan karena SDK Android/Gradle/JDK compiler tidak tersedia pada lingkungan pembuatan paket ini.

1. Buka direktori `android` melalui Android Studio dan sediakan dependensi SDK/Gradle.
2. Tetapkan SDK lokal melalui Android Studio atau `local.properties` milik mesin sendiri.
3. Dengan Gradle 8.13 terpasang, jalankan:

```bash
cd android
gradle :app:assembleDebug
```

Lokasi hasil setelah build berhasil: `android/app/build/outputs/apk/debug/app-debug.apk`.

Paket tidak memuat Gradle wrapper binary. Jika diperlukan, buat wrapper menggunakan Gradle terpasang (`gradle wrapper --gradle-version 8.13`), lalu gunakan `./gradlew :app:assembleDebug`. APK release bertanda tangan dan distribusi belum disiapkan.

## Batas operasional fondasi

- Satu server pribadi, satu router, dan satu perangkat aktif. Tidak dirancang untuk layanan publik multi-pengguna.
- Tugas chat berjalan interaktif; belum ada worker pekerjaan persisten atau scheduler yang melanjutkan proses setelah server mati.
- Riwayat model dibatasi lima percakapan lengkap; tampilan menyimpan 30 pesan terakhir. Maksimum 100 usulan perubahan; pengarsipan belum dibuat.
- Perubahan queue hanya mendukung `max-limit` pada simple queue statis. Usulan berlaku sepuluh menit. `applying`/`unknown` perlu pemeriksaan ulang oleh operator; tidak ada auto-retry.
- Usulan perubahan memerlukan tombol penerapan di versi fondasi. Kebijakan tindakan rutin otomatis sesuai izin tetap menjadi pengembangan produk berikutnya.
- Error HTTP kepada aplikasi dibuat umum agar kredensial tidak bocor; diagnosis koneksi lebih rinci masih perlu ditambahkan.
- TLS RouterOS, perilaku model sebenarnya, UI Android, dan seluruh siklus integrasi harus diuji pada lingkungan milik pengguna sebelum penggunaan operasional.

## Rujukan implementasi

- [OpenAI Function calling](https://developers.openai.com/api/docs/guides/function-calling)
- [OpenAI Authentication](https://developers.openai.com/api/reference/overview#authentication)
- [GPT-5 mini](https://developers.openai.com/api/docs/models/gpt-5-mini)
- [RouterOS API](https://help.mikrotik.com/docs/spaces/ROS/pages/47579160/API)
- [RouterOS Queues](https://help.mikrotik.com/docs/spaces/ROS/pages/328088/Queues)
- [Android Gradle Plugin 8.13](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
- [Mikhmon resmi](https://github.com/laksa19/mikhmonv3)
