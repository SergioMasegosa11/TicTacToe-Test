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

# sensitive = true: wird nicht einfach in Logs ausgegeben. Trotzdem gilt: NIE committen,
# nur einmalig lokal exportieren -> als GitHub-Secret eintragen (siehe docs/deployment.md).
output "ssh_private_key" {
  description = "Privater SSH-Schluessel (PEM). Export: tofu output -raw ssh_private_key > key.pem"
  value       = tls_private_key.ssh.private_key_pem
  sensitive   = true
}
