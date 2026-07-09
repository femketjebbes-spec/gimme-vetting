# Parallel Delegation Plan: WI-CA-003 — View Source Excel Files in Analyst Dashboard

## Architecture Constraints

- **D-CA-002**: Analyst API endpoints are unauthenticated for MVP. The source file serving endpoint inherits this constraint.
- **D-026**: Unauthenticated MVP endpoints.
- **D-EXCEL-001**: Excel intake store is filesystem-based with configurable path (`gimme.excel-store-path`). Files are stored with UUID filenames. Same pattern as `FileBackedPoCStoreService`.
- **D-EXCEL-002**: Invoice entity gains nullable `sourceFileId` (VARCHAR(64)) and `sourceFilename` (VARCHAR(256)) fields.
- **D-EXCEL-003**: Source file serving endpoint is `GET /api/v1/analyst/invoices/{id}/source-file`. Returns raw file bytes with `Content-Type` and `Content-Disposition` headers.
- **D-EXCEL-004**: Upload flow is extended to persist the original file during existing intake processing. No new upload endpoint is created.
- **D-EXCEL-005**: `AnalystInvoiceDTO` gains `sourceFileId` and `sourceFilename` fields (both nullable strings).
- **S-006**: Error responses must not expose stack traces, SQL, or server internals.
- **S-007**: UUID filenames prevent path traversal attacks on the file store.
- **S-008**: `source_filename` must be sanitised to prevent HTTP header injection.
- **S-009**: `Content-Type` must match the actual stored file format, not derived from user input.
- **S-010**: File size limit: 50MB maximum at upload time.

## Shared Contract

`docs/api-contract-wi-ca-003.md` (v1.0.0)

This contract defines one new endpoint and two modified endpoints:

1. **New**: `GET /api/v1/analyst/invoices/{id}/source-file` — returns raw Excel file bytes with `Content-Type` and `Content-Disposition` headers. Responses: 200 OK, 404 Not Found, 400 Bad Request, 500 Internal Server Error.
2. **Modified**: `GET /api/v1/analyst/invoices/{id}` — detail endpoint response schema extended with `sourceFileId` (string, nullable) and `sourceFilename` (string, nullable).
3. **Modified**: `GET /api/v1/analyst/invoices` — list endpoint `AnalystInvoiceItem` definition extended with `sourceFileId` and `sourceFilename`.

Database migration: `V3__add_source_file_id_to_invoices.sql` adds `source_file_id VARCHAR(64)` and `source_filename VARCHAR(256)` columns to the `invoices` table.

## Subtasks

### Subtask 1: Frontend Implementation

- **Assigned Agent**: Femke (Frontend Agent)
- **Input Artefact**: `docs/api-contract-wi-ca-003.md` (versioned API contract)
- **Output Artefact**: Frontend code in `4-frontend/src/business-service/`
- **Constraints**:
  - All changes must conform to the versioned API contract `docs/api-contract-wi-ca-003.md`.
  - Add a `fetchSourceFile(id)` function to `4-frontend/src/business-service/api/analystApi.js` that calls `GET /api/v1/analyst/invoices/{id}/source-file`. The function must fetch the raw file bytes (use `response.blob()` or `response.arrayBuffer()` for binary download).
  - Update `analystApi.js` response parsing to include `sourceFileId` and `sourceFilename` fields in both list and detail responses.
  - Wire the "Bekijken" link in `4-frontend/src/business-service/components/InvoiceDrawer.jsx` to call `fetchSourceFile(invoice.id)` when clicked.
  - Enable the "Bekijken" link when `sourceFileId` is non-null. Disable it when `sourceFileId` is null.
  - Use a direct `<a>` tag download: `<a href={url} download onClick={handleDownload}>Bekijken</a>`. Do not implement blob downloads or JavaScript-mediated downloads unless the contract endpoint returns JSON error responses that require parsing.
  - Construct the download URL as `/api/v1/analyst/invoices/${id}/source-file` using the invoice ID.
  - All fetch calls must target endpoints declared in the contract. All response parsing must conform to the JSON schemas defined in the contract.
  - Error handling: when the source file endpoint returns 404, silently disable or hide the "Bekijken" link. When it returns 500, display a user-friendly error message (not the raw API error).
  - The existing frontend project uses React 18 with Vite build pipeline. Jest testing is configured via `jest.config.js`.
  - Write Jest tests for the new `fetchSourceFile` function in `4-frontend/src/business-service/api/__tests__/analystApi.test.js`.
  - Do not modify any files outside `4-frontend/src/business-service/`.
- **Security Considerations**:
  - The `id` path parameter must be validated as a positive integer before constructing the URL.
  - No authentication handling is required per D-CA-002.
  - Error messages displayed to the user must not expose raw API error details (stack traces, SQL, server internals) per S-006.
  - The `<a>` tag must not use user-provided filenames in the `href` attribute. Only the invoice ID (from the API response) is used.

### Subtask 2: Backend Implementation

- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `docs/api-contract-wi-ca-003.md` (versioned API contract)
- **Output Artefact**: Java backend source code in `5-backend/business-service/src/main/java/com/gimmevettingsolution/`
- **Constraints**:
  - All code must conform to the versioned API contract `docs/api-contract-wi-ca-003.md`.
  - Implement `FileBackedExcelStoreService` in `5-backend/business-service/src/main/java/com/gimmevettingsolution/excel/` following the `FileBackedPoCStoreService` pattern:
    - Injectable `Path` for store location via `@Value("${gimme.excel-store-path}")`.
    - UUID-based filename generation for stored files.
    - Method `save(MultipartFile file)`: validates file format (.xlsx or .csv only), generates UUID, saves file to store path, returns UUID string.
    - Method `getFile(UUID fileId)`: returns `Resource` for the stored file. Throws exception if file not found.
    - MIME type validation on both write (only `.xlsx` and `.csv` allowed) and read.
  - Update `Invoice` entity in `5-backend/business-service/src/main/java/com/gimmevettingsolution/invoice/entity/Invoice.java`:
    - Add `sourceFileId` (String, nullable) field with JPA annotations.
    - Add `sourceFilename` (String, nullable) field with JPA annotations.
  - Create Flyway migration `V3__add_source_file_id_to_invoices.sql` in `5-backend/business-service/src/main/resources/db/migration/`:
    - Add column `source_file_id VARCHAR(64)` (nullable, default null) with comment.
    - Add column `source_filename VARCHAR(256)` (nullable, default null) with comment.
  - Update `AnalystInvoiceDTO` in `5-backend/business-service/src/main/java/com/gimmevettingsolution/analyst/dto/AnalystInvoiceDTO.java`:
    - Add `sourceFileId` (String, nullable) field.
    - Add `sourceFilename` (String, nullable) field.
  - Add new endpoint to `AnalystController` in `5-backend/business-service/src/main/java/com/gimmevettingsolution/analyst/controller/AnalystController.java`:
    - `GET /api/v1/analyst/invoices/{id}/source-file` — returns `ResponseEntity<Resource>` with raw file bytes.
    - Look up invoice by `id`. If `sourceFileId` is null, return 404 with JSON body `{"error": "Not Found", "message": "No source file available for this invoice"}`.
    - Resolve file from `FileBackedExcelStoreService`. If file not found, return 500 with JSON body `{"error": "Internal Server Error", "message": "Source file is unavailable"}`.
    - Set `Content-Type` based on actual file format: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` for `.xlsx`, `text/csv` for `.csv`.
    - Set `Content-Disposition: inline; filename="<original-filename>"` using the stored `sourceFilename`.
    - Validate `id` is a positive long. Return 400 if invalid.
  - Modify `ExcelIntakeController` in `5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/ExcelIntakeController.java`:
    - Inject `FileBackedExcelStoreService`.
    - After successful Excel parsing, save the uploaded file to the store using `save(file)` which returns a UUID.
    - Store the returned UUID and original filename on each Invoice entity created from the parsed rows (set `sourceFileId` and `sourceFilename`).
  - Update `ExcelParsingService` if needed to pass the original `MultipartFile` to the service layer for persistence.
  - All existing tests must continue to pass. Write new tests for:
    - `FileBackedExcelStoreService` save and get operations.
    - The new `getSourceFile` endpoint (200, 404, 400, 500 responses).
    - Excel intake flow file persistence.
    - File format validation (.xlsx, .csv accepted; other formats rejected at upload).
    - MIME type validation.
    - File size limit enforcement (50MB, per NFR-003).
  - The Excel store path must be configurable via `application.yml` property `gimme.excel-store-path`.
- **Security Considerations**:
  - **S-007 (Path traversal)**: UUID filenames prevent path traversal. The `FileBackedExcelStoreService` must use only UUID-based lookup — never user-supplied filenames for filesystem access.
  - **S-008 (Header injection)**: `source_filename` must be sanitised at write time. Reject or escape newlines (`\n`, `\r`), semicolons (`;`), and other control characters (ASCII 0-31) before storing.
  - **S-009 (MIME type spoofing)**: `Content-Type` in the source file response must be determined from the stored file format based on the file extension, validated against an allowlist (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `text/csv`). Never derive Content-Type from user input.
  - **S-010 (File size)**: Files exceeding 50MB must be rejected at upload time with HTTP 413 `Request Entity Too Large`.
  - **S-006 (Error responses)**: Error responses must not expose stack traces, SQL error codes, file system paths, or the `source_file_id` UUID in any message.
  - No authentication is implemented per D-CA-002 (documented MVP limitation).
  - The store path must not appear in error messages or log output.

## Parallel Phase Completion Criteria

The parallel phase is considered complete when both Femke and Naut have submitted their respective alignment review requests to the Alignment Agent.
