#!/usr/bin/env bash
# ==============================================================================
# Frontend Startup Script - Weekly Report & Team Dashboard
# Installs npm dependencies and runs the Angular development server.
# ==============================================================================

set -e

# Change to script directory (frontend root)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=================================================="
echo "  Starting Weekly Report Frontend (Angular)"
echo "=================================================="

# 1. Check Node.js and npm
if ! command -v node >/dev/null 2>&1; then
    echo "Error: Node.js is not installed or not in PATH."
    echo "Please install Node.js LTS (v18 or v20) to proceed."
    exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
    echo "Error: npm is not installed or not in PATH."
    exit 1
fi

NODE_VER=$(node -v)
NPM_VER=$(npm -v)
echo "Detected Node: $NODE_VER, npm: $NPM_VER"

# 2. Install dependencies
if [ ! -d "node_modules" ]; then
    echo "node_modules not found. Installing dependencies via npm install..."
    npm install
else
    echo "Checking and updating dependencies (npm install)..."
    npm install --prefer-offline --no-audit
fi

# 3. Start Angular development server
echo "Starting Angular dev server on http://localhost:4200 ..."
npm start
