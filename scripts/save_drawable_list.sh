#!/usr/bin/env bash
set -euo pipefail

# Répertoire source
SRC_DIR="./../app/src/main/res/drawable-nodpi"
# Fichier de sortie
OUT_FILE="./drawable_files.txt"

# Vérification
if [ ! -d "$SRC_DIR" ]; then
  echo "❌ Source directory not found: $SRC_DIR"
  exit 1
fi

# Sauvegarde de la liste
ls -1 "$SRC_DIR" > "$OUT_FILE"

echo "✅ Saved drawable filenames to $OUT_FILE"
