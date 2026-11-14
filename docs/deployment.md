# Deployment: TicTacTest als Web-App

Dieses Dokument beschreibt, wie TicTacTest auf der Infrastruktur aus
[`docs/iac.md`](iac.md) läuft und deployt wird: das Release-JAR unverändert in einer
Web-Konsole (ttyd, siehe [`docs/webkonsole.md`](webkonsole.md)) auf der EC2-Instanz, das
statische Frontend in einem S3-Bucket. Zentrale Regel: **Infrastruktur und Applikation
sind getrennt** - ein neuer Release erfordert weder `tofu apply` noch eine neue Instanz.

## Übersicht

```text
Browser ───────────────► Frontend  (S3 Static Website: index.html, app.js, version.js,
   │                                config.js ← von OpenTofu geschrieben)
   │ iframe / fetch
   ▼
EC2-Instanz (Elastic IP)
   ├─ ttyd :7681 ── run-tictactest.sh ── java -jar current/tictactest-X.Y.Z.jar
   └─ version-server.py :8080 ── current/version.txt   (laufende Version per HTTP)

infra/ (OpenTofu, selten, bewusst)        deploy.yml (automatisch, pro Release)
┌────────────────────────────────┐        ┌──────────────────────────────────────┐
│ tofu apply                     │        │ release: published                    │
│  -> Instanz, SG, EIP, Key,     │        │  -> Diff seit letztem DEPLOYTEN Tag   │
│     S3-Bucket, config.js,      │ einmal │  -> Backend geändert?  JAR per SSH    │
│     LabInstanceProfile         │ ─────► │     drauf, Dienst-Restart, Versions-  │
│  -> cloud-init: ttyd, Java,    │        │     Check über :8080                  │
│     Platzhalter-Dienst         │        │  -> Frontend geändert? Release-Zip    │
│ bleibt stehen bis tofu destroy │        │     nach S3 (ohne config.js)          │
└────────────────────────────────┘        └──────────────────────────────────────┘
```

Ein **Push auf `main` deployt nichts** (`deploy.yml` hat keinen `push`-Trigger). Nur ein
veröffentlichtes Release (Tag `vX.Y.Z`, siehe [`RELEASE.md`](../RELEASE.md)) oder ein
manueller, bewusst ausgewählter Tag löst ein Deployment aus. Deployt werden immer die
**Artefakte aus dem Release** (JAR, Frontend-Zip) - nie ein neu gebauter Stand.

## Secrets-Konzept

### Welche Zugangsdaten braucht die Pipeline?

| Wert | Typ | Wofür | Woher | Gültigkeit |
|---|---|---|---|---|
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | **Secret** | `infra.yml` (tofu) und Frontend-Deploy nach S3 | AWS Learner Lab → "AWS Details", pro Session neu | **läuft mit der Lab-Session ab (~4 h)** |
| `EC2_SSH_PRIVATE_KEY` | **Secret** | Backend-Deploy per SSH/SCP auf die Instanz | `tofu output -raw ssh_private_key` (einmalig) | bis zum nächsten `tofu destroy` |
| `EC2_HOST`, `EC2_USER` | **Variable** | Ziel für SSH und Versions-Check | `tofu output public_ip` / AMI-Doku (`ec2-user`) | bis `destroy` (EIP bleibt stabil) |
| `S3_BUCKET_FRONTEND`, `FRONTEND_URL` | **Variable** | Ziel/Prüfung des Frontend-Deploys | `tofu output frontend_bucket` / `frontend_url` | bis `destroy` |
| `GITHUB_TOKEN` | automatisch | Release-Assets laden, Deployment vermerken | von GitHub Actions pro Lauf erzeugt | ein Workflow-Lauf |

**Secrets vs. Variables:** Secrets werden verschlüsselt gespeichert, in Logs maskiert und
sind nach dem Speichern nicht mehr lesbar - dorthin gehört alles, womit man sich
**authentifizieren** kann (Schlüssel, Tokens). Variables sind Klartext-Konfiguration -
dorthin gehört, was ohnehin öffentlich sichtbar ist (die IP steht in jeder Spiel-URL, der
Bucket-Name in der Frontend-URL). Wer alles in Secrets steckt, verliert Übersicht und
Log-Lesbarkeit ohne Sicherheitsgewinn; wer Zugangsdaten in Variables steckt, hat sie
veröffentlicht.

### Abgelaufene Learner-Lab-Credentials

Die AWS-Zugangsdaten laufen nach jeder Lab-Session ab. Konsequenzen:

- **Backend-Deploys funktionieren trotzdem:** sie brauchen nur SSH (langlebiger
  Schlüssel), keine AWS-Zugangsdaten. Das ist der häufigste Fall (neues JAR).
- **Frontend-Deploys und `infra.yml` schlagen mit abgelaufenen Credentials fehl** - und
  zwar sichtbar: der S3-Schritt bricht mit `ExpiredToken` ab, der Workflow wird rot, der
  Deployment-Vermerk wird nicht gesetzt (der nächste Lauf vergleicht also weiterhin mit
  dem letzten *erfolgreichen* Stand).
- **Nachdeployen:** neue Session starten, die drei AWS-Werte in den Repo-Secrets
  aktualisieren (Settings → Secrets and variables → Actions), dann *Actions → Deploy →
  Run workflow* mit dem Tag (und `force`, falls der Diff schon verbucht wäre). Es wird
  exakt dasselbe Release-Artefakt deployt - nichts wird neu gebaut.

### Wer sieht die Secrets? Logs, Forks, Pull Requests

- Lesen kann die Secret-Werte niemand mehr, auch Admins nicht - nur überschreiben.
  Benutzen kann sie, wer Workflows im Repo auslösen darf.
- In **Logs** maskiert GitHub registrierte Secrets (`***`). Trotzdem gilt: nie bewusst
  `echo $SECRET`, und **nach jedem Lauf die Logs prüfen** - Maskierung schützt nicht vor
  z.B. base64-kodierter Ausgabe.
- **Forks und Pull Requests** von Forks bekommen keine Secrets - sonst könnte jeder per
  PR einen Workflow einreichen, der sie exfiltriert. Deshalb deployt hier auch nichts an
  PRs: nur `release:published` und manuelles `workflow_dispatch`.

### Schaden im Ernstfall und Reaktion

- **AWS-Keys öffentlich:** jemand kann bis Session-Ende im Lab-Konto Ressourcen
  erstellen/löschen (Kosten, Datenverlust). Reaktion: Session beenden (invalidiert die
  Keys), Secrets rotieren, prüfen was erstellt wurde. In einem echten Konto wäre das ein
  Sicherheitsvorfall mit Kostenrisiko in beliebiger Höhe (Crypto-Mining) - Keys sofort
  deaktivieren, CloudTrail auswerten.
- **SSH-Key öffentlich:** Vollzugriff auf die Instanz (aber nur auf sie). Reaktion:
  `tofu destroy`/`apply` erzeugt ein neues Key-Paar, neues Secret setzen.
- Generell: Git-Historie gilt als unlöschbar - ein einmal committetes Secret ist
  kompromittiert, auch wenn der Commit später entfernt wird. Rotieren, nicht hoffen.

### Wie löst man das ohne Learner Lab? (OIDC)

In einem echten Setup speichert man **gar keine langlebigen AWS-Keys** in GitHub:
GitHub Actions holt sich per **OIDC** (OpenID Connect) pro Workflow-Lauf ein
kurzlebiges Token. AWS vertraut dabei einer konfigurierten Identität ("Repo X, Branch/
Tag Y darf Rolle Z übernehmen", `aws-actions/configure-aws-credentials` mit
`role-to-assume`). Vorteile: nichts zu rotieren, nichts läuft ab "zur falschen Zeit",
feingranulare Rechte pro Repo/Umgebung. Im Learner Lab nicht möglich, weil man keine
IAM-Rollen/Identity-Provider anlegen darf - genau deshalb die Krücke mit den
Session-Credentials als Secrets.

## Wie kommt das JAR auf die Instanz? (Entscheidung)

Gewählt: **per SSH/SCP von aussen** (GitHub Actions lädt das Release-Asset und kopiert es
auf die Instanz). Abwägung:

| Variante | Beurteilung |
|---|---|
| Instanz lädt direkt vom GitHub-Release | Scheitert, sobald das Repository privat ist (Token müsste AUF die Instanz). Instanz braucht Internet-Zugriff auf GitHub. Nach Reboot ok (JAR liegt lokal), aber der Versionswechsel müsste trotzdem von aussen angestossen werden. |
| privater S3-Bucket + Instance Profile | Sauber (keine Keys auf der Instanz), aber ein zusätzlicher Kopierschritt und Bucket nur dafür; die Pipeline bräuchte für JEDES Backend-Deploy gültige AWS-Credentials - im Learner Lab laufen die ständig ab. |
| **SSH/SCP von aussen (gewählt)** | Pipeline braucht nur den langlebigen SSH-Key (Learner-Lab-tauglich!), funktioniert mit privatem Repo (Token bleibt im Runner), Versionswechsel und Dienst-Restart passieren im selben Schritt. |

- **Nach einem Neustart** weiss die Instanz selbst, was laufen soll: der
  `current`-Symlink zeigt auf `/opt/tictactest/releases/<version>/` (JAR + `version.txt`
  liegen lokal auf der Platte), systemd startet beide Dienste automatisch.
- **Versionswechsel ohne neue Instanz:** neues JAR nach `releases/<neu>/` kopieren,
  `bootstrap.sh <neu>` setzt den Symlink um und startet den Dienst neu - die Instanz,
  ihre IP und die Infrastruktur bleiben unverändert (`tofu plan`: keine Änderungen).

**SSH vs. `aws ssm send-command` aus GitHub Actions:** SSM braucht keine offenen Ports
und keinen SSH-Key (Auth über AWS-Credentials + Instance Profile), Befehle und Ergebnisse
sind in AWS auditierbar - dafür bräuchte die Pipeline bei jedem Deploy gültige
AWS-Credentials, die im Learner Lab dauernd ablaufen, und Dateiübertragung ist mit SSM
umständlich (erst S3, dann herunterladen). SSH braucht Port 22 und ein Key-Secret, ist
dafür direkt (scp) und von den kurzlebigen Lab-Credentials unabhängig. Hier gewinnt SSH;
mit OIDC in einem echten Konto würde die Abwägung eher Richtung SSM kippen.

## Änderungen zwischen Releases erkennen

- **Pfad-Zuordnung** (siehe [`scripts/deploy-diff.sh`](../scripts/deploy-diff.sh)):
  `src/main/**`, `build.gradle`, `gradle.properties` → Backend (JAR-Inhalt);
  `deploy/**` → ebenfalls Backend (Betrieb des Dienstes auf der Instanz);
  `frontend/**` → Frontend. Doku, Tests, Workflows, `infra/` lösen **kein** Deployment
  aus - Infrastruktur ändert sich nur bewusst per `tofu apply`, nie als Nebenwirkung
  eines Releases.
- **Verglichen wird mit dem zuletzt DEPLOYTEN Tag** (GitHub-Deployments-API), nicht mit
  dem letzten Commit: zwischen zwei Releases liegen beliebig viele Commits - der
  Vergleich mit nur dem letzten Commit würde fast alle Änderungen übersehen. Und falls
  ein Release-Deploy fehlschlug, ist der richtige Vergleichspunkt der letzte wirklich
  ausgerollte Stand, nicht der letzte Tag.
- **Allererstes Release** (kein Vergleichspunkt): es wird vollständig deployt
  (Backend + Frontend) - dasselbe gilt bei `force: true`.
- **Warum hilft der `paths`-Filter von GitHub Actions nicht?** Er entscheidet nur, ob
  ein Workflow bei einem *Push-Event* überhaupt startet, und vergleicht dabei die
  gepushten Commits. Er kann weder zwei Release-Tags vergleichen noch "seit dem letzten
  erfolgreichen Deploy" berücksichtigen, und bei `release:`-Events greift er gar nicht.

## Manuelles Deployment (Vorlage für die Pipeline)

So wird von Hand deployt - exakt diese Schritte führt `deploy.yml` automatisch aus.
Vorbereitung einmalig: Frontend-Bucket per CLI erstellen (Learner-Lab-SCP verbietet
`s3:GetBucketObjectLockConfiguration`, das der AWS-Provider bei jedem Refresh eines
verwalteten `aws_s3_bucket` aufruft - Begründung in [`infra/s3.tf`](../infra/s3.tf)),
Namen in `terraform.tfvars` eintragen, dann `tofu apply`:

```bash
aws s3api create-bucket --bucket tictactest-frontend-<eindeutiges-suffix>
cd infra && tofu apply       # Website-Config, Policy, config.js, Instanz, EIP, ...
```

```bash
# --- Variablen aus den OpenTofu-Outputs ---
cd infra
IP=$(tofu output -raw public_ip)
BUCKET=$(tofu output -raw frontend_bucket)
tofu output -raw ssh_private_key > key.pem && chmod 600 key.pem
VERSION=1.0.2   # zu deployende Release-Version (Tag ohne "v")

# --- 1. Release-Assets herunterladen (NICHT bauen) ---
gh release download "v$VERSION" --pattern "tictactest-*"
sha256sum -c "tictactest-$VERSION.jar.sha256"

# --- 2. Backend: JAR + Betriebs-Skripte auf die Instanz, Dienst neu starten ---
ssh -i key.pem ec2-user@$IP "mkdir -p /opt/tictactest/releases/$VERSION /opt/tictactest/deploy-src"
scp -i key.pem "tictactest-$VERSION.jar" ec2-user@$IP:/opt/tictactest/releases/$VERSION/
scp -i key.pem ../deploy/bootstrap.sh ../deploy/run-tictactest.sh ../deploy/version-server.py \
    ec2-user@$IP:/opt/tictactest/deploy-src/
ssh -i key.pem ec2-user@$IP "chmod +x /opt/tictactest/deploy-src/bootstrap.sh && /opt/tictactest/deploy-src/bootstrap.sh $VERSION"

# --- 3. Versions-Check: läuft wirklich die neue Version? ---
curl "http://$IP:8080/version.txt"            # muss $VERSION ausgeben

# --- 4. Frontend: Release-Archiv nach S3 (config.js NIE anfassen) ---
sha256sum -c "tictactest-frontend-$VERSION.zip.sha256"
mkdir -p /tmp/frontend-dist && unzip -o "tictactest-frontend-$VERSION.zip" -d /tmp/frontend-dist
aws s3 sync /tmp/frontend-dist "s3://$BUCKET" --delete --exclude "config.js"

# --- 5. Prüfen ---
tofu output -raw frontend_url                 # im Browser öffnen und spielen
tofu plan                                     # muss "No changes" zeigen: das Deployment
                                              # hat die Infrastruktur nicht verändert
```

Für ein zweites Release (B): nur `VERSION` ändern und Schritte 1-5 wiederholen - **ohne
`tofu apply`, ohne neue Instanz**. `tofu plan` zeigt weiterhin keine Änderungen.

## Automatisches Deployment (`deploy.yml`)

1. **Diff:** Job `diff` holt den zuletzt deployten Tag über die Deployments-API und
   vergleicht (`scripts/deploy-diff.sh`) → Outputs `backend`/`frontend`.
2. **Backend** (nur wenn geändert): Release-JAR laden, Prüfsumme verifizieren, per
   SCP auf die Instanz, `bootstrap.sh <version>` (Symlink + Dienst-Restart). Danach
   pollt der Workflow bis zu 2 Minuten `http://<host>:8080/version.txt` - meldet die
   Instanz nicht **genau** die neue Version, schlägt der Workflow fehl.
3. **Frontend** (nur wenn geändert): Frontend-Zip aus dem Release laden, Prüfsumme
   verifizieren, `aws s3 sync --delete --exclude config.js` in den Bucket. Danach wird
   geprüft, dass `version.js` die neue Version ausliefert **und** `config.js` noch da
   ist. Würde das Deployment `config.js` löschen, wüsste das Frontend nicht mehr, wo
   die Web-Konsole läuft - deshalb ist die Datei vom `--delete`-Sync ausgenommen und
   wird zusätzlich nach jedem Deploy geprüft.
4. **Concurrency:** `concurrency: deploy-production` - zwei gleichzeitige Deployments
   würden sich auf Instanz und Bucket gegenseitig überschreiben; der zweite Lauf wartet.
5. **Vermerken:** GitHub-Deployment (`environment=production`) mit dem Tag - der
   Vergleichspunkt für den nächsten Lauf.

**Spieler während des Neustarts** verlieren ihre laufende Partie: der Dienst-Restart
beendet alle ttyd-Kindprozesse (ihre JVMs). Bei einem 2-Minuten-Spiel ist das vertretbar.
Verhindern könnte man es mit einer Drain-Phase (keine neuen Verbindungen, alte Partien
zu Ende spielen lassen) oder blau/grün: neue Version auf zweitem Port/zweiter Instanz
starten und erst dann umschalten (nur Konzept, hier nicht umgesetzt).

### Gezielt eine Version deployen (Rollback)

*Actions → Deploy (Web-Konsole) → Run workflow*, Tag wählen (z.B. `v1.0.2`),
`force: true`. Es werden Backend **und** Frontend genau dieser Version ausgerollt -
aus den damaligen Release-Assets, ohne Build, ohne Infrastruktur-Änderung. Dieselbe
Mechanik deckt drei Fälle ab:

- **Rollback:** v1.3.0 ist kaputt → `v1.2.0` mit `force` deployen.
- **Kaputtes Release:** der Versions-Check (Schritt 2/3 oben) schlägt fehl, der Workflow
  wird rot. Wiederherstellung = ein `force`-Deploy der letzten guten Version, Dauer
  wenige Minuten. (Achtung: ein JAR, das gar nicht startet, fällt erst hier auf - der
  Release-Prozess testet zwar `java -jar`, aber nicht die Umgebung der Instanz.)
- **Nach `tofu destroy` + `apply`:** neue IP/Bucket → in GitHub `EC2_HOST`,
  `S3_BUCKET_FRONTEND`, `FRONTEND_URL` (Variables) und `EC2_SSH_PRIVATE_KEY` (Secret)
  aktualisieren, dann die gewünschte Version mit `force` deployen.

## Experimente (Robustheit)

| Experiment | Ergebnis |
|---|---|
| Learner-Lab-Session beenden und neu starten | Instanz wird vom Lab gestoppt und wieder gestartet. Elastic IP bleibt → gleiche URL. systemd startet `tictactest-console` und `tictactest-version` automatisch, der `current`-Symlink zeigt unverändert auf die zuletzt deployte Version → gleiche Version läuft, ohne dass man etwas tut. |
| Einrichtung der Instanz ändern (z.B. deploy/bootstrap.sh) | `tofu plan`: **keine Änderungen** - `user_data` steht unter `lifecycle.ignore_changes`, weil cloud-init ohnehin nur beim allerersten Start läuft. Die Änderung wird stattdessen beim nächsten Backend-Deploy per SSH ausgerollt. Ohne diese Vorkehrung (`user_data_replace_on_change = true`) würde jede Skript-Änderung die **Instanz ersetzen**: neue Instanz, deployter Stand weg - die EIP bliebe zwar bestehen, aber genau die Trennung Infrastruktur/Applikation wäre verletzt. |
| `tofu destroy`, dann `tofu apply` | Instanz, EIP, Key-Paar, Security Group und alle S3-Einstellungen werden neu erstellt: neue Instanz-ID, **neue IP**, neues SSH-Key-Paar; OpenTofu schreibt `config.js` automatisch mit der neuen IP. Der Bucket selbst bleibt bestehen (per CLI erstellt, nicht im State - Learner-Lab-SCP, siehe oben), das zuletzt deployte Frontend damit auch. cloud-init richtet ttyd/Java/Dienste wieder ein, aber das Backend ist leer ("Noch kein Release deployt"). Bis dieselbe Version wieder läuft: GitHub-Variables (`EC2_HOST`) und das Secret `EC2_SSH_PRIVATE_KEY` auf die neuen Werte setzen und die Version per `force`-Deploy (oder von Hand) erneut ausrollen. |

## Reflexion

### Weg einer Änderung (Commit → läuft)

```text
Backend:  Commit auf Branch → PR/Review → main → Version+Changelog → Tag vX.Y.Z
          → release.yml (Tests, JAR bauen, Release mit Assets)
          → deploy.yml: diff=backend → JAR per SSH → Restart → Versions-Check ✓

Frontend: ... derselbe Weg bis zum Release ...
          → deploy.yml: diff=frontend → Zip nach S3 (ohne config.js) → version.js-Check ✓
```

**Automatisiert:** Tests, Release, Diff-Erkennung, beide Deploy-Wege, Verifikation.
**Manuell bleiben:** `tofu apply` bei Infrastruktur-Änderungen (bewusst: `plan` lesen!)
und das Auffrischen der Learner-Lab-Credentials bzw. der GitHub-Werte nach `destroy`.

### Mehrere Umgebungen (dev → staging → prod)

- **OpenTofu:** dieselbe Konfiguration, pro Umgebung eigener State + eigene Werte
  (`dev.tfvars`/`staging.tfvars`/`prod.tfvars` oder Workspaces): gleicher Code, gleiche
  Ressourcen-Struktur; unterschiedlich sind z.B. `name_prefix`, `instance_type`,
  `allowed_cidr`. Pro Umgebung entstehen eigene Instanz, EIP und Bucket.
- **GitHub Environments** pro Stufe mit eigenen Variables/Secrets (`EC2_HOST` usw. je
  Umgebung) und Schutzregeln - nach `prod` deployt nur, wer das Environment freigeben
  darf (**Required Reviewers**).
- **Promotion:** dasselbe, bereits getestete Artefakt wird weitergereicht - nie pro
  Umgebung neu gebaut (sonst wäre nicht garantiert, dass prod genau das Getestete
  bekommt). Auslöser: Release → automatisch nach dev; nach staging per manueller
  Freigabe oder grünem Smoke-Test (z.B. Versions-Check + eine Partie per Skript);
  nach prod nur per manueller Freigabe durch Berechtigte.
- **Änderungserkennung je Umgebung:** der Vergleichspunkt ist der zuletzt in *dieser*
  Umgebung deployte Tag (Deployments-API mit `environment=dev|staging|prod`) - genau
  deshalb vergleicht der Workflow heute schon gegen das Deployment und nicht gegen
  "das vorherige Release": Umgebungen dürfen unterschiedlich weit hinterherhinken.

### Nachweis der Trennung (Auftrag 3, Teil 6)

Nach dem Merge dieses Branches durchspielen und hier die Links zu den Workflow-Läufen
eintragen:

- [ ] Nur-Frontend-Release (z.B. Farbe in `frontend/style.css`): Frontend-Version neu,
      Backend-Version unverändert, kein SSH-Schritt im Log
- [ ] Nur-Backend-Release (z.B. GreedyPlayer in `main()` fest einschalten):
      Backend-Version neu, Frontend-Job-Schritte übersprungen
- [ ] Release mit beidem: beide Wege laufen
- [ ] Push auf `main` ohne Tag: kein Deploy-Workflow-Lauf
