# TicTacTest

Ein kleines TicTacToe für die Konsole: ein Mensch (X) spielt gegen einen Computer-Gegner
(O). Entstanden als Schulprojekt (BBW, Modul 450) - von Tests über Releases bis zum
Deployment als Web-App.

## Web-App

🎮 **[TicTacTest spielen](http://tictactest-frontend-20261114172637170800000001.s3-website-us-east-1.amazonaws.com)**
(Frontend; nur das Terminal: [Web-Konsole](http://54.236.134.78:7681), laufende Version:
[version.txt](http://54.236.134.78:8080/version.txt))

Läuft unverändert als `tictactest-<version>.jar` aus dem aktuellen Release in einer
Web-Konsole (ttyd) auf einer EC2-Instanz; das statische Frontend (`frontend/`) kommt aus
einem S3-Bucket. Details: [docs/webkonsole.md](docs/webkonsole.md) und
[docs/deployment.md](docs/deployment.md).

## Lokal spielen

```bash
./gradlew build
java -jar build/libs/tictactest-<version>.jar
```

Oder das fertige JAR von der [Release-Seite](https://github.com/SergioMasegosa11/TicTacToe-Test/releases)
herunterladen (ab Java 21).

## Dokumentation

| Thema | Dokument |
|---|---|
| Spielregeln, Testkonzept, gefundene Fehler | [`Testkonzept_TicTacTest.md`](Testkonzept_TicTacTest.md) |
| Versionen, was sich geändert hat | [`CHANGELOG.md`](CHANGELOG.md) |
| Wie ein Release entsteht | [`RELEASE.md`](RELEASE.md) |
| Infrastructure as Code (OpenTofu, AWS Learner Lab) | [`docs/iac.md`](docs/iac.md) |
| Deployment als Web-App (Secrets, Ablauf, Reflexion/Promotion-Strategie) | [`docs/deployment.md`](docs/deployment.md) |
| DevContainer | [`docs/devcontainer.md`](docs/devcontainer.md) |
| Coverage & Mutation Testing | [`docs/coverage-design.md`](docs/coverage-design.md), [`docs/mutation-testing.md`](docs/mutation-testing.md) |
