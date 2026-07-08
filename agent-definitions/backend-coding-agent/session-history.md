# Naut Session History

Summary of each session conducted by Naut.

## Session 1 - 2026-07-03

**Explored:** Naut's role boundaries, inputs, outputs, and position in the development pipeline. Established that Naut activates after Robbie produces requirements and Archibald produces architecture decisions and delegation plans. Clarified the strict backend-only constraint.

**Decided:** The agent uses Java with Maven as the build tool and JUnit 5 as the testing framework. The agent follows whatever architectural pattern Archibald defines. Frontend code is strictly off-limits. The agent's trigger is Archibald's delegation plan assigning backend subtasks.

**Remains Open:** Specific Java version, backend directory structure, Spring Boot version, and database technology. These will be defined by Robbie and Archibald before the agent activates on real tasks.

**Assumptions:** The project has a clear separation between backend and frontend code. Archibald will document the architectural pattern before the agent begins implementation work.

## Session 2 - 2026-07-06

**Explored:** The Gerard-to-Naut handover protocol. The user requested that the Alignment Agent gate be enforced before Naut activates, mirroring the Femke-to-Gerard pattern.

**Decided:** Naut's trigger section was updated to require confirmed Alignment Agent approval of Gerard's work in Archibald's delegation plan. The anti-patterns section was updated to include activation before Alignment Agent approval of Gerard's work as a user-request error to watch for.

**Remains Open:** None from this specific change.

**Assumptions:** Archibald reads the Alignment Agent decision and includes confirmation of `greenlightForNextAgent: true` for Gerard in the delegation plan passed to Naut.

## Session 1 - 2026-07-07 — WI-002 Backend Implementation

**Explored:** WI-002 delegation plan, API contract v2.0.0, architecture decisions D-024 through D-029. Implemented Excel file upload and batch processing pipeline endpoint.

**Implemented:**
- `ExcelInvoiceRow` domain object with rowIndex, invoiceNumber, debtorName, address, phoneNumber, bankAccountNumber, parseErrors fields
- `ExcelUploadResponse` response DTO matching contract section 5.1
- `InvalidFileFormatResponse` error DTO matching contract section 5.2
- `ColumnNameMismatchResponse` error DTO matching contract section 5.3
- `InternalErrorResponse` error DTO matching contract section 5.4
- `ExcelParsingService` with MIME type validation, filename sanitization, XLSX/CSV parsing, header detection, column name allowlist validation, column position mapping, empty row skipping, partial row handling, return Excel generation
- `ExcelIntakeController` with `POST /api/v1/intake/excel` endpoint and `GET /api/v1/intake/excel/download/{filename}` download endpoint

**Tests Written:** 55 tests (ExcelParsingServiceTest: 37 tests, ExcelIntakeControllerTest: 18 tests). All 69 tests pass (including existing WI-001 tests).

**Decided:**
- Used Apache POI 5.2.5 for both .xlsx and .csv parsing (D-029)
- Case-insensitive column name matching against fixed allowlist (D-024, S-008)
- Server-side MIME type validation using allowed list (S-007)
- Filename sanitization with path traversal check on raw input before path extraction (S-010)
- Return Excel includes "Issue" column with MISSING_FIELDS or MISSING_POC reason
- Synchronous processing model (D-028)
- No authentication (D-026) — Javadoc note included

**Open Questions:**
- Temporary file cleanup for return Excel files is not yet implemented. Files remain in temp directory until JVM shutdown.
- MIME type detection for CSV files from MultipartFile may return null for some upload clients. The controller handles this by falling back to file extension.

**Assumptions:** Business rule checks (RQ-002, RQ-003) are out of scope for WI-002. PoC existence verification is handled by downstream work items. All rows that pass mandatory field checks are considered "passed."

## Session 2 - 2026-07-07 — Wi-006 Project Build Orchestration

**Explored:** WI-006 delegation plan, WI-006 specification, backend Maven structure, frontend npm structure. Created the project root Makefile.

**Implemented:**
- [`Makefile`](Makefile) at project root with all eight targets: build, test, clean, backend, frontend, backend-test, frontend-test, check-tools
- `make build` — runs backend then frontend in dependency order
- `make test` — runs backend-test and frontend-test
- `make clean` — removes `4-frontend/dist` and `5-backend/**/target` directories
- `make backend` — runs `mvn clean package -DskipTests` in `5-backend/`
- `make frontend` — runs `npm run build` in `4-frontend/`
- `make backend-test` — runs `mvn test` in `5-backend/`
- `make frontend-test` — runs `npm test` in `4-frontend/`
- `make check-tools` — verifies mvn, node, npm are installed with clear error messages for missing tools

**Testing Results:**
- `make check-tools` — PASSED (all tools detected)
- `make clean` — PASSED (artifacts removed)
- `make backend` — PASSED (all 3 Maven modules compiled successfully)
- `make frontend` — FAILED (pre-existing project issue: missing `index.html` entry point in `4-frontend/`)
- `make frontend-test` — PASSED (16 tests passed across 2 test suites)
- `make backend-test` — FAILED (pre-existing project issue: missing PostgreSQL driver in client-service tests)
- `make build` — backend part PASSED, frontend part FAILED (same pre-existing frontend issue)
- `make test` — initiates backend-test then frontend-test (same pre-existing issues)

**Decided:**
- Makefile uses POSIX-compliant syntax with GNU Make extensions
- Each target fails fast (Make default behavior, no `|| true` except in clean for non-critical find)
- No production code was modified. Only the Makefile was created.
- No authentication, API contract, or business logic changes were made.

**Open Questions:**
- Frontend project lacks `index.html` entry point. This is a pre-existing project structure gap outside the Makefile scope.
- Client-service tests fail due to missing PostgreSQL driver. This is a pre-existing backend test configuration gap outside the Makefile scope.

**Assumptions:** The Makefile correctly delegates to Maven and npm. Pre-existing build issues in the frontend and client-service modules are outside the scope of Wi-006.
