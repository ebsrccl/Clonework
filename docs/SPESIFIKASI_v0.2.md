# Mikrotik Automation System — Spesifikasi Agen Android

Versi 0.2 · 6 September 2026

Status: spesifikasi dengan fondasi kode Android dan server agen terpisah. Belum ada APK terkompilasi, instalasi Mikhmon, atau pengujian pada router pengguna.

## 1. Kebutuhan yang ditetapkan pengguna

APK berfungsi sebagai agen khusus MikroTik. Pengguna memberi perintah dalam bahasa sehari-hari; agen mengakses router, membaca keadaan sebenarnya, menjalankan perubahan, dan memeriksa hasilnya. API OpenAI menjadi otak agen. Semua kemampuan eksekusi dibatasi pada pengelolaan MikroTik dan integrasi Mikhmon.

Pengaturan koneksi dilakukan sekali, lalu digunakan kembali pada pembukaan aplikasi berikutnya. Aplikasi tidak meminta ulang kredensial untuk setiap percakapan atau tindakan. Masuk ulang tetap diperlukan jika akses dicabut, kredensial diganti, atau data aplikasi dihapus.

Pengguna mengonfirmasi bahwa belum ada instalasi Mikhmon. Proyek mencakup penyiapan Mikhmon baru bersama server pendamping agen, dengan profil router berasal dari pengaturan awal APK. Pengguna tidak perlu menyediakan instalasi atau akun Mikhmon lama. Fitur voucher langsung melalui RouterOS tidak boleh dianggap sebagai bukti bahwa integrasi Mikhmon sudah selesai.

## 2. Alur penggunaan

1. Pengguna membuka halaman **Hubungkan MikroTik & Asisten**.
2. Pengguna mengisi koneksi router dan API key OpenAI dalam satu proses pengaturan.
3. Aplikasi menguji koneksi MikroTik dan akses OpenAI secara terpisah, lalu menunjukkan hasil masing-masing.
4. Layanan Mikhmon baru akan ditautkan oleh server pendamping memakai profil router yang sama. Status **Belum dipasang**, **Belum terhubung**, dan **Siap** harus dibedakan. Pengguna tidak diminta alamat Mikhmon lama.
5. Setelah berhasil, aplikasi membuka **Chat Agen**. Pembukaan berikutnya kembali ke percakapan dan router yang tersimpan.
6. Agen menampilkan progres tugas: memeriksa, menjalankan, memverifikasi, lalu selesai atau gagal dengan penjelasan yang sesuai hasil sebenarnya.

Alamat remote bukan pengganti autentikasi router. API RouterOS merupakan layanan yang perlu diaktifkan dan dijangkau. REST memakai kredensial pengguna RouterOS. [Dokumentasi API MikroTik](https://help.mikrotik.com/docs/spaces/ROS/pages/47579160/API), [dokumentasi REST MikroTik](https://help.mikrotik.com/docs/spaces/ROS/pages/47579162/REST+API).

| Isian pengaturan | Kegunaan |
| --- | --- |
| Nama router | Label agar pengguna mengenali router yang sedang dioperasikan |
| Alamat remote dan port API | Tujuan koneksi; port dapat berbeda jika penyedia remote menggunakan pemetaan port |
| Username dan password MikroTik | Autentikasi dengan hak akses yang diberikan pada akun tersebut |
| API key OpenAI | Menghubungkan kemampuan model milik pengguna |
| Model OpenAI | Dipilih dari model yang tersedia dan sesuai kebutuhan; belum ditetapkan pada tahap ini |
| Koneksi Mikhmon | Disiapkan pada server proyek; pengguna tidak harus mempunyai Mikhmon sebelumnya |

Kredensial diisi melalui formulir aplikasi atau pengaturan server yang aman. Tidak diperlukan pengiriman password atau API key melalui percakapan proyek ini.

## 3. Arsitektur yang diusulkan

**APK Android** menyediakan percakapan, status router, daftar kemampuan, dan riwayat tugas. Fondasi pertama memakai Java dan antarmuka Android native tanpa dependensi UI tambahan. Target awal Android 8 atau lebih baru; proyek memakai compile/target SDK 36. Build dan pengujian pada perangkat Android belum dilakukan.

**Server pendamping agen** mengelola sesi, mengakses OpenAI, menjalankan fungsi MikroTik, dan menyimpan status tugas. Lokasi server belum ditetapkan. Aplikasi tetap memberikan satu pengalaman masuk; keberadaan server tidak berarti pengguna harus masuk ulang pada setiap tindakan.

**OpenAI Responses API** digunakan dengan function calling. Model mengusulkan pemanggilan fungsi; server aplikasi menjalankan fungsi tersebut dan mengirim hasilnya kembali. Kredensial router tidak perlu dimasukkan ke prompt model. [OpenAI: Function calling](https://developers.openai.com/api/docs/guides/function-calling).

**Konektor RouterOS** menangani autentikasi, pembacaan data, dan perubahan konfigurasi. Target kompatibilitas adalah RouterOS 6 dan 7 melalui API RouterOS, dengan API-SSL sebagai jalur utama. REST melalui HTTPS dapat ditambahkan untuk RouterOS 7 yang mendukungnya. API-SSL menggunakan port standar 8729, tetapi port remote aktual harus dapat dikonfigurasi. Sertifikat harus diverifikasi. Dukungan tersebut masih merupakan target implementasi dan perlu diuji pada versi router yang dipakai. [API MikroTik](https://help.mikrotik.com/docs/spaces/ROS/pages/47579160/API), [REST MikroTik](https://help.mikrotik.com/docs/spaces/ROS/pages/47579162/REST+API).

**Konektor Mikhmon** ditujukan ke instalasi baru yang disiapkan dalam proyek, idealnya pada server yang sama dengan agen. Mikhmon merupakan aplikasi web untuk pengelolaan hotspot MikroTik; repositori V3 mencakup fungsi voucher, profil, dan laporan. Bentuk konektor harus mengikuti versi yang dipasang, bukan mengasumsikan ada REST API standar. Fondasi kode pertama hanya menampilkan status belum dipasang; operasi Mikhmon belum diimplementasikan. [Situs pengembang Mikhmon](https://laksa19.github.io/), [repositori Mikhmon V3](https://github.com/laksa19/mikhmonv3).

Untuk tugas yang perlu berlanjut saat APK ditutup, eksekusi dirancang berjalan pada server pendamping. APK menampilkan kembali progres saat tersambung. Ini adalah rancangan produk; belum ada layanan yang dijalankan atau dijadwalkan.

## 4. Penyimpanan akses sekali masuk

API key OpenAI tidak ditanam ke dalam APK. OpenAI menganjurkan penyimpanan key pada server, bukan pada kode aplikasi klien. Rancangan ini menggunakan penyimpanan rahasia terenkripsi pada server; APK menyimpan token perangkat yang dapat dicabut. [OpenAI: Authentication](https://developers.openai.com/api/reference/overview#authentication).

Kredensial router dan Mikhmon juga disimpan terpisah dari percakapan. Log tidak memuat password, API key, atau konfigurasi lengkap yang mengandung rahasia. Token aplikasi dilindungi menggunakan penyimpanan yang didukung Android Keystore. Detail pelaksanaan dan siklus pencabutan token harus diverifikasi pada implementasi Android.

Untuk pemasangan pribadi, rancangan memakai satu server yang menjangkau router dan menjadi tempat layanan Mikhmon baru. Lokasi server belum ditetapkan. Alamat Winbox, WebFig, atau layanan remote tertentu belum tentu menyediakan akses ke port API; pemeriksaan awal harus mengidentifikasi layanan yang tersedia tanpa mengubah konfigurasi jaringan otomatis.

## 5. Cakupan kemampuan agen

Seluruh baris berikut menjelaskan cakupan produk. Fondasi kode baru mencakup pembacaan identitas/sumber daya/interface/profil hotspot/simple queue, usulan perubahan batas simple queue, penerapan lewat tombol aplikasi, dan pemeriksaan ulang. Cakupan lain belum diimplementasikan. Seluruh integrasi masih memerlukan uji nyata.

| Kelompok | Kemampuan yang dituju | Bukti hasil yang harus ditampilkan |
| --- | --- | --- |
| Pemeriksaan router | Identitas, versi, uptime, beban CPU, memori, interface, trafik, dan log relevan | Data aktual beserta waktu pembacaan |
| Diagnosis koneksi | Menelusuri interface, alamat IP, DHCP, DNS, route, dan aturan terkait | Temuan berdasarkan keadaan router; bedakan fakta dan dugaan |
| Hotspot | Kelola pengguna, profil, sesi aktif, dan voucher | Jumlah berhasil/gagal, objek yang berubah, dan verifikasi ulang |
| PPPoE | Kelola akun, profil, status aktif, dan parameter layanan | Akun yang tepat dan keadaan setelah perubahan |
| Bandwidth | Baca dan ubah simple queue serta batas kecepatan yang didukung | Nilai sebelum dan sesudah |
| Konfigurasi jaringan | Kelola IP, DHCP, DNS, NAT, firewall, dan routing sesuai modul yang tersedia | Rincian perubahan serta pemeriksaan konektivitas |
| Pemeliharaan | Backup, ekspor terkontrol, pemulihan yang didukung, dan reboot | Artefak atau status nyata; jangan menganggap sambungan terputus sebagai bukti sukses |
| Mikhmon | Jalankan fungsi pengguna, voucher, profil, template/cetak, dan laporan sesuai instalasi | Hasil yang terkonfirmasi dari Mikhmon yang terhubung |

Kemampuan operasional dibatasi oleh daftar fungsi pada server dan hak akses akun RouterOS. Prompt membantu menjaga fokus percakapan, tetapi pembatasan eksekusi harus diterapkan dalam kode. Menambah kemampuan berarti menambah fungsi dan validasinya, bukan sekadar menyuruh model mengabaikan batasan.

## 6. Siklus tindakan agen

Setiap tugas mempunyai identitas, router tujuan, parameter, batas langkah, dan status. Agen membaca keadaan yang diperlukan sebelum memilih tindakan. Target yang ambigu diperjelas; model tidak mengarang nama interface, ID aturan, profil, atau harga voucher.

Tindakan rutin yang diminta secara jelas dapat dilaksanakan sesuai izin yang telah ditetapkan, tanpa meminta persetujuan berulang. Perubahan yang dapat memutus akses pengelolaan atau menghapus banyak konfigurasi menampilkan dampak spesifik dan meminta persetujuan untuk perubahan tersebut sebelum dijalankan.

Setelah menulis konfigurasi, server membaca ulang objek yang berubah. Pengiriman perintah saja belum berarti hasil berhasil diverifikasi. Jika koneksi terputus, status menjadi **hasil belum dapat dipastikan** sampai pemeriksaan lanjutan selesai.

Untuk operasi berulang seperti pembuatan voucher, setiap tugas memerlukan penanda agar percobaan ulang tidak membuat duplikat. Kegagalan sebagian harus ditampilkan secara rinci. Pemulihan hanya dijanjikan untuk operasi yang memang mempunyai jalur pemulihan teruji; backup tidak dianggap sebagai pembatalan instan semua perubahan.

## 7. Contoh perilaku

**“Cek kenapa internet lambat.”** Agen membaca data interface, pemakaian sumber daya, dan konfigurasi relevan, lalu melaporkan temuan. Diagnosis belum otomatis berarti mengubah jaringan.

**“Batasi pelanggan Budi jadi 5 Mbps.”** Agen mencari pelanggan yang dimaksud dan memastikan arah upload/download serta targetnya cukup jelas. Setelah menulis batas kecepatan, agen membaca ulang nilai yang tersimpan.

**“Buat 50 voucher paket 3 jam lewat Mikhmon.”** Agen mencari profil paket yang sesuai pada instalasi terhubung, menggunakan aturan paket tersebut, membuat voucher, dan menyiapkan keluaran cetak. Durasi pemakaian, masa berlaku kalender, harga, dan pencatatan penjualan tidak boleh diasumsikan sama tanpa membaca konfigurasi. Jika Mikhmon belum terhubung, agen menyatakan kebutuhan koneksi tersebut; operasi langsung RouterOS tidak dilaporkan sebagai operasi Mikhmon.

## 8. Layar awal yang akan dibangun

| Layar | Isi utama |
| --- | --- |
| Hubungkan MikroTik & Asisten | Formulir sekali pengaturan, uji router/OpenAI/Mikhmon, dan status yang terpisah |
| Chat Agen | Percakapan, router aktif, progres tugas, dan hasil tindakan |
| Router | Ringkasan status, koneksi, dan keadaan terakhir dengan waktu pengambilan |
| Kemampuan | Modul yang tersedia dan status kesiapan masing-masing konektor |
| Aktivitas | Riwayat tugas, perubahan, kegagalan sebagian, dan hasil verifikasi |
| Pengaturan | Ganti koneksi/key, kelola perangkat, dan putuskan akses |

## 9. Batas tahap ini dan informasi berikutnya

Dokumentasi OpenAI, API RouterOS, dan sumber resmi Mikhmon telah diperiksa. Fondasi source Android dan server Node.js sudah dibuat. Belum ada koneksi ke router pengguna, pengujian OpenAI menggunakan key pengguna, integrasi Mikhmon aktif, atau APK terkompilasi. Pengujian inti menggunakan router dan respons OpenAI tiruan; hasilnya tidak membuktikan kompatibilitas perangkat nyata.

Pada fondasi pertama, pengaturan juga memuat alamat server agen dan kode pemasangan yang diberikan pengelola server. Keduanya ada dalam formulir yang sama, bukan login berulang. Penyederhanaan melalui tautan aktivasi merupakan pengembangan berikutnya. Pengaturan rahasia tersimpan sekali dan dapat digunakan setelah server dimulai ulang. Pencabutan atau pemasangan ulang perangkat memerlukan pengaturan ulang.

Kebutuhan instalasi lama sudah terjawab: **Mikhmon belum ada dan akan disiapkan dalam proyek**. Pemilihan lokasi server, penyiapan akses router sebenarnya, dan pemasangan Mikhmon dilakukan pada tahap integrasi. Pengembangan antarmuka serta pengujian inti agen dengan data tiruan dapat dilanjutkan tanpa meminta kredensial pengguna.

Urutan implementasi yang diusulkan: pengaturan sekali masuk dan sesi; chat agen dengan pembacaan router; operasi hotspot/bandwidth dengan verifikasi; konektor Mikhmon sesuai instalasi; kemudian modul jaringan dan pemeliharaan tambahan.
