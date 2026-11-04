#!/usr/bin/env bash
# Idempotentes Setup-/Update-Skript fuer die TicTacTest-Web-Konsole auf der EC2-Instanz.
# Wird zweimal verwendet (siehe docs/deployment.md):
#   1. Einmalig beim ersten "tofu apply" ueber cloud-init (ohne Argument: nur Pakete und
#      einen Platzhalter-Dienst einrichten, noch kein JAR vorhanden).
#   2. Bei jedem App-Deploy ueber SSH (.github/workflows/deploy.yml), mit der Version als
#      Argument - OHNE dass die Infrastruktur dafuer neu erstellt wird.
#
# Usage: bootstrap.sh [<version>]
# Erwartet, dass run-tictactest.sh im selben Verzeichnis liegt wie dieses Skript, und
# (falls <version> angegeben ist) dass /opt/tictactest/releases/<version>/ das passende
# JAR schon enthaelt.
set -euo pipefail

VERSION="${1:-}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR=/opt/tictactest
RUN_SCRIPT="$APP_DIR/run-tictactest.sh"
SERVICE=/etc/systemd/system/tictactest-console.service

sudo mkdir -p "$APP_DIR/releases"

if ! command -v java >/dev/null 2>&1; then
  sudo dnf install -y java-21-amazon-corretto-headless
fi

if ! command -v ttyd >/dev/null 2>&1; then
  sudo dnf install -y ttyd || {
    # Amazon Linux 2023 hat ttyd nicht in jedem Repo-Snapshot: Fallback auf das
    # statische Release-Binary von https://github.com/tsl0922/ttyd
    curl -fsSL -o /tmp/ttyd https://github.com/tsl0922/ttyd/releases/latest/download/ttyd.x86_64
    sudo install -m 755 /tmp/ttyd /usr/local/bin/ttyd
  }
fi

sudo install -m 755 "$SCRIPT_DIR/run-tictactest.sh" "$RUN_SCRIPT"

if [ -n "$VERSION" ]; then
  sudo ln -sfn "$APP_DIR/releases/$VERSION" "$APP_DIR/current"
fi

sudo tee "$SERVICE" > /dev/null <<EOF
[Unit]
Description=TicTacTest Web-Konsole (ttyd)
After=network.target

[Service]
ExecStart=/usr/bin/env ttyd -p 7681 -W $RUN_SCRIPT
Restart=always
User=ec2-user

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable tictactest-console
sudo systemctl restart tictactest-console
