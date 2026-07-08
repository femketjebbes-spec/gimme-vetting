# Delegation Plan: WI-005 — Separate PoC File Upload Endpoint (Gerard Phase)

## Architecture Constraints

- Reference: `re-workspace/work-items/wi-005-separate-poc-upload-endpoint.md` (work item specification)
- RQ-009: Separate PoC Upload — the client uploads PoC files separately from the Excel invoice batch
- D-001: PoC filename matching is case-insensitive, full-string comparison
- D-003: PoC store path is configurable via application.yml; path traversal protection is mandatory
- D-020 / D-026: No authentication for MVP
- D-021: No file size limit for MVP
- New D-015: PoC upload endpoint rejects non-PDF files with a 400 Bad Request response. The endpoint validates MIME type is `application/pdf` server-side.
- New D-016: PoC upload overwrites existing files when a duplicate filename is uploaded
- New D-017: PoC upload endpoint path is `POST /api/v1/poc-upload`. The request body is multipart/form-data with a single field named `file`. The filename is extracted from the uploaded file.
- Existing `PoCStoreService` interface at `5-backend/business-service/src/main/java/com/gimmevettingsolution/poc/PoCStoreService.java`
- Existing `FileBackedPoCStoreService` implementation at `5-backend/business-service/src/main/java/com/gimmevettingsolution/poc/FileBackedPoCStoreService.java`
- Existing controller pattern: `ExcelIntakeController` at `5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/ExcelIntakeController.java`

## Subtasks

### Subtask 1: Define API Contract for PoC Upload Endpoint

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: `docs/wi-005-delegation-gerard.md` (this delegation plan), `re-workspace/work-items/wi-005-separate-poc-upload-endpoint.md`, `agent-definitions/architecture-decisions.md`
- **Output Artefact**: API contract at `docs/api-contract-wi-005.md`
- **Constraints**:
  - The contract must specify `POST /api/v1/poc-upload` endpoint
  - Request: `multipart/form-data` with a single field `file`
  - The filename is extracted from the uploaded file — no separate invoice number parameter
  - The filename must be sanitized against path traversal (same pattern as ExcelIntakeController)
  - Response on success: 200 OK with JSON body `{"status": "UPLOADED", "invoiceNumber": "<extracted-invoice-number>"}`
  - Response on non-PDF file: 400 Bad Request with JSON body `{"status": "INVALID_FILE_FORMAT", "errorDetail": "Only PDF files are accepted. Uploaded file type: <detected-mime-type>"}`
  - Response on path traversal: 400 Bad Request with JSON body `{"status": "INVALID_FILE_FORMAT", "errorDetail": "Path traversal detected in filename"}`
  - Response on internal error: 500 Internal Server Error
  - No authentication required (MVP)
  - No file size limit documented
  - The contract must reference that uploaded files are stored using the existing `PoCStoreService` and are immediately available for PoC matching via the existing `hasMatchingPoC()` algorithm
  - The contract must document D-015 (PDF-only enforcement) and D-016 (overwrite on duplicate)
- **Security Considerations**: The contract must mandate server-side MIME type validation. The contract must mandate filename sanitization against path traversal. The response error messages must not expose server internals (stack traces, file paths, directory structure).

### Subtask 2: Submit for Alignment Review

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: Updated `docs/api-contract-wi-005.md`
- **Output Artefact**: Alignment review request at `docs/alignment-review-request.md`
- **Constraints**: Submit with `pipelineStage: "gerard contract review for wi-005"` and list the contract as the produced artefact

## Expected API Contract Structure

The contract at `docs/api-contract-wi-005.md` should follow the format of `docs/api-contract-wi-003.md` and `docs/api-contract-wi-002.md` with these sections:

```
# API Contract: Wi-005 — Separate PoC File Upload Endpoint

Version: 5.0.0
Base Path: /api/v1

## POST /poc-upload

### Description
Upload a PoC (Proof of Correspondence) file for a specific invoice. The invoice number is
extracted from the filename. Files are stored in the same PoC store as the single-invoice
intake path (WI-001).

### Request

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | File | Yes | The PoC file to upload. Must be PDF. |

**File format constraints:**

| Format | Extension | MIME Type |
|--------|-----------|-----------|
| PDF | `.pdf` | `application/pdf` |

### Response — 200 OK (Upload Successful)

```json
{
  "status": "UPLOADED",
  "invoiceNumber": "INV-2026-0042"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | Always `"UPLOADED"` on success. |
| `invoiceNumber` | string | The invoice number extracted from the filename (lowercase, without `.pdf` extension). |

### Response — 400 Bad Request (Non-PDF File)

```json
{
  "status": "INVALID_FILE_FORMAT",
  "errorDetail": "Only PDF files are accepted. Uploaded file type: application/msword"
}
```

### Response — 400 Bad Request (Path Traversal Detected)

```json
{
  "status": "INVALID_FILE_FORMAT",
  "errorDetail": "Path traversal detected in filename"
}
```

### Response — 500 Internal Server Error

```json
{
  "status": "INTERNAL_ERROR",
  "errorDetail": "Unexpected error during PoC upload"
}
```

## Notes

- Authentication is absent for the MVP phase.
- No file size limit is enforced for the MVP phase.
- Duplicate filenames overwrite existing files in the PoC store.
- Uploaded files are immediately available for PoC matching via the existing `hasMatchingPoC()` algorithm.
- The filename-to-invoice-number mapping uses case-insensitive full-string comparison with `.pdf` extension stripping.
```

## Completion Criteria

The Gerard phase is complete when:
1. `docs/api-contract-wi-005.md` exists with the full PoC upload endpoint specification
2. Alignment review request submitted at `docs/alignment-review-request.md`
