#!/usr/bin/env bash
# Rehberin iki sürümünü aynı Typst kaynağından üretir.
# Kullanım: ./docs/rehber/uret.sh   (repo kökünden ya da docs/rehber içinden)
#           ./docs/rehber/uret.sh <çıktı klasörü>   (varsayılan: docs/rehber/pdf)
set -euo pipefail

cd "$(dirname "$0")"

BEKLENEN_TYPST="0.15.1"
CIKTI="${1:-pdf}"

# Sürüm etiketi ve yürürlük tarihinin UNIX zamanı (PDF oluşturma tarihi buna sabitlenir;
# böylece aynı kaynaktan her üretimde bayt bayt aynı PDF çıkar).
SURUMLER=(
  "2026.10:1790812800"
  "2026.11:1793491200"
)

if ! command -v typst >/dev/null 2>&1; then
  echo "HATA: typst bulunamadı. Kurulum: brew install typst (README.md)" >&2
  exit 1
fi

KURULU_TYPST="$(typst --version | awk '{print $2}')"
if [[ "$KURULU_TYPST" != "$BEKLENEN_TYPST" ]]; then
  echo "UYARI: Typst $BEKLENEN_TYPST bekleniyordu, kurulu sürüm $KURULU_TYPST. PDF'ler repodakilerle bayt bayt aynı olmayabilir." >&2
fi

mkdir -p "$CIKTI"

ORTAK=(--root kaynak --font-path kaynak/fontlar --ignore-system-fonts --ignore-embedded-fonts)

for kayit in "${SURUMLER[@]}"; do
  surum="${kayit%%:*}"
  zaman="${kayit##*:}"
  hedef="$CIKTI/Rasit-Bankasi-Urun-Hizmet-Rehberi-$surum.pdf"
  typst compile "${ORTAK[@]}" --input "surum=$surum" --creation-timestamp "$zaman" kaynak/rehber.typ "$hedef"
  sayfa="$(typst eval "${ORTAK[@]}" --input "surum=$surum" --in kaynak/rehber.typ 'query(<sayfa-sayisi>).first().value')"
  echo "$hedef: $sayfa sayfa"
done
