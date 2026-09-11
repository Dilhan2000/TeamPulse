#!/usr/bin/env bash
# ==============================================================================
# Backend Startup Script - Weekly Report & Team Dashboard
# Installs dependencies, applies environment variables, and runs Spring Boot.
# ==============================================================================

set -e

# Change to script directory (backend root)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=================================================="
echo "  Starting Weekly Report Backend (Spring Boot)"
echo "=================================================="

# 1. Check Java version
if ! command -v java >/dev/null 2>&1; then
    echo "Error: Java is not installed or not in PATH."
    echo "Please install Java 21 LTS (JDK 21) to proceed."
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
echo "☕ Detected Java version: $JAVA_VER"
if [ "$JAVA_VER" -lt 21 ]; then
    echo "Warning: Java 21 or higher is recommended (found Java $JAVA_VER)."
fi

# 2. Load environment variables if .env exists in parent or current directory
if [ -f "$SCRIPT_DIR/../.env" ]; then
    echo "Loading environment variables from ../.env"
    set -a
    # shellcheck disable=SC1091
    source <(grep -v '^\s*#' "$SCRIPT_DIR/../.env" | grep -v '^\s*$')
    set +a
elif [ -f "$SCRIPT_DIR/.env" ]; then
    echo "Loading environment variables from .env"
    set -a
    # shellcheck disable=SC1091
    source <(grep -v '^\s*#' "$SCRIPT_DIR/.env" | grep -v '^\s*$')
    set +a
fi

# 3. Ensure Gradle wrapper is executable
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
else
    echo "Error: gradlew wrapper not found in $SCRIPT_DIR"
    exit 1
fi

# 4. Install dependencies / compile classes
echo "Resolving dependencies and compiling..."
./gradlew classes --no-daemon

# 5. Start Spring Boot application
echo "Running Spring Boot application on http://localhost:8080 ..."
./gradlew bootRun
