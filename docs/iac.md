# Infrastructure as Code: erste Cloud-Infrastruktur mit OpenTofu

Dieses Dokument beantwortet den Auftrag "OpenTofu/Terraform - Erste Cloud-Infrastruktur"
für TicTacTest. Es beschreibt die Konfiguration in [`infra/`](../infra), die Grundbegriffe,
die State/Git-Regeln, die Kostenkontrolle und den Ausblick auf spätere Deployment-Umgebungen
(siehe dazu auch [`docs/deployment.md`](deployment.md)).

## 1. Zugang zum AWS Learner Lab

Der Zugriff erfolgt über die Einladung zum Kurs "324 HS26 IAC" in AWS Academy. Nach dem
Öffnen des Learner Lab stehen im Lern-Account unter anderem EC2, S3 und VPC (nur die
Standard-VPC) zur Verfügung; eigene IAM-Benutzer oder -Rollen können **nicht** angelegt
werden. Das prägt die Konfiguration unten: Es wird ausschliesslich die vorhandene
Standard-VPC verwendet, es werden keine IAM-Ressourcen definiert.

## 2. Grundbegriffe (in eigenen Worten)

| Begriff | Bedeutung |
|---|---|
| **Infrastructure as Code (IaC)** | Infrastruktur (Server, Netzwerk, …) wird als Text/Code beschrieben statt von Hand in einer Web-Konsole geklickt. Dieser Code ist versioniert, nachvollziehbar und beliebig oft wiederholbar. |
| **OpenTofu** | Open-Source-Fork von Terraform, entstanden, weil Teile der Community eine offene, unabhängige Alternative zu HashiCorp wollten. Gleiche Sprache (HCL), gleiche Konzepte, Befehl heisst `tofu` statt `terraform`. In der BBW bevorzugt, weil Open Source. |
| **Terraform** | Das Original-Tool von HashiCorp, auf dem OpenTofu basiert. Deklaratives Modell: man beschreibt den gewünschten Zustand, nicht die einzelnen Schritte dorthin. |
| **Provider** | Die Schnittstelle zu einer konkreten Plattform, hier `hashicorp/aws` ([`infra/main.tf`](../infra/main.tf)). Er weiss, wie man mit der AWS-API spricht (Region, Zugangsdaten). |
| **Resource** | Ein konkretes Objekt, das entstehen soll, z.B. `aws_instance`, `aws_security_group`, `aws_key_pair`. Jede Resource in `main.tf` entspricht einem echten AWS-Objekt. |
| **Variable** | Ein Platzhalter für Werte, die sich ändern können, ohne den Code anzupassen (z.B. `instance_type`, `region` in [`infra/variables.tf`](../infra/variables.tf)). Macht die Konfiguration wiederverwendbar. |
| **Output** | Ein Wert, der nach `apply` angezeigt wird, z.B. die öffentliche IP ([`infra/outputs.tf`](../infra/outputs.tf)). Outputs sind das "Ergebnis" eines Applys, das man weiterverwenden kann (z.B. für SSH oder spätere Deploy-Skripte). |
| **State** | Die Datei (`terraform.tfstate`), in der OpenTofu merkt, welche Ressourcen es bereits erzeugt hat und wie sie mit der Konfiguration zusammenhängen. Ohne State wüsste `plan`/`apply` nicht, was schon existiert. |

## 3. Die Konfiguration in `infra/`

```text
infra/
├── main.tf                     Provider, Daten der Standard-VPC/AMI, Key-Pair, Security Group, EC2-Instanz
├── variables.tf                region, instance_type, allowed_cidr, name_prefix
├── outputs.tf                  public_ip, public_dns, ssh_command, ssh_private_key (sensitive)
└── terraform.tfvars.example    unkritische Beispielwerte (siehe Abschnitt 4)
```

- **Provider** (`provider "aws"`): verbindet OpenTofu mit dem AWS-Learner-Lab-Account über
  die Region aus `var.region`. Die Zugangsdaten selbst stehen **nicht** im Code, sondern
  kommen aus Umgebungsvariablen/GitHub-Secrets (siehe `docs/deployment.md`, Secrets-Konzept).
- **Resources**:
  - `tls_private_key.ssh` erzeugt das SSH-Schlüsselpaar direkt in OpenTofu (kein lokales
    `ssh-keygen` nötig). Der private Schlüssel existiert nur im State und als Output.
  - `aws_key_pair.tictactest` lädt den öffentlichen Teil davon zu AWS hoch.
  - `aws_security_group.tictactest` öffnet nur Port 22 (SSH), eingeschränkt auf
    `var.allowed_cidr`.
  - `aws_instance.tictactest` ist die eigentliche EC2-Instanz (`t3.micro`), in der
    Standard-VPC, mit dem Key-Pair und der Security Group oben.
- **Rolle von Provider und Resource:** Der Provider stellt die Verbindung zu AWS her und
  weiss, wie die API-Aufrufe aussehen; die Resources beschreiben, *was* in dieser Verbindung
  entstehen soll. Ohne Provider wüsste OpenTofu nicht, bei welcher Plattform es die
  Ressourcen anlegen soll; ohne Resources gäbe es nichts anzulegen.
- **Vorbereitung für ein späteres Deployment:** Diese Instanz ist bewusst "nackt" (nur SSH).
  Im nächsten Auftrag ([`docs/deployment.md`](deployment.md)) wird genau diese Instanz
  wiederverwendet: Eine Web-Konsole (ttyd) und das TicTacTest-JAR werden **ohne erneutes
  `apply`** per SSH draufgespielt. Die Trennung "Infrastruktur einmal erzeugen" vs.
  "Anwendung beliebig oft neu deployen" ist genau der Punkt, den dieser Auftrag vorbereitet.

## 4. Ablauf: init → plan → apply → destroy

```text
tofu init       lädt die Provider (aws, tls) herunter, legt .terraform/ an (nicht committet)
tofu validate   prüft Syntax/Logik der .tf-Dateien
tofu plan       vergleicht main.tf+variables mit dem aktuellen State und der echten
                 AWS-Welt, zeigt an, was sich ändern würde - ohne etwas auszuführen
tofu apply      erzeugt/ändert die Ressourcen wirklich, aktualisiert den State
tofu output     zeigt die Outputs an (z.B. public_ip)
tofu destroy    entfernt alle erzeugten Ressourcen wieder
```

Lokal mit eigenen Werten:

```bash
cd infra
cp terraform.tfvars.example terraform.tfvars   # anpassen, v.a. allowed_cidr
tofu init
tofu plan
tofu apply
tofu output -raw ssh_private_key > key.pem && chmod 600 key.pem
ssh -i key.pem ec2-user@$(tofu output -raw public_ip)
```

`plan` wird **immer** vor `apply` angeschaut: Es zeigt genau, welche Ressourcen neu
entstehen, geändert oder gelöscht würden, bevor es wirklich passiert - das ist einer der
grössten Vorteile von IaC gegenüber manuellem Klicken in der AWS-Konsole.

## 5. State und Git: was gehört ins Repository, was nicht

Der State enthält den vollständigen, aktuellen Zustand aller erzeugten Ressourcen -
inklusive Geheimnisse, die als Attribut irgendeiner Resource auftauchen (hier: der private
SSH-Schlüssel aus `tls_private_key`). Er ist deshalb **genauso sensibel wie ein Passwort**
und darf nie veröffentlicht werden. Ausserdem beschreibt er, was *wirklich* existiert; geht
er verloren oder weicht er vom echten Zustand ab, "verliert" OpenTofu den Überblick über
seine eigenen Ressourcen (Lösung dafür in grösseren Projekten: ein Remote-State-Backend,
siehe Abschnitt 7).

| Darf ins Repository | Gehört **nie** ins Repository |
|---|---|
| `*.tf` (Provider, Resources, Variablen, Outputs) | `*.tfstate` / `*.tfstate.*` (State, enthält den privaten SSH-Schlüssel im Klartext) |
| `terraform.tfvars.example` (unkritische Beispielwerte) | `terraform.tfvars` (reale, projektspezifische Werte - können sensibel sein) |
| `README`/Dokumentation zur Konfiguration | `.terraform/` (heruntergeladene Provider-Binaries, lokal, gross) |
| | `*.pem` / private SSH-Schlüssel |
| | AWS Access Keys, Secret Keys, Session Tokens |
| | `crash.log` (kann interne Details/Daten enthalten) |

Umgesetzt in diesem Repository über [`.gitignore`](../.gitignore) (die bestehende Regel
`.*` deckt `.terraform/` bereits ab; `*.tfstate*`, `*.tfvars` mit Ausnahme der
`.example`-Datei, `*.pem` und `crash.log` wurden ergänzt).

## 6. Kostenkontrolle

- Die Instanzgrösse ist bewusst `t3.micro` (kleinste sinnvolle Grösse, im AWS-Free-Tier
  bzw. im Learner-Lab-Rahmen günstig/kostenlos).
- **Mindestens zwei Massnahmen zur Kostenkontrolle:**
  1. `tofu destroy` am Ende jeder Lernsitzung/Testphase - laufende Instanzen kosten auch,
     wenn niemand sie benutzt. Das Learner Lab stoppt die Umgebung zusätzlich automatisch
     nach Ablauf der Session-Zeit.
  2. Instanzgrösse klein halten (`micro`) und nur eine einzige Instanz statt mehrerer
     Testumgebungen parallel laufen lassen.
- Warum wichtig: In einer echten Cloud-Umgebung (ausserhalb des kostenlosen Lern-Labs)
  laufen Kosten pro Stunde, unabhängig davon, ob die Instanz genutzt wird. Vergessene
  Testressourcen sind eine der häufigsten Ursachen unerwartet hoher Cloud-Rechnungen.
- IaC hilft dabei: Weil die ganze Infrastruktur als Code vorliegt, lässt sie sich mit einem
  einzigen `tofu destroy` vollständig und nachvollziehbar wieder entfernen - es bleiben keine
  von Hand erstellten, schnell vergessenen Ressourcen übrig.

## 7. Ausblick: Infrastruktur, CI/CD und dev/staging/prod

- Die hier erzeugte Infrastruktur ist absichtlich minimal (eine Instanz, ein Zweck). In
  einer Pipeline würde ein eigener Workflow (`tofu plan`/`apply` bei Änderungen an `infra/`)
  Infrastrukturänderungen automatisch erkennen und anzeigen, bevor jemand sie freigibt -
  genau wie `plan` das hier schon lokal tut.
- Für mehrere Umgebungen (`dev`, `staging`, `prod`) würde man **nicht** dieselbe
  `terraform.tfvars` für alle verwenden, sondern z.B. eigene `dev.tfvars`/`staging.tfvars`/
  `prod.tfvars` (oder OpenTofu-Workspaces) mit eigenem State pro Umgebung. So lässt sich
  z.B. `instance_type` oder `allowed_cidr` pro Umgebung unterschiedlich setzen, ohne den
  Code zu duplizieren.
- Zugangsdaten würden pro Umgebung als eigene Secrets in GitHub Environments liegen (siehe
  [`docs/deployment.md`](deployment.md)), mit zunehmend strengeren Freigabe-Regeln je näher
  man an `prod` kommt (z.B. manuelle Review-Pflicht vor `apply` auf `prod`).
- Wichtig für produktive Anwendungen: Infrastrukturänderungen sollen genauso nachvollziehbar,
  getestet (`plan` vor `apply`) und reproduzierbar sein wie Code-Änderungen - ein falscher
  Klick in der AWS-Konsole lässt sich nicht per Pull-Request-Review verhindern, ein falscher
  `tofu apply` schon eher, weil der `plan`-Schritt die Änderung vorher sichtbar macht.

## Checkliste (Auftrag erfüllt)

- [x] AWS Learner Lab: Zugriff und verfügbare Services dokumentiert (Abschnitt 1)
- [x] Grundbegriffe in eigenen Worten (Abschnitt 2)
- [x] Erste Konfiguration mit Provider, Resource, Variable, Output (Abschnitt 3, `infra/`)
- [x] EC2-Instanz der Grösse `micro`, SSH mit eigenem Key-Paar (`infra/main.tf`)
- [x] State/Git-Regeln dokumentiert (Abschnitt 5)
- [x] Kostenkontrolle: mindestens zwei Massnahmen (Abschnitt 6)
- [x] Ausblick auf CI/CD und dev/staging/prod (Abschnitt 7)
