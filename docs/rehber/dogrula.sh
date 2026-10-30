#!/usr/bin/env bash
# Rehber PDF'lerini Apache PDFBox ile okur ve kontrolleri (D1-D10, D12-D14) çalıştırır.
# Kullanım: ./docs/rehber/dogrula.sh                (docs/rehber/pdf altındaki PDF'ler)
#           ./docs/rehber/dogrula.sh <pdf klasörü>  (ör. negatif deneme için başka bir klasör)
# Her kontrol için GEÇTİ ya da KALDI yazar; bir kontrol kalırsa çıkış kodu 1'dir.
set -euo pipefail

cd "$(dirname "$0")"

PDFBOX_SURUM="3.0.6"
PDFBOX_SHA256="28948291a7d6addb91a158292f2e9348d2143720e25a9c87c91bbdd4b088475f"
PDFBOX_URL="https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox-app/$PDFBOX_SURUM/pdfbox-app-$PDFBOX_SURUM.jar"
ONBELLEK="${XDG_CACHE_HOME:-$HOME/.cache}/rasit-rehber"
JAR="$ONBELLEK/pdfbox-app-$PDFBOX_SURUM.jar"
PDF_KLASORU="${1:-pdf}"

if ! command -v java >/dev/null 2>&1; then
  echo "HATA: java bulunamadı. JDK 21 ya da üstü gerekir (README.md)" >&2
  exit 1
fi

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

if [[ ! -f "$JAR" ]]; then
  mkdir -p "$ONBELLEK"
  echo "PDFBox $PDFBOX_SURUM indiriliyor: $JAR"
  curl -fsSL -o "$JAR.tmp" "$PDFBOX_URL"
  mv "$JAR.tmp" "$JAR"
fi

if [[ "$(sha256 "$JAR")" != "$PDFBOX_SHA256" ]]; then
  echo "HATA: $JAR dosyasının SHA-256 değeri beklenenle uyuşmuyor. Dosyayı silip yeniden çalıştırın." >&2
  exit 1
fi

java -cp "$JAR" dogrulama/RehberDogrulama.java "$PDF_KLASORU"
