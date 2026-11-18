#!/usr/bin/env bash
# Wird von ttyd als Dauerbefehl in der Web-Konsole gestartet (siehe docs/deployment.md).
# Das JAR selbst bleibt unveraendert - es startet/beendet sich wie gewohnt pro Spiel.
# Diese Schleife sorgt nur dafuer, dass die Web-Konsole direkt ein neues Spiel anbietet.
set -u

JAR_DIR=/opt/tictactest/current

while true; do
  JAR=$(ls "$JAR_DIR"/tictactest-*.jar 2>/dev/null | head -n1)
  if [ -z "$JAR" ]; then
    echo "Noch kein Release deployt. Siehe docs/deployment.md im Repository."
    sleep 5
    continue
  fi

  echo "=== TicTacTest ($(basename "$JAR")) - es spielt: Mensch (X) gegen RandomPlayer (O) ==="
  java -jar "$JAR"

  echo
  echo "Neues Spiel in 3 Sekunden..."
  sleep 3
done
