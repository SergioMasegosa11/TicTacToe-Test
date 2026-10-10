#!/usr/bin/env bash
# Prueft, ob die angegebenen CI-Checks (GitHub-Actions-Jobs) auf einem Commit erfolgreich waren.
# Laufen sie noch, wird gewartet (hoechstens TIMEOUT_SECONDS, Standard 15 Minuten).
# Schlaegt ein Check fehl oder fehlt er nach dem Timeout, endet das Skript mit Exit-Code 1.
#
# Aufruf: scripts/check-ci-status.sh <commit-sha> <check-name>...
#         z.B. scripts/check-ci-status.sh abc123 test e2e pitest
# Benoetigt: gh (GitHub CLI) mit GH_TOKEN, GITHUB_REPOSITORY (owner/repo)
set -euo pipefail

commit="$1"
shift
timeout="${TIMEOUT_SECONDS:-900}"
interval=20
waited=0

for check in "$@"; do
  while true; do
    # neuester Lauf dieses Checks auf dem Commit: "<status> <conclusion>"
    result=$(gh api "repos/$GITHUB_REPOSITORY/commits/$commit/check-runs?check_name=$check&per_page=1" \
      --jq '.check_runs[0] | "\(.status) \(.conclusion)"' 2>/dev/null || true)

    case "$result" in
      "completed success")
        echo "OK      $check"
        break
        ;;
      completed*)
        echo "::error::CI-Check '$check' ist auf $commit nicht erfolgreich ($result) - kein Release"
        exit 1
        ;;
    esac

    if [ "$waited" -ge "$timeout" ]; then
      echo "::error::CI-Check '$check' auf $commit nach ${timeout}s nicht abgeschlossen (${result:-nicht gefunden}) - kein Release"
      exit 1
    fi
    echo "warte   $check (${result:-noch nicht gestartet})"
    sleep "$interval"
    waited=$((waited + interval))
  done
done

echo "Alle CI-Checks auf $commit erfolgreich."
