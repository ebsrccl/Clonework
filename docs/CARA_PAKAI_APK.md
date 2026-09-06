# Cara mendapatkan dan memakai APK

Status 6 September 2026: APK debug `0.1.0-foundation` berhasil dibangun. Dua belas pengujian inti dan pemeriksaan tanda tangan lulus. Server agen HTTPS dan Mikhmon belum dipasang; koneksi router/OpenAI nyata serta interaksi pada perangkat Android belum diuji.

## Pasang hasil build yang sudah tersedia

1. Buka [build berhasil nomor 34038044684](https://github.com/ebsrccl/Clonework/actions/runs/34038044684) dari akun GitHub, lalu unduh artefak **Mikrotik-Agent-APK**. Artefak ini dijadwalkan kedaluwarsa pada 20 September 2026; sesudah itu workflow dapat dibangun ulang.
2. Ekstrak ZIP dan buka **Mikrotik-Agent-debug.apk** pada Android 8 atau lebih baru. Jika menerima APK langsung dari percakapan, cukup buka berkas APK tersebut.
3. Jika Android meminta izin pemasangan, izinkan pengelola berkas/browser yang dipakai, lalu lanjutkan pemasangan.
4. Buka **MikroTik Agent → Lihat demo tanpa koneksi**. Demo tidak memerlukan API key atau router dan memakai data contoh.

Ukuran APK pertama: **26.773 byte**. SHA-256:

```text
0df178f8e3a26fcaa84db58659ccf50cafef39545e39f2ed7896965ebd39c9e5
```

## Repository Clonework

Source berada di repository publik [ebsrccl/Clonework](https://github.com/ebsrccl/Clonework). Build APK memakai workflow **Build APK MikroTik** di [halaman Actions](https://github.com/ebsrccl/Clonework/actions). Commit kode untuk APK pertama adalah `4bcb31a3f6522a0343a9c4a9f59e3f28420ed19f`.

## Jalur browser HP: build melalui GitHub Actions

GitHub menjalankan kompilasi pada mesinnya. Setelah pekerjaan sukses, pemilik akun dapat mengunduh hasil dari halaman Actions. Akun harus mempunyai akses tulis untuk menjalankan workflow dan akses baca untuk mengunduh artefak. [Menjalankan workflow](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow), [mengunduh hasil](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts).

1. Siapkan akun GitHub dan repository milik sendiri. Untuk bantuan langsung dari percakapan, pasang plugin GitHub dan hubungkan akun melalui alur resminya. Ketersediaan akses tulis serta workflow perlu diperiksa setelah koneksi selesai.
2. Ekstrak ZIP source. Isi folder `mikrotik-agent` harus menjadi akar repository, sehingga `android`, `server`, `docs`, dan direktori konfigurasi workflow berada langsung di akar. Mengunggah ZIP saja ke repository belum membuatnya menjadi proyek yang bisa dibangun.
3. Pastikan berkas `.github/workflows/build-apk.yml` ada pada branch default repository. Jika pengunggah dari HP melewatkan folder tersebut, buat berkas dengan path lengkap itu menggunakan editor GitHub dan salin isi persis dari paket.
4. Buka repository melalui browser, lalu pilih **Actions → Build APK MikroTik → Run workflow**. Tampilan situs desktop dapat membantu jika navigasinya sulit terlihat di HP.
5. Tunggu hasil pekerjaan. Lanjutkan hanya jika pekerjaan sukses. Jika gagal, buka langkah yang merah; APK tidak dianggap tersedia sebelum build dan pemeriksaan tanda tangan berhasil.
6. Pada bagian **Artifacts**, unduh **Mikrotik-Agent-APK**. Hasil unduhan berupa ZIP; ekstrak untuk memperoleh `Mikrotik-Agent-debug.apk` dan `SHA256SUMS.txt`.
7. Buka berkas `.apk` melalui pengelola berkas Android. Jika Android meminta izin pemasangan aplikasi dari sumber tersebut, berikan izin untuk pengelola berkas/browser yang digunakan, kemudian lanjutkan pemasangan.
8. Buka **MikroTik Agent** dan pilih **Lihat demo tanpa koneksi** untuk mencoba tampilannya terlebih dahulu.

Workflow menggunakan Node.js 24, Java 17, Gradle 8.13, dan Android SDK 36. Pengujian inti dijalankan sebelum build. Berkas OpenAI API key, password router, atau konfigurasi server tidak diperlukan untuk membangun APK dan tidak boleh dimasukkan ke repository. Workflow dapat dijalankan manual dan otomatis berjalan ketika kode Android, server, atau workflow berubah di branch `main`; pemasangan di HP dilakukan pengguna.

Hasilnya APK debug untuk uji coba. Build terpisah dapat memakai kunci debug berbeda; pembaruan di atas instalasi lama mungkin ditolak jika tanda tangan berubah. Penyimpanan kunci penandatanganan rilis yang tetap perlu disiapkan sebelum penggunaan berkelanjutan. Jangan menghapus instalasi aktif tanpa merencanakan pemulihan sesi perangkat.

## Jalur komputer: Android Studio

Buka folder `android` di Android Studio, siapkan SDK 36 dan Gradle 8.13, pilih varian **debug**, lalu gunakan menu **Build → Generate Bundle(s) / APK(s) → Generate APK(s)**. Nama menu dapat berbeda pada versi lebih lama. APK debug hasil menu build ditandatangani untuk pemasangan uji coba. [Panduan Android](https://developer.android.com/build/build-for-release).

Alternatif dengan Gradle terpasang:

```bash
cd android
gradle :app:assembleDebug
```

Hasil setelah berhasil: `android/app/build/outputs/apk/debug/app-debug.apk`. Pindahkan berkas ini ke HP, lalu pasang dan buka mode demo.

## Agar agen benar-benar mengoperasikan MikroTik

Mode demo memakai data contoh. Untuk koneksi nyata, server dari folder `server` harus dijalankan pada mesin yang dapat menjangkau router, dengan alamat HTTPS yang dapat diakses HP. Cara menyiapkan server dan sesi ada di README utama.

Setelah server siap, isi sekali dari APK: alamat server dan kode pemasangan, alamat/port API-SSL router beserta akun router, serta API key dan model OpenAI. Membuat APK melalui GitHub Actions tidak menyediakan server agen yang terus menyala.

Kemampuan fondasi yang dapat diuji setelah integrasi: membaca status router/interface/profil hotspot/simple queue dan menerapkan usulan perubahan bandwidth. Integrasi nyata belum diuji dalam tahap pembuatan source ini.

Mikhmon belum dipasang dan konektor operasinya belum dibuat. Pemasangan Mikhmon baru serta fungsi voucher/cetak/laporan masih menjadi pekerjaan pengembangan. Membangun APK tidak otomatis menyelesaikan bagian tersebut.
