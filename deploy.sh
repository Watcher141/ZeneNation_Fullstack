#!/usr/bin/env bash
# =============================================================================
#  ZeneNation Fast Re-Deployment Script
#  Run this script on the VPS whenever you push new changes
# =============================================================================

set -e
PROJECT_DIR="/var/www/zenenation"

cd "$PROJECT_DIR"

echo "==> Pulling latest changes from Git..."
git pull origin main

echo "==> Building and starting backend & database containers..."
docker compose down
docker compose up -d --build

echo "==> Building frontend..."
cd frontend
npm ci
npm run build
cd ..

echo "==> Reloading Nginx..."
sudo systemctl reload nginx

echo "==> Checking services health..."
docker compose ps
curl -s http://127.0.0.1:8080/actuator/health || true
echo ""
echo " Deployment finished successfully!"

