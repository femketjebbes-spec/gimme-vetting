#!/usr/bin/env bash
# Run the entire MVP locally — backend + frontend with one command.
# Usage: ./run_MVP1_locally.sh
# Then open http://localhost:5173 in your browser.
# No PostgreSQL required. The backend runs with an in-memory H2 database.

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Kill any previous instances occupying our ports
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

# --- Backend ---
echo "=== Starting backend on http://localhost:8082 ==="
cleanup_stale_processes
cd "$SCRIPT_DIR/5-backend"
mvn spring-boot:run -pl business-service &
BACKEND_PID=$!
echo "Backend PID: $BACKEND_PID"

# Wait for backend to be ready (up to 30 seconds)
echo "Waiting for backend to start..."
for i in $(seq 1 30); do
  if curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/api/v1/intake 2>/dev/null | grep -q "400\|405\|200"; then
    echo "Backend is ready."
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "WARNING: Backend did not respond after 30 seconds. It may still be starting."
  fi
  sleep 1
done

# --- Frontend ---
echo ""
echo "=== Starting frontend on http://localhost:5173 ==="
cd "$SCRIPT_DIR/4-frontend"
npm run dev &
FRONTEND_PID=$!
echo "Frontend PID: $FRONTEND_PID"

echo ""
echo "============================================"
echo "  Open http://localhost:5173 in your browser"
echo "  Press Ctrl+C to stop both services"
echo "============================================"

# Clean up on exit
cleanup() {
  echo ""
  echo "Stopping services..."
  kill $BACKEND_PID 2>/dev/null
  kill $FRONTEND_PID 2>/dev/null
  wait
  echo "Done."
}
trap cleanup EXIT INT TERM

wait
