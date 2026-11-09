#!/usr/bin/env bash
# =============================================================================
#  ZeneNation VPS Automated Initial Setup Script
#  Target OS: Ubuntu 24.04 LTS
# =============================================================================

set -e

echo "==> [1/6] Setting up 4GB Swap file (protects 2GB RAM from OOM crashes)..."
if [ ! -f /swapfile ]; then
    sudo fallocate -l 4G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=4096
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
    sudo sysctl vm.swappiness=10
    echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf
    echo "Swap created successfully!"
else
    echo "Swap file already exists."
fi

echo "==> [2/6] Updating system packages..."
sudo apt update && sudo apt upgrade -y

echo "==> [3/6] Installing Docker & Docker Compose Plugin..."
sudo apt install -y ca-certificates curl gnupg lsb-release
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg --yes
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"

echo "==> [4/6] Installing Nginx & Certbot (Let's Encrypt SSL)..."
sudo apt install -y nginx certbot python3-certbot-nginx git

echo "==> [5/6] Installing Node.js 20 LTS (for building frontend)..."
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

echo "==> [6/6] Configuring Firewall (UFW)..."
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw --force enable

echo "=========================================================="
echo " Setup complete! Next steps:"
echo " 1. Clone repository to /var/www/zenenation"
echo " 2. Copy .env.example to .env and fill in credentials"
echo " 3. Run docker compose up -d --build"
echo " 4. Build frontend: cd frontend && npm install && npm run build"
echo " 5. Link nginx config & run certbot --nginx -d zenenation.in -d www.zenenation.in"
echo "=========================================================="

