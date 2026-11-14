#!/usr/bin/env bash
# Idempotentes Setup-/Update-Skript fuer die TicTacTest-Web-Konsole auf der EC2-Instanz.
# Wird zweimal verwendet (siehe docs/deployment.md):
#   1. Einmalig beim ersten "tofu apply" ueber cloud-init (ohne Argument: nur Pakete und
#      einen Platzhalter-Dienst einrichten, noch kein JAR vorhanden).
#   2. Bei jedem App-Deploy ueber SSH (.github/workflows/deploy.yml), mit der Version als
#      Argument - OHNE dass die Infrastruktur dafuer neu erstellt wird.
#
# Usage: bootstrap.sh [<version>]
# Erwartet, dass run-tictactest.sh und version-server.py im selben Verzeichnis liegen wie
# dieses Skript, und (falls <version> angegeben ist) dass /opt/tictactest/releases/<version>/
# das passende JAR schon enthaelt.
set -euo pipefail

VERSION="${1:-}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR=/opt/tictactest
RUN_SCRIPT="$APP_DIR/run-tictactest.sh"
VERSION_SERVER="$APP_DIR/version-server.py"
SERVICE=/etc/systemd/system/tictactest-console.service
VERSION_SERVICE=/etc/systemd/system/tictactest-version.service

# Laufzeitumgebung reproduzierbar: feste Versionen, Pruefsumme fuer das ttyd-Binary
# (Auftrag 2: "Wie installieren Sie es reproduzierbar?").
JAVA_PACKAGE=java-25-amazon-corretto-headless
TTYD_VERSION=1.7.7
TTYD_SHA256=8a217c968aba172e0dbf3f34447218dc015bc4d5e59bf51db2f2cd12b7be4f55

sudo mkdir -p "$APP_DIR/releases" "$APP_DIR/deploy-src"
# Das App-Verzeichnis gehoert ec2-user: Deploys (scp aus der Pipeline) schreiben ohne
# root, und der Dienst laeuft ohnehin als ec2-user.
sudo chown -R ec2-user:ec2-user "$APP_DIR"

if ! command -v java >/dev/null 2>&1; then
  sudo dnf install -y "$JAVA_PACKAGE"
fi

# ttyd ist nicht im Paket-Repository von Amazon Linux 2023: statisches Release-Binary
# in FESTER Version laden und die Pruefsumme verifizieren, damit die Installation
# reproduzierbar ist und kein manipuliertes Binary laeuft.
if ! command -v ttyd >/dev/null 2>&1; then
  curl -fsSL -o /tmp/ttyd \
    "https://github.com/tsl0922/ttyd/releases/download/${TTYD_VERSION}/ttyd.x86_64"
  echo "${TTYD_SHA256}  /tmp/ttyd" | sha256sum -c -
  sudo install -m 755 /tmp/ttyd /usr/local/bin/ttyd
fi

sudo install -m 755 "$SCRIPT_DIR/run-tictactest.sh" "$RUN_SCRIPT"
sudo install -m 755 "$SCRIPT_DIR/version-server.py" "$VERSION_SERVER"

if [ -n "$VERSION" ]; then
  # version.txt stammt automatisch aus dem Release-Tag (Argument dieses Skripts) und
  # wird vom Versions-Endpoint ausgeliefert - nie von Hand gepflegt.
  echo "$VERSION" | sudo tee "$APP_DIR/releases/$VERSION/version.txt" > /dev/null
  # Der current-Symlink entscheidet, welche Version laeuft. Er ueberlebt einen Neustart
  # der Instanz: nach einem Reboot startet systemd dieselbe Version wie vorher.
  sudo ln -sfn "$APP_DIR/releases/$VERSION" "$APP_DIR/current"
fi

# Web-Konsole als Dienst: startet beim Booten, laeuft NICHT als root (User=ec2-user)
# und startet nach einem Absturz neu (Restart=always). ttyd startet ausschliesslich
# das Wrapper-Skript mit dem JAR - NIE eine Shell (siehe docs/webkonsole.md, Sicherheit).
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

# Versions-Endpoint als eigener Dienst (Port 8080): liefert die laufende Version per
# HTTP, damit Frontend und Pipeline sie abfragen koennen (Auftrag 1 Teil 2 / Auftrag 3).
sudo tee "$VERSION_SERVICE" > /dev/null <<EOF
[Unit]
Description=TicTacTest Versions-Endpoint
After=network.target

[Service]
ExecStart=/usr/bin/python3 $VERSION_SERVER 8080
Restart=always
User=ec2-user

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable tictactest-console tictactest-version
sudo systemctl restart tictactest-console tictactest-version
