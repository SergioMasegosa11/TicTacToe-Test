# Changelog

Alle wichtigen Änderungen an TicTacTest werden in dieser Datei dokumentiert.
Das Format orientiert sich an [Keep a Changelog](https://keepachangelog.com/de/1.1.0/),
die Versionsnummern folgen [Semantic Versioning](https://semver.org/lang/de/) (siehe `RELEASE.md`).

## [Unreleased]

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

[Unreleased]: https://github.com/SergioMasegosa11/TicTacToe-Test/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/SergioMasegosa11/TicTacToe-Test/releases/tag/v1.0.0
