#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

for tool in java node npm; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "error: '$tool' is required but was not found on PATH" >&2
    exit 1
  fi
done

if [ ! -f .env ]; then
  cp .env.example .env
  echo "Created .env from .env.example"
fi

echo "Installing backend dependencies..."
(cd backend && ./mvnw -q -B -DskipTests dependency:resolve)

echo "Installing frontend dependencies..."
(cd frontend && npm ci)

echo "Setup complete."
