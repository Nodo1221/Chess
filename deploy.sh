#!/usr/bin/env bash
# Deploy latest build to the droplet.
# Usage: bash deploy.sh <user@droplet-ip>
# Example: bash deploy.sh root@123.45.67.89
set -euo pipefail

REMOTE="${1:?Usage: $0 <user@host>}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "==> Building client..."
cd "$SCRIPT_DIR/client"
npm ci
npm run build   # outputs to client/dist

echo "==> Building server..."
cd "$SCRIPT_DIR/server"
./mvnw -q package -DskipTests   # outputs to server/target/server-*.jar

JAR=$(ls "$SCRIPT_DIR"/server/target/server-*.jar | grep -v sources | head -1)

echo "==> Uploading to $REMOTE..."
ssh "$REMOTE" "mkdir -p /opt/chess/public"
scp "$JAR" "$REMOTE":/opt/chess/server.jar
rsync -a --delete "$SCRIPT_DIR/client/dist/" "$REMOTE":/opt/chess/public/

echo "==> Restarting service..."
ssh "$REMOTE" "systemctl restart chess && systemctl reload nginx"

echo ""
echo "✓ Deployed. Visit http://$(echo "$REMOTE" | cut -d@ -f2)"
