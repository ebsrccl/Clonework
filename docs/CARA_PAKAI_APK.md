# Cara mendapatkan dan memakai APK

Status 6 September 2026: paket ini masih kode sumber. Konfigurasi build GitHub Actions telah ditambahkan dan diperiksa secara statis, tetapi belum dijalankan. Tidak ada APK hasil build atau deployment server saat dokumen ini dibuat.

## Repository Clonework

Target yang telah disetujui adalah repository publik [ebsrccl/Clonework](https://github.com/ebsrccl/Clonework). Akses tulis telah dipulihkan dan unggahan berkas pertama berhasil. Build APK memakai workflow **Build APK MikroTik** di [halaman Actions](https://github.com/ebsrccl/Clonework/actions). Hasil APK hanya tersedia setelah pekerjaan build dan pemeriksaan tanda tangan berhasil.

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
