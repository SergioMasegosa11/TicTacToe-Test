# OpenTofu-Konfiguration fuer eine erste Cloud-Infrastruktur (AWS Learner Lab).
# Siehe docs/iac.md fuer Erklaerungen und den Ablauf (init -> plan -> apply -> destroy).

terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }
}

# Provider: das Bindeglied zwischen OpenTofu und AWS. Er weiss, wie die AWS-API
# angesprochen wird (Region, Zugangsdaten aus Umgebungsvariablen/Learner-Lab-Session).
provider "aws" {
  region = var.region
}

# AWS Learner Lab stellt nur die Standard-VPC zur Verfuegung (keine eigenen VPCs/IAM-Rollen).
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Learner Lab: eigene IAM-Rollen/-Profile sind verboten, aber das vorbereitete
# LabInstanceProfile darf verwendet werden. Damit greift die Instanz ohne Access Keys
# auf andere AWS-Dienste zu (z.B. S3) - auf der Instanz liegen NIE Zugangsdaten.
data "aws_iam_instance_profile" "lab" {
  name = "LabInstanceProfile"
}

# Aktuelles Amazon Linux 2023 Image, statt eine fest eingetragene AMI-ID zu pflegen.
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
}

# Ressource: das SSH-Schluesselpaar wird von OpenTofu selbst erzeugt, damit kein
# lokales ssh-keygen noetig ist. Der private Schluessel landet NUR im State (siehe
# docs/iac.md, Abschnitt State) und wird einmalig per "tofu output" exportiert,
# nie committet.
resource "tls_private_key" "ssh" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "aws_key_pair" "tictactest" {
  key_name   = "${var.name_prefix}-key"
  public_key = tls_private_key.ssh.public_key_openssh
}

# Ressource: Security Group, die SSH und die ttyd-Web-Konsole erlaubt. var.allowed_cidr
# sollte auf die eigene IP eingeschraenkt werden (siehe docs/iac.md, Sicherheit).
resource "aws_security_group" "tictactest" {
  name        = "${var.name_prefix}-sg"
  description = "TicTacTest: SSH und Web-Konsole (ttyd) fuer die EC2-Instanz"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.allowed_cidr]
  }

  # Web-Konsole (ttyd): das Release-JAR unveraendert im Browser spielbar, siehe
  # docs/deployment.md. Bewusst offener als SSH, da hier nur das Spiel selbst laeuft.
  ingress {
    description = "ttyd Web-Konsole"
    from_port   = 7681
    to_port     = 7681
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Versions-Endpoint (deploy/version-server.py): liefert NUR die Versionsnummer des
  # laufenden Releases als Text. Muss oeffentlich sein, damit das Frontend (S3, anderer
  # Origin) und die Deploy-Pipeline die laufende Version pruefen koennen.
  ingress {
    description = "Versionsabfrage (version.txt)"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "aller ausgehender Verkehr"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.name_prefix}-sg"
  }
}

# Ressource: die eigentliche EC2-Instanz. Groesse ueber var.instance_type (micro,
# Learner-Lab-/Kosten-tauglich), Netzwerk/Security Group wie oben definiert.
# user_data installiert einmalig ttyd + den Platzhalter-Dienst (siehe deploy/); das
# eigentliche App-Deployment (JAR) laeuft danach per SSH ueber
# .github/workflows/deploy.yml, OHNE dass diese Instanz neu erstellt wird.
resource "aws_instance" "tictactest" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = var.instance_type
  subnet_id              = data.aws_subnets.default.ids[0]
  key_name               = aws_key_pair.tictactest.key_name
  vpc_security_group_ids = [aws_security_group.tictactest.id]
  iam_instance_profile   = data.aws_iam_instance_profile.lab.name

  user_data = templatefile("${path.module}/../deploy/cloud-init.sh.tftpl", {
    run_tictactest_sh = file("${path.module}/../deploy/run-tictactest.sh")
    bootstrap_sh      = file("${path.module}/../deploy/bootstrap.sh")
    version_server_py = file("${path.module}/../deploy/version-server.py")
  })

  # user_data (cloud-init) laeuft nur beim ALLERERSTEN Start der Instanz. Spaetere
  # Aenderungen an den deploy/-Skripten werden per SSH-Deploy ausgerollt, nicht ueber
  # die Infrastruktur. Ohne ignore_changes wuerde jede Skript-Aenderung hier einen
  # plan-Diff erzeugen; mit user_data_replace_on_change = true wuerde sie sogar die
  # Instanz ERSETZEN (neue Instanz, Deploy-Stand weg) - genau das, was die Trennung
  # "Infrastruktur vs. Applikation" verhindern soll. Nach destroy+apply greift
  # automatisch wieder der aktuelle Stand der Skripte.
  lifecycle {
    ignore_changes = [user_data]
  }

  tags = {
    Name = "${var.name_prefix}-instance"
  }
}

# Elastic IP: haelt die IP ueber Stop/Start der Instanz hinweg stabil, damit der
# Web-Konsole-Link im README nicht nach jedem Lab-Neustart angepasst werden muss.
# Learner-Lab-Hinweis: EIPs sind kontingentiert - falls die Zuweisung fehlschlaegt,
# kann dieser Block entfernt und stattdessen aws_instance.tictactest.public_ip direkt
# verwendet werden (siehe docs/deployment.md, Reflexion).
resource "aws_eip" "tictactest" {
  instance = aws_instance.tictactest.id
  domain   = "vpc"

  tags = {
    Name = "${var.name_prefix}-eip"
  }
}
