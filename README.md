# PlayLish Lite 🎵⚡

**PlayLish Lite** adalah pemutar musik minimalis, sporty, dan bertenaga terinspirasi sistem AIMP dengan dukungan audio resolusi tinggi (Hi-Res), sistem acak berurutan inovatif, pengatur waktu tidur cerdas dengan peredam desibel bertahap, dan deteksi lirik otomatis.

---

## ✨ Fitur Unggulan

- 🔀 **Sistem Acak Berurutan (Ordered Shuffle Queue)**: Setiap lagu yang diacak memiliki nomor urutan tetap (`#1`, `#2`, `#3`...) sehingga Anda tahu persis lagu apa yang akan diputar berikutnya. Tombol *Acak Ulang* memperbarui indeks urutan secara dinamis.
- 🌙 **Pengatur Waktu Tidur & Ear-Safe Limiter**:
  - Timer tidur fleksibel (slider 5-120 menit & tombol cepat).
  - Opsi *Tunggu Lagu Selesai Dahulu* vs *Langsung Berhenti*.
  - Penurunan suara bertahap (*Fade-Out*) di menit-menit akhir.
  - Batas aman desibel pendengaran (*Hearing Protection*) mencegah lonjakan suara mendadak saat Anda tertidur.
- 🎼 **Dukungan Audio Resolusi Tinggi (Hi-Res)**:
  - FLAC, ALAC, WAV, DSD, OPUS, MP3 hingga 24-bit/96kHz & 32-bit/192kHz.
  - Pemrosesan internal 32-bit Float Audio Engine.
  - Equalizer grafis 5-band + Bass Boost subwoofer dengan preset.
- 🎤 **Deteksi Lirik Otomatis (Synced LRC Karaoke)**:
  - Mendeteksi lirik `.lrc` lokal secara otomatis.
  - Penyorotan baris aktif dinamis dengan *auto-scroll* dan *tap-to-seek*.
- 💾 **Pengaturan Tersimpan Permanen**:
  - Seluruh konfigurasi tersimpan aman di Jetpack DataStore dan tidak mudah hilang.

---

## 🚀 Cara Build di GitHub

Proyek ini telah dikonfigurasi lengkap dengan **Gradle Wrapper (`./gradlew`)** dan **GitHub Actions CI (`.github/workflows/android.yml`)**.

### Opsi 1: Otomatis via GitHub Actions
1. Push repository ini ke GitHub.
2. Buka tab **Actions** di repository GitHub Anda.
3. Workflow **Android CI / Build APK** akan otomatis berjalan, menguji (*unit tests*), dan menghasilkan file APK.
4. Anda dapat mengunduh APK langsung dari bagian **Artifacts** (*PlayLish-Lite-APK*).
5. Anda juga dapat memicu build kapan saja secara manual melalui tombol **Run workflow** di tab Actions.

### Opsi 2: Build Manual di Terminal / Mesin Lokal
Pastikan Anda telah menginstal JDK 17 atau 21:

```bash
# Beri izin eksekusi pada gradlew jika di Linux/macOS
chmod +x gradlew

# Jalankan unit tests
./gradlew testDebugUnitTest

# Build APK Debug (dihasilkan di app/build/outputs/apk/debug/app-debug.apk)
./gradlew assembleDebug
```

---

## 🛠️ Spesifikasi Teknis
- **Kotlin**: 2.2.10
- **Android Gradle Plugin (AGP)**: 9.1.1
- **Jetpack Compose**: Compose BOM 2024.09.00 (Material 3)
- **Min SDK**: 24 (Android 7.0+)
- **Target SDK**: 36
