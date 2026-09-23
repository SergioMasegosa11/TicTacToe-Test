# Design: Coverage Time-Series auf GitHub Pages

Design der Lösung für Auftrag 2. Umgesetzt in `.github/workflows/coverage-pages.yml`,
`scripts/coverage.sh` und `pages/index.html`.

## Design-Fragen

| Frage | Antwort |
|---|---|
| **Woher kommt der aktuelle Coverage-Wert?** | Aus dem JaCoCo-XML-Report (`build/reports/jacoco/test/jacocoTestReport.xml`), ausgelesen mit `scripts/coverage.sh`. |
| **Welche Metrik wird gespeichert?** | Line-Coverage in Prozent, auf eine Nachkommastelle gerundet (`covered / (covered + missed) * 100`). |
| **Wo und in welchem Format werden die historischen Daten gespeichert?** | Als CSV-Datei `coverage-history.csv` auf dem Branch `gh-pages`. Format: `Datum,Commit,Coverage`. |
| **Wie kommen neue Messwerte dazu?** | Der Workflow checkt den `gh-pages`-Branch aus, kopiert die bestehende CSV und hängt eine neue Zeile an. Beim ersten Lauf wird die CSV mit Kopfzeile neu erstellt. |
| **Wie entsteht die Time-Series?** | `pages/index.html` lädt die CSV im Browser und zeichnet ein Liniendiagramm mit Chart.js. |
| **Wie wird veröffentlicht?** | Der Ordner `site/` (index.html, CSV, aktueller HTML-Report) wird mit `JamesIves/github-pages-deploy-action` auf den Branch `gh-pages` gepusht. GitHub Pages liefert diesen Branch aus. |
| **Wann wird die Seite aktualisiert?** | Bei jedem Push auf `main` (auch durch Merge eines Pull Requests) und manuell über `workflow_dispatch`. |

Beispiel-Datensatz:

```text
Datum,Commit,Coverage
2026-09-01,abc123,72.4
2026-09-02,def456,74.1
```

## Architektur

```text
Push / Merge auf main
        │
        ▼
GitHub Actions (coverage-pages.yml)
  1. ./gradlew test jacocoTestReport   ->  jacocoTestReport.xml + HTML
  2. scripts/coverage.sh               ->  Coverage in %
  3. gh-pages auschecken, alte CSV holen, neue Zeile anhängen
  4. index.html + CSV + HTML-Report nach site/
  5. site/ -> Branch gh-pages
        │
        ▼
GitHub Pages  ->  https://sergiomasegosa11.github.io/TicTacToe-Test/
                  (Liniendiagramm + Tabelle + aktueller Report)
```

## Einmalige Einrichtung

In GitHub unter **Settings → Pages → Build and deployment**:
Source = *Deploy from a branch*, Branch = `gh-pages` / `(root)`.

## Coverage Gate (Auftrag 3)

`.github/workflows/coverage-gate.yml` läuft bei jedem Öffnen/Aktualisieren eines Pull Requests
und manuell. Er misst die Coverage des Branches und von `main` (gleiche Metrik, gleiches Script),
zeigt beide Werte, die Differenz in Prozentpunkten und PASS/FAIL im Log und in der Job-Summary an,
schreibt das Ergebnis als Kommentar in den Pull Request und schlägt fehl, wenn die Coverage
des Branches tiefer ist als die von `main`.
