#!/usr/bin/env bash
set -euo pipefail

# === Config par défaut ===
RES_ROOT="./../app/src/main/res"
LIST_FILE="./drawable_files.txt"
DRAWABLE_DST="$RES_ROOT/drawable-nodpi-struct"

# === Vérifs de base ===
if [ ! -f "$LIST_FILE" ]; then
  echo "❌ Liste introuvable: $LIST_FILE"
  echo "   Crée d'abord ce fichier avec: ./save_drawable_list.sh"
  exit 1
fi

mkdir -p "$DRAWABLE_DST"

# === Fonction: calcule le nom de res @drawable à partir du nom de fichier ===
to_res_name() {
  local fname="$1"
  local lower="${fname,,}"

  # Nine-patch: foo.9.png -> foo
  if [[ "$lower" == *.9.png ]]; then
    echo "${fname%.*}"      | sed 's/\.9$//' 
    return
  fi

  # Autres images: enlève juste l'extension finale
  echo "${fname%.*}"
}

# === Boucle sur la liste ===
created=0
skipped=0

while IFS= read -r line || [ -n "$line" ]; do
  # Trim espaces
  name="$(echo "$line" | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
  # Ignore lignes vides et commentaires
  [[ -z "$name" || "$name" =~ ^# ]] && continue

  lower="${name,,}"
  res_name="$(to_res_name "$name")"
  out="$DRAWABLE_DST/$res_name.xml"

  # Filtrage extensions courantes d'images (info, non bloquant)
  if [[ ! "$lower" =~ \.(png|jpg|jpeg|webp|xml)$ && ! "$lower" =~ \.9\.png$ ]]; then
    echo "⚠️  Extension inattendue: '$name' → génération d'un stub XML quand même."
  fi

  # Stub XML transparent
  cat > "$out" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#00000000"/>
</shape>
EOF

  echo "✅ Stub drawable: $out"
  created=$((created + 1))
done < "$LIST_FILE"

echo "—"
echo "Done. Created: $created stubs in $DRAWABLE_DST"
