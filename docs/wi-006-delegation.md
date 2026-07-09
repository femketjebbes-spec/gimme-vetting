# Delegation Plan: Wi-006 — Project Build Orchestration

## Architecture Constraints

- Makefile must be placed at the project root: `/home/luukie/Documents/Gimme vetting solution/Makefile`
- Backend is located at `5-backend/` with Maven multi-module structure (`5-backend/pom.xml`)
- Frontend is located at `4-frontend/` with Vite + npm (`4-frontend/package.json`)
- Frontend build output: `4-frontend/dist`
- Backend build output: `5-backend/**/target`
- No authentication or API contract changes are involved
- No production code is modified

## Subtasks

### Subtask 1: Create Project Root Makefile
- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `re-workspace/work-items/wi-006-project-build-orchestration.md` (WI-006 specification)
- **Output Artefact**: `Makefile` at project root with targets: `build`, `test`, `clean`, `backend`, `frontend`, `backend-test`, `frontend-test`, `check-tools`
- **Constraints**: 
  - Must be POSIX-compliant with GNU Make extensions
  - `make build` must build backend first, then frontend
  - `make check-tools` must verify `mvn`, `node`, `npm` are installed with clear error messages
  - `make clean` must remove `4-frontend/dist` and `5-backend/**/target`
  - `make test` must run `mvn test` for backend and `npm test` for frontend
  - Each target must fail fast (stop on first error)
- **Security Considerations**: None. This is build infrastructure only.

## Completion Criteria

The subtask is complete when:
1. `Makefile` exists at the project root
2. `make build` succeeds from the project root
3. `make test` runs all tests and reports results
4. `make clean` removes all build artifacts
5. `make check-tools` produces clear error messages for missing tools
6. Naut submits an alignment review request to the Alignment Agent at `docs/alignment-review-request.md`

## Testing Steps for Naut

After creating the Makefile, execute the following from the project root:
1. `make check-tools` — verify tool detection
2. `make clean` — clean all artifacts
3. `make build` — full build
4. `make test` — run all tests
5. `make clean` — clean again
6. `make backend` — backend only build
7. `make frontend` — frontend only build
