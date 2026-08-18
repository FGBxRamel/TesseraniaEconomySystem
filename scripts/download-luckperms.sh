#!/bin/bash
# Downloads the latest Paper-compatible LuckPerms build into the given plugins directory,
# skipping the download if that exact build is already present. Used by the local dev/test
# server (see docs/dev-server.md) via exec-maven-plugin — LuckPerms is not on Hangar, only on
# Modrinth and luckperms.net, so it can't be pulled through a Maven repository.
set -euo pipefail

PLUGINS_DIR="${1:?Usage: download-luckperms.sh <plugins-directory>}"
mkdir -p "$PLUGINS_DIR"

URL=$(curl -fsSL "https://api.modrinth.com/v2/project/luckperms/version?loaders=%5B%22paper%22%5D" \
  | python3 -c "import json, sys; print(json.load(sys.stdin)[0]['files'][0]['url'])")
FILENAME=$(basename "$URL")

if [ -f "$PLUGINS_DIR/$FILENAME" ]; then
  echo "LuckPerms already up to date ($FILENAME)"
  exit 0
fi

echo "Downloading latest LuckPerms ($FILENAME)..."
rm -f "$PLUGINS_DIR"/LuckPerms-Bukkit-*.jar
curl -fsSL "$URL" -o "$PLUGINS_DIR/$FILENAME"
echo "Installed $FILENAME"
