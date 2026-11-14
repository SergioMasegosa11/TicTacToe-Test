# Frontend-Hosting: S3-Bucket als Static Website (siehe docs/webkonsole.md und
# docs/deployment.md). Der Bucket gehoert zur INFRASTRUKTUR (einmal erstellen), der
# INHALT (die Frontend-Dateien aus dem Release) wird per Deployment aktualisiert -
# mit einer Ausnahme: config.js schreibt OpenTofu selbst, weil nur die Infrastruktur
# die Adresse der EC2-Instanz kennt.
#
# LEARNER-LAB-EINSCHRAENKUNG: Der Bucket selbst kann hier NICHT als
# resource "aws_s3_bucket" verwaltet werden. Der AWS-Provider liest bei jedem Refresh
# u.a. die Object-Lock-Konfiguration (s3:GetBucketObjectLockConfiguration), und genau
# dieser Aufruf ist im Learner Lab per Service Control Policy explizit verboten -
# jeder plan/apply wuerde mit AccessDenied abbrechen. Deshalb wird der Bucket einmalig
# per AWS CLI erstellt (S3-Bucket-Namen sind weltweit eindeutig, daher mit Suffix):
#
#   aws s3api create-bucket --bucket tictactest-frontend-<suffix>
#
# und hier ueber var.frontend_bucket referenziert. Alles RUND um den Bucket (Website-
# Konfiguration, Public-Access-Einstellungen, Bucket-Policy, config.js) bleibt
# vollstaendig von OpenTofu verwaltet - diese APIs sind im Lab erlaubt.
#
# In diesem oeffentlichen Bucket duerfen NUR die statischen Frontend-Dateien liegen -
# nie Zugangsdaten, private Schluessel, State-Dateien oder das JAR.

resource "aws_s3_bucket_website_configuration" "frontend" {
  bucket = var.frontend_bucket

  index_document {
    suffix = "index.html"
  }
}

# Oeffentlicher Lesezugriff ist hier gewollt (statische Webseite) - deshalb werden die
# Public-Access-Sperren fuer genau diesen Bucket deaktiviert.
resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = var.frontend_bucket

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_policy" "frontend" {
  bucket = var.frontend_bucket
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "PublicReadGetObject"
      Effect    = "Allow"
      Principal = "*"
      Action    = "s3:GetObject"
      Resource  = "arn:aws:s3:::${var.frontend_bucket}/*"
    }]
  })

  depends_on = [aws_s3_bucket_public_access_block.frontend]
}

# config.js wird von der INFRASTRUKTUR geschrieben: Das Frontend erfaehrt so die URL der
# Web-Konsole und des Versions-Endpoints, ohne dass sie im Code steht. Ein spaeteres
# Frontend-Deployment synchronisiert alle Dateien AUSSER config.js (deploy.yml:
# aws s3 sync --exclude config.js), damit dieser Wert erhalten bleibt.
resource "aws_s3_object" "config_js" {
  bucket       = var.frontend_bucket
  key          = "config.js"
  content_type = "application/javascript"

  content = <<-EOT
    // Von OpenTofu generiert (infra/s3.tf) - NICHT von Hand aendern und NICHT durch
    // ein Frontend-Deployment ueberschreiben (deploy.yml schliesst config.js aus).
    window.TICTACTEST_CONFIG = {
      consoleUrl: "http://${aws_eip.tictactest.public_ip}:7681",
      versionUrl: "http://${aws_eip.tictactest.public_ip}:8080/version.txt"
    };
  EOT
}
