<p align="center"><img src="docs/veloris-logo.svg" width="96" alt="Veloris"></p>
<h1 align="center">Veloris</h1>
<p align="center">Pemutar & penjelajah musik modern — Jetpack Compose, tersinkron dengan YouTube dan Spotify.</p>

> Veloris adalah nama baru dari **Auralis**. Package: `com.veloris.app`.

## Fitur v2.0.0
- **Navbar baru**: pil melayang bergaya kaca, indikator pegas, label muncul di tab aktif.
- **Beranda real-time**: Quick Picks, Trending (polling 60 dtk saat layar aktif), mix Chill / Pop Indonesia / Hip-Hop / Lofi, rak "Dari library kamu", badge LIVE.
- **Cari** dengan debounce.
- **Library & Profil**: login OAuth (PKCE) ke YouTube dan Spotify; profil, playlist, dan lagu disukai tersinkron otomatis (saat dibuka, saat terhubung, dan tiap 15 menit via WorkManager), disimpan sebagai cache lokal.
- **Optimasi**: R8 full + shrink resources, baseline profile, animasi berbasis pegas, daftar lazy dengan key stabil, kuota API dihemat.

## Konfigurasi
Salin `local.properties.example` → `local.properties` (atau set env var / GitHub Secrets):

| Key | Dari mana |
|---|---|
| `YT_API_KEY` | Google Cloud → YouTube Data API v3 → API key |
| `GOOGLE_CLIENT_ID` | OAuth client tipe **Android** (package `com.veloris.app` + SHA-1); redirect `com.veloris.app:/oauth2redirect` |
| `SPOTIFY_CLIENT_ID` | Spotify Developer Dashboard; Redirect URI `veloris://callback` |

> Kuota YouTube 10.000 unit/hari: trending = 1 unit, pencarian = 100 unit. Mix beranda disegarkan ±30 menit sekali.

## Build
- Android Studio: buka folder ini lalu **Build → Build APK(s)**.
- GitHub Actions (`.github/workflows/build.yml`):
  - **Debug APK** dibuat di setiap push, PR, dan dispatch → artifact `Veloris-debug.apk` (file .apk langsung, tidak dibungkus zip).
  - **Release APK** dibuat di push ke `main`, tag `v*`, dan dispatch → artifact `Veloris-v<versi>-<code>-release.apk` (file .apk langsung) dan ringkasan verifikasi di halaman run.
  - Tag `v*` (mis. `v2.0.1`) otomatis membuat GitHub Release dengan file `.apk` dan `SHA256SUMS.txt` sebagai aset langsung.
- Build rilis di CI **gagal** bila secret penandatanganan belum lengkap; tidak pernah diam-diam memakai debug key.

## Signing rilis (v1 + v2 + v3)
Semua APK rilis ditandatangani dengan skema **v1 (JAR), v2, dan v3**, lalu diverifikasi dengan `apksigner` sebelum diunggah. Workflow menolak APK bila salah satu skema gagal.

1. Buat keystore sekali (simpan hasilnya di brankas aman):
   ```bash
   scripts/generate-keystore.sh
   ```
   Skrip ini juga menampilkan perintah `gh secret set` untuk langkah berikutnya.
2. Isi GitHub Secrets di repositori:

   | Secret | Isi |
   |---|---|
   | `ANDROID_KEYSTORE_BASE64` | keystore dalam Base64 (hasil skrip) |
   | `ANDROID_KEYSTORE_PASSWORD` | password keystore |
   | `ANDROID_KEY_ALIAS` | alias kunci (default `veloris`) |
   | `ANDROID_KEY_PASSWORD` | password kunci (sama dengan keystore) |
   | `ANDROID_CERT_SHA256` | *opsional, dianjurkan*: SHA-256 sertifikat; CI menolak bila tidak cocok |

3. Untuk build rilis lokal, isi `VELORIS_KEYSTORE_PATH`, `VELORIS_KEYSTORE_PASSWORD`, `VELORIS_KEY_ALIAS`, dan `VELORIS_KEY_PASSWORD` di `local.properties`. Tanpa itu, build rilis lokal memakai debug key dan menampilkan peringatan.

> ⚠️ **Kunci rilis tidak boleh diganti** setelah aplikasi dirilis. Pengguna tidak bisa memasang pembaruan yang ditandatangani kunci berbeda. Simpan cadangan keystore dan passwordnya.

`versionCode` di CI = nomor run GitHub Actions, sehingga selalu naik. Lokal memakai `1`.

Mengetuk lagu membuka YouTube Music / Spotify. Pemutar in-app belum disertakan.

## Kontributor
<img src="docs/claude-logo.svg" width="40" align="left" alt="Claude"> **Claude** (Anthropic) — UI/UX, arsitektur, implementasi. Lihat [CONTRIBUTORS.md](CONTRIBUTORS.md).
