#!/bin/bash
set -euxo pipefail

dnf update -y
dnf install -y docker awscli amazon-ssm-agent curl

systemctl enable --now docker
systemctl enable --now amazon-ssm-agent

usermod -aG docker ec2-user || true

mkdir -p /opt/jobradar/secrets
chown -R ec2-user:ec2-user /opt/jobradar
