#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

echo "Building backend..."
(cd backend && ./gradlew --no-daemon -x test bootJar)

echo "Building frontend..."
(cd frontend && npm run build)

echo "Build complete."
