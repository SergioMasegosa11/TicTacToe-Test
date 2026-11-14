# Outputs zeigen nach "tofu apply" die wichtigsten Informationen an (siehe docs/iac.md).

output "public_ip" {
  description = "Oeffentliche (stabile) IP-Adresse der EC2-Instanz (Elastic IP)"
  value       = aws_eip.tictactest.public_ip
}

output "public_dns" {
  description = "Oeffentlicher DNS-Name der EC2-Instanz"
  value       = aws_instance.tictactest.public_dns
}

output "ssh_command" {
  description = "Fertiger SSH-Befehl, sobald der private Schluessel exportiert ist (siehe docs/iac.md)"
  value       = "ssh -i key.pem ec2-user@${aws_eip.tictactest.public_ip}"
}

output "web_console_url" {
  description = "URL der TicTacTest-Web-Konsole (siehe docs/deployment.md). Fuer das README."
  value       = "http://${aws_eip.tictactest.public_ip}:7681"
}

# --- Outputs fuer Benutzer und Deploy-Pipeline (Auftrag 2, Teil 2) ---
# Alles hier ist UNKRITISCH (oeffentliche URLs/IDs): die IP ist oeffentlich erreichbar,
# der Bucket-Name steht in jeder Frontend-URL. Vertraulich ist nur ssh_private_key
# (unten, sensitive) - und die AWS-Zugangsdaten, die gar nie in OpenTofu auftauchen.

output "frontend_url" {
  description = "URL zum Spielen: das statische Frontend (S3 Static Website)"
  value       = "http://${aws_s3_bucket_website_configuration.frontend.website_endpoint}"
}

output "version_url" {
  description = "Versionsabfrage: welche Backend-Version laeuft gerade (fuer Pipeline und Frontend)"
  value       = "http://${aws_eip.tictactest.public_ip}:8080/version.txt"
}

output "frontend_bucket" {
  description = "Name des S3-Buckets fuers Frontend (Ziel fuer das Frontend-Deployment, GitHub-Variable S3_BUCKET_FRONTEND)"
  value       = var.frontend_bucket
}

output "instance_id" {
  description = "ID der EC2-Instanz (z.B. fuer aws ssm send-command oder Diagnose)"
  value       = aws_instance.tictactest.id
}

# sensitive = true: wird nicht einfach in Logs ausgegeben. Trotzdem gilt: NIE committen,
# nur einmalig lokal exportieren -> als GitHub-Secret eintragen (siehe docs/deployment.md).
output "ssh_private_key" {
  description = "Privater SSH-Schluessel (PEM). Export: tofu output -raw ssh_private_key > key.pem"
  value       = tls_private_key.ssh.private_key_pem
  sensitive   = true
}
