# API Contract: Wi-005 — Separate PoC File Upload Endpoint

**Version**: 5.0.0
**Base Path**: `/api/v1`
**Produced By**: Gerard (API-Agent)
**Timestamp**: 2026-07-08
**Source**: `re-workspace/work-items/wi-005-separate-poc-upload-endpoint.md` (RQ-009)
**Architectural Decisions**: D-001, D-003, D-015, D-016, D-017, D-020, D-021, D-026

## Overview

This contract specifies the API endpoint for uploading Proof of Correspondence (PoC) files separately from the Excel invoice batch. The client uploads a PDF file named after the invoice number. The system stores the file in the existing PoC store and makes it immediately available for PoC matching via the existing `hasMatchingPoC()` algorithm.

## POST /poc-upload

### Description

Upload a PoC (Proof of Correspondence) file for a specific invoice. The invoice number is extracted from the filename. Files are stored in the same PoC store as the single-invoice intake path (WI-001).

### Request

**Method**: `POST`
**Path**: `/api/v1/poc-upload`
**Content-Type**: `multipart/form-data`
**Authentication**: None (MVP, per D-020)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | File | Yes | The PoC file to upload. Must be PDF. |

**File format constraints:**

| Format | Extension | MIME Type |
|--------|-----------|-----------|
| PDF | `.pdf` | `application/pdf` |

**Filename rules:**
- The filename is extracted from the uploaded file
- The filename must pass path traversal sanitization (alphanumeric, hyphens, underscores, dots only)
- The invoice number is derived from the filename by stripping the `.pdf` extension and lowercasing
- No separate invoice number parameter is required

**Constraints (per architectural decisions):**
- D-015: Non-PDF files are rejected with a 400 Bad Request response
- D-016: Duplicate filenames overwrite existing files in the PoC store
- D-017: Invoice number is extracted from filename, not a separate parameter
- D-020: No authentication for MVP
- D-021: No file size limit for MVP

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

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | `"INVALID_FILE_FORMAT"` |
| `errorDetail` | string | Human-readable description. Indicates the detected MIME type of the rejected file. |

### Response — 400 Bad Request (Path Traversal Detected)

```json
{
  "status": "INVALID_FILE_FORMAT",
  "errorDetail": "Path traversal detected in filename"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | `"INVALID_FILE_FORMAT"` |
| `errorDetail` | string | Fixed message: `"Path traversal detected in filename"` |

### Response — 500 Internal Server Error

```json
{
  "status": "INTERNAL_ERROR",
  "errorDetail": "Unexpected error during PoC upload"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | `"INTERNAL_ERROR"` |
| `errorDetail` | string | Technical description of the failure. Must not expose server internals (stack traces, file paths, directory structure). |

## Integration Notes

### PoC Store Storage

Uploaded files are stored using the existing [`PoCStoreService`](5-backend/business-service/src/main/java/com/gimmevettingsolution/poc/PoCStoreService.java) interface via a new `store(MultipartFile file)` method on the implementation (`FileBackedPoCStoreService`). Files are stored in the configurable PoC store directory (D-003).

### PoC Matching

Uploaded files are immediately available for PoC matching. A subsequent Excel upload containing the corresponding invoice number will pass the PoC existence gate via the existing `hasMatchingPoC()` algorithm:

```
poCFileName.toLowerCase().endsWith(".pdf")
    ? poCFileName.toLowerCase().substring(0, poCFileName.toLowerCase().length() - 4)
    : poCFileName.toLowerCase()
== invoiceNumber.toLowerCase()
```

### Error Mapping Registry

| Internal Error | API Response | Description |
|----------------|--------------|-------------|
| `IOException` (store write failure) | 500 Internal Server Error | File store write failed |
| `IllegalArgumentException` (bad filename) | 400 Bad Request (Path Traversal) | Path traversal detected |
| `IllegalArgumentException` (non-PDF MIME type) | 400 Bad Request (Invalid File Format) | Non-PDF file rejected |

## Security Considerations

- Server-side MIME type validation is mandatory (D-015)
- Filename sanitization against path traversal is mandatory (D-003)
- Error messages must not expose server internals (stack traces, file paths, directory structure)
- No authentication for MVP (D-020) — flagged for future remediation

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 5.0.0 | 2026-07-08 | Initial contract for Wi-005 PoC upload endpoint |
