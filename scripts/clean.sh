#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

echo "Cleaning backend..."
(cd backend && ./gradlew --no-daemon clean)

echo "Cleaning frontend..."
rm -rf frontend/dist frontend/node_modules

echo "Stopping and removing containers and volumes..."
docker compose down -v

echo "Clean complete."
