#!/usr/bin/env bash
# Run the entire MVP locally on a clean slate.
# Usage: ./run_MVP1_locally.sh
# Then open http://localhost:5173 in your browser.
# No PostgreSQL required. The backend runs with an in-memory H2 database.
#
# NOTE: This script performs a full clean build before each run.
#       Expect 1-4 minutes for the first run.

set -egit branch

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# --- Cleanup ---
cleanup_stale_processes() {
  local port_pid
  port_pid=$(lsof -ti :8082 2>/dev/null || true)
  if [ -n "$port_pid" ]; then
    echo "WARNING: Killing stale process on port 8082 (PID: $port_pid)"
    kill -9 $port_pid 2>/dev/null || true
  fi
  port_pid=$(lsof -ti :5173 2>/dev/null || true)
  if [ -n "$port_pid" ]; then
    echo "WARNING: Killing stale process on port 5173 (PID: $port_pid)"
    kill -9 $port_pid 2>/dev/null || true
  fi
}

cleanup() {
  echo ""
  echo "Stopping services..."
  if [ -n "${BACKEND_PID:-}" ]; then
    kill $BACKEND_PID 2>/dev/null || true
  fi
  if [ -n "${FRONTEND_PID:-}" ]; then
    kill $FRONTEND_PID 2>/dev/null || true
  fi
  wait $BACKEND_PID 2>/dev/null || true
  wait $FRONTEND_PID 2>/dev/null || true
  echo "Done."
}

# --- Main Flow ---
trap cleanup EXIT INT TERM

echo "============================================"
echo "  Gimme Vetting Solution - Local MVP (Clean)"
echo "  This will clean, build, and start services."
echo "  Estimated time: 1-4 minutes."
echo "============================================"
echo ""

# Step 1: Clean
echo "=== Step 1: Cleaning build artifacts ==="
cleanup_stale_processes
cd "$SCRIPT_DIR/5-backend"
mvn clean -q || echo "WARNING: mvn clean returned an error, continuing anyway."
cd "$SCRIPT_DIR/4-frontend"
rm -rf dist node_modules/.vite 2>/dev/null || true
echo "Cleanup complete."
echo ""

# Step 2: Backend Build
echo "=== Step 2: Building backend ==="
echo "(may take 1-3 minutes)..."
cd "$SCRIPT_DIR/5-backend"
mvn clean package -DskipTests
echo "Backend build complete."
echo ""

# Step 3: Frontend Build
echo "=== Step 3: Building frontend ==="
echo "(may take 30-60 seconds)..."
cd "$SCRIPT_DIR/4-frontend"
npm run build || echo "WARNING: Frontend build failed. Backend will still start."
echo "Frontend build complete."
echo ""

# Step 4: Start Backend
echo "=== Step 4: Starting backend ==="
cd "$SCRIPT_DIR/5-backend"
mvn spring-boot:run -pl business-service &
BACKEND_PID=$!
echo "Backend PID: $BACKEND_PID"

# Wait for backend readiness (up to 60 seconds)
echo "Waiting for backend to start..."
for i in $(seq 1 60); do
  if curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/api/v1/intake 2>/dev/null | grep -q "400\|405\|200"; then
    echo "Backend is ready."
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "ERROR: Backend did not respond after 60 seconds. Aborting."
    kill $BACKEND_PID 2>/dev/null || true
    exit 1
  fi
  sleep 1
done
echo ""

# Step 5: Start Frontend
echo "=== Step 5: Starting frontend ==="
cd "$SCRIPT_DIR/4-frontend"
npm run dev &
FRONTEND_PID=$!
echo "Frontend PID: $FRONTEND_PID"
echo ""

echo "============================================"
echo "  Open http://localhost:5173 in your browser"
echo "  Press Ctrl+C to stop both services"
echo "============================================"

wait
