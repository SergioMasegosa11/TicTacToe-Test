#!/usr/bin/env bash
# Aktiviert den Schutz des Hauptbranches (Ruleset .github/rulesets/main-schutz.json) auf GitHub.
# Legt das Ruleset an oder aktualisiert es, wenn es schon existiert. Siehe RELEASE.md, "Schutz von main".
#
# Aufruf:  GITHUB_TOKEN=<token> scripts/protect-main.sh [owner/repo]
# Der Token braucht Admin-Rechte auf dem Repository (classic: Scope "repo").
set -euo pipefail

repo="${1:-SergioMasegosa11/TicTacToe-Test}"
ruleset_file="$(dirname "$0")/../.github/rulesets/main-schutz.json"
api="https://api.github.com/repos/$repo/rulesets"
auth=(-H "Authorization: Bearer ${GITHUB_TOKEN:?GITHUB_TOKEN muss gesetzt sein}" -H "Accept: application/vnd.github+json")

name=$(grep -o '"name": *"[^"]*"' "$ruleset_file" | head -1 | sed -E 's/.*"([^"]*)"$/\1/')

# Existiert bereits ein Ruleset mit diesem Namen?
id=$(curl -s "${auth[@]}" "$api" | tr -d '\n' | grep -o "\"id\": *[0-9]*, *\"name\": *\"$name\"" | grep -o '[0-9]\+' | head -1 || true)

if [ -n "$id" ]; then
  echo "Aktualisiere Ruleset '$name' (id $id) in $repo"
  method=PUT
  url="$api/$id"
else
  echo "Lege Ruleset '$name' in $repo an"
  method=POST
  url="$api"
fi

status=$(curl -s -o /tmp/ruleset-response.json -w "%{http_code}" -X "$method" "${auth[@]}" "$url" --data-binary @"$ruleset_file")
if [ "$status" != "200" ] && [ "$status" != "201" ]; then
  echo "Fehler: GitHub antwortet mit HTTP $status" >&2
  cat /tmp/ruleset-response.json >&2
  exit 1
fi
echo "OK: main ist geschuetzt (nur noch per Pull Request mit gruenen Checks test, e2e, pitest, gate)."
