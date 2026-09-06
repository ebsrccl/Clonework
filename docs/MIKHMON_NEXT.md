# Penyiapan Mikhmon baru

Pengguna mengonfirmasi pada 6 September 2026 bahwa belum mempunyai Mikhmon. Pembangunan tidak menunggu kredensial atau alamat instalasi lama.

Rancangan yang dipilih adalah satu server pendamping dengan dua layanan: agen dan Mikhmon. Keduanya mengakses router yang sama. Server menjadi pihak yang menghubungkan pengaturan awal APK ke profil router di Mikhmon. Kredensial admin Mikhmon dibuat khusus untuk instalasi tersebut; tidak diasumsikan sama dengan akun router.

Mikhmon akan tetap merupakan instalasi perangkat lunak Mikhmon yang sebenarnya. Kemampuan cetak dan laporan tidak diganti diam-diam dengan pembuatan akun hotspot biasa melalui RouterOS. Belum ada paket Mikhmon yang dipasang, disalin, dipublikasikan, atau diberi akses router dalam tahap ini.

Pekerjaan integrasi berikutnya:

1. Pilih mesin server, jalur jaringan ke router, versi Mikhmon, serta runtime PHP yang sudah diuji bersama.
2. Pasang sumber resmi yang dipilih dengan versi/commit tercatat. Uji login, penyimpanan profil, dan koneksi router.
3. Buat konektor terbatas untuk operasi yang terverifikasi pada versi itu. Jangan mengasumsikan endpoint REST standar.
4. Hubungkan proses pembuatan profil dari data pengaturan awal. Gunakan otorisasi layanan tersendiri dan jangan mengekspos berkas konfigurasi atau password kepada model.
5. Uji pembuatan satu voucher pada router uji, profil paket, aturan masa berlaku, pencatatan laporan, dan hasil cetak. Lanjutkan uji batch termasuk kegagalan sebagian serta penanganan duplikat.
6. Ubah status aplikasi menjadi siap hanya setelah layanan dan fungsi yang ditampilkan benar-benar lolos pemeriksaan.

Sumber resmi: [Mikhmon](https://laksa19.github.io/) dan [repositori V3](https://github.com/laksa19/mikhmonv3). Isi paket saat ini hanya menyediakan status `not_installed` dan rancangan integrasi; belum menyediakan adapter Mikhmon atau installer otomatis.
