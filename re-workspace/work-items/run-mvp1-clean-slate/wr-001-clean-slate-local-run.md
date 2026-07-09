# Work Item WR-001: `run_MVP1_locally.sh` Starts on a Clean Slate

- **Document ID**: WR-001
- **Version**: 1.0
- **Last Updated**: 2026-07-09
- **Status**: Proposed
- **Priority**: Medium
- **Component**: DevOps / Scripts (`run_MVP1_locally.sh`)
- **Related Bug**: [BR-001](../../bug-reports/BR-001-mime-type-based-file-validation.md) (operational root cause: stale backend process serving old compiled classes)

---

## 1. Overview

The current [`run_MVP1_locally.sh`](../../run_MVP1_locally.sh) script starts the backend and frontend services but **does not rebuild or clean** before launch. This means that if a developer modifies source code and re-runs the script, the old compiled classes continue to be served — as observed in [BR-001](../../bug-reports/BR-001-mime-type-based-file-validation.md) where the fix was compiled but the stale backend process continued serving the pre-fix code.

This work item requires updating `run_MVP1_locally.sh` so that every invocation starts on a **clean slate**: stale processes are killed, build artifacts are removed, services are rebuilt, and fresh instances are launched.

---

## 2. Problem Statement

When developers modify backend or frontend source code and re-run `./run_MVP1_locally.sh`, the script:

1. Kills stale processes on ports 8082 and 5173 (already implemented).
2. **Launches the backend directly via `mvn spring-boot:run`** without a prior `mvn clean package`.
3. **Launches the frontend via `npm run dev`** without a prior clean build.

`mvn spring-boot:run` uses the **previously compiled classes** in the `target/` directory. If `mvn clean package` was not run before the script, the running backend serves stale bytecode. This is the operational root cause identified in [BR-001](../../bug-reports/BR-001-mime-type-based-file-validation.md:223).

---

## 3. Functional Requirements

### FR-WR001-01: Kill Stale Processes Before Each Run

Before starting any new services, the script MUST ensure no existing processes occupy the required ports.

**Acceptance Criteria:**

1. The script MUST check for and kill any process on port 8082 (backend) before starting the backend.
2. The script MUST check for and kill any process on port 5173 (frontend dev server) before starting the frontend.
3. If no process is found on a port, the script MUST continue without error.
4. The script MUST print a warning message when killing a stale process, including the PID.

### FR-WR001-02: Clean Build Artifacts Before Each Run

Before building and starting the services, the script MUST remove all previous build artifacts to ensure fresh compilation.

**Acceptance Criteria:**

1. The script MUST run `make clean` (or equivalent `mvn clean` + `rm -rf 4-frontend/dist`) before building.
2. Backend `target/` directories under `5-backend/` MUST be removed.
3. Frontend `dist/` and `node_modules/.vite/` directories under `4-frontend/` MUST be removed.
4. If any cleanup command fails (e.g., directory does not exist), the script MUST continue without error (use `|| true` or equivalent).

### FR-WR001-03: Rebuild Services Before Each Run

Before starting the services, the script MUST build both the backend and frontend from scratch.

**Acceptance Criteria:**

1. The script MUST run a full backend build (`mvn clean package -DskipTests`) in the `5-backend/` directory before starting `spring-boot:run`.
2. The script MUST run a full frontend build (`npm run build`) in the `4-frontend/` directory before starting the dev server.
3. If the backend build fails, the script MUST abort and NOT start any services. Print a clear error message.
4. If the frontend build fails, the script MUST start the backend (if not already running) but print a clear warning and NOT start the frontend dev server.

### FR-WR001-04: Start Fresh Service Instances

After building, the script MUST start fresh instances of the backend and frontend.

**Acceptance Criteria:**

1. Backend MUST be started via `mvn spring-boot:run` in the `business-service` module, running on port 8082.
2. Frontend MUST be started via `npm run dev` in the `4-frontend/` directory, running on port 5173.
3. The script MUST wait for the backend to be ready (health check via `curl`) before starting the frontend.
4. The backend readiness check MUST poll up to 60 seconds (increased from 30) since a clean build takes longer than incremental builds.

### FR-WR001-05: Logging and User Feedback

The script MUST provide clear feedback at each step so the developer understands what is happening.

**Acceptance Criteria:**

1. The script MUST print `"=== Step 1: Cleaning build artifacts ==="` before cleanup.
2. The script MUST print `"=== Step 2: Building backend ==="` before the backend build.
3. The script MUST print `"=== Step 3: Building frontend ==="` before the frontend build.
4. The script MUST print `"=== Step 4: Starting backend ==="` before starting the backend.
5. The script MUST print `"=== Step 5: Starting frontend ==="` before starting the frontend.
6. Each step MUST print completion messages (e.g., `"Backend build complete."`).
7. The total estimated time per step MUST be communicated where applicable (e.g., `"Backend build in progress (may take 1-3 minutes)..."`).

### FR-WR001-06: Windows Compatibility via `run_MVP1_locally.bat`

The Windows batch script counterpart MUST receive equivalent updates.

**Acceptance Criteria:**

1. `run_MVP1_locally.bat` MUST implement the same clean-slate behavior as the shell script.
2. Platform-specific commands MUST be adapted (e.g., `taskkill` instead of `kill`, `rmdir /S /Q` instead of `rm -rf`).
3. If the batch script does not exist, it MUST be created.

---

## 4. Non-Functional Requirements

### NFR-WR001-01: Build Time

A clean build is slower than an incremental build. The script should communicate this to the user.

| Step | Typical Duration (SSD) |
|------|----------------------|
| Cleanup | < 1 second |
| Backend clean build (`mvn clean package -DskipTests`) | 30–90 seconds |
| Frontend clean build (`npm run build`) | 10–30 seconds |
| Backend startup + readiness | 10–20 seconds |
| Frontend dev server startup | 5–10 seconds |
| **Total estimated** | **~1–4 minutes** |

### NFR-WR001-02: Idempotency

Running the script multiple times in succession must always produce the same result — a clean build and fresh service instances. Partial failures must not leave the system in an inconsistent state.

### NFR-WR001-03: Graceful Exit

If the script is interrupted (Ctrl+C), all child processes MUST be terminated cleanly. The trap handler MUST kill both backend and frontend PIDs.

---

## 5. Implementation Notes

### 5.1 Current Script Analysis

| Step | Current Behavior | Required Change |
|------|-----------------|-----------------|
| Kill stale processes | Done (lines 12–24) | No change needed |
| Build backend | None — `mvn spring-boot:run` uses stale classes | Add `mvn clean package -DskipTests` before `spring-boot:run` |
| Build frontend | None — `npm run dev` uses existing artifacts | Add `npm run build` before `npm run dev` (optional for dev server but ensures clean state) |
| Backend readiness check | 30 seconds timeout | Increase to 60 seconds |
| Logging | Minimal step labels | Add structured step-by-step logging |

### 5.2 Proposed `run_MVP1_locally.sh` Structure

```bash
#!/usr/bin/env bash
# Run the entire MVP locally on a clean slate.
# Usage: ./run_MVP1_locally.sh
# Then open http://localhost:5173 in your browser.
# No PostgreSQL required. The backend runs with an in-memory H2 database.
#
# NOTE: This script performs a full clean build before each run.
#       Expect 1-4 minutes for the first run.

set -e

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
  kill $BACKEND_PID 2>/dev/null || true
  kill $FRONTEND_PID 2>/dev/null || true
  wait $BACKEND_PID 2>/dev/null || true
  wait $FRONTEND_PID 2>/dev/null || true
  echo "Done."
}

# --- Main Flow ---
trap cleanup EXIT INT TERM

echo "============================================"
echo "  Gimme Vetting Solution — Local MVP (Clean)"
echo "  This will clean, build, and start services."
echo "  Estimated time: 1-4 minutes."
echo "============================================"
echo ""

# Step 1: Clean
echo "=== Step 1: Cleaning build artifacts ==="
cleanup_stale_processes
cd "$SCRIPT_DIR/5-backend"
mvn clean -q || true
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
npm run build
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
```

### 5.3 Frontend Dev Server Note

The frontend `npm run dev` (Vite dev server) does not produce a `dist/` directory — it serves files from memory. The `npm run build` step before `npm run dev` is included to validate the build compiles without errors, but the dev server does not depend on the built output. An alternative is to run `npm run build` only as a validation step and then start `npm run dev` immediately.

---

## 6. Test Plan

### TR-WR001-01: Full Clean Run from Scratch

1. Stop all services.
2. Run `./run_MVP1_locally.sh`.
3. Verify output includes all five step labels.
4. Verify backend starts and responds on port 8082.
5. Verify frontend starts and responds on port 5173.
6. Upload an Excel file through the frontend and verify end-to-end processing works.

### TR-WR001-02: Re-run After Code Changes

1. Modify a backend Java source file (e.g., add a comment to `IntakeService.java`).
2. Re-run `./run_MVP1_locally.sh`.
3. Verify the script cleans and rebuilds before restarting.
4. Verify the running backend reflects the new code.

### TR-WR001-03: Build Failure Handling

1. Introduce a compilation error in a backend Java file.
2. Run `./run_MVP1_locally.sh`.
3. Verify the script aborts with a clear error message.
4. Verify no stale processes are left running.

### TR-WR001-04: Stale Process Detection

1. Start a backend manually: `cd 5-backend && mvn spring-boot:run -pl business-service &`.
2. Run `./run_MVP1_locally.sh` in another terminal.
3. Verify the script detects and kills the stale process.
4. Verify the new clean instance starts correctly.

---

## 7. Completion Criteria

This work item is complete when:

1. [`run_MVP1_locally.sh`](../../run_MVP1_locally.sh) implements all five steps (clean, backend build, frontend build, backend start, frontend start).
2. [`run_MVP1_locally.bat`](../../run_MVP1_locally.bat) implements equivalent behavior on Windows (or is confirmed not required for the project's target platforms).
3. The script waits up to 60 seconds for backend readiness.
4. All four test cases in Section 6 pass.
5. No code changes are required in the backend or frontend — this is a script-only change.
