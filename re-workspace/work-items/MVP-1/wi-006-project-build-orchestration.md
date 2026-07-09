# Work Item: WI-006 — Project Build Orchestration

**Source:** Project structure gap analysis
**Created:** 2026-07-07
**Parent Requirement:** None (cross-cutting infrastructure requirement)
**Status:** Completed
**Completed:** 2026-07-08
**Implemented By:** Femke (Frontend build fix), run_MVP1_locally.sh created

---

## Purpose

Provide a single entry point to build, test, and verify the entire Gimme Vetting Solution project from the repository root. Currently, each subproject (frontend and backend) has its own build tool but there is no root-level orchestration.

---

## Current State

| Component | Build Tool | Location | Root-level Script |
|-----------|-----------|----------|-------------------|
| Frontend | Vite + npm | [`4-frontend/package.json`](4-frontend/package.json) | Exists (`npm run build`, `npm test`) |
| Backend | Maven (multi-module) | [`5-backend/pom.xml`](5-backend/pom.xml) | Exists (`mvn clean package`) |
| **Project Root** | — | — | **Missing** |

The project root (`/home/luukie/Documents/Gimme vetting solution`) contains no build orchestration. There is no `Makefile`, no `build.sh`, no root `package.json`, and no CI configuration.

---

## Functional Requirements

### FR-001: Single Command Full Build
- **Description**: A single command from the project root must trigger builds for all subprojects in the correct dependency order.
- **Priority**: High
- **Acceptance Criteria**:
  - Running the build command from the project root succeeds without needing to `cd` into subdirectories.
  - Backend builds before frontend (frontend has no runtime dependency on backend, but logical flow requires backend artifacts to be available).
  - All subproject tests pass as part of the build.

### FR-002: Incremental Build Support
- **Description**: The build system must support building individual subprojects without rebuilding the entire project.
- **Priority**: Medium
- **Acceptance Criteria**:
  - A command targeting only the backend succeeds without invoking the frontend build.
  - A command targeting only the frontend succeeds without invoking the backend build.

### FR-003: Test Execution
- **Description**: Running tests must be a first-class operation, both individually and as part of the full build.
- **Priority**: High
- **Acceptance Criteria**:
  - A dedicated test command runs all tests across all subprojects.
  - A dedicated test command can target a specific subproject.
  - Test results are reported in a human-readable format.

### FR-004: Clean Operation
- **Description**: A clean command must remove all build artifacts from all subprojects.
- **Priority**: Medium
- **Acceptance Criteria**:
  - Running the clean command removes build outputs from both `4-frontend/dist` (or equivalent) and `5-backend/**/target`.
  - After clean, a full rebuild must succeed from scratch.

---

## Non-Functional Requirements

### NFR-001: Cross-Platform Compatibility
- **Description**: The build system should work on Linux and macOS at minimum (Windows is a stretch goal).
- **Metrics**: Build script executes successfully on Ubuntu 22.04+ and macOS 13+ with identical commands.

### NFR-002: Tool Availability Detection
- **Description**: The build system must detect available tools and provide clear error messages when required tools are missing.
- **Metrics**: Missing `node`, `npm`, `mvn`, or `java` results in a clear error message listing the missing tool and a suggested installation command.

### NFR-003: Build Idempotency
- **Description**: Running the build twice without changes must be fast (no unnecessary recompilation where the underlying tool supports it).
- **Metrics**: Second build without changes completes in under 30 seconds for the full project (on reference hardware: 4-core CPU, SSD).

---

## Implementation Options

### Option A: Shell Script (`build.sh`)
- A POSIX-compliant shell script at the project root.
- Pros: Simple, no dependencies, works everywhere with a shell.
- Cons: Limited error handling, less portable than Make.

### Option B: Makefile
- A GNU Make Makefile at the project root.
- Pros: Well-established convention, supports incremental builds naturally, clear dependency declarations.
- Cons: Requires Make installation (standard on Linux/macOS, needs WSL on Windows).

### Option C: Root `package.json` with `prebuild`/`postbuild` hooks
- A minimal root-level Node.js project that orchestrates via npm scripts.
- Pros: Unified ecosystem if frontend team is Node-centric.
- Cons: Introduces Node.js as a build dependency for backend work, adds unnecessary indirection.

### Recommendation: **Option B (Makefile)**
- Make is the most natural fit for a multi-component build with clear dependencies.
- It supports incremental builds out of the box.
- It is the standard convention for multi-language projects.

---

## Proposed Makefile Targets

| Target | Description |
|--------|-------------|
| `make build` | Full build: backend compile + test, frontend compile |
| `make test` | Run all tests across all subprojects |
| `make clean` | Remove all build artifacts |
| `make backend` | Build backend only |
| `make frontend` | Build frontend only |
| `make backend-test` | Test backend only |
| `make frontend-test` | Test frontend only |
| `make check-tools` | Verify required tools are installed |

---

## Dependency Chain

```
make build
├── make backend-check        (verify tools)
├── make backend              (mvn clean package -DskipTests)
│   └── 5-backend/pom.xml
├── make frontend             (npm run build)
│   └── 4-frontend/package.json
└── make verify              (optional: smoke check that all artifacts exist)
```

---

## Out of Scope

- CI/CD pipeline configuration (separate work item)
- Docker container build orchestration (future enhancement)
- Linting or code formatting as part of the build (can be added as prerequisites later)
- Windows native support (documented as known limitation)

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Maven or Node not installed | Build fails silently | `check-tools` target validates prerequisites |
| Different Java versions across environments | Inconsistent builds | Document minimum Java version (17+) in README |
| npm dependency changes break builds | Frontend build fails | Document that `npm ci` should be run after `package.json` changes |

---

## Dependencies

- None. This is a foundational infrastructure work item.

---

## Acceptance Summary

This work item is complete when:
1. A `Makefile` exists at the project root with all targets listed above.
2. `make build` succeeds from a clean checkout.
3. `make test` runs all tests and reports results.
4. `make clean` removes all build artifacts.
5. Individual subproject targets (`make backend`, `make frontend`) work independently.
6. A `README.md` entry or similar documents the build commands for contributors.
