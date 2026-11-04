# TicTacTest

Ein kleines TicTacToe für die Konsole: ein Mensch (X) spielt gegen einen Computer-Gegner
(O). Entstanden als Schulprojekt (BBW, Modul 450) - von Tests über Releases bis zum
Deployment als Web-App.

## Web-App

🎮 **[TicTacTest spielen](http://<EC2-IP>:7681)** <!-- TODO: nach `tofu apply` die echte
Adresse eintragen, siehe docs/deployment.md (Output `web_console_url`) -->

Läuft unverändert als `tictactest-<version>.jar` aus dem aktuellen Release in einer
Web-Konsole (ttyd) auf einer EC2-Instanz. Mensch (X) gegen den Computer-Gegner aus
`TicTacToeMain.main()`.

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
