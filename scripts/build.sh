#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

echo "Building backend..."
(cd backend && ./mvnw -q -B -DskipTests package)

echo "Building frontend..."
(cd frontend && npm run build)

echo "Build complete."
