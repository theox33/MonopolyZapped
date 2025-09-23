#!/usr/bin/env bash
set -euo pipefail

# === Config par défaut ===
RES_ROOT="./../app/src/main/res"
LIST_FILE="raw_files.txt"
RAW_DST="$RES_ROOT/raw-struct"

# === Vérifs de base ===
if [ ! -f "$LIST_FILE" ]; then
  echo "❌ Liste introuvable: $LIST_FILE"
  echo "   Crée d'abord ce fichier (ex: via un script save_raw_list.sh) contenant un nom par ligne."
  exit 1
fi

mkdir -p "$RAW_DST"

# === Helpers ===
have_ffmpeg() { command -v ffmpeg >/dev/null 2>&1; }

gen_wav_silence_py() {
  # $1 = output wav path
  python3 - "$1" <<'PY'
import sys, wave, struct
out = sys.argv[1]
fr = 44100
frames = fr * 1  # 1 seconde
with wave.open(out, 'wb') as w:
    w.setnchannels(1)   # mono
    w.setsampwidth(2)   # 16-bit
    w.setframerate(fr)
    w.writeframes(struct.pack('<h', 0) * frames)
PY
}

gen_mp3_silence_ffmpeg() {
  # $1 = output mp3 path
  ffmpeg -hide_banner -loglevel error \
    -f lavfi -i anullsrc=r=44100:cl=mono -t 1 \
    -q:a 9 -acodec libmp3lame "$1"
}

created=0

# === Lecture de la liste ===
while IFS= read -r line || [ -n "$line" ]; do
  # Trim + ignorer vides/commentaires
  name="$(echo "$line" | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
  [[ -z "$name" || "$name" =~ ^# ]] && continue

  base_lower="${name,,}"
  ext="${base_lower##*.}"
  stem="${name%.*}"

  case "$ext" in
    wav)
      out="$RAW_DST/$stem.wav"
      gen_wav_silence_py "$out"
      echo "🎧 WAV stub (1s silence): $out"
      created=$((created+1))
      ;;
    mp3)
      out="$RAW_DST/$stem.mp3"
      if have_ffmpeg; then
        gen_mp3_silence_ffmpeg "$out"
        echo "🎧 MP3 stub (1s silence via ffmpeg): $out"
      else
        : > "$out"
        echo "⚠️  ffmpeg non trouvé → MP3 vide créé: $out (lecture échouera à l'exécution)"
      fi
      created=$((created+1))
      ;;
    *)
      out="$RAW_DST/$name"
      : > "$out"
      echo "📄 Stub vide (ext. non gérée): $out"
      created=$((created+1))
      ;;
  esac
done < "$LIST_FILE"

echo "—"
echo "✅ Terminé. Fichiers générés: $created dans $RAW_DST"
