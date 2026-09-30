# Release-Prozess für TicTacTest

Diese Anleitung beschreibt, wie aus einem Stand des Git-Repositories eine veröffentlichte Version
von TicTacTest entsteht. Sie ist so geschrieben, dass jede Person im Team einen Release
durchführen kann.

## Kurzfassung

```bash
# auf main, alles gemergt, Version + Changelog im PR angepasst
git checkout main && git pull
git tag -a v1.1.0 -m "Release v1.1.0"
git push origin v1.1.0
# → GitHub Actions baut, testet und veröffentlicht den Release mit JAR
```

## Ablauf

```text
Entwicklung (Feature-Branch)
   ↓  Pull Request (CI: Tests, Coverage Gate) → Review → Merge
Änderungen auf main
   ↓
Release-PR: version in gradle.properties + CHANGELOG.md anpassen → Merge
   ↓
Git-Tag vX.Y.Z auf dem Commit in main erstellen und pushen
   ↓
GitHub Actions "Release" startet
   ↓  Bedingungen prüfen → Tests → Build → JAR erstellen → JAR starten
GitHub Release mit tictactest-X.Y.Z.jar (+ SHA-256)
   ↓
JAR herunterladen und testen
```

## Fragen und Antworten

### Wann wird ein Release erstellt?

Bewusst dann, wenn ein Stand auf `main` an Benutzer weitergegeben werden soll, z.B. nach einer
neuen Funktion oder einer wichtigen Fehlerkorrektur. Nicht nach jedem Merge.

### Wer beziehungsweise was startet einen Release?

Eine Person aus dem Team, indem sie einen **Release-Tag** (`vX.Y.Z`) pusht. Der Tag startet den
Workflow [`.github/workflows/release.yml`](.github/workflows/release.yml). Alternativ kann der
Workflow unter *Actions → Release → Run workflow* für einen **bestehenden** Tag erneut gestartet
werden (z.B. wenn ein Lauf wegen eines GitHub-Problems abgebrochen ist).

### Warum löst nicht jeder Push auf `main` einen Release aus?

Der Release-Workflow reagiert nur auf Tags der Form `vX.Y.Z` (`on: push: tags`), nicht auf
Branches. Auf `main` landen auch Zwischenstände, Refactorings oder Dokumentation. Ein Release
ist eine bewusste Entscheidung: Version, Changelog und Zeitpunkt werden von Menschen festgelegt.

### Wie wird die Versionsnummer festgelegt?

Nach **Semantic Versioning** `MAJOR.MINOR.PATCH`. Die Version steht an genau einer Stelle:
`version=` in [`gradle.properties`](gradle.properties). Von dort bekommen das JAR seinen Namen
(`tictactest-1.0.0.jar`) und sein Manifest (`Implementation-Version`). Der Git-Tag ist dieselbe
Version mit einem `v` davor (`v1.0.0`).

| Teil | Wann erhöhen? | Beispiel für TicTacTest |
|---|---|---|
| **MAJOR** (`2.0.0`) | Inkompatible Änderung: Bestehende Benutzung funktioniert nicht mehr wie vorher | anderes Eingabeformat (z.B. `A1` statt `0`–`8`), neue minimale Java-Version, `TicTacToePlayer`-Interface geändert |
| **MINOR** (`1.1.0`) | Neue Funktion, rückwärtskompatibel | neuer Computer-Gegner, Spielstand-Anzeige, neue Kommandozeilen-Option |
| **PATCH** (`1.0.1`) | Fehlerkorrektur ohne neue Funktion | falsche Gewinnerkennung korrigiert, Tippfehler in der Ausgabe, bessere Fehlermeldung |

Beim Erhöhen werden die hinteren Stellen auf 0 gesetzt: `1.2.3` → `1.3.0` → `2.0.0`.

> **Hinweis:** Die Versionen des DevContainer-Images (`.devcontainer/VERSION`, siehe
> [`docs/devcontainer.md`](docs/devcontainer.md)) sind davon unabhängig. Sie sind Tags in der
> GitHub Container Registry, keine Git-Tags, und lösen keinen Release aus.

### Wie wird ein Release-Tag erstellt?

Als **annotierter** Tag auf dem aktuellen Commit von `main`:

```bash
git checkout main
git pull
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

Prüfen, auf welchen Commit der Tag zeigt:

```bash
git show v1.0.0 --no-patch        # lokal
```

Auf GitHub: *Code → Tags* bzw. auf der Release-Seite steht der Commit direkt in den Release-Notes.

Ein veröffentlichter Tag wird **nie verschoben oder gelöscht**. Bei einem Fehler wird eine neue
Version erstellt (z.B. `v1.0.1`).

### Welcher Commit gehört zu einem Release?

Genau der Commit, auf den der Tag zeigt. Der Workflow checkt diesen Commit aus, baut daraus das
JAR und schreibt die Commit-ID in die Release-Notes. Er prüft zusätzlich, dass der Commit auf
`main` liegt. Releases von Feature-Branches werden abgelehnt.

### Wie wird das JAR gebaut?

Mit Gradle (`./gradlew build`). In [`build.gradle`](build.gradle) ist festgelegt:

- Name `tictactest-<version>.jar` (aus `rootProject.name` und `version`)
- `Main-Class: ch.bbw.m450.tictactoe.TicTacToeMain` im Manifest, darum startbar mit `java -jar`
- Bytecode für Java 21: Das JAR läuft mit jeder Java-Runtime ab Version 21

Lokal ausprobieren:

```bash
./gradlew clean build
java -jar build/libs/tictactest-1.0.0.jar
```

### Wo kann das JAR heruntergeladen werden?

Auf der Release-Seite des Repositories:
<https://github.com/SergioMasegosa11/TicTacToe-Test/releases>

Jeder Release enthält unter *Assets*:

- `tictactest-X.Y.Z.jar`: die ausführbare Anwendung
- `tictactest-X.Y.Z.jar.sha256`: SHA-256-Prüfsumme zum Überprüfen des Downloads

### Was enthält das Changelog?

[`CHANGELOG.md`](CHANGELOG.md) beschreibt pro Version, was sich für Benutzer und Entwickler
geändert hat, nicht einfach eine Liste aller Commits:

- Versionsnummer und Datum des Releases (`## [1.1.0] - 2026-10-01`)
- `### Added`: neue Funktionen
- `### Changed`: Änderungen bestehender Funktionen
- `### Fixed`: behobene Fehler (mit Issue-Nummer, falls vorhanden, z.B. `(#12)`)

Neue Einträge werden laufend unter `## [Unreleased]` gesammelt und beim Release in eine
Versionsnummer umbenannt. Der Abschnitt der Version wird automatisch als Release-Notes auf GitHub
übernommen.

### Was passiert, wenn beim Release ein Test fehlschlägt?

Der Workflow bricht sofort ab. Es wird **kein** GitHub Release erstellt und kein JAR
veröffentlicht. Der Tag existiert dann zwar, zeigt aber auf einen fehlerhaften Stand. Vorgehen:

1. Fehler auf einem Branch beheben, per PR nach `main` mergen.
2. Version erhöhen (z.B. `1.0.0` → `1.0.1`), Changelog anpassen.
3. Neuen Tag `v1.0.1` erstellen und pushen.

Der fehlgeschlagene Tag kann gelöscht werden, solange es dazu **keinen** veröffentlichten
Release gibt (`git push --delete origin v1.0.0`, danach `git tag -d v1.0.0`).

Dasselbe gilt, wenn eine andere Bedingung nicht erfüllt ist, z.B. Tag passt nicht zu
`gradle.properties` oder es fehlt der Changelog-Eintrag.

## Bedingungen vor einem Release

| Bedingung | Wie sichergestellt |
|---|---|
| Die Anwendung ist auf dem gewünschten Stand | Entscheidung im Team; alle geplanten PRs sind gemergt |
| Alle Änderungen sind auf `main` | Workflow prüft: Tag-Commit muss auf `main` liegen |
| Das Projekt lässt sich bauen | Workflow: `./gradlew build`; vorher schon CI bei jedem Push/PR |
| Alle Tests sind erfolgreich | Workflow: `./gradlew test` bricht bei einem Fehler ab; Coverage Gate im PR |
| Das Changelog ist aktualisiert | Workflow prüft: Eintrag `## [X.Y.Z]` muss in `CHANGELOG.md` existieren |
| Die Versionsnummer ist festgelegt | Workflow prüft: Tag `vX.Y.Z` muss zu `version=X.Y.Z` in `gradle.properties` passen |
| Das JAR funktioniert | Workflow startet das fertige JAR mit `java -jar` und spielt ein Spiel durch |

## Schritt für Schritt: neuen Release erstellen

1. **Release-PR vorbereiten** (Branch z.B. `release/1.1.0`):
   - `gradle.properties`: `version=1.1.0`
   - `CHANGELOG.md`: `## [Unreleased]` → `## [1.1.0] - JJJJ-MM-TT`, darüber einen neuen leeren
     `## [Unreleased]`-Abschnitt, unten die Links ergänzen
2. **PR erstellen**, CI abwarten (Tests, Coverage Gate), mergen.
3. **Tag erstellen und pushen:**
   ```bash
   git checkout main && git pull
   git tag -a v1.1.0 -m "Release v1.1.0"
   git push origin v1.1.0
   ```
4. **Workflow beobachten:** *Actions → Release*. Bei Rot: siehe „Was passiert, wenn beim Release
   ein Test fehlschlägt?“.
5. **Release testen:** Release-Seite öffnen, JAR herunterladen und starten:
   ```bash
   java -jar tictactest-1.1.0.jar
   ```
   Optional die Prüfsumme kontrollieren:
   ```bash
   sha256sum -c tictactest-1.1.0.jar.sha256          # Linux / Git Bash
   Get-FileHash tictactest-1.1.0.jar -Algorithm SHA256   # PowerShell
   ```
   Erst wenn das heruntergeladene JAR funktioniert, gilt der Release als erfolgreich.

## Wie entsteht aus einem Git-Stand eine veröffentlichte Version?

- **Nachvollziehbar:** Der Tag `vX.Y.Z` zeigt unveränderlich auf einen Commit. Die Release-Notes
  nennen Tag, Commit und Änderungen aus dem Changelog.
- **Reproduzierbar:** Der Build läuft automatisch in GitHub Actions mit festgelegter Java-Version
  und dem Gradle-Wrapper (feste Gradle-Version). Aus demselben Tag entsteht immer ein JAR mit
  demselben Quellcode, denselben Abhängigkeiten und derselben Version.
- **Kontrolliert:** Nur ein bewusst gepushter Tag startet den Release. Der Workflow veröffentlicht
  nur, wenn Tag-Version, Changelog und `main` zusammenpassen und alle Tests und der JAR-Test
  erfolgreich sind.
