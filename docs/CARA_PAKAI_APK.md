# Memakai MikroTik Agent Lokal 0.2

Versi ini menggantikan arsitektur APK gateway 0.1 atas permintaan pengguna. Seluruh fungsi agen berjalan di HP; tidak perlu VPS, server agen, atau kode pemasangan. OpenAI tetap memerlukan internet.

## Pasang APK

[Build lokal yang berhasil](https://github.com/ebsrccl/Clonework/actions/runs/34040031683): unduh artefak **Mikrotik-Agent-Lokal-APK** dan ekstrak APK jika mengambil dari GitHub. Artefak GitHub dijadwalkan kedaluwarsa 20 September 2026; salinan APK juga disediakan langsung dalam percakapan.

APK pertama versi lokal berukuran **42.872 byte**, SHA-256 `6cc7d72d351e781d81b40c66ac36806b7e17ea491a17c20a785fba4e82de04c6`.

Pasang `Mikrotik-Agent-Lokal-0.2.apk`, lalu buka **MikroTik Agent Lokal**. Android minimal versi 8. Aplikasi lokal memakai ID berbeda dari versi 0.1, sehingga kedua aplikasi dapat terpasang bersamaan. Pastikan membuka yang bernama **Lokal**. Tidak perlu menghapus aplikasi lama untuk memasang versi ini.

Jika Android meminta izin pemasangan, izinkan pengelola berkas/browser yang digunakan untuk membuka berkas APK. **Lihat demo tanpa koneksi** tersedia sebelum pengaturan akun dan memakai data contoh.

## Isi sekali

| Isian | Isi |
| --- | --- |
| Nama router | Nama bebas untuk mengenali koneksi |
| IP / hostname MikroTik | Alamat MikroTik yang dapat dijangkau HP; tanpa `http://`, path, atau port |
| API-SSL | Aktif untuk TLS; matikan hanya jika memakai API biasa melalui LAN/VPN |
| Port API | Port layanan router; standar SSL 8729 atau API biasa 8728; port remote bisa berbeda |
| Username dan password MikroTik | Akun router dengan izin API serta izin operasi yang digunakan |
| SHA-256 sertifikat | Opsional; untuk mempercayai tepat satu sertifikat router sendiri. Ambil fingerprint SHA-256 dari sumber router yang tepercaya. Kosongkan untuk sertifikat yang sudah dipercaya Android dan sesuai hostname |
| API key OpenAI | Key pribadi dari akun OpenAI API |
| Model | ID model yang tersedia pada akun API; isian awal `gpt-5-mini` |

Tekan **Uji koneksi & simpan di HP**. Aplikasi menguji login/baca identitas MikroTik dan akses model OpenAI, kemudian menyimpan profil terenkripsi. Jika salah satu uji gagal, profil baru tidak disimpan. Kata sandi tetap di HP dan dikirim hanya ke router; key dikirim ke OpenAI untuk autentikasi. Pesan dan hasil router yang diperlukan diproses OpenAI.

## Jika berada dekat router

Hubungkan HP ke Wi-Fi jaringan MikroTik. Gunakan IP lokal router yang sebenarnya (misalnya `192.168.88.1` jika memang itu IP router). Jika layanan `api` port 8728 sudah aktif, nonaktifkan checkbox API-SSL dan isi 8728. Untuk `api-ssl`, sertifikat router harus tersedia dan bisa diverifikasi. APK tidak mengaktifkan layanan router atau mengubah firewall secara otomatis.

Untuk akses dari luar jaringan, gunakan alamat remote API-SSL yang sudah dapat dijangkau, atau VPN yang sudah tersedia ke jaringan router. Membuat agen lokal tidak otomatis membuka akses remote ke router.

## Memakai agen

- **Chat:** "cek status router", "daftar interface", "tampilkan profil hotspot", "lihat simple queue".
- **Bandwidth:** sebutkan queue yang tepat dan arah upload/download. Agen membuat usulan; buka **Aktivitas**, tinjau, lalu tekan **Terapkan batas kecepatan ini**.
- **Skill → Cek router langsung:** membaca router tanpa permintaan OpenAI, setelah profil tersimpan.
- **Skill → Hapus profil lokal / ganti koneksi:** menghapus kredensial serta riwayat HP setelah konfirmasi. Perubahan yang sudah ditulis ke MikroTik tetap berlaku.

Jika koneksi putus saat menulis, aplikasi menandai hasil belum pasti dan tidak mengulang perintah otomatis. Periksa router sebelum membuat usulan baru. Jaga aplikasi terbuka selama tugas; belum ada worker persisten untuk tugas saat proses Android dihentikan.

Mikhmon dan fungsi voucher belum terintegrasi. Berkas ini tidak menyatakan router/OpenAI nyata sudah diuji menggunakan akun pengguna.

## Membuat ulang dari GitHub

Buka [Actions di Clonework](https://github.com/ebsrccl/Clonework/actions), jalankan **Build APK MikroTik**, lalu unduh artefak **Mikrotik-Agent-Lokal-APK** setelah sukses. Ekstrak ZIP untuk APK dan checksum. Workflow juga berjalan saat source Android berubah pada branch `main`.
