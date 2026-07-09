# Session History

## Session 5 — 2026-07-07 — WI-002 Excel File Upload and Parsing

### What Was Explored

User requested implementation of the next work item. After reviewing work items.md and the completion status of WI-001 (fully approved by Alignment Agent), WI-002 was identified as the correct next item. It is the entry point of the MVP Excel intake pipeline with no upstream dependencies.

Architectural decisions D-024 through D-029 and security requirements S-007 through S-011 were documented in architecture-decisions.md. The user confirmed strict column name matching (no aliases accepted, case-insensitive).

### What Was Decided

1. **D-024**: Strict column name matching — only the five allowlisted column names accepted (case-insensitive). No aliases.
2. **D-025**: Format support (.xlsx, .csv), optional header row, column-position fallback mapping.
3. **D-026**: No authentication for MVP. Javadoc/JSDoc notes required.
4. **D-027**: No file size limit for MVP.
5. **D-028**: Synchronous processing model.
6. **D-029**: Apache POI 5.2.5 mandated with XML entity expansion disabled.
7. **S-007**: Server-side MIME type validation.
8. **S-008**: Column name allowlist enforcement.
9. **S-010**: Filename sanitization against path traversal.
10. **S-011**: Temporary file cleanup policy for return Excel files.

Gerard produced `docs/api-contract-wi-002.md` (version 2.0.0). Alignment Agent approved. Femke produced frontend component `ExcelUpload.jsx` with 16 passing Jest tests. Naut produced backend implementation with 69 passing Maven tests. Alignment Agent approved Naut's backend implementation.

### What Remains Open

- WI-003 (Per-Row Mandatory Field Validation) — downstream of WI-002
- WI-004 (Return Excel Generation) — downstream of WI-002 and WI-003
- WI-005 (Separate PoC Upload Endpoint) — parallel after WI-001

### Assumptions Made

- Apache POI 5.2.5 is sufficient for MVP file size and performance requirements. EasyExcel can be evaluated if large-file performance becomes critical (AUNV-006).
- Synchronous processing is acceptable for MVP. Async processing can be evaluated if file sizes or processing times become problematic.
- Temporary file cleanup for return Excel files uses server-side temporary directory with no explicit retention period for MVP. This is a minor operational gap.

## Session 7 — 2026-07-08 — WI-005 Separate PoC Upload Endpoint (Gerard Phase)

### What Was Explored

User requested implementation of WI-005. The work item specifies a separate PoC upload endpoint for the client portal, allowing clients to upload PoC files (PDF) by invoice number filename independently of the Excel batch upload pipeline.

Two architectural decisions from the WI-005 specification required resolution: (1) file type enforcement for uploaded PoC files — the specification allowed either rejection or silent ignore of non-PDF files; (2) duplicate filename handling — the specification allowed either overwrite or reject. Both were resolved by the architect.

The existing `PoCStoreService` interface and `FileBackedPoCStoreService` implementation were reviewed. No `store()` method exists yet. The new endpoint will need to add a `store(MultipartFile file)` method to the interface.

### What Was Decided

1. **D-015**: PoC upload endpoint rejects non-PDF files with a 400 Bad Request. Server-side MIME type validation for `application/pdf`.
2. **D-016**: Duplicate filename overwrites existing PoC file in the store.
3. **D-017**: Endpoint path is `POST /api/v1/poc-upload`. Request is multipart/form-data with single field `file`. Invoice number is extracted from filename, not a separate parameter.
4. `docs/wi-005-delegation-gerard.md` produced with two subtasks: (a) API contract definition for `docs/api-contract-wi-005.md`, (b) alignment review submission.
5. `agent-definitions/architect-agent/models/2026-07-08-session7-wi005-delegation.mmd` produced as the delegation flow diagram.

### What Remains Open

- `docs/api-contract-wi-005.md` — to be produced by Gerard
- Backend implementation by Naut (PoCUploadController, PoCStoreService.store())
- Frontend implementation by Femke (display missing PoC invoice numbers, upload UI)
- Alignment review approvals for both backend and frontend phases

### Assumptions Made

- The `PoCStoreService` will gain a `store(MultipartFile)` method. The method uses the same path traversal protection pattern (SAFE_PATTERN) as the `hasMatchingPoC()` method.
- Filename sanitization for the upload endpoint uses the same approach as ExcelIntakeController: reject filenames containing path separators or matching the SAFE_PATTERN.
- The frontend UI for displaying missing PoC invoice numbers will derive the list from the return Excel response produced by WI-004, or from a dedicated endpoint to be defined by Gerard.

## Session 8 — 2026-07-08 — WI-005 Parallel Delegation (Naut and Femke Phase)

### What Was Explored

Alignment Agent approved Gerard's API contract for WI-005 with greenlightForNextAgent set to true. Contract readiness signal `docs/wi-005-contract-ready.md` confirmed. Parallel implementation phase activated.

Existing code was reviewed to inform subtask constraints: `ExcelUpload.jsx` provides the frontend upload pattern for Femke to follow. `PoCStoreService` interface has only `hasMatchingPoC()` method — Naut must add `store(MultipartFile file)`. `FileBackedPoCStoreService` provides the implementation pattern for Naut (configurable path, SAFE_PATTERN, case-insensitive matching). No existing PoC upload code exists in either frontend or backend.

### What Was Decided

1. `docs/wi-005-delegation-parallel.md` produced with two parallel subtasks:
   - Naut: Backend implementation — add `store()` method to `PoCStoreService` and `FileBackedPoCStoreService`, create `PoCUploadController` with `POST /api/v1/poc-upload`, write unit and integration tests.
   - Femke: Frontend implementation — create PoC upload UI component, display missing PoC invoice numbers, add upload buttons and success/error feedback.
2. Both agents consume the identical contract file: `docs/api-contract-wi-005.md` (version 5.0.0).
3. Both agents must submit alignment review requests upon completion.

### What Remains Open

- Naut backend implementation and alignment review
- Femke frontend implementation and alignment review
- Pipeline completion after both alignment reviews are approved

## Session 9 — 2026-07-08 — WI-005 Parallel Phase Completion

### What Was Explored

The Alignment Agent approved Naut's backend submission for WI-005 (review ID WI-005-NAUT-001, status APPROVED, greenlightForNextAgent: true). Femke's frontend submission was approved in a prior cycle. Both agents produced artefacts conforming to the versioned API contract (docs/api-contract-wi-005.md, version 5.0.0).

Naut produced: PoCStoreService.java (interface addition), FileBackedPoCStoreService.java (store() implementation), PoCUploadController.java (POST /api/v1/poc-upload), PoCUploadSuccessResponse.java (DTO), PoCStoreServiceTest.java (6 new tests), PoCUploadControllerTest.java (12 new tests). All 28 new tests pass. Full backend suite: 114 tests, zero regressions.

Femke produced: PoCUpload.jsx (frontend component), PoCUpload.test.jsx (15 Jest tests). All frontend tests pass.

### What Was Decided

1. Both Femke and Naut backend and frontend submissions for WI-005 are approved by the Alignment Agent.
2. The parallel implementation phase for WI-005 is complete.
3. No architectural drift detected — all artefacts conform to D-001, D-003, D-015, D-016, D-017.

### What Remains Open

- Integration testing of the complete PoC upload flow (frontend + backend + API contract)
- Potential next work item: WI-003 (Per-Row Mandatory Field Validation) or WI-004 (Return Excel Generation)

## Session 10 — 2026-07-08 — WI-CA-001 Case Analyst API (Gerard Phase)

### What Was Explored

User requested implementation of WI-CA-001 (Case Analyst Invoice List & Detail API). This is a new work item in the MVP-1-Case-analyst track, separate from the Client track (WI-001 through WI-006). The work item requires two REST API endpoints: paginated invoice list with filtering/sorting and single invoice detail. The existing Invoice entity lacks a `resubmissionCount` field required by the analyst API.

Two architectural decisions from the work item specification required stakeholder confirmation: (1) resubmission strategy — the specification offered two options (update existing row vs. create new row); (2) authentication — the specification did not specify auth requirements, and the existing endpoints are unauthenticated per D-026.

The existing backend was reviewed: `Invoice.java` has fields for id, invoiceNumber, debtorName, address, bankAccountNumber, phoneNumber, poCStatus, rejectionType, status. No resubmissionCount field exists. The Flyway migration `V1__create_invoices_table.sql` does not include this column. The `InvoiceRepository` is a standard Spring Data JPA repository.

### What Was Decided

1. **D-CA-001**: Resubmission uses Option A — update existing row, increment resubmissionCount by 1. When a client submits an invoice with an existing invoiceNumber, the existing database row is updated and the counter is incremented. No new row is created.
2. **D-CA-002**: Analyst API endpoints are unauthenticated for MVP, consistent with D-026. This is a documented security limitation requiring future remediation.
3. **D-CA-003**: Flyway migration `V2__add_resubmission_count.sql` adds `resubmission_count INTEGER NOT NULL DEFAULT 0` to the invoices table.
4. **D-CA-004**: Analyst API endpoints use `/api/v1/analyst/` path prefix, following the existing `/api/v1/` convention.
5. `docs/wi-ca-001-delegation-gerard.md` produced with one subtask for Gerard: produce the versioned API contract `docs/api-contract-wi-ca-001.md` and submit alignment review.
6. `agent-definitions/architect-agent/models/2026-07-08-session8-wi-ca-001-delegation.mmd` produced as the delegation flow diagram.

### What Remains Open

- `docs/api-contract-wi-ca-001.md` — to be produced by Gerard
- Alignment Agent approval for the API contract
- Parallel delegation plan for Femke (frontend dashboard) and Naut (backend implementation) — produced after Alignment Agent approval
- Backend implementation by Naut: `AnalystInvoiceController`, `AnalystInvoiceService`, `AnalystInvoiceDTO`, `resubmissionCount` field addition, Flyway V2 migration, resubmission logic in intake service
- Frontend implementation by Femke: invoice dashboard component consuming the versioned API contract

### Assumptions Made

- JPA `Specification` is the preferred mechanism for dynamic filtering (status filter + free-text search). This is consistent with Spring Data JPA conventions used in the existing codebase.
- The `InvoiceRepository` can accept `Pageable` and `Specification` parameters simultaneously for paginated, filtered queries.
- The search is a database-level LIKE operation (case-insensitive partial match) across invoiceNumber, debtorName, and address fields. A full-text search engine (e.g., PostgreSQL pg_trgm) is not required for MVP.
- Rate limiting on the analyst API endpoints is deferred to MVP+1. The security risk of bulk data extraction is documented but not mitigated in MVP.

## Session 11 — 2026-07-08 — WI-CA-001 Frontend Routing Decision

### What Was Explored

User asked where the case analyst dashboard frontend components would be written. The existing frontend at 4-frontend/ has main.jsx rendering only ExcelUpload. The analyst dashboard work item (WI-CA-002) specifies new components but does not specify how they are wired into the application.

### What Was Decided

1. **D-CA-005**: React Router is added to 4-frontend/. Two routes are defined: client upload page at `/` (existing ExcelUpload) and analyst dashboard at `/analyst` (new AnalystDashboard). The main.jsx entry point is refactored to wrap both routes in a BrowserRouter.
2. All analyst dashboard components (AnalystDashboard.jsx, InvoiceTable.jsx, InvoiceDrawer.jsx, StatusBadge.jsx, SearchFilterBar.jsx) and the analyst API service (analystApi.js) are written to `4-frontend/src/frontend/components/` and `4-frontend/src/frontend/services/` respectively.
3. The routing decision ensures both the client upload portal and the analyst dashboard share the same build pipeline, dependency management, and testing infrastructure.

### What Remains Open

- The parallel delegation plan must be updated to specify Femke's output paths explicitly (main.jsx refactoring, React Router setup, new component files)
- The existing unit tests must be reviewed to ensure BrowserRouter wrapping does not break Jest test setup
## Session 8 — 2026-07-08 — WI-008 BR-001 Content-Based File Validation Fix

### What Was Explored

User reported BR-001: MIME-type-based file validation rejects valid Excel files. The backend validates files exclusively by MIME type, causing rejection of valid .xlsx files when browsers report unrecognized MIME types such as `application/octet-stream`, `application/zip`, or empty strings. The bug affects the Excel upload endpoint (POST /api/v1/intake/excel) — the core MVP feature.

Architectural decision D-BR001 was documented: content-based file format detection via magic byte inspection replaces MIME-type-only validation. ZIP signature (50 4B 03 04) for XLSX, text detection for CSV.

### What Was Decided

1. **D-BR001**: Content-based file format detection via magic byte inspection. MIME type is a supplementary hint only. Detection precedence: fast path for known MIME types, fallback to content inspection for unrecognized types, reject if content inspection fails.
2. **Contract update**: API contract `docs/api-contract-wi-002.md` updated from version 2.0.0 to 2.1.0. Section 3.2 documents content-based detection. Magic byte constants specified. Error messages require specific detection reason.
3. **Frontend change**: Femke removed client-side MIME type validation from `ExcelUpload.jsx` that contradicted the new contract. HTML `accept=".xlsx,.csv"` attribute retained for file picker dialog filtering only.
4. **Backend implementation**: Naut created `FileType` enum, added `detectFileType()` method to `ExcelParsingService`, restructured `ExcelIntakeController.uploadExcel()` with fast path and fallback path. 19 new tests added. All 148 tests pass.

### Pipeline Execution

1. Gerard: Updated API contract to v2.1.0, Alignment Agent approved (WI-008-REV-001), produced `docs/wi-008-contract-ready.md`
2. Femke: Removed client-side MIME validation from ExcelUpload.jsx, confirmed alignment
3. Naut: Implemented content-based detection, all tests pass, submitted for Alignment Agent review
4. Naut is the last agent in the pipeline. Alignment Agent approval of Naut's final submission completes WI-008.

### Architecture Model

Mermaid flowchart saved at `agent-definitions/architect-agent/models/2026-07-08-session8-br001-fix-flow.mmd` showing the detection flow with magic byte inspection.

## Session 9 — 2026-07-09 — BR-001 Status Check and Handover Fix

### What Was Explored

User requested status update on BR-001, noting that "some things have changed but by the wrong agent." Investigation revealed:

- BR-001 backend fix was verified at runtime on port 8082 using curl against the template download endpoint
- All 40 frontend tests pass, including 3 new BR-001 regression tests using real XLSX bytes with non-standard MIME types
- All 148 backend tests pass
- No actual source code changes occurred between the runtime verification and the status check. The git working tree changes were limited to the BR-001 bug report file and the ExcelUpload test file.

### Handover Violation Discovered

The file `docs/alignment-review-request.md` was corrupted. Gerard (WI-CA-001) wrote his JSON review request to the file while Naut's BR-001 submission (which was in non-standard markdown format) was still present. The file contained two review cycles mashed together:

- Lines 1-24: Naut's BR-001 submission in markdown format (not JSON)
- Lines 26-47: Gerard's WI-CA-001 submission in JSON format

This is a **handover violation**. The Alignment Agent expects a single JSON review request. Gerard overwrote the file without checking if Naut's submission had been processed. It is also a **format violation**. Naut's submission was in non-standard markdown format, not the required JSON format defined in the Alignment Agent agent definition.

### What Was Fixed

The file was rewritten with Naut's BR-001 submission in the correct JSON format expected by the Alignment Agent. Gerard's WI-CA-001 submission is deferred until Naut's BR-001 review cycle has been processed and approved by the Alignment Agent.

### What Remains Open

- Naut's BR-001 submission must be reviewed by the Alignment Agent first
- Gerard's WI-CA-001 submission must be submitted only after Naut's is approved
- Frontend BR-001 regression tests should be submitted to Alignment Agent for review
