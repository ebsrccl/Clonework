# Hasil validasi fondasi

Tanggal: 6 September 2026. Runtime pengujian: Node.js v24.19.0.

Perintah: `node --test test/core.test.mjs`, dijalankan dari direktori `server`.

Hasil: **12 pengujian lulus; 0 gagal.**

| Area | Hasil |
| --- | --- |
| Encoding panjang kata RouterOS pada batas protokol | Lulus |
| Parsing UTF-8 terfragmentasi dan beberapa balasan | Lulus |
| Penghapusan kolom yang tidak diminta, termasuk rahasia | Lulus |
| Vault terenkripsi, pembacaan ulang, dan penolakan kunci salah | Lulus |
| Penolakan tool umum dan parameter tambahan | Lulus |
| Usulan tanpa mutasi, penerapan, verifikasi, dan deduplikasi | Lulus |
| Penolakan usulan usang dan kedaluwarsa | Lulus |
| Penyimpanan hasil tidak pasti tanpa pengulangan mutasi | Lulus |
| Perintah diterima tetapi nilai baca ulang berbeda | Lulus |
| Perubahan target queue saat penulisan | Lulus |
| Siklus function calling serta penerusan item reasoning | Lulus |
| Autentikasi gateway HTTP, sesi lintas restart, dan pencabutan | Lulus |

Lima berkas XML Android berhasil diparse, dan `package.json` valid. Pemeriksaan ini bukan kompilasi atau pengujian UI Android.

Vault kriptografis dan server HTTP localhost dijalankan dalam pengujian. RouterOS dan OpenAI menggunakan objek/respons tiruan. Tidak ada kredensial pengguna yang dipakai. Parser protokol diuji pada data uji; koneksi TLS ke router nyata belum diuji.

Belum terverifikasi: kompilasi APK, rendering dan interaksi Android, kompatibilitas RouterOS 6/7 nyata, panggilan OpenAI nyata, pemasangan server HTTPS, instalasi/konektor Mikhmon, beban operasional, dan pemulihan setelah gangguan pada deployment sebenarnya.

Lingkungan pembuatan tidak menyediakan Gradle, Android SDK, adb, atau javac. Paket berisi source dan instruksi build, bukan APK. Source Mikhmon tidak disertakan atau dipasang; rencana integrasinya ada di `MIKHMON_NEXT.md`.

Tambahan jalur build: workflow GitHub Actions manual dan otomatis pada perubahan di branch `main` ditambahkan bersama `CARA_PAKAI_APK.md`. Struktur YAML, path proyek, task Gradle, dan path output diperiksa secara statis. Referensi action dipatok ke commit yang diverifikasi melalui GitHub. Workflow belum diunggah atau dijalankan pada GitHub; pemeriksaan ini bukan bukti kompilasi APK berhasil.

Percobaan unggah ke repository publik `ebsrccl/Clonework` telah diizinkan pengguna. Percobaan awal ditolak GitHub dengan HTTP 403, `Resource not accessible by integration`. Setelah pengguna menyelesaikan pengaturan GitHub App, pemeriksaan menunjukkan instalasi tersedia dan pembuatan berkas pertama berhasil pada commit `d0e7035d8dcb64dfb70c2535eb10bf3fe61f6433`. Pengunggahan source dilanjutkan melalui akses resmi yang telah dipulihkan; hasil build dicatat setelah workflow selesai.
