#!/usr/bin/env python3
"""Minimaler HTTP-Endpoint fuer die Versionsabfrage (siehe docs/webkonsole.md, Teil 2).

Liefert unter /version.txt den Inhalt von /opt/tictactest/current/version.txt, also die
Version des aktuell deployten Releases. Die Datei wird beim Deployment von bootstrap.sh
aus dem Release-Tag geschrieben - die Version stimmt damit automatisch und wird nie von
Hand gepflegt. Das JAR selbst bleibt unveraendert.

CORS ist noetig, weil das Frontend von einem anderen Origin (S3 Static Website bzw.
lokal ein anderer Port) auf diesen Endpoint zugreift. Es wird nur eine einzige,
oeffentliche Information ausgeliefert (die Versionsnummer), daher ist "*" vertretbar.
"""
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

VERSION_FILE = Path("/opt/tictactest/current/version.txt")
PORT = 8080


class VersionHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path.split("?")[0] not in ("/", "/version.txt"):
            self.send_error(404)
            return
        try:
            version = VERSION_FILE.read_text().strip()
        except OSError:
            version = "unbekannt (noch kein Release deployt)"
        body = (version + "\n").encode()
        self.send_response(200)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt, *args):  # kein Access-Log noetig
        pass


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else PORT
    HTTPServer(("0.0.0.0", port), VersionHandler).serve_forever()
