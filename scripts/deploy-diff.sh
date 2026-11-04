#!/usr/bin/env bash
# Ermittelt, ob sich Backend (JAR-Inhalt) bzw. Frontend (Web-Konsole) zwischen zwei Refs
# geaendert haben, damit ein Release nur das deployt, was sich wirklich geaendert hat
# (siehe docs/deployment.md). Aufruf: scripts/deploy-diff.sh <alter-ref> <neuer-ref>
# Gibt "backend=true|false" und "frontend=true|false" aus, zusaetzlich nach
# $GITHUB_OUTPUT, falls gesetzt.
set -euo pipefail

old_ref="$1"
new_ref="$2"

changed=$(git diff --name-only "$old_ref" "$new_ref")

backend=false
frontend=false

if echo "$changed" | grep -qE '^(src/main/|build\.gradle$|gradle\.properties$)'; then
  backend=true
fi
if echo "$changed" | grep -qE '^deploy/'; then
  frontend=true
fi

echo "backend=$backend"
echo "frontend=$frontend"

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "backend=$backend"
    echo "frontend=$frontend"
  } >> "$GITHUB_OUTPUT"
fi
