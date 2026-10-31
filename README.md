# 📱 GenieACS Mobile Mod

Aplikasi **100% Native Android** (Kotlin + Material Design 3) untuk memonitor, mengonfigurasi, dan mengontrol perangkat ONT/ONU/Modem di server GenieACS TR-069, dilengkapi dengan Asisten Suara AI berbahasa Indonesia.

![Logo GenieACS MOD](logo_mod_master.png)

---

## ✨ Fitur Utama

1. **Dashboard & Statistik Realtime:**
   - Total Modem Terdaftar, Online, Offline, dan Redaman Kritis.
   - Grafik distribusi sinyal Rx Optical Power.

2. **Daftar & Detail Perangkat (Modem / ONU):**
   - Pencarian cerdas berdasarkan Nama, PPPoE Username, Serial Number, MAC, atau IP.
   - Status live, Rx Optical Power dengan indikator warna (Hijau, Kuning, Merah).
   - Edit SSID WiFi & Sandi WiFi dengan tombol terpisah dan sinkronisasi instan ke ACS.
   - Tombol Reboot Modem langsung dari ponsel.

3. **🗺️ Peta ODP & Sebaran Modem:**
   - Navigasi peta interaktif menampilkan lokasi ODP dan modem pelanggan.

4. **🎙️ Voice AI Assistant Berbahasa Indonesia:**
   - Menggunakan Azure Neural Voice (`id-ID-GadisNeural`) yang natural dan manusiawi.
   - **Phonetic Speech Intent Normalizer**: Otomatis mengoreksi salah tangkap suara mikrofon (seperti *"chat rekaman"* ➔ *"cek redaman"*, *"chat model"* ➔ *"cek modem"*, *"ribut"* ➔ *"reboot"*).
   - **Quick Action Chips (1-Tap)**: Tombol instan untuk Cek Redaman Kritis, Modem Mati, Status Modem, dan Modem Baru.

---

## 📥 Download APK Langsung

- **Rilis v1.0.6 Terbaru:** [GenieacsMobileMod-v1.0.6.apk](GenieacsMobileMod-v1.0.6.apk)
- **Shortcut File APK Terkini:** [GenieacsMobileMod-latest.apk](GenieacsMobileMod-latest.apk)

---

## 🏗️ Struktur Proyek

- **Bahasa:** Kotlin
- **Min SDK:** 24 (Android 7.0) | **Target SDK:** 35 (Android 15)
- **Arsitektur:** MVVM + ViewBinding + OkHttp3 + Material Design Components
- **Signing:** Release Key Android (`alijaya-release.jks`)
