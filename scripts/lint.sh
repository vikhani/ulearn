#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

echo "Linting frontend..."
(cd frontend && npm run lint)

echo "Validating OpenSpec artifacts..."
openspec validate --changes --strict

echo "Lint complete."
