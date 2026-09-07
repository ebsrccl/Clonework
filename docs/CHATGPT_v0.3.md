# MikroTik Agent ChatGPT 0.3 — preview Android ARM64

Versi ini menjalankan agen di HP dan memakai login akun ChatGPT melalui runtime Codex. Tidak perlu VPS, kolom server agen, atau API key OpenAI. Ini aplikasi komunitas, bukan aplikasi resmi OpenAI atau MikroTik.

## Cara memakai

1. Pasang `Mikrotik-Agent-ChatGPT-0.3-ARM64.apk` di Android ARM64, minimal Android 8. Izinkan pemasangan dari sumber unduhan saat diminta Android.
2. Buka APK dan tekan **Login ChatGPT**. Selesaikan login dan persetujuan di halaman resmi OpenAI dalam browser.
3. Kembali ke APK dan tekan **Periksa login**. Pemakaian AI mengikuti akses dan kuota Codex akun yang dipakai; langganan ChatGPT bukan jaminan semua paket atau akun memiliki akses yang sama.
4. Isi IP/hostname MikroTik, port RouterOS API, username, dan password router. Tekan **Uji koneksi & simpan di HP**. Alamat Winbox atau URL web bukan alamat server agen.
5. Coba “Cek status router” atau “Tampilkan simple queue”. Untuk perubahan bandwidth, agen membuat usulan. Buka **Aktivitas**, periksa target dan nilai upload/download, lalu tekan **Terapkan**.

Profil dan login tersimpan di HP sehingga tidak perlu diisi setiap membuka APK. Login ulang bisa diperlukan jika sesi dicabut, kedaluwarsa tanpa bisa diperbarui, atau data aplikasi dihapus. HP harus dapat menjangkau MikroTik melalui LAN, VPN, atau alamat remote yang benar dan memiliki internet untuk ChatGPT.

API-SSL biasanya menggunakan port 8729; API biasa biasanya 8728. API biasa hanya diizinkan aplikasi ke alamat privat/LAN/VPN dan tetap mengirim kredensial tanpa TLS. Alamat publik wajib API-SSL. Untuk sertifikat sendiri, gunakan sidik jari SHA-256 sertifikat yang diperoleh secara tepercaya dari router. Port remote yang diteruskan bisa berbeda dari port standar.

## Fitur versi ini

- Membaca identitas, sumber daya, interface/counter trafik, profil hotspot, dan simple queue.
- Membuat usulan perubahan batas kecepatan pada simple queue statis yang sudah ada.
- Menerapkan perubahan melalui tombol pengguna, membaca ulang hasil, dan menampilkan status terverifikasi/tidak pasti.
- Menolak usulan kedaluwarsa, perubahan konfigurasi sejak usulan dibuat, parameter asing, dan pengulangan penerapan.
- Mengecek router langsung tanpa permintaan ChatGPT setelah profil tersimpan.

Mikhmon belum terpasang dan belum terintegrasi. Voucher, PPPoE, firewall, NAT, backup, penjadwalan, serta operasi RouterOS umum belum tersedia. Runtime AI hanya diberi fungsi MikroTik yang tercantum, tanpa lingkungan terminal atau akses berkas melalui alat model.

## Login dan data

Runtime lokal menggunakan protokol [Codex App Server](https://learn.chatgpt.com/docs/app-server) dan [autentikasi Codex](https://learn.chatgpt.com/docs/auth). APK meminta runtime memulai login ChatGPT, membuka URL resmi, dan membaca status akun. Runtime resmi mengurus callback localhost, pertukaran token, penyimpanan sesi, serta pembaruan token. APK tidak meminta password ChatGPT, cookie, atau token yang disalin manual.

Password router disimpan dengan AES-GCM dan kunci Android Keystore. Sesi ChatGPT disimpan oleh Codex dalam ruang privat aplikasi yang dikecualikan dari backup Android. Pesan dan hasil pembacaan router yang diperlukan dikirim ke layanan AI OpenAI. Password router tidak disertakan dalam parameter AI dan disamarkan jika muncul dalam data teks. Riwayat aplikasi lokal tetap disimpan sampai data dihapus. Thread AI bersifat ephemeral; ini tidak menjanjikan tidak adanya retensi data di sisi layanan.

**Keluar akun ChatGPT** melepas sesi AI. **Hapus profil lokal / ganti koneksi** menghapus profil, sesi AI, dan riwayat di HP; perubahan yang sudah diterapkan di MikroTik tetap berlaku.

## Build dan batas pengujian

Identitas aplikasi: `id.mas.agent.chatgpt`, versi `0.3.0-chatgpt-preview`, target SDK 36. Bisa dipasang berdampingan dengan versi API 0.2; data versi lama tidak dipindahkan otomatis. APK preview ditandatangani kunci debug CI, sehingga pembaruan dari build CI berikutnya mungkin memerlukan uninstall dan login ulang.

Sumber runtime: OpenAI Codex 0.153.4, commit `3d2ee51ca2d5db578f328aa75e20aa22c0197c9a`. Dibangun dengan Android NDK 27.2, Rust 1.95, OpenSSL vendored, dan dukungan `flock` Android pada pustaka standar Rust. Mekanisme autentikasi dan kebijakan Codex tidak diubah. Lisensi dan pemberitahuan Codex ikut dikemas di APK. Workflow native menyimpan Cargo.lock hasil resolusi dan checksum binari; `android/codex-runtime.json` menunjuk build runtime yang digunakan.

Uji otomatis menggunakan router/protokol AI tiruan serta emulator Android untuk startup runtime dan awal/pembatalan login browser. Penyelesaian OAuth dengan akun pengguna, percakapan AI berbayar/berkuota, dan perubahan pada MikroTik fisik memerlukan pengujian di perangkat pengguna. Lihat hasil build terkait sebelum memakai APK; keberhasilan kompilasi saja bukan bukti login berhasil.

Hasil terperinci: [CHATGPT_VALIDATION.md](CHATGPT_VALIDATION.md).
