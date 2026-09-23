#!/usr/bin/env bash
# Liest das DevContainer-Image aus .devcontainer/devcontainer.json und prueft,
# dass es eine offiziell freigegebene Version ist (Tag vX.Y.Z, kein latest).
# Aufruf: scripts/devcontainer-image.sh [pfad-zu-devcontainer.json]
set -euo pipefail

json="${1:-.devcontainer/devcontainer.json}"

image=$(grep -o '"image": *"[^"]*"' "$json" | sed -E 's/.*"([^"]*)"$/\1/')

if [ -z "$image" ]; then
  echo "Kein \"image\" in $json gefunden" >&2
  exit 1
fi

if ! echo "$image" | grep -Eq '^ghcr\.io/[a-z0-9._-]+/tictactoe-test:v[0-9]+\.[0-9]+\.[0-9]+$'; then
  echo "Nicht freigegebenes Image: $image (erlaubt ist nur ghcr.io/<owner>/tictactoe-test:vX.Y.Z)" >&2
  exit 1
fi

echo "$image"
