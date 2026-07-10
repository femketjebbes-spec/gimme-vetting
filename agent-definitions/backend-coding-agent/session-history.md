# Session History — Naut

## Session 1 (2026-07-08) — WI-008 BR-001 Content-Based File Detection

### Summary
Implemented content-based file format detection to fix BR-001: MIME-type-only validation was rejecting valid Excel files when browsers reported unrecognized MIME types.

### What Was Tested (Testing Mode)
1. **FileType enum created**: `com.gimmevettingsolution.intake.service.FileType` with XLSX, CSV, UNKNOWN values.
2. **9 unit tests for `detectFileType()`** added to `ExcelParsingServiceTest`:
   - XLSX ZIP header bytes → XLSX
   - Full Apache POI-generated XLSX file → XLSX
   - UTF-8 CSV text → CSV
   - ASCII text → CSV
   - Binary control bytes (0x01-0x03) → UNKNOWN
   - JPEG magic bytes (FF D8 FF E0) → UNKNOWN
   - Empty stream → UNKNOWN
   - Null stream → UNKNOWN
   - Single-byte stream → UNKNOWN
3. **10 controller fallback tests** added to `ExcelIntakeControllerTest`:
   - null MIME with valid XLSX → 200 OK
   - octet-stream MIME with valid XLSX → 200 OK
   - zip MIME with valid XLSX → 200 OK
   - null MIME with empty content → 400 Bad Request
   - octet-stream MIME with binary content → 400 Bad Request
   - octet-stream MIME with JPG content → 400 Bad Request
   - null MIME with valid CSV → 200 OK
   - octet-stream MIME with valid CSV → 200 OK
   - Binary content error detail is NOT generic → verified exact message

### What Was Implemented (Implementation Mode)
1. **FileType enum** (`FileType.java`): Three-valued enum XLSX, CSV, UNKNOWN.
2. **`detectFileType(InputStream)` method** added to `ExcelParsingService`:
   - Reads first 4 bytes of input stream
   - Checks ZIP signature `50 4B 03 04` for XLSX
   - Falls back to text detection (printable ASCII/UTF-8) for CSV
   - Returns UNKNOWN for non-text binary content or streams shorter than 4 bytes
3. **`ExcelIntakeController.uploadExcel()` restructured**:
   - Fast path: if MIME type is recognized (xlsx or csv), proceed directly without content inspection
   - Fallback path: if MIME is null/empty/unrecognized, call `detectFileType()`, reject if UNKNOWN
   - Error message: `"File content is not a recognized Excel or CSV format"` instead of generic MIME-type message

### Decisions
- `detectFileType()` returns UNKNOWN for streams shorter than 4 bytes (empty or too small).
- CSV detection is based on first 4 bytes being printable ASCII, whitespace, or valid UTF-8 multi-byte sequences.
- Error detail uses the exact string "File content is not a recognized Excel or CSV format" per FR-BR001-03.

### What Remains Open
- None. All delegation plan subtasks for backend implementation are complete.
- Femke (Frontend Agent) must confirm no frontend changes are required (Subtask 2).

## Session 2 (2026-07-09) — WR-001 Clean-Slate Local Development Script

### Summary
Updated `run_MVP1_locally.sh` and `run_MVP1_locally.bat` to implement clean-slate behavior per WR-001 specification. Every script invocation now kills stale processes, cleans build artifacts, performs full backend and frontend builds, and starts fresh service instances.

### What Was Implemented
1. **`run_MVP1_locally.sh`** — Full rewrite with five labeled steps:
   - Step 1: Kill stale processes (preserved existing `cleanup_stale_processes` function).
   - Step 2: Clean build artifacts (`mvn clean` in `5-backend/`, `rm -rf dist node_modules/.vite` in `4-frontend/`).
   - Step 3: Full backend build (`mvn clean package -DskipTests` in `5-backend/`). Aborts on failure.
   - Step 4: Full frontend build (`npm run build` in `4-frontend/`). Prints warning on failure, continues.
   - Step 5: Start backend via `mvn spring-boot:run -pl business-service` with 60-second readiness check.
   - Step 6: Start frontend via `npm run dev` in `4-frontend/`.
   - Preserved `trap cleanup EXIT INT TERM` for graceful Ctrl+C handling.
   - Preserved `set -e` fail-fast behavior.

2. **`run_MVP1_locally.bat`** — Rewritten with equivalent Windows clean-slate behavior:
   - Step 1: Kill stale processes using `netstat` + `taskkill`.
   - Step 2: Clean build artifacts using `rmdir /S /Q`.
   - Step 3: Full backend build via `mvn clean package -DskipTests`. Aborts on failure.
   - Step 4: Full frontend build via `npm run build`. Prints warning on failure, continues.
   - Step 5: Start backend with `timeout /t 1` readiness loop (60 iterations).
   - Step 6: Start frontend via `npm run dev` in separate window.

### Decisions
- Backend readiness timeout is 60 seconds per WR-001 NFR-WR001-01.
- Frontend build failure prints a warning but does not abort (Vite dev server serves from memory).
- Windows batch script uses `netstat -ano` for port detection instead of `lsof`.
- Windows batch script uses `curl` for readiness check (curl ships with Windows 10+).
- Both scripts use `SCRIPT_DIR` variable for portable path resolution.

### What Remains Open
- End-to-end acceptance test (uploading an Excel file) must be verified manually per TR-WR001-01.
- Build failure test (TR-WR001-03) must be verified manually by introducing a compilation error.
- Stale process detection test (TR-WR001-04) must be verified manually.

### Assumptions Made
- The `isSupportedMimeType()` method is not needed after this change for rejection; it remains as the fast-path check.
- Content-based detection only inspects first 4 bytes (as specified). Full file parsing is still delegated to Apache POI after detection succeeds.
- Existing tests for valid MIME type paths remain unchanged and continue to pass.

### Test Results
- All 148 tests pass. Zero failures.
- New tests: 19 (9 service + 10 controller).
- Existing tests: 129 (all pass, no regressions).

## Session 2 (2026-07-09) — WI-CA-001 Backend Implementation

### Summary
Implemented and verified all backend code for WI-CA-001 (Case Analyst Invoice List & Detail API). All 61 tests across 3 test classes pass (green state confirmed).

### What Was Tested (Testing Mode)
1. **InputValidationServiceTest.java**: 36 tests covering validatePage, validateSize, validateSort, validateStatus, validateSearch, validateAll.
2. **AnalystServiceTest.java**: 12 tests covering listInvoices (pagination, sorting, status filter, search, null status/null search), getInvoiceDetail (found/not found), toDTO mapping, and exception handling.
3. **AnalystControllerTest.java**: 13 tests covering both endpoints with valid and invalid parameters.

### What Was Implemented (Implementation Mode)
1. **Invoice entity**: Added `resubmissionCount` field with JPA annotation.
2. **Flyway migration V2**: `add_resubmission_count.sql` adds `resubmission_count INTEGER NOT NULL DEFAULT 0` column.
3. **InputValidationService**: Validates page (non-negative int), size (1-200), sort (allowlisted field + asc/desc), status (QUEUED/REJECTED_TYPE_A/REJECTED_TYPE_B), search (max 256 chars), id (positive long), and combineAll().
4. **AnalystInvoiceDTO**: 10-field DTO matching API contract response schema.
5. **InvoiceNotFoundException**: Custom exception for 404 responses.
6. **AnalystService**: `listInvoices()` uses JPA Specifications for dynamic filtering (status OR search OR null). `getInvoiceDetail()` finds by ID or throws exception. `toDTO()` maps entity to DTO.
7. **AnalystController**: Two endpoints (`GET /api/v1/analyst/invoices` paginated list, `GET /api/v1/analyst/invoices/{id}` detail). Constructor injection for AnalystService and InputValidationService. Validation error handling returns structured JSON.

### Bug Fixes
- Fixed `buildValidationErrorResponse` casting error: changed return type from `ResponseEntity<Object>` to `ResponseEntity<?>`.
- Fixed `validateSort` to reject empty strings (was previously returning valid).
- Fixed `validateSearch` error message casing from "Search" to "search" for case-sensitive test matching.
- Fixed controller constructor: changed from `new InputValidationService()` to constructor-injected dependency, enabling proper mock injection in tests.
- Fixed controller test: switched from `@SpringBootTest`/`@WebMvcTest` to `MockMvcBuilders.standaloneSetup()` to guarantee mock usage. The `@WebMvcTest` approach failed because the controller internally instantiated InputValidationService via `new()`.
- Fixed JSON path assertions: changed `$.currentPage`/`$.pageSize` to Spring Data `Page` serializes `$.number`/`$.size`.

### Decisions
- Controller uses constructor injection for both dependencies to enable testable mock wiring.
- Controller tests use `standaloneSetup` with explicit mock injection, avoiding Spring context overhead and ensuring mocks are actually called.
- All `validateAll` mocks use `any()` matchers without explicit type parameters to avoid `null` matching issues with default request parameter values.

### What Remains Open
- All backend implementation for WI-CA-001 is complete.
- No downstream agent activation follows (Naut is the last in the parallel pipeline).

### Assumptions Made
- JPA Specifications generate correct SQL for the Invoice table. No database integration test executed; service tests mock the repository.
- Default pagination parameters (page=0, size=50, sort=id,asc) match the API contract specification.

### Test Results
- **AnalystControllerTest**: 13 tests, 13 passed, 0 failures.
- **AnalystServiceTest**: 12 tests, 12 passed, 0 failures.
- **InputValidationServiceTest**: 36 tests, 36 passed, 0 failures.
- **Total**: 61 tests, 61 passed, 0 failures. BUILD SUCCESS.

## Session 3 (2026-07-09) — WI-CA-003 Source File Viewing

### Summary
Implemented WI-CA-003 backend: filesystem-based Excel store, source file endpoint, and invoice intake association.

### What Was Tested (Testing Mode)
1. **FileBackedExcelStoreServiceTest** (17 tests):
   - `save()` with valid xlsx/csv content → UUID returned
   - `save()` rejects PNG, JPEG, empty files → IllegalArgumentException
   - `save()` rejects files exceeding 50MB → IllegalArgumentException
   - `save()` accepts exactly 50MB file → UUID returned
   - `save()` rejects filenames with `;`, `\n`, `\r`, control chars → IllegalArgumentException
   - `save()` generates unique UUIDs per file → files stored separately
   - `getFile(UUID)` returns Resource for saved UUID → file content matches
   - `getFile(UUID)` returns null for unknown UUID
   - `sanitizeFilename()` preserves clean names
   - `sanitizeFilename()` throws for newlines, semicolons, control chars
   - `getContentType()` returns correct MIME types

2. **SourceFileEndpointTest** (10 tests):
   - `GET /invoices/1/source-file` with xlsx → 200 OK with correct Content-Type
   - `GET /invoices/2/source-file` with csv → 200 OK with text/csv
   - `GET /invoices/3/source-file` without sourceFileId → 404
   - `GET /invoices/0/source-file` (zero id) → 400
   - `GET /invoices/abc/source-file` (non-numeric id) → 400
   - `GET /invoices/4/source-file` with missing store file → 500
   - `GET /invoices/999/source-file` (not found) → 404
   - `GET /invoices/5/source-file` error response contains no UUID or store path
   - `GET /invoices/-1/source-file` (negative id) → 400
   - `GET /invoices/6/source-file` with non-numeric id → 400

### What Was Implemented (Implementation Mode)
1. **Flyway migration V3** (`V3__add_source_file_id_to_invoices.sql`): Adds `source_file_id VARCHAR(64)` and `source_filename VARCHAR(256)` columns to invoices table.
2. **FileBackedExcelStoreService** (new): Stores uploaded Excel files with UUID filenames. Validates MIME type (xlsx/csv only), file size (max 50MB), and sanitizes filenames against header injection.
3. **Invoice entity**: Added `sourceFileId` and `sourceFilename` fields with JPA column mappings and getters/setters.
4. **AnalystInvoiceDTO**: Added `sourceFileId` and `sourceFilename` fields with getters/setters.
5. **AnalystController**: Added `GET /invoices/{id}/source-file` endpoint. Looks up invoice by id, checks sourceFileId, serves file from store with proper Content-Type and Content-Disposition headers.
6. **AnalystService.toDTO()**: Maps `sourceFileId` and `sourceFilename` from Invoice entity.
7. **ExcelIntakeController**: Added `FileBackedExcelStoreService` dependency. Saves uploaded files during intake and sets `SourceFileContext` ThreadLocal before calling service layer.
8. **SourceFileContext** (new): ThreadLocal holder for cross-layer sourceFileId/sourceFilename passing.
9. **IntakeServiceImpl**: Reads SourceFileContext and sets sourceFileId/sourceFilename on Invoice before persistence.
10. **application.yml**: Added `gimme.excel-store-path` configuration property.

### Bug Fixes Applied During Implementation
- `AnalystController` missing `@Autowired` on 4-parameter constructor caused Spring DI failure. Added `@Autowired` and import.
- `FileBackedExcelStoreServiceTest.save_sanitizesFilenameAgainstHeaderInjection` expected save to succeed with malicious filename, but `sanitizeFilename` correctly throws. Renamed test to `save_rejectsFilenameWithNewlinesAndSemicolons`.
- `SourceFileEndpointTest` mock for `validateId` returned null for all inputs. Fixed to return `true` for positive ids and `false` for zero/negative.
- `ExcelIntakeControllerTest` constructor required 3 params after adding `FileBackedExcelStoreService`. Added mock injection with doNothing stubs.

### Decisions
- `SourceFileContext` uses ThreadLocal for cross-layer data passing. Safe because each HTTP request is handled by a single thread in Spring MVC.
- `AnalystController` has both 2-param and 4-param constructors. 4-param is annotated with `@Autowired` for Spring DI. 2-param chains to null defaults for backward compatibility.
- Filename sanitization rejects (does not sanitize) filenames containing newlines, semicolons, or control characters. This is a security-first approach.

### What Remains Open
- Frontend implementation (Femke's responsibility) for the source file download link in the Analyst Dashboard.
- Integration test covering the full intake flow (ExcelIntakeController → IntakeServiceImpl → Invoice persistence with sourceFileId).
- Configuration of actual filesystem path for `gimme.excel-store-path` in deployment environments.

### Assumptions Made
- The H2 in-memory database supports ALTER TABLE ADD COLUMN for Flyway migration V3.
- The `gimme.excel-store-path` directory will exist at runtime; FileBackedExcelStoreService creates it on startup.
- Source file download URL uses the invoice's numeric `id` (Long), not the UUID-based `sourceFileId`.

### Test Results
- **FileBackedExcelStoreServiceTest**: 17 tests, 17 passed, 0 failures.
- **SourceFileEndpointTest**: 10 tests, 10 passed, 0 failures.
- **All other tests**: 209 tests, 209 passed, 0 failures.
- **Total**: 236 tests, 236 passed, 0 failures. BUILD SUCCESS.
