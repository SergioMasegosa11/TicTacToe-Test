// Umgebungsspezifische Konfiguration des Frontends (siehe docs/webkonsole.md).
//
// Die URL der Web-Konsole ist bewusst NICHT im Code (app.js) fest verdrahtet:
// - Lokal gilt diese Datei hier (localhost-Werte).
// - In der Cloud schreibt OpenTofu diese Datei selbst in den S3-Bucket
//   (infra/s3.tf, aws_s3_object "config_js"), weil nur die Infrastruktur die
//   Adresse der EC2-Instanz kennt.
// - Das Frontend-Deployment (deploy.yml) synchronisiert alle Dateien AUSSER
//   config.js (--exclude), damit die Infrastruktur-Konfiguration erhalten bleibt.
window.TICTACTEST_CONFIG = {
  consoleUrl: "http://localhost:7681",
  versionUrl: "http://localhost:8080/version.txt"
};
