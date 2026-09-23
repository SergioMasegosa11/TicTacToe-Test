# DevContainer: Aufbau, Versionierung und Freigabe

Dieses Dokument beschreibt den ganzen Prozess rund um den DevContainer von TicTacTest,
vom Dockerfile bis zur automatischen Verwendung in der CI/CD-Pipeline und lokal in VS Code.

## Übersicht

```text
 Dockerfile + VERSION ändern
          │  Pull Request
          ▼
 DevContainer Release (PR)      baut + testet das Image, prüft "Version erhöht?"
          │  Review + Merge auf main
          ▼
 DevContainer Release (main)    baut + testet, pusht ghcr.io/…/tictactoe-test:vX.Y.Z
          │                     und erstellt automatisch einen Update-PR
          ▼
 Update-PR "DevContainer auf vX.Y.Z aktualisieren"
          │  CI läuft mit dem neuen Image → Review + Merge  = FREIGABE
          ▼
 devcontainer.json auf main zeigt auf vX.Y.Z
          ├──► alle CI-Jobs verwenden vX.Y.Z
          └──► lokale DevContainer verwenden vX.Y.Z (nach git pull → Rebuild)
```

## Beteiligte Dateien

| Datei | Zweck |
|---|---|
| `.devcontainer/Dockerfile` | Definition des Images: Alpine 3.24, Java 25, Gradle, Git, Bash, User `devuser` (UID:GID 1000:1000) |
| `.devcontainer/VERSION` | Version, die beim nächsten Release gebaut wird (SemVer `X.Y.Z`) |
| `.devcontainer/devcontainer.json` | **Einzige Stelle** mit der verwendeten (freigegebenen) Image-Version, z.B. `…/tictactoe-test:v1.0.0`, und die VS Code Extensions |
| `scripts/devcontainer-image.sh` | Liest das Image aus `devcontainer.json` und bricht ab, wenn es keine freigegebene `vX.Y.Z`-Version ist |
| `.github/workflows/devcontainer-image.yml` | Wiederverwendbarer Workflow: liefert das Image an alle CI-Jobs |
| `.github/workflows/devcontainer-release.yml` | Baut, testet und veröffentlicht das Image und erstellt den Update-PR |
| `.github/workflows/jaCoCo.yml`, `coverage-gate.yml`, `coverage-pages.yml` | CI-Jobs, die im DevContainer-Image laufen |

## Auftrag 1: DevContainer

- Das Image basiert auf `alpine:3.24` (feste Version, damit Builds reproduzierbar sind).
- Installiert: `openjdk25`, `gradle`, `git`, `bash`, dazu `libstdc++`/`procps` (für den
  VS Code Server) und `rsync` (für die GitHub-Pages-Deploy-Action).
- JUnit 5 und AssertJ kommen als Abhängigkeiten über `build.gradle`.
- Benutzer `devuser` mit UID:GID `1000:1000`. Lokal arbeitet VS Code als dieser Benutzer
  (`remoteUser`), in der CI laufen die Jobs als root (GitHub Actions setzt das voraus).
- VS Code Extensions: Java Extension Pack, Gradle, GitHub Actions, YAML.
- Die Java-Extension verwendet das JDK aus dem Image (`java.jdt.ls.java.home`).

**Lokal starten:** VS Code → Extension *Dev Containers* → *Reopen in Container*.

Ohne VS Code:

```bash
docker run -it --rm -v "${PWD}:/workspace" ghcr.io/sergiomasegosa11/tictactoe-test:v1.0.0 bash
./gradlew test
```

## Auftrag 2: Container von Hand bauen und in die GHCR hochladen

Einmalig nötig ist ein GitHub Personal Access Token (classic) mit dem Scope `write:packages`.

```bash
# 1. Image bauen und taggen
docker build -f .devcontainer/Dockerfile \
  -t ghcr.io/sergiomasegosa11/tictactoe-test:v1.0.0 \
  -t ghcr.io/sergiomasegosa11/tictactoe-test:latest \
  .devcontainer

# 2. In die GitHub Container Registry hochladen
docker login ghcr.io -u SergioMasegosa11   # Passwort = Personal Access Token
docker push ghcr.io/sergiomasegosa11/tictactoe-test:v1.0.0
docker push ghcr.io/sergiomasegosa11/tictactoe-test:latest
```

3. In der Pipeline wird das Image über `container:` verwendet, z.B. in `jaCoCo.yml`:

```yaml
jobs:
  devcontainer:
    uses: ./.github/workflows/devcontainer-image.yml   # liest devcontainer.json

  test:
    needs: devcontainer
    runs-on: ubuntu-latest
    container:
      image: ${{ needs.devcontainer.outputs.image }}
      credentials:
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
```

`setup-java` wird nicht mehr gebraucht, Java und Gradle kommen aus dem Image.

## Auftrag 3: Continuous Deployment

### Versionierungskonzept

Die Images werden nach **Semantic Versioning** getaggt: `vMAJOR.MINOR.PATCH`.

| Teil | Wann erhöhen? | Beispiele |
|---|---|---|
| **MAJOR** | Änderung, die bestehende Builds brechen kann | neue Java-Hauptversion, Wechsel Alpine → Ubuntu, anderer Benutzer |
| **MINOR** | neues Werkzeug oder neue Funktion, rückwärtskompatibel | zusätzliches Paket, neue Extension-Voraussetzung |
| **PATCH** | kleine Korrektur ohne neue Funktion | Neubau wegen Sicherheitsupdates, Tippfehler, Labels |

Pro Release werden diese Tags in die GHCR gepusht:

| Tag | Verwendung |
|---|---|
| `v1.2.3` | **Einziger Tag, der verwendet werden darf.** Unveränderlich, wird nie überschrieben |
| `sha-abc1234` | Nachvollziehbarkeit: aus welchem Commit das Image gebaut wurde |
| `latest` | Nur zum Ausprobieren von Hand, **nie** in CI oder `devcontainer.json` |

### Freigabeprozess: keine nicht freigegebenen Container verwenden

Ein Image gilt erst als **freigegeben**, wenn seine Version in `devcontainer.json` auf `main`
eingetragen ist. Das wird so sichergestellt:

1. **Nur main veröffentlicht.** Bei einem Pull Request wird das Image nur gebaut und getestet,
   nie gepusht. Auf `main` kommt eine Änderung nur per Review und Merge.
2. **Nur getestete Images werden gepusht.** Vor dem Push prüft der Workflow Java, Gradle und
   den Benutzer 1000:1000 und führt `./gradlew test` im neuen Image aus.
3. **Versionen sind unveränderlich.** Existiert `vX.Y.Z` schon in der GHCR, wird nichts
   überschrieben. Im PR muss `.devcontainer/VERSION` höher sein als auf `main`.
4. **Freigabe per Update-PR.** Nach dem Push erstellt der Workflow automatisch einen PR, der
   `devcontainer.json` auf die neue Version setzt. Die CI läuft darin schon mit dem neuen
   Image. Erst mit dem Merge ist die Version freigegeben.
5. **Nur `vX.Y.Z` erlaubt.** `scripts/devcontainer-image.sh` bricht jeden CI-Job ab, wenn in
   `devcontainer.json` etwas anderes steht (z.B. `latest` oder `sha-…`).

### Automatisch die neueste Version verwenden

- **CI:** Alle Jobs lesen das Image zur Laufzeit aus `devcontainer.json`
  (`devcontainer-image.yml`). Nach dem Merge des Update-PRs nutzen sie also sofort die neue
  Version, ohne dass Workflow-Dateien angepasst werden müssen.
- **Lokal:** Nach `git pull` erkennt VS Code die geänderte `devcontainer.json` und bietet
  *Rebuild Container* an. Dabei wird die neue Version aus der GHCR geladen. Weil jede Version
  einen eigenen Tag hat, gibt es keine veralteten, zwischengespeicherten `latest`-Images.

### Ablauf für ein neues Release (Beispiel: Paket hinzufügen)

1. Branch erstellen, `.devcontainer/Dockerfile` ändern.
2. `.devcontainer/VERSION` erhöhen, z.B. `1.0.0` → `1.1.0`.
3. PR eröffnen → *DevContainer Release* baut und testet das Image.
4. PR mergen → *DevContainer Release* pusht `v1.1.0` und erstellt den PR
   *DevContainer auf v1.1.0 aktualisieren*.
5. CI im Update-PR prüfen und mergen → `v1.1.0` ist freigegeben und wird überall verwendet.

### Einmalige Einstellungen im Repository

| Einstellung | Wo | Warum |
|---|---|---|
| *Allow GitHub Actions to create and approve pull requests* | Settings → Actions → General → Workflow permissions | Damit der Workflow den Update-PR erstellen darf |
| Package mit dem Repository verknüpft | Package *tictactoe-test* → Package settings → *Manage Actions access* | Damit die Workflows das Image pullen und pushen dürfen (passiert automatisch durch das Label `org.opencontainers.image.source`) |
| optional: Secret `DEVCONTAINER_PR_TOKEN` | Settings → Secrets → Actions | PAT mit `repo`-Scope. Damit startet die CI im Update-PR ganz normal. Ohne PAT startet der Workflow JaCoCo und das Coverage Gate per `workflow_dispatch` selbst |
