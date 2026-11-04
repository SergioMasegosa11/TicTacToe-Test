# Variablen machen die Konfiguration wiederverwendbar (siehe docs/iac.md).

variable "region" {
  description = "AWS-Region fuer alle Ressourcen"
  type        = string
  default     = "eu-central-1"
}

variable "instance_type" {
  description = "Groesse der EC2-Instanz (micro fuer Lernzwecke/Kostenkontrolle)"
  type        = string
  default     = "t3.micro"
}

variable "allowed_cidr" {
  description = "CIDR-Bereich, der per SSH auf die Instanz zugreifen darf. Fuer den eigenen Rechner z.B. \"203.0.113.4/32\" statt 0.0.0.0/0 verwenden."
  type        = string
  default     = "0.0.0.0/0"
}

variable "name_prefix" {
  description = "Praefix fuer die Namen aller erzeugten Ressourcen"
  type        = string
  default     = "tictactest"
}
