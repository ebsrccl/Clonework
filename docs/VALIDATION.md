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
