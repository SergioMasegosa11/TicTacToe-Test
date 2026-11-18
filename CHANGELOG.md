# Changelog

Alle wichtigen Änderungen an TicTacTest werden in dieser Datei dokumentiert.
Das Format orientiert sich an [Keep a Changelog](https://keepachangelog.com/de/1.1.0/),
die Versionsnummern folgen [Semantic Versioning](https://semver.org/lang/de/) (siehe `RELEASE.md`).

## [Unreleased]

## [1.0.4] - 2026-11-18

Reines Frontend-Release (Nachweis der Deployment-Trennung, Auftrag 3 Teil 6):
Akzentfarbe der Webseite von Grün auf Blau geändert. Am Spiel/Backend hat sich
nichts geändert - das Deployment darf nur das Frontend ausrollen.

### Changed
- Frontend: Akzentfarbe von Grün (#95c11e) auf Blau (#4da3ff)

## [1.0.3] - 2026-11-04

Deployment als Web-App. Am Spiel selbst hat sich nichts geändert (weiterhin RandomPlayer
als Standard-Gegner; GreedyPlayer bleibt wie vom Auftrag verlangt jederzeit verfügbar, um
ihn in `main()` fest einzuschalten).

### Added (für Entwickler)
- OpenTofu-IaC (`infra/`) für eine EC2-Instanz im AWS Learner Lab, siehe `docs/iac.md`
- Web-Konsole (ttyd) macht das Release-JAR unverändert im Browser spielbar (`deploy/`), siehe `docs/deployment.md`
- Deploy-Workflow (`.github/workflows/deploy.yml`): deployt bei jedem veröffentlichten Release nur, was sich seit dem letzten Deploy geändert hat (Backend und/oder Web-Konsole); eine frühere Version kann gezielt erneut deployt werden; ein Push auf `main` deployt nichts
- Infra-Workflow (`.github/workflows/infra.yml`): manuelles `plan`/`apply`/`destroy` der EC2-Infrastruktur, getrennt vom App-Deployment
- Statisches Frontend (`frontend/`): bettet die Web-Konsole ein, zeigt Backend- und Frontend-Version; wird als `tictactest-frontend-<version>.zip` mit jedem Release veröffentlicht und über einen S3-Bucket (Static Website) ausgeliefert, siehe `docs/webkonsole.md`
- Versionsabfrage per HTTP (`deploy/version-server.py`, Port 8080): die laufende Backend-Version stammt automatisch aus dem Release-Tag; die Deploy-Pipeline prüft sie nach jedem Backend-Deploy
- S3-Hosting, Instance Profile (LabInstanceProfile) und Port 8080 in der OpenTofu-Konfiguration; `config.js` des Frontends wird von der Infrastruktur geschrieben

## [1.0.2] - 2026-10-10

Verbesserter Release-Prozess. Am Spiel selbst hat sich nichts geändert.

### Added (für Entwickler)
- Release Notes werden automatisch erstellt: Changelog-Abschnitt plus von GitHub generierte Liste aller Pull Requests seit dem letzten Release, gruppiert nach Labels (`.github/release.yml`)
- Ein Release wird nur erstellt, wenn alle Tests erfolgreich sind: eigener Job `tests` (Unit-, E2E- und Mutation-Tests) und Prüfung der CI-Ergebnisse des getaggten Commits
- Schutz von `main`: Änderungen nur noch per Pull Request mit grünen Checks (Ruleset `.github/rulesets/main-schutz.json`, aktivierbar mit `scripts/protect-main.sh`)

## [1.0.1] - 2026-09-30

Fehlerkorrekturen: Das Spiel stürzt bei Tippfehlern nicht mehr ab, und der Computer-Gegner ist nicht mehr vorhersehbar.

### Changed
- Der Computer-Gegner (O) wählt jetzt ein zufälliges freies Feld, statt immer das tiefste freie Feld (0, 1, 2, …) zu nehmen (BUG-06)
- Eine ungültige Eingabe beendet das Spiel nicht mehr, sondern wird mit einer Meldung abgelehnt, und der Spieler wird erneut gefragt
- Erlaubt ist nur noch genau eine Ziffer `0`–`8`; Leerzeichen davor und danach werden ignoriert

### Fixed
- Absturz bei Text, leerer Eingabe, Zahlen ausserhalb von 0–8 oder einem besetzten Feld (BUG-01)
- `+4`, `04` und andere Unicode-Ziffern wurden als Feld 4 akzeptiert; die Eingabe konnte über den Stacktrace Terminal-Steuerzeichen ausgeben (BUG-02)
- Absturz, wenn die Eingabe endet (z.B. Ctrl+Z / Ctrl+D): das Spiel endet jetzt mit `game aborted: no more input` (BUG-03)
- Denial of Service: eine endlos lange Eingabezeile liess das Spiel hängen; jetzt sind höchstens 10 ungültige Versuche und begrenzt lange Zeilen möglich (BUG-04)
- Tippfehler in der Eingabeaufforderung `where to to put` (BUG-05)
- Bei einem besetzten Feld kommt jetzt eine klare Meldung, z.B. `field 4 is already taken by CIRCLE, please choose a free field` (BUG-07)

### Added (für Entwickler)
- End-to-End-Tests mit JUnit Pioneer im eigenen Gradle-Task `e2eTest` und in der CI-Pipeline
- Mutation Testing mit PIT (Mutation Score 99 %)
- Optionaler Startwert für den Zufall (`-Dtictactest.seed=<zahl>`), damit Spiele in Tests reproduzierbar sind

## [1.0.0] - 2026-09-23

Erste veröffentlichte Version von TicTacTest.

### Added
- Spielbares TicTacToe in der Konsole: ein Mensch (X) spielt gegen einen einfachen Computer-Gegner (O)
- Das Spielfeld wird nach jedem Zug mit den Feldnummern 0–8 angezeigt
- Gewinnerkennung für alle Reihen, Spalten und Diagonalen sowie Erkennung eines Unentschiedens
- Ungültige Züge (ausserhalb des Feldes oder auf ein besetztes Feld) beenden das Spiel mit einer Fehlermeldung
- Ausführbares JAR `tictactest-<version>.jar`, startbar mit `java -jar` (ab Java 21)
- Automatisierte Tests mit JUnit 5 und AssertJ, Line-Coverage 100 %
- Coverage-Verlauf auf GitHub Pages und Coverage Gate für Pull Requests
- DevContainer (Java 25, Gradle) für eine einheitliche Entwicklungsumgebung
- Automatischer Release-Prozess über Git-Tags (`RELEASE.md`)

### Fixed
- Gradle-Build im Alpine-DevContainer: Gradle lud ein nicht lauffähiges JDK herunter, statt das JDK 25 aus dem Container zu verwenden
- Shell-Skripte werden auch unter Windows mit LF-Zeilenenden ausgecheckt und laufen dadurch im Container

[Unreleased]: https://github.com/SergioMasegosa11/TicTacToe-Test/compare/v1.0.3...HEAD
[1.0.3]: https://github.com/SergioMasegosa11/TicTacToe-Test/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/SergioMasegosa11/TicTacToe-Test/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/SergioMasegosa11/TicTacToe-Test/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/SergioMasegosa11/TicTacToe-Test/releases/tag/v1.0.0
