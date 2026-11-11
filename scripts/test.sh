#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

echo "Testing backend..."
(cd backend && ./gradlew --no-daemon test)

echo "Testing frontend..."
(cd frontend && npm test)

echo "Tests complete."
