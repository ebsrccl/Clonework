# Validasi agen lokal Android 0.2

Tanggal: 6 September 2026. Commit kode: `f85aa6f083d9125c376e0d38eed1da0bc99dc28d`.

[Workflow 34040031683](https://github.com/ebsrccl/Clonework/actions/runs/34040031683), job `101504985879`, **berhasil**. Gradle melaporkan `BUILD SUCCESSFUL in 54s`.

## Hasil

- **15 tes JVM untuk implementasi Android lokal lulus; 0 gagal, 0 error, 0 dilewati.**
- Tes mencakup encoding panjang RouterOS, pembacaan UTF-8 terfragmentasi, login/perintah melalui socket TCP localhost sungguhan, penghapusan field yang tidak diminta, penolakan plaintext ke IP publik, pembatasan fungsi, proses Responses/reasoning, redaksi kredensial, snapshot terisolasi, dan mutasi queue.
- Skenario mutasi: usulan belum menulis, apply terverifikasi, penolakan perubahan target/keadaan usang, kedaluwarsa, gagal persist sebelum menulis, hasil tidak pasti, restart saat applying, dan deduplikasi lintas restart.
- 12 tes gateway lama juga lulus; hasil tersebut dipisahkan dari 15 tes Android dan bukan bukti bahwa APK memerlukan gateway.
- `lintDebug`: **0 error, 4 warning**. Warning yang tersisa: target SDK 36 belum versi terbaru, versi dependensi org.json untuk tes, penggunaan trust manager sertifikat pin, serta satu teks Indonesia pada checkbox.
- Pemeriksaan trust manager: mode normal menggunakan trust Android + hostname. Mode pin mengharuskan hash SHA-256 sertifikat persis cocok dan sertifikat belum kedaluwarsa; tidak ada trust-all. TLS dengan router nyata belum diuji.
- `:app:assembleDebug` berhasil. `apksigner verify --verbose` berhasil, skema v2 valid, satu signer.

## Berkas yang diserahkan

- Nama: `Mikrotik-Agent-Lokal-0.2.apk`.
- Application ID: `id.mas.agent.local`; versi `0.2.0-local`, min SDK 26, target/compile SDK 36.
- Ukuran: **42.872 byte**.
- SHA-256 APK: `6cc7d72d351e781d81b40c66ac36806b7e17ea491a17c20a785fba4e82de04c6`.
- Artefak GitHub: `9991394220`; SHA-256 ZIP `b38f172e5d38d23bfa25e83776e8156cdfe6128dbd2aecff347a7e044d303266`.

Hash ZIP cocok dengan digest GitHub; hash APK cocok dengan checksum runner. Struktur ZIP dan CRC valid. APK memuat core LocalAgent, endpoint OpenAI, serta perintah RouterOS langsung; tidak memuat endpoint setup gateway atau kolom alamat server lama. APK telah disimpan sebagai unduhan tersendiri.

Belum diuji: pemasangan/interaksi pada HP atau emulator, Android Keystore pada perangkat fisik, TLS ke router nyata, akun OpenAI pengguna, dan operasi router nyata. Pengujian inti memakai penyimpanan/responder tiruan; satu uji protokol memakai socket TCP lokal sungguhan dengan server RouterOS tiruan. Mikhmon, voucher, dan tugas latar belakang persisten belum tersedia.

---

## Arsip: validasi versi gateway 0.1

> Catatan versi lokal 0.2: arsitektur server pada dokumen ini adalah riwayat versi sebelumnya. Arsitektur aktif tanpa VPS dijelaskan di [LOKAL_v0.2.md](LOKAL_v0.2.md).

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

Belum terverifikasi: rendering dan interaksi Android, kompatibilitas RouterOS 6/7 nyata, panggilan OpenAI nyata, pemasangan server HTTPS, instalasi/konektor Mikhmon, beban operasional, dan pemulihan setelah gangguan pada deployment sebenarnya.

Lingkungan penyuntingan lokal tidak menyediakan Gradle, Android SDK, adb, atau javac. Kompilasi APK dilaksanakan pada GitHub Actions. Source Mikhmon tidak disertakan atau dipasang; rencana integrasinya ada di `MIKHMON_NEXT.md`.

Workflow GitHub Actions manual dan otomatis pada perubahan di branch `main` tersedia bersama `CARA_PAKAI_APK.md`. Referensi action dipatok ke commit yang diverifikasi melalui GitHub. Workflow pertama berhasil menjalankan pengujian inti, kompilasi, verifikasi tanda tangan, dan unggah artefak.

## Bukti build APK pertama

- Repository: `ebsrccl/Clonework`.
- Commit kode: `4bcb31a3f6522a0343a9c4a9f59e3f28420ed19f`.
- Workflow: [34038044684](https://github.com/ebsrccl/Clonework/actions/runs/34038044684), job `101499622657`, selesai sukses pada 6 September 2026.
- Perintah: `gradle --no-daemon --stacktrace :app:assembleDebug`; log menyatakan `BUILD SUCCESSFUL in 41s`.
- Node.js 24, Java 17, Gradle 8.13, Android SDK 36; pengujian inti pada runner **12 lulus, 0 gagal**.
- `apksigner verify --verbose`: `Verifies`, skema v2 valid, satu penanda tangan.
- Artefak GitHub `9990801794`: SHA-256 ZIP `aa1f2ed502588edeb67431d7df6b7d54ab03eadd77e7aae7539c5f03ed0a6cf7`.
- APK `Mikrotik-Agent-debug.apk`: 26.773 byte, SHA-256 `0df178f8e3a26fcaa84db58659ccf50cafef39545e39f2ed7896965ebd39c9e5`.

ZIP yang diunduh cocok dengan digest artefak GitHub. APK cocok dengan `SHA256SUMS.txt` dari runner, struktur ZIP/CRC valid, dan memuat manifest Android, `classes.dex`, serta resource terkompilasi. APK telah disimpan sebagai berkas unduhan terpisah. Pemeriksaan ini tidak mencakup pemasangan atau pengujian interaksi di emulator/HP.
