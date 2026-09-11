#!/usr/bin/env bash
# ==============================================================================
# Master Startup Script - Weekly Report & Team Dashboard
# Usage:
#   ./run.sh backend    - Install dependencies and start Spring Boot backend
#   ./run.sh frontend   - Install dependencies and start Angular frontend
#   ./run.sh            - Display usage options
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET="${1:-}"

case "$TARGET" in
  backend|server|api)
    exec "$SCRIPT_DIR/backend/run.sh"
    ;;
  frontend|client|ui)
    exec "$SCRIPT_DIR/frontend/run.sh"
    ;;
  *)
    echo "=================================================="
    echo "  Weekly Report Generator & Team Dashboard"
    echo "=================================================="
    echo "Usage:"
    echo "  ./run.sh backend    -> Run backend (Spring Boot on :8080)"
    echo "  ./run.sh frontend   -> Run frontend (Angular on :4200)"
    echo ""
    echo "Or run the individual scripts directly:"
    echo "  cd backend && ./run.sh"
    echo "  cd frontend && ./run.sh"
    echo "=================================================="
    exit 0
    ;;
esac
