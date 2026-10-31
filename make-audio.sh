#!/data/data/com.termux/files/usr/bin/bash
# Makes the Greek audio clips and packs them into a new zip.
# Needs internet. Uses Google Translate's Greek voice via curl.
set -e
OUT="${OUT_DIR:-/sdcard/Download}"
for t in curl unzip zip; do command -v "$t" >/dev/null || pkg install -y "$t" >/dev/null; done
LATEST="$(ls -t "$OUT"/*.zip 2>/dev/null | head -1 || true)"
read -rp "Path of the project .zip${LATEST:+ (Enter = $LATEST)}: " Z
Z="${Z%\"}"; Z="${Z#\"}"; Z="${Z/#\~/$HOME}"; [ -z "$Z" ] && Z="$LATEST"
[ -f "$Z" ] || { echo "Zip not found"; exit 1; }
W="$HOME/.audio-work"; rm -rf "$W"; mkdir -p "$W"; unzip -q "$Z" -d "$W"
M="$(find "$W" -maxdepth 3 -name audio-manifest.tsv | head -1)"
[ -n "$M" ] || { echo "No audio-manifest.tsv in zip"; exit 1; }
P="$(dirname "$M")"; R="$P/app/src/main/res/raw"; mkdir -p "$R"
N=0; BAD=0; TOTAL=$(wc -l < "$M")
while IFS=$'\t' read -r name text; do
  [ -z "$name" ] && continue
  N=$((N+1)); f="$R/$name.mp3"
  for try in 1 2 3; do
    curl -s -m 20 -A "Mozilla/5.0" -G "https://translate.google.com/translate_tts" \
      --data-urlencode "ie=UTF-8" --data-urlencode "client=tw-ob" --data-urlencode "tl=el" \
      --data-urlencode "q=$text" -o "$f" || true
    [ "$(stat -c %s "$f" 2>/dev/null || echo 0)" -gt 800 ] && break
    sleep 1
  done
  if [ "$(stat -c %s "$f" 2>/dev/null || echo 0)" -le 800 ]; then rm -f "$f"; BAD=$((BAD+1)); echo "  failed: $text"; fi
  printf '\r%s/%s' "$N" "$TOTAL"; sleep 0.2
done < "$M"
echo; echo "Done. $((N-BAD)) clips, $BAD failed (failed ones fall back to phone TTS)."
rm -f "$R/README.txt"
NEW="$OUT/$(basename "${Z%.zip}")-audio.zip"; rm -f "$NEW"
( cd "$(dirname "$P")" && zip -qr "$NEW" "$(basename "$P")" )
rm -rf "$W"; echo "Created: $NEW  (now run build-from-zip.sh on it)"
