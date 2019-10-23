#!/usr/bin/env bash

if [[ $EUID -ne 0 ]]; then
  printf "Run the script as root!"
  exit 1
fi

# Install dependencies
sudo apt-get update
if [[ $(java -version 2>&1 | grep -c "not found") == 0 ]]; then
  sudo apt-get install -y default-jdk
fi
if [[ $(mysql -V 2>&1 | grep -c "not found") == 0 ]]; then
  sudo apt-get install -y mysql-server
  sudo mysql_secure_installation -D
fi
if [[ $(kotlin -version 2>&1 | grep -c "not found") == 0 ]]; then
  sudo snap install --classic kotlin
fi

# Terraform install
TERRAFORM_VERSION="0.12.9"
installed_version=$(terraform -v | head -n 1 | awk {'print $2'} | sed 's/v//g')
if [[ $installed_version != $TERRAFORM_VERSION ]]; then
  printf "Installing Terraform v%s...\n" "$TERRAFORM_VERSION"
  curl -L https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip -o /tmp/terraform.zip
  sudo unzip -o /tmp/terraform.zip -d /usr/local/bin
  sudo chmod +x /usr/local/bin/terraform && rm /tmp/terraform.zip
  printf "Terraform installed. %s\n" "$(terraform -v)"
fi

# Terraform plugins install
AWS_PROVIDER_VERSION="2.28.1"
GCP_PROVIDER_VERSION="2.15.0"
mkdir -p ~/.terraform.d/plugins
if [[ ! -f ~/.terraform.d/plugins/terraform-provider-aws_v${AWS_PROVIDER_VERSION}_x4 ]]; then
  printf "Installing Terraform AWS provider plugin v%s...\n" "$AWS_PROVIDER_VERSION"
  curl -L https://releases.hashicorp.com/terraform-provider-aws/${AWS_PROVIDER_VERSION}/terraform-provider-aws_${AWS_PROVIDER_VERSION}_linux_amd64.zip -o /tmp/aws_provider.zip
  unzip -o /tmp/aws_provider.zip -d ~/.terraform.d/plugins && rm /tmp/aws_provider.zip
  printf "Terraform AWS plugins installed\n"
fi
if [[ ! -f ~/.terraform.d/plugins/terraform-provider-google_v${GCP_PROVIDER_VERSION}_x4 ]]; then
  printf "Installing Terraform GCP provider plugin v%s...\n" "$GCP_PROVIDER_VERSION"
  curl -L https://releases.hashicorp.com/terraform-provider-google/${GCP_PROVIDER_VERSION}/terraform-provider-google_${GCP_PROVIDER_VERSION}_linux_amd64.zip -o /tmp/google_provider.zip
  unzip -o /tmp/google_provider.zip -d ~/.terraform.d/plugins && rm /tmp/google_provider.zip
  printf "Terraform Google plugins installed\n"
fi