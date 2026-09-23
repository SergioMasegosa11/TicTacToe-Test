#!/usr/bin/env bash
# Gibt den Abschnitt einer Version aus CHANGELOG.md aus (ohne die Ueberschrift).
# Bricht ab, wenn es fuer die Version keinen Eintrag gibt.
# Aufruf: scripts/changelog-section.sh 1.0.0 [pfad-zu-CHANGELOG.md]
set -euo pipefail

version="$1"
changelog="${2:-CHANGELOG.md}"

# Alles zwischen "## [1.0.0]" und der naechsten "## "-Ueberschrift bzw. den Link-Definitionen
section=$(awk -v v="$version" '
  /^## / { if (found) exit; if (index($0, "## [" v "]") == 1) { found = 1; next } }
  /^\[[^]]+\]: / { if (found) exit }
  found { print }
' "$changelog" | sed -e '/./,$!d')

if [ -z "$(echo "$section" | tr -d '[:space:]')" ]; then
  echo "Kein Eintrag fuer Version $version in $changelog gefunden" >&2
  exit 1
fi

echo "$section"
