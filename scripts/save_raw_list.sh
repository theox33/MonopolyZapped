#!/usr/bin/env bash
set -euo pipefail

# Répertoire source
SRC_DIR="./../app/src/main/res/raw"
# Fichier de sortie
OUT_FILE="./raw_files.txt"

# Vérification
if [ ! -d "$SRC_DIR" ]; then
  echo "❌ Source directory not found: $SRC_DIR"
  exit 1
fi

# Sauvegarde de la liste
ls -1 "$SRC_DIR" > "$OUT_FILE"

echo "✅ Saved raw filenames to $OUT_FILE"
