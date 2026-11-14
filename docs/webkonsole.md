# Web-Konsole: TicTacTest im Browser

Dieses Dokument beantwortet den Auftrag "TicTacTest im Browser – Web-Konsole":
Werkzeugwahl, lokaler Start, Sicherheit, Versionsabfrage, Frontend und Release-Erweiterung.
Das Spiel selbst wird dabei **nicht verändert** - betrieben wird immer das fertige JAR aus
dem GitHub-Release (Artefakt), nie ein lokal gebauter Stand (Quellcode).

## 1. Werkzeugwahl: ttyd

| Werkzeug | Beurteilung |
|---|---|
| **ttyd** (gewählt) | Statisches Binary ohne Abhängigkeiten, echtes Terminal (Pseudo-Terminal), xterm.js im Browser schon eingebaut, aktiv gepflegt. ANSI-Farben des Spiels funktionieren ohne Zusatzaufwand. |
| GoTTY | Gleiches Konzept, aber seit Jahren kaum gepflegt - für den Betrieb ein Risiko. |
| websocketd | Überträgt nur Textzeilen, kein echtes Terminal: eigene HTML/JS-Seite nötig, ANSI-Farbcodes müssten selbst behandelt werden. Zu viel Eigenbau für keinen Mehrwert. |

**Wie es funktioniert:** ttyd lauscht auf einem Port (hier 7681). Pro Browser-Verbindung
startet es den konfigurierten Befehl in einem eigenen **Pseudo-Terminal (PTY)** als eigenen
Prozess und verbindet dessen Ein-/Ausgabe über einen **WebSocket** mit xterm.js im Browser.
Jeder Besucher bekommt also seinen eigenen Prozess - und sieht nur sein eigenes Spiel.

## 2. Lokal starten

Das JAR stammt aus dem letzten GitHub-Release (nicht lokal gebaut!):

```bash
# JAR + Prüfsumme aus dem Release laden und verifizieren
gh release download v1.0.2 --pattern "tictactest-*.jar" --pattern "*.sha256"
sha256sum -c tictactest-1.0.2.jar.sha256

# Web-Konsole starten (Linux/Devcontainer; ttyd-Binary siehe deploy/bootstrap.sh)
ttyd -p 7681 -W java -jar tictactest-1.0.2.jar
```

Dann http://localhost:7681 öffnen und spielen.

- **`-W` (writable) ist die entscheidende Option:** ohne sie ist das Terminal im Browser
  schreibgeschützt - man sieht das Spielfeld, kann aber nicht tippen.
- `-p 7681` legt den Port fest.

### Beobachtungen

- **Spielende:** `java -jar` beendet sich nach einer Partie; ttyd schliesst dann die
  Verbindung ("Neu laden für ein neues Spiel"). Damit die Web-Konsole direkt ein neues
  Spiel anbietet, startet im Betrieb ein kleines Wrapper-Skript
  ([`deploy/run-tictactest.sh`](../deploy/run-tictactest.sh)) das JAR in einer Schleife
  neu. Das JAR bleibt unverändert.
- **Ungültige Eingabe:** behandelt das Spiel selbst (`invalid input, please enter a free
  field (0-8)`), nach zu vielen Fehlversuchen bricht es die Partie kontrolliert ab -
  die Schleife startet danach einfach eine neue.
- **Zwei Browser-Tabs = zwei Java-Prozesse:** ttyd startet pro Verbindung einen eigenen
  Prozess. Eine JVM für dieses kleine Spiel braucht grob 50-100 MB. Auf einem Server mit
  **1 GB RAM** sind also nur etwa **5-10 gleichzeitige Spiele** realistisch, bevor der
  Speicher ausgeht (dann killt der Kernel Prozesse per OOM-Killer). Für den Lernzweck
  in Ordnung; für echten Betrieb müsste man Verbindungen limitieren (`ttyd -m <max>`)
  oder die Architektur ändern (ein Server-Prozess statt Prozess pro Besucher, siehe
  "Optional - Eigenes Web-Backend" im Auftrag).

## 3. Sicherheit

Die Web-Konsole ist für **alle erreichbar, die die URL kennen**. Deshalb gilt:

- **ttyd startet ausschliesslich das Wrapper-Skript mit dem JAR - nie eine Shell.**
  Wäre `ttyd bash` konfiguriert, hätte **jeder Besucher weltweit eine Shell auf dem
  Server**: Dateien lesen/löschen, Schadsoftware installieren, den Server in ein Botnetz
  einreihen, über die AWS-Instanz-Metadaten (und das Instance Profile!) auf das
  AWS-Konto zugreifen. Das wäre eine vollständige Übernahme der Instanz.
- Der Dienst läuft **nicht als root**, sondern als `ec2-user`
  ([`deploy/bootstrap.sh`](../deploy/bootstrap.sh), systemd `User=`). Selbst wenn jemand
  aus dem Spiel ausbrechen könnte, hätte er keine Root-Rechte.
- Das Spiel selbst begrenzt Eingaben (Zeilenlänge, Anzahl Fehlversuche, Eingaben werden
  nie ungefiltert zurückgegeben - siehe `HumanPlayer.java`), sodass bösartige Eingaben
  weder das Terminal kapern noch den Prozess ewig blockieren.
- Die Security Group öffnet nur die nötigen Ports (22 eingeschränkt, 7681, 8080 -
  Begründung in [`infra/main.tf`](../infra/main.tf)).

## 4. Versionsabfrage per HTTP

Das JAR bietet selbst keinen Versions-Endpoint, und es wird nicht verändert. Lösung:

1. Beim Deployment schreibt [`deploy/bootstrap.sh`](../deploy/bootstrap.sh) die Version
   **aus dem Release-Tag** (Argument des Skripts, von `deploy.yml` übergeben) in
   `/opt/tictactest/releases/<version>/version.txt`. Der `current`-Symlink zeigt auf die
   laufende Version - die Datei stimmt also **automatisch**, niemand pflegt sie von Hand.
2. Ein minimaler HTTP-Server ([`deploy/version-server.py`](../deploy/version-server.py),
   eigener systemd-Dienst, Port 8080) liefert den Inhalt unter `/version.txt` aus - mit
   CORS-Header, damit das Frontend (anderer Origin) ihn per `fetch` lesen darf.

```bash
curl http://<instanz-ip>:8080/version.txt   # -> z.B. "1.0.2"
```

**Warum aus dem Tag und nicht aus Dateiname oder Manifest?** Der Tag ist die Quelle der
Wahrheit des Release-Prozesses (RELEASE.md prüft Tag == `gradle.properties` == Changelog).
Das Deployment kennt den Tag ohnehin - Dateiname und Manifest wären nur indirekte Wege
zur selben Information.

## 5. Frontend

[`frontend/`](../frontend) ist eine statische Seite in plain HTML/CSS/JS, ohne Framework
und ohne Build-Step:

- `index.html` - Titel, Spielregeln (Feld 0-8 eintippen, Enter), eingebettetes Terminal
  (`<iframe>`), Versionsanzeige
- `app.js` - setzt die iframe-URL, fragt die Backend-Version per `fetch` ab
- `config.js` - **die einzige umgebungsabhängige Datei**: URLs der Web-Konsole und der
  Versionsabfrage. Im Repository stehen localhost-Werte; in der Cloud schreibt OpenTofu
  diese Datei selbst in den S3-Bucket ([`infra/s3.tf`](../infra/s3.tf)). Der Code
  (`app.js`) enthält **keine** URL.
- `version.js` - Frontend-Version, im Repo `"dev"`; beim Release automatisch mit der
  echten Nummer überschrieben (`release.yml`).

Lokal testen (zwei getrennte Server, wie im Auftrag):

```bash
ttyd -p 7681 -W java -jar tictactest-1.0.2.jar     # Web-Konsole auf 7681
python3 deploy/version-server.py 8080 &            # Versionsabfrage auf 8080
cd frontend && python3 -m http.server 8000         # Frontend auf 8000
```

→ http://localhost:8000. Zeigt der Footer `nicht erreichbar`, hilft die
Entwickler-Konsole des Browsers: typisch sind `ERR_CONNECTION_REFUSED` (Endpoint läuft
nicht) oder eine CORS-Meldung (Endpoint ohne `Access-Control-Allow-Origin`).

## 6. Release-Erweiterung

Ein Tag `v*` erzeugt weiterhin den GitHub-Release mit dem versionierten JAR und neu
zusätzlich (`.github/workflows/release.yml`):

- `tictactest-frontend-<version>.zip` (+ `.sha256`) - das Frontend, mit automatisch
  gesetzter `version.js`

**Warum lohnt sich das Frontend als Release-Asset, obwohl es nur ein paar Dateien sind?**
Weil damit *alles, was läuft, aus einem Release stammt*: Backend und Frontend einer
Version gehören nachweisbar zusammen, jede Version lässt sich exakt so wieder deployen
(Rollback!), und das Deployment braucht keinen Zugriff auf den Quellcode - nur auf
Release-Assets. "Es sind nur ein paar Dateien" ist genau der Grund, warum es fast nichts
kostet - der Nutzen (Nachvollziehbarkeit, Rollback, Trennung Build/Deploy) bleibt gleich
gross wie beim JAR.

**Was gehört zum Release, was zur Infrastruktur?** Das Frontend-Archiv und das JAR sind
Release-Artefakte (ändern sich pro Version, werden deployt). Die Betriebs-Skripte in
`deploy/` (systemd-Units, Wrapper, Versions-Endpoint) gehören zum Betrieb der Instanz:
sie werden beim Backend-Deploy direkt aus dem Git-Tag mitkopiert, und beim allerersten
Start der Instanz von cloud-init eingerichtet - sie brauchen kein eigenes Release-Asset.
`config.js` schliesslich gehört zur Infrastruktur (OpenTofu schreibt sie), weil nur die
Infrastruktur die Adressen kennt.
