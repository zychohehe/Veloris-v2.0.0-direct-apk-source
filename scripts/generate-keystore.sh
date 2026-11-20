#!/usr/bin/env bash
# Membuat keystore rilis Veloris (RSA 4096, PKCS12, berlaku 10.000 hari) dan menyiapkan
# nilai GitHub Secrets untuk workflow .github/workflows/build.yml.
#
# Pemakaian:  scripts/generate-keystore.sh [berkas.jks] [alias]
#
# PENTING: kunci rilis tidak boleh berubah setelah aplikasi dirilis. Kalau kunci hilang,
# pengguna tidak bisa memasang pembaruan di atas versi lama. Simpan cadangan terenkripsi
# di tempat yang aman (password manager / brankas), JANGAN di repository.
set -euo pipefail

OUT="${1:-veloris-release.jks}"
ALIAS="${2:-veloris}"

if [ -e "$OUT" ]; then
  echo "Gagal: '$OUT' sudah ada. Keystore rilis tidak boleh ditimpa." >&2
  exit 1
fi
command -v keytool >/dev/null 2>&1 || { echo "keytool tidak ditemukan. Pasang JDK 17." >&2; exit 1; }

read -r -s -p "Password keystore (min 6 karakter): " PASS; echo
read -r -s -p "Ulangi password: " PASS2; echo
[ "$PASS" = "$PASS2" ] || { echo "Password tidak sama." >&2; exit 1; }
[ "${#PASS}" -ge 6 ] || { echo "Password terlalu pendek." >&2; exit 1; }

# PKCS12 hanya mendukung satu password untuk store dan key, jadi keduanya disamakan.
umask 077
STORE_PASS="$PASS" keytool -genkeypair \
  -keystore "$OUT" -storetype PKCS12 \
  -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Veloris, OU=Mobile, O=Veloris, C=ID" \
  -storepass:env STORE_PASS -keypass:env STORE_PASS

chmod 600 "$OUT"

echo
echo "=== Sertifikat (SHA-256 dicocokkan ke ANDROID_CERT_SHA256 jika diisi) ==="
STORE_PASS="$PASS" keytool -list -v -keystore "$OUT" -storepass:env STORE_PASS -alias "$ALIAS" \
  | grep -E 'SHA256:' || true

B64="$OUT.b64"
base64 < "$OUT" | tr -d '\n' > "$B64"
chmod 600 "$B64"

cat <<EOF

Keystore dibuat: $OUT
Versi Base64 untuk secret: $B64

Tambahkan secrets di GitHub (Settings → Secrets and variables → Actions), atau pakai gh CLI:

  gh secret set ANDROID_KEYSTORE_BASE64 < "$B64"
  gh secret set ANDROID_KEYSTORE_PASSWORD      # isi: password yang tadi
  gh secret set ANDROID_KEY_ALIAS --body "$ALIAS"
  gh secret set ANDROID_KEY_PASSWORD           # isi: password yang sama
  gh secret set ANDROID_CERT_SHA256 --body "<SHA-256 dari daftar di atas>"   # opsional, dianjurkan

Setelah secrets terpasang, hapus file Base64 (tidak perlu disimpan):
  rm -f "$B64"

Simpan $OUT di brankas aman. Jangan di-commit (sudah diabaikan oleh .gitignore).
EOF
