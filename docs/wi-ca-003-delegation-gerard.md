# Delegation Plan: WI-CA-003 — View Source Excel Files in Analyst Dashboard

## Architecture Constraints

- **D-CA-002**: Analyst API endpoints are unauthenticated for MVP. The source file serving endpoint inherits this constraint.
- **D-CA-003**: Resubmission Count column added via Flyway migration `V2__add_resubmission_count.sql`. The column is `INTEGER NOT NULL DEFAULT 0`.
- **D-CA-004**: API endpoints use `/api/v1/analyst/` path prefix.
- **D-CA-005**: Controller-service-repository layering must be maintained.
- **D-EXCEL-001**: Excel intake store is filesystem-based with configurable path (`gimme.excel-store-path`). Files are stored with UUID filenames. Same pattern as `FileBackedPoCStoreService`.
- **D-EXCEL-002**: Invoice entity gains nullable `sourceFileId` (VARCHAR(64)) and `sourceFilename` (VARCHAR(256)) fields.
- **D-EXCEL-003**: Source file serving endpoint is `GET /api/v1/analyst/invoices/{id}/source-file`. Returns raw file bytes with `Content-Type` and `Content-Disposition` headers.
- **D-EXCEL-004**: Upload flow is extended to persist the original file during existing intake processing.
- **D-EXCEL-005**: AnalystInvoiceDTO gains `sourceFileId` and `sourceFilename` fields (both nullable strings).
- **D-020** (from Session 5): No authentication on client portal endpoints for MVP.
- **D-026**: Unauthenticated MVP endpoints.

## Subtasks

### Subtask 1: API Contract Definition

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: `re-workspace/work-items/MVP-1-Case-analyst/wi-ca-003-view-poc-documents.md` (work item specification), `docs/api-contract-wi-ca-001.md` (existing contract for reference)
- **Output Artefact**: `docs/api-contract-wi-ca-003.md` (versioned API contract)
- **Constraints**:
  - The contract must define the new endpoint: `GET /api/v1/analyst/invoices/{id}/source-file`.
  - Path variable: `id` (Long, required, `> 0`).
  - Response 200 OK: raw file bytes. `Content-Type`: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` for `.xlsx`, `text/csv` for `.csv`. `Content-Disposition`: `inline; filename="<original-filename>"`.
  - Response 404 Not Found: when `source_file_id` is null or file missing. JSON body: `{"error": "Not Found", "message": "No source file available for this invoice"}`.
  - The contract must document the schema changes to the existing detail endpoint (`GET /api/v1/analyst/invoices/{id}`): two new fields added to the response DTO — `sourceFileId` (string, nullable) and `sourceFilename` (string, nullable).
  - The contract must document changes to the list endpoint (`GET /api/v1/analyst/invoices`): the `AnalystInvoiceItem` definition gains `sourceFileId` and `sourceFilename` fields.
  - The contract must specify the new Excel store persistence integration: the intake endpoint `POST /api/v1/intake/excel` is extended to persist the original file. This does NOT create a new endpoint but modifies the existing one.
  - The contract must reference the Flyway migration `V3__add_source_file_id_to_invoices.sql` adding `source_file_id VARCHAR(64)` and `source_filename VARCHAR(256)` columns.
  - Error responses must conform to the existing pattern: no stack traces, no SQL, no server internals (per S-006 and contract section 5).
  - The contract must include all architectural decisions from the delegation plan (D-EXCEL-001 through D-EXCEL-005).
  - The contract must include security considerations: UUID filenames prevent path traversal, filename sanitisation prevents header injection, MIME type correctness, no authentication (MVP limitation).
- **Security Considerations**:
  - The source file serving endpoint must use UUID-based file lookup to prevent path traversal.
  - The `source_filename` field must be sanitised to prevent HTTP header injection (reject or escape newlines, semicolons, and other control characters).
  - `Content-Type` must be determined from the stored file format, not from user input.
  - The endpoint returns raw file bytes — ensure the MIME type does not allow execution (e.g., no `.js` or `.html` content in Excel files, but validate anyway).
  - No authentication is required per D-CA-002.

### Subtask 2: Database Migration

- **Assigned Agent**: Gerard delegates to Database Engineer
- **Input Artefact**: `docs/api-contract-wi-ca-003.md` (versioned API contract), `5-backend/business-service/src/main/resources/db/migration/V1__create_invoices_table.sql` (existing migration), `5-backend/business-service/src/main/resources/db/migration/V2__add_resubmission_count.sql` (existing migration)
- **Output Artefact**: `5-backend/business-service/src/main/resources/db/migration/V3__add_source_file_id_to_invoices.sql`
- **Constraints**:
  - Migration must add two columns: `source_file_id VARCHAR(64)` and `source_filename VARCHAR(256)`.
  - Both columns are nullable (null for single-invoice API imports).
  - Add a comment on each column describing its purpose.
  - The migration must be idempotent (check if column exists before adding, or rely on Flyway versioning to run once).
  - Existing data should have null values for both columns (invoices imported before this migration).
- **Security Considerations**:
  - No sensitive data introduced. Both columns store non-sensitive metadata.
  - `source_filename` stores user-provided filenames. No SQL injection risk from the column definition itself, but application code using this field must parameterise all queries.

### Subtask 3: Naut Delegation — Backend Implementation

- **Assigned Agent**: Gerard delegates to Naut (Backend Agent)
- **Input Artefact**: `docs/api-contract-wi-ca-003.md` (versioned API contract), `5-backend/business-service/src/main/java/com/gimmevettingsolution/invoice/entity/Invoice.java` (existing entity), `5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/ExcelIntakeController.java` (existing controller)
- **Output Artefact**: Java backend source code implementing:
  1. `FileBackedExcelStoreService` — Excel file persistence and retrieval
  2. Updated `Invoice` entity with `sourceFileId` and `sourceFilename` fields
  3. Updated `AnalystInvoiceDTO` with two new fields
  4. Updated `AnalystController` with the new `getSourceFile` endpoint
  5. Modified `ExcelIntakeController` to persist uploaded files
- **Constraints**:
  - `FileBackedExcelStoreService` must follow the `FileBackedPoCStoreService` pattern: injectable `Path` for store location, UUID-based filename generation, MIME type validation on write, resource-based read.
  - The service must store both the file and a filename-to-UUID mapping. The mapping can be stored in a separate `Map<String, String>` in-memory service, or persisted in a properties file, or stored alongside the invoice entity. Given MVP scope, in-memory storage with the assumption that the service runs as a single instance is acceptable. However, if durability across restarts is desired, a properties file or the `source_filename` column on the Invoice entity should be used.
  - **Decision**: Store original filename in the `source_filename` column on the Invoice entity. The `FileBackedExcelStoreService` only stores files with UUID names. When a file is uploaded, generate a UUID, save the file, and store the UUID + original filename in the Invoice entity.
  - The new endpoint `GET /api/v1/analyst/invoices/{id}/source-file` must be added to `AnalystController`.
  - The endpoint looks up the invoice by ID, retrieves `sourceFileId`, resolves the file from the store, and returns it as a `ResponseEntity<Resource>`.
  - If the invoice has no `sourceFileId` (null), return 404 with JSON error body.
  - If the file is not found in the store, return 500 with message "Source file is unavailable".
  - The `ExcelIntakeController` must be modified to call `FileBackedExcelStoreService.save(file)` after successful validation, and store the returned UUID and original filename on each Invoice entity created from the parsed rows.
  - `InvoiceRepository` must be updated if new query methods are needed (e.g., find by sourceFileId).
  - The existing `Invoice` entity must gain `sourceFileId` (String) and `sourceFilename` (String) fields with JPA annotations.
  - `AnalystInvoiceDTO` must gain `sourceFileId` (String) and `sourceFilename` (String) fields.
  - All existing tests must continue to pass. New tests must cover the new endpoint, the store service, and the upload flow changes.
  - The Excel store path must be configurable via `@Value("${gimme.excel-store-path}")` with a default value.
  - File size limit: files larger than 50MB shall be rejected at upload time (per NFR-003).
- **Security Considerations**:
  - UUID filenames prevent path traversal attacks on the file store.
  - The `source_filename` must be sanitised to remove control characters before storing.
  - `Content-Type` in the response must be set explicitly based on file extension, not from user input.
  - Error responses must not expose the Excel store path, file system structure, or internal error details.
  - The `FileBackedExcelStoreService` must validate MIME type on both write and read.
  - No authentication on the serving endpoint is a documented MVP limitation per D-CA-002.

### Subtask 4: Contract Verification and Automated Tests

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: `docs/api-contract-wi-ca-003.md` (versioned API contract), Naut's implementation artefacts
- **Output Artefact**: Automated contract verification tests, contract verification report, updated `docs/alignment-review-request.md`
- **Constraints**:
  - Gerard must produce automated tests that verify the contract between frontend and backend.
  - Tests must cover: endpoint availability, response schema (including new fields), error responses, MIME type correctness, Content-Disposition header format.
  - Gerard must verify that the `AnalystInvoiceDTO` response includes `sourceFileId` and `sourceFilename` in both the list and detail endpoints.
  - Gerard must verify that the source file endpoint returns the correct MIME type for `.xlsx` and `.csv` files.
  - Gerard must verify path traversal protection: requesting a file path with `../` must not expose files outside the store.
- **Security Considerations**:
  - Contract tests must verify that error responses do not leak file paths or server internals.
  - Contract tests must verify that the source file endpoint validates the `source_file_id` exists before serving.

## Gerard Phase Completion Criteria

Gerard is considered complete when:
1. `docs/api-contract-wi-ca-003.md` is produced and submitted to the Alignment Agent.
2. The contract includes all endpoints, schema changes, security considerations, and architectural decisions.
3. The contract references the correct Flyway migration and entity changes.
4. Gerard produces `docs/wi-ca-003-contract-ready.md` signalling completion.
