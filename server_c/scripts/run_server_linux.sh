#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$SCRIPT_DIR/.."

echo "==================================================="
echo "[START] Compiling & Launching C Server (Linux/POSIX)"
echo "==================================================="
cd "$SERVER_DIR" || exit 1
make
./server 8888 exam_system.db
