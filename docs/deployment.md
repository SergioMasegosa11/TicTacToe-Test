# Deployment: TicTacTest als Web-App

Dieses Dokument beschreibt, wie TicTacTest auf der EC2-Instanz aus [`docs/iac.md`](iac.md)
als Web-App läuft: das Release-JAR unverändert in einer Web-Konsole (ttyd) im Browser
spielbar, ohne dass die Infrastruktur dafür bei jedem Deploy neu erstellt wird.

## Übersicht

```text
infra.yml (manuell, selten)                 deploy.yml (automatisch, pro Release)
┌──────────────────────────────┐            ┌─────────────────────────────────────┐
│ tofu apply                   │            │ release: published                   │
│  -> EC2-Instanz, Security    │            │  -> JAR aus dem Release herunterladen│
│     Group, Key-Pair, EIP     │            │     (NICHT neu gebaut, RELEASE.md)   │
│  -> cloud-init installiert   │  einmalig  │  -> Diff seit letztem Deploy prüfen   │
│     ttyd + Platzhalter-Dienst│  bereitet  │     (Backend? Frontend? beides?)      │
│                               │  vor  →    │  -> nur das per SSH aktualisieren,    │
│ bleibt stehen, bis            │            │     Dienst neu starten               │
│ "tofu destroy"                │            │  -> Deployment in GitHub vermerken    │
└──────────────────────────────┘            └─────────────────────────────────────┘
```

Ein **Push auf `main` deployt nichts** (`deploy.yml` hat keinen `push`-Trigger). Nur ein
veröffentlichtes Release (Tag `vX.Y.Z`, siehe [`RELEASE.md`](../RELEASE.md)) oder ein
manueller, bewusst ausgewählter Tag löst ein Deployment aus.

## Secrets-Konzept

| Secret | Wofür | Workflow | Woher |
|---|---|---|---|
| `AWS_ACCESS_KEY_ID` | Zugriff auf AWS (EC2 erstellen/ändern/löschen) | `infra.yml` | AWS Learner Lab → "AWS Details" |
| `AWS_SECRET_ACCESS_KEY` | wie oben | `infra.yml` | AWS Learner Lab → "AWS Details" |
| `AWS_SESSION_TOKEN` | wie oben (Learner-Lab-Zugänge sind **temporär**, Session Token zusätzlich zu Access Key/Secret nötig) | `infra.yml` | AWS Learner Lab → "AWS Details" |
| `EC2_HOST` | IP/DNS der Instanz, Ziel für SSH/SCP | `deploy.yml` | Output `public_ip` von `tofu apply` |
| `EC2_USER` | Login-Benutzer für SSH (z.B. `ec2-user` bei Amazon Linux) | `deploy.yml` | AMI-Doku |
| `EC2_SSH_PRIVATE_KEY` | privater SSH-Schlüssel für den Zugriff auf die Instanz | `deploy.yml` | Output `ssh_private_key` von `tofu apply` (`tofu output -raw ssh_private_key`) |
| `GITHUB_TOKEN` | JAR aus dem Release laden, Deployment vermerken | beide | automatisch von GitHub Actions bereitgestellt |

Grundsatz (siehe auch `docs/iac.md`, Abschnitt State/Git): **keines** dieser Secrets landet
je in einer Datei im Repository, sondern ausschliesslich als GitHub-Actions-Secret.

**Learner-Lab-Besonderheit:** Die AWS-Zugangsdaten laufen nach wenigen Stunden ab. Deshalb
sind `infra.yml` und `deploy.yml` bewusst getrennt: `infra.yml` braucht die (kurzlebigen)
AWS-Secrets und läuft nur manuell, wenn die Infrastruktur wirklich geändert werden soll.
`deploy.yml` braucht dagegen nur den (langlebigen) SSH-Schlüssel, keine AWS-Zugangsdaten,
und kann darum bei jedem Release automatisch laufen, auch wenn die Learner-Lab-Session
gerade abgelaufen ist.

## Ablauf im Detail

### 1. Infrastruktur einmalig bereitstellen (`infra.yml` oder lokal `tofu apply`)

Siehe `docs/iac.md`. Ergebnis: eine laufende EC2-Instanz mit SSH-Zugriff, Security Group für
Port 22 (SSH) und 7681 (ttyd), und einem von cloud-init eingerichteten, aber noch leeren
`tictactest-console`-Dienst ("Noch kein Release deployt").

### 2. App deployen (`deploy.yml`, automatisch bei jedem Release)

1. **Diff ermitteln:** Der Job `diff` fragt über die GitHub-Deployments-API den zuletzt
   erfolgreich deployten Tag ab und vergleicht ihn mit dem neuen Tag
   (`scripts/deploy-diff.sh`):
   - `src/main/**`, `build.gradle`, `gradle.properties` geändert → **Backend** (`backend=true`)
   - `deploy/**` (Bootstrap-/Wrapper-Skripte für die Web-Konsole) geändert → **Frontend** (`frontend=true`)
   - Gibt es noch keinen Vorgänger, oder wurde `force` gewählt, gilt beides als geändert.
2. **Deployen, nur wenn nötig:** Der Job `deploy` läuft nur, wenn `backend` oder `frontend`
   `true` ist. Betrifft es nur das Backend, wird nur das neue JAR hochgeladen; betrifft es
   nur das Frontend, werden nur `deploy/bootstrap.sh`/`deploy/run-tictactest.sh`
   aktualisiert. Danach setzt `bootstrap.sh <version>` in jedem Fall den `current`-Symlink
   auf die neu deployte Version und startet den systemd-Dienst `tictactest-console` neu -
   die **Infrastruktur selbst wird dabei nicht verändert**.
3. **Vermerken:** Ein neuer GitHub-Deployment-Eintrag (`ref=<Tag>`, Environment
   `production`) wird angelegt, damit der nächste Lauf wieder den richtigen Vergleichspunkt
   findet.

### 3. Eine frühere Version gezielt erneut deployen

*Actions → Deploy (Web-Konsole) → Run workflow*, mit dem gewünschten `tag` (z.B. `v1.0.2`)
und `force: true`. `force` überspringt den Diff-Vergleich, lädt genau dieses Release-JAR
und setzt den `current`-Symlink darauf zurück - ohne etwas neu zu bauen oder die
Infrastruktur anzufassen.

## Reflexion

### Warum getrennte Workflows für Infrastruktur und App?

Weil sie sich in Häufigkeit, Risiko und benötigten Zugangsdaten stark unterscheiden:
Infrastruktur ändert sich selten und bewusst (braucht die kurzlebigen AWS-Zugangsdaten und
einen `plan`-Blick vorher), ein App-Deploy soll dagegen bei jedem Release unkompliziert und
automatisch laufen (braucht nur SSH). Diese Trennung ist genau das, was der
IaC-Auftrag ("Infrastruktur, CI/CD, spätere Deployment-Umgebungen") vorbereitet hat.

### Warum nur deployen, was sich geändert hat?

Ein Deploy ist ein Eingriff auf einem laufenden System (Datei kopieren, Dienst neu
starten - kurze Downtime der Web-Konsole). Ändert sich z.B. nur `CHANGELOG.md`, braucht es
gar kein Deployment; ändert sich nur die Web-Konsole, muss nicht das (unveränderte) JAR neu
hochgeladen werden. Das macht Deploys schneller, nachvollziehbarer (die Logs zeigen genau,
was diesmal passiert ist) und reduziert unnötige Neustarts des laufenden Spiels.

### Promotion-Strategie: build once, deploy many

Der Deploy-Workflow baut **nie** selbst ein JAR - er lädt immer ein bereits im
Release-Prozess gebautes und getestetes Artefakt (`tictactest-X.Y.Z.jar`) herunter und
prüft dessen Checksumme. Dasselbe, unveränderte Artefakt würde man in einer grösseren
Pipeline durch mehrere Umgebungen reichen, statt für jede Umgebung neu zu bauen:

```text
Release-Tag vX.Y.Z (RELEASE.md: Build + Tests einmal, JAR + Checksum als Artefakt)
        │
        ▼
   deploy (dev)      - automatisch bei jedem Release, kleine/instabile Umgebung
        │  manuelle Freigabe (GitHub-Environment-Protection-Rule)
        ▼
   deploy (staging)  - gleiches JAR, eigene infra/*.tfvars bzw. OpenTofu-Workspace
        │  manuelle Freigabe, ggf. zusätzliche Tests/Abnahme
        ▼
   deploy (prod)     - gleiches JAR, eigene Secrets/Environment, strengere Freigabe
```

Für diesen Lernschritt gibt es nur eine Umgebung (`production`, eine einzelne EC2-Instanz).
Der Ausbau auf echte `dev`/`staging`/`prod`-Stufen würde bedeuten:

- **GitHub Environments** pro Stufe, mit eigenen Secrets (`EC2_HOST` etc. pro Umgebung) und
  zunehmend strengeren Freigabe-Regeln (z.B. Pflicht-Review vor `prod`).
- **Eigener State pro Umgebung** in `infra/` (z.B. `dev.tfvars`/`staging.tfvars`/
  `prod.tfvars` oder OpenTofu-Workspaces), damit ein `tofu apply` für `dev` nie versehentlich
  `prod` verändert.
- **Dasselbe JAR** wandert durch alle Stufen (dieser Workflow lädt es bereits pro Tag von
  der Release-Seite statt es neu zu bauen) - das ist die eigentliche Promotion: nicht neu
  bauen, sondern eine bereits geprüfte Version weiterreichen.
- Ein Redeploy einer älteren Version (siehe oben, `force: true`) ist in diesem Modell auch
  ein Rollback-Werkzeug: dieselbe Mechanik, die eine ältere Version erneut ausrollt, würde
  im Ernstfall auch eine fehlerhafte `prod`-Version schnell durch die letzte funktionierende
  ersetzen.

### Was würde ich als Nächstes verbessern?

- Eine zweite, kleinere Instanz als echte `staging`-Umgebung, bevor `production` aktualisiert
  wird (aktuell gibt es nur eine Stufe).
- `allowed_cidr` in `infra/variables.tf` einschränken, statt für SSH `0.0.0.0/0` zu erlauben
  (aktuell bewusst offen gehalten, damit der Zugriff von überall zum Testen funktioniert).
- Ein Remote-State-Backend (z.B. S3) für `infra/`, sobald mehrere Personen/Umgebungen den
  gleichen State teilen (siehe `docs/iac.md`, Abschnitt 7).
