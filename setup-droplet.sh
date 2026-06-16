#!/usr/bin/env bash
# One-time setup for a fresh Ubuntu 22.04/24.04 droplet.
# Run as root: bash setup-droplet.sh <your-droplet-ip-or-domain>
set -euo pipefail

HOST="${1:?Usage: $0 <host>}"

# ── Swap (prevents OOM on small droplets) ────────────────────────────────────
if [ ! -f /swapfile ]; then
    fallocate -l 2G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# ── Dependencies ──────────────────────────────────────────────────────────────
apt-get update -y
apt-get install -y curl gnupg2 nginx postgresql

# Java 21
apt-get install -y ca-certificates
curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
  | gpg --dearmor -o /etc/apt/trusted.gpg.d/adoptium.gpg
echo "deb https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo "$VERSION_CODENAME") main" \
  > /etc/apt/sources.list.d/adoptium.list
apt-get update -y
apt-get install -y temurin-21-jdk

# Node 22
curl -fsSL https://deb.nodesource.com/setup_22.x | bash -
apt-get install -y nodejs

# ── PostgreSQL ────────────────────────────────────────────────────────────────
systemctl enable postgresql --now
sudo -u postgres psql -c "CREATE USER chess WITH PASSWORD 'chess';" 2>/dev/null || true
sudo -u postgres psql -c "CREATE DATABASE chess OWNER chess;" 2>/dev/null || true

# ── App directory ─────────────────────────────────────────────────────────────
mkdir -p /opt/chess
useradd -r -s /bin/false chess 2>/dev/null || true
chown chess:chess /opt/chess

# ── Systemd service ───────────────────────────────────────────────────────────
cat > /etc/systemd/system/chess.service <<EOF
[Unit]
Description=Chess Spring Boot
After=network.target postgresql.service

[Service]
User=chess
WorkingDirectory=/opt/chess
ExecStart=/usr/bin/java -jar /opt/chess/server.jar
Restart=on-failure
Environment=DB_URL=jdbc:postgresql://localhost:5432/chess
Environment=DB_USER=chess
Environment=DB_PASSWORD=chess

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable chess

# ── Nginx ─────────────────────────────────────────────────────────────────────
cat > /etc/nginx/sites-available/chess <<EOF
server {
    listen 80;
    server_name ${HOST};

    root /opt/chess/public;
    index index.html;

    # Vue router — serve index.html for all non-asset paths
    location / {
        try_files \$uri \$uri/ /index.html;
    }

    # REST API proxy
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }

    # WebSocket proxy
    location /ws {
        proxy_pass http://127.0.0.1:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host \$host;
        proxy_read_timeout 3600s;
    }
}
EOF
ln -sf /etc/nginx/sites-available/chess /etc/nginx/sites-enabled/chess
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl enable nginx --now
systemctl reload nginx

echo ""
echo "✓ Setup complete. Now run deploy.sh from your local machine."
