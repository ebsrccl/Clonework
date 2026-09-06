# Arsitektur aktif: agen lokal Android 0.2

Permintaan pengguna: tanpa server/VPS; eksekusi lokal dan hanya layanan model menggunakan OpenAI. Dokumen ini menggantikan keputusan arsitektur server dalam spesifikasi lama.

Aplikasi Android menyimpan konfigurasi dan riwayat pada SharedPreferences terenkripsi AES-256-GCM. Kunci enkripsi berada di Android Keystore, backup aplikasi dimatikan. `LocalAgent` mengelola daftar fungsi, riwayat, usulan, dan penerapan lokal. `RouterApi` membuka socket langsung ke host yang dimasukkan pengguna, menggunakan login RouterOS modern (6.43+). Tidak ada endpoint HTTP lokal maupun port listener di HP.

Satu executor aplikasi menserialkan tugas. Operasi jaringan dan pembacaan state yang mungkin menunggu tugas lain dilakukan di worker, bukan thread UI. Proses Android dapat dihentikan OS; status mutasi dipersistenkan sebelum menulis sehingga restart tidak mengulang hasil yang tidak pasti. Belum ada scheduler/foreground service.

OpenAI dipanggil langsung melalui HTTPS Responses API, dengan `store:false`, fungsi strict, kelanjutan reasoning terenkripsi, dan batas putaran/payload. APK memakai key milik pengguna sendiri, tanpa key bawaan. Payload tidak berisi profil kredensial router. Data hasil fungsi yang diperlukan dikirim ke model; respons router dianggap data tidak tepercaya. Model tidak memperoleh fungsi shell atau fungsi penerapan perubahan.

API biasa hanya diizinkan jika hasil DNS berupa alamat lokal/private/link-local/ULA atau rentang CGNAT yang dapat dipakai VPN. Socket terhubung ke alamat yang sudah divalidasi sehingga tidak melakukan lookup DNS kedua. TLS memverifikasi trust Android dan hostname, atau pin SHA-256 sertifikat yang secara eksplisit dimasukkan pengguna, termasuk masa berlaku sertifikat. Tidak ada trust-all atau penurunan otomatis ke plaintext.

Batas kemampuan versi ini: pembacaan status/interface/profil hotspot/queue dan perubahan max-limit simple queue statis. Mikhmon belum dipasang atau dioperasikan. Source server lama tetap ada sebagai arsip, tanpa dependensi dari APK.
