#!/bin/bash
set -euo pipefail

# Script to upload a new version of the plugin to Modrinth using its HTTP API.
# Environment variables:
#   MODRINTH_TOKEN - API token (required)
#   PROJECT_ID     - Modrinth project ID (required)
#   VERSION_NAME   - Version display name (required)
#   VERSION_NUMBER - Version number (required)
#   GAME_VERSIONS_JSON - JSON array of supported Minecraft versions (default ["1.20.1"])
#   LOADERS_JSON       - JSON array of loaders (default ["spigot"])
#   VERSION_TYPE       - release, beta, or alpha (default release)
#   CHANGELOG_FILE     - optional changelog file
#   FEATURED           - whether the release should be featured (default false)
#   JAR_PATH           - path to plugin jar (default target/ReanimateMC.jar)

JAR_PATH=${JAR_PATH:-target/ReanimateMC.jar}
PROJECT_ID=${PROJECT_ID:?Project ID not set}
MODRINTH_TOKEN=${MODRINTH_TOKEN:?API token not set}
VERSION_NAME=${VERSION_NAME:?Version name not set}
VERSION_NUMBER=${VERSION_NUMBER:?Version number not set}
GAME_VERSIONS_JSON=${GAME_VERSIONS_JSON:-["1.20.1"]}
LOADERS_JSON=${LOADERS_JSON:-["spigot"]}
VERSION_TYPE=${VERSION_TYPE:-release}
CHANGELOG_FILE=${CHANGELOG_FILE:-}
FEATURED=${FEATURED:-false}

if [ ! -f "$JAR_PATH" ]; then
  echo "Jar file $JAR_PATH does not exist" >&2
  exit 1
fi

CHANGELOG=""
if [ -n "$CHANGELOG_FILE" ]; then
  CHANGELOG=$(sed 's/"/\\"/g' "$CHANGELOG_FILE")
fi

read -r -d '' JSON <<EOF_JSON
{
  "project_id": "$PROJECT_ID",
  "name": "$VERSION_NAME",
  "version_number": "$VERSION_NUMBER",
  "changelog": "$CHANGELOG",
  "game_versions": $GAME_VERSIONS_JSON,
  "version_type": "$VERSION_TYPE",
  "loaders": $LOADERS_JSON,
  "featured": $FEATURED,
  "dependencies": [],
  "file_parts": ["file"],
  "primary_file": "file"
}
EOF_JSON

curl -fSL \
  -H "Authorization: $MODRINTH_TOKEN" \
  -F "data=$JSON;type=application/json" \
  -F "file=@$JAR_PATH" \
  https://api.modrinth.com/v2/version
