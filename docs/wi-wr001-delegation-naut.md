# Delegation Plan: WR-001 — Clean-Slate Local Development Script

## Architecture Constraints

- This is a **DevOps / script-only** change. No production code, no API contracts, no database changes.
- The script must be POSIX-compatible (`#!/usr/bin/env bash`) to run on macOS and Linux.
- If the backend runs on Windows (via `run_MVP1_locally.bat`), equivalent changes must be applied there.

## Input Artefacts

| Artefact | Path |
|----------|------|
| WR-001 Specification | [`re-workspace/work-items/run-mvp1-clean-slate/wr-001-clean-slate-local-run.md`](re-workspace/work-items/run-mvp1-clean-slate/wr-001-clean-slate-local-run.md) |
| Current script (shell) | [`run_MVP1_locally.sh`](run_MVP1_locally.sh) |
| Current script (Windows) | [`run_MVP1_locally.bat`](run_MVP1_locally.bat) |

## Output Artefact

| Artefact | Path |
|----------|------|
| Updated local run script (shell) | [`run_MVP1_locally.sh`](run_MVP1_locally.sh) |
| Updated local run script (Windows, if applicable) | [`run_MVP1_locally.bat`](run_MVP1_locally.bat) |

## Subtask 1: Update `run_MVP1_locally.sh` with Clean-Slate Behaviour

- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `re-workspace/work-items/run-mvp1-clean-slate/wr-001-clean-slate-local-run.md`
- **Output Artefact**: Updated `run_MVP1_locally.sh`
- **Constraints**:
  - Implement all five steps defined in Section 5.2 of the WR-001 specification.
  - Step 1: Kill stale processes on ports 8082 and 5173 (already exists, keep).
  - Step 2: Clean build artifacts — `mvn clean` in `5-backend/`, remove `4-frontend/dist` and `4-frontend/node_modules/.vite`.
  - Step 3: Full backend build — `mvn clean package -DskipTests` in `5-backend/`.
  - Step 4: Full frontend build — `npm run build` in `4-frontend/`.
  - Step 5: Start backend via `mvn spring-boot:run -pl business-service` with 60-second readiness timeout.
  - Step 6: Start frontend via `npm run dev` in `4-frontend/`.
  - All steps must print structured progress messages as defined in FR-WR001-05.
  - If the backend build fails, abort without starting any services (FR-WR001-03).
  - If the frontend build fails, start the backend but print a warning (FR-WR001-03).
  - Preserve the existing cleanup trap for graceful Ctrl+C handling.
  - The script must maintain `set -e` behavior (fail fast on build errors).

## Subtask 2: Update `run_MVP1_locally.bat` with Equivalent Windows Behaviour

- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `re-workspace/work-items/run-mvp1-clean-slate/wr-001-clean-slate-local-run.md`
- **Output Artefact**: Updated `run_MVP1_locally.bat`
- **Constraints**:
  - If `run_MVP1_locally.bat` does not exist, check whether it is needed for the project's target platforms. If it exists, implement equivalent clean-slate behavior.
  - Use `taskkill` for process termination on Windows.
  - Use `rmdir /S /Q` for directory removal.
  - Use PowerShell or `netstat`-based port checks for stale process detection on Windows.
  - Backend readiness check on Windows may require a different curl equivalent (`Invoke-WebRequest` or `Test-NetConnection`).
  - If Windows is not a target platform for this project, skip this subtask and document the decision.

## Acceptance Criteria

1. `./run_MVP1_locally.sh` prints five numbered step labels (Section 5.2 of WR-001 spec).
2. A clean build completes successfully from a state where `5-backend/target/` and `4-frontend/dist/` have been manually deleted.
3. Backend responds on `http://localhost:8082/api/v1/intake` within 60 seconds.
4. Frontend dev server starts on `http://localhost:5173`.
5. Uploading an Excel file through the frontend works end-to-end.
6. Running the script twice in succession produces the same result (idempotency).
7. Introducing a backend compilation error causes the script to abort with a clear error message and no services running.
8. Running the script while a stale backend is on port 8082 detects and kills it (existing behavior preserved).

## Parallel Phase Completion Criteria

The task is considered complete when all acceptance criteria pass and the script is committed to the repository.
