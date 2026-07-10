# API Contract: Source File Viewing in Analyst Dashboard

**Work Item:** WI-CA-003 (View Source Excel Files in Analyst Dashboard)
**Endpoints:** `GET /api/v1/analyst/invoices/{id}/source-file` (new), `GET /api/v1/analyst/invoices` (modified), `GET /api/v1/analyst/invoices/{id}` (modified)
**Version:** 1.0.0
**Date:** 2026-07-09
**Owner:** Gerard (API-Agent)
**Status:** Submitted to Alignment Agent for review

---

## 1. Overview

This contract defines a new source file serving endpoint and schema extensions to existing endpoints for the Case Analyst dashboard. The new endpoint allows analysts to download the original Excel file that a client uploaded during intake. The existing detail and list endpoints gain two new fields to support the "Bekijken" (View) link state.

**Architectural Decisions Applied:**
- D-EXCEL-001: Excel intake store is filesystem-based with configurable path (`gimme.excel-store-path`). Files are stored with UUID filenames. Same pattern as `FileBackedPoCStoreService`.
- D-EXCEL-002: Invoice entity gains nullable `sourceFileId` (VARCHAR(64)) and `sourceFilename` (VARCHAR(256)) fields.
- D-EXCEL-003: Source file serving endpoint is `GET /api/v1/analyst/invoices/{id}/source-file`. Returns raw file bytes with `Content-Type` and `Content-Disposition` headers.
- D-EXCEL-004: Upload flow is extended to persist the original file during existing intake processing.
- D-EXCEL-005: `AnalystInvoiceDTO` gains `sourceFileId` and `sourceFilename` fields (both nullable strings).
- D-CA-002: Analyst API endpoints are unauthenticated for MVP.
- D-026: Unauthenticated MVP endpoints.
- S-006: Error responses must not expose stack traces, SQL, or server internals.

---

## 2. New Endpoint: Source File Serving

### 2.1 Definition

| Property | Value |
|----------|-------|
| Path | `/api/v1/analyst/invoices/{id}/source-file` |
| Method | `GET` |
| Authentication | None (MVP limitation per D-CA-002) |
| Path Variable | `id` (Long, required, `> 0`) |
| Response Content-Type | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` for `.xlsx`, `text/csv` for `.csv` |
| Response Content-Disposition | `inline; filename="<original-filename>"` |

### 2.2 Request

| Property | Value |
|----------|-------|
| Path | `/api/v1/analyst/invoices/{id}/source-file` |
| Method | `GET` |
| Headers | None required |

### 2.3 Response — 200 OK

Returns the original Excel file bytes.

| Header | Value |
|--------|-------|
| `Content-Type` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` (for `.xlsx`) or `text/csv` (for `.csv`) |
| `Content-Disposition` | `inline; filename="<original-filename>"` where `<original-filename>` is the filename the client originally uploaded (e.g., `batch-001.xlsx`) |
| Body | Raw file bytes |

**Example Response Headers:**
```
HTTP/1.1 200 OK
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: inline; filename="batch-001.xlsx"
Content-Length: 24576
```

**Security Note:** The `Content-Type` must be determined from the stored file format, not from user input. The `source_filename` stored in the database must be sanitised against HTTP header injection (reject or escape newlines, semicolons, and other control characters before storing).

### 2.4 Error Response — 404 Not Found

Returned when the invoice has no `sourceFileId` (null, e.g., imported via single-invoice API) or when the source file is not found in the store.

```json
{
  "error": "Not Found",
  "message": "No source file available for this invoice"
}
```

### 2.5 Error Response — 500 Internal Server Error

Returned when the file exists in the database mapping but is missing or corrupted in the file store.

```json
{
  "error": "Internal Server Error",
  "message": "Source file is unavailable"
}
```

**Security Note:** Error responses must not expose the Excel store path, file system structure, or internal error details. The `source_file_id` UUID must not appear in error messages.

### 2.6 Error Response — 400 Bad Request

Returned when the invoice ID is invalid (not a positive integer).

```json
{
  "error": "Bad Request",
  "message": "Invalid invoice id parameter"
}
```

### 2.7 Security Considerations

| Concern | Mitigation |
|---------|------------|
| Path traversal | UUID filenames prevent path traversal. The `sourceFileId` is a 64-character UUID string used as the sole filesystem lookup key. |
| Header injection | `source_filename` must be sanitised: reject or escape characters including newlines (`\n`, `\r`), semicolons (`;`), and other control characters (ASCII 0-31). |
| MIME type spoofing | `Content-Type` must be set explicitly based on the actual stored file format, not derived from `source_filename` or user input. |
| Execution prevention | Validate that stored files are `.xlsx` or `.csv` only. Reject any file with a different extension on write. |
| File size | Files exceeding 50MB shall be rejected at upload time with 413 Request Entity Too Large. |

---

## 3. Modified Endpoint: Single Invoice Detail

### 3.1 Definition (Unchanged from Contract wi-ca-001 v1.0.0)

| Property | Value |
|----------|-------|
| Path | `/api/v1/analyst/invoices/{id}` |
| Method | `GET` |
| Authentication | None (MVP limitation per D-CA-002) |
| Content-Type | `application/json` |
| Path Variable | `id` (Long, required, `> 0`) |

### 3.2 Response Schema — 200 OK (Modified)

The response schema from `docs/api-contract-wi-ca-001.md` is extended with two new nullable fields: `sourceFileId` and `sourceFilename`.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["id", "invoiceNumber", "debtorName", "address", "bankAccountNumber", "phoneNumber", "status", "poCStatus", "rejectionType", "resubmissionCount", "sourceFileId", "sourceFilename"],
  "properties": {
    "id": {
      "type": "integer",
      "format": "int64",
      "description": "Database primary key."
    },
    "invoiceNumber": {
      "type": "string",
      "maxLength": 128,
      "pattern": "^[A-Za-z0-9\\-_.]+$",
      "description": "System Identifier. Used for PoC filename matching."
    },
    "debtorName": {
      "type": "string",
      "maxLength": 256,
      "description": "Full legal name of the debtor."
    },
    "address": {
      "type": "string",
      "maxLength": 512,
      "description": "Concatenated address: street, postal code, city (as stored in the database)."
    },
    "bankAccountNumber": {
      "type": "string",
      "maxLength": 34,
      "description": "IBAN or national account number."
    },
    "phoneNumber": {
      "type": "string",
      "maxLength": 20,
      "description": "Contact phone number."
    },
    "status": {
      "type": "string",
      "enum": ["QUEUED", "REJECTED_TYPE_A", "REJECTED_TYPE_B"],
      "description": "Current invoice processing status."
    },
    "poCStatus": {
      "type": "string",
      "enum": ["VERIFIED", "MISSING", "PENDING"],
      "description": "Proof of Correspondence verification status."
    },
    "rejectionType": {
      "type": ["string", "null"],
      "enum": [null, "REJECTED_TYPE_A", "REJECTED_TYPE_B"],
      "description": "Rejection type, if applicable. Null for non-rejected invoices."
    },
    "resubmissionCount": {
      "type": "integer",
      "description": "Number of times this invoice has been resubmitted by the client."
    },
    "sourceFileId": {
      "type": ["string", "null"],
      "pattern": "^[0-9a-fA-F-]{36}$",
      "maxLength": 64,
      "description": "UUID of the source Excel file. Null for invoices not originating from Excel upload (e.g., single-invoice API imports)."
    },
    "sourceFilename": {
      "type": ["string", "null"],
      "maxLength": 256,
      "description": "Original filename the client uploaded (e.g., 'batch-001.xlsx'). Null when sourceFileId is null. Sanitised against header injection."
    }
  },
  "additionalProperties": false
}
```

### 3.3 Example Response — 200 OK (With Source File)

```json
{
  "id": 42,
  "invoiceNumber": "INV-2026-0042",
  "debtorName": "Jan de Vries",
  "address": "Voorbeeldstraat 1, 1234AB Amsterdam",
  "bankAccountNumber": "NL12BUNQ0123456789",
  "phoneNumber": "+31612345678",
  "status": "QUEUED",
  "poCStatus": "VERIFIED",
  "rejectionType": null,
  "resubmissionCount": 0,
  "sourceFileId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "sourceFilename": "batch-001.xlsx"
}
```

### 3.4 Example Response — 200 OK (Without Source File)

```json
{
  "id": 99,
  "invoiceNumber": "INV-2026-0099",
  "debtorName": "Pieter Janssen",
  "address": "Teststraat 10, 1000AA Rotterdam",
  "bankAccountNumber": "NL67INGB0000000000",
  "phoneNumber": "+31698765432",
  "status": "QUEUED",
  "poCStatus": "PENDING",
  "rejectionType": null,
  "resubmissionCount": 0,
  "sourceFileId": null,
  "sourceFilename": null
}
```

---

## 4. Modified Endpoint: Paginated Invoice List

### 4.1 Definition (Unchanged from Contract wi-ca-001 v1.0.0)

| Property | Value |
|----------|-------|
| Path | `/api/v1/analyst/invoices` |
| Method | `GET` |
| Authentication | None (MVP limitation per D-CA-002) |
| Content-Type | `application/json` |

### 4.2 Query Parameters (Unchanged from Contract wi-ca-001 v1.0.0)

| Parameter | Type | Required | Default | Constraints | Description |
|-----------|------|----------|---------|-------------|-------------|
| `page` | integer | No | `0` | `>= 0` | Page number (0-indexed) |
| `size` | integer | No | `50` | `1 <= size <= 200` | Items per page |
| `sort` | string | No | `id,asc` | Format: `field,direction` | Sort field(s) and direction |
| `status` | string | No | none | Comma-separated: `QUEUED`, `REJECTED_TYPE_A`, `REJECTED_TYPE_B` | Filter by status |
| `search` | string | No | none | Max length: 256 | Case-insensitive partial match |

### 4.3 Response Schema — 200 OK (Modified)

The `AnalystInvoiceItem` definition in the list response gains two new fields: `sourceFileId` and `sourceFilename`.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["content", "totalElements", "totalPages", "currentPage", "pageSize"],
  "properties": {
    "content": {
      "type": "array",
      "items": {
        "$ref": "#/definitions/AnalystInvoiceItem"
      }
    },
    "totalElements": { "type": "integer" },
    "totalPages": { "type": "integer" },
    "currentPage": { "type": "integer" },
    "pageSize": { "type": "integer" }
  },
  "definitions": {
    "AnalystInvoiceItem": {
      "type": "object",
      "required": ["id", "invoiceNumber", "debtorName", "address", "bankAccountNumber", "phoneNumber", "status", "poCStatus", "rejectionType", "resubmissionCount", "sourceFileId", "sourceFilename"],
      "properties": {
        "id": { "type": "integer", "format": "int64" },
        "invoiceNumber": { "type": "string", "maxLength": 128, "pattern": "^[A-Za-z0-9\\-_.]+$" },
        "debtorName": { "type": "string", "maxLength": 256 },
        "address": { "type": "string", "maxLength": 512 },
        "bankAccountNumber": { "type": "string", "maxLength": 34 },
        "phoneNumber": { "type": "string", "maxLength": 20 },
        "status": { "type": "string", "enum": ["QUEUED", "REJECTED_TYPE_A", "REJECTED_TYPE_B"] },
        "poCStatus": { "type": "string", "enum": ["VERIFIED", "MISSING", "PENDING"] },
        "rejectionType": { "type": ["string", "null"], "enum": [null, "REJECTED_TYPE_A", "REJECTED_TYPE_B"] },
        "resubmissionCount": { "type": "integer" },
        "sourceFileId": {
          "type": ["string", "null"],
          "pattern": "^[0-9a-fA-F-]{36}$",
          "maxLength": 64,
          "description": "UUID of the source Excel file. Null for non-Excel invoices."
        },
        "sourceFilename": {
          "type": ["string", "null"],
          "maxLength": 256,
          "description": "Original filename the client uploaded. Null when sourceFileId is null."
        }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false
}
```

### 4.4 Example Response — 200 OK

```json
{
  "content": [
    {
      "id": 42,
      "invoiceNumber": "INV-2026-0042",
      "debtorName": "Jan de Vries",
      "address": "Voorbeeldstraat 1, 1234AB Amsterdam",
      "bankAccountNumber": "NL12BUNQ0123456789",
      "phoneNumber": "+31612345678",
      "status": "QUEUED",
      "poCStatus": "VERIFIED",
      "rejectionType": null,
      "resubmissionCount": 0,
      "sourceFileId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "sourceFilename": "batch-001.xlsx"
    },
    {
      "id": 99,
      "invoiceNumber": "INV-2026-0099",
      "debtorName": "Pieter Janssen",
      "address": "Teststraat 10, 1000AA Rotterdam",
      "bankAccountNumber": "NL67INGB0000000000",
      "phoneNumber": "+31698765432",
      "status": "QUEUED",
      "poCStatus": "PENDING",
      "rejectionType": null,
      "resubmissionCount": 0,
      "sourceFileId": null,
      "sourceFilename": null
    }
  ],
  "totalElements": 150,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 50
}
```

---

## 5. Error Mapping Registry

| Internal Error | HTTP Status | API Response | Rationale |
|---------------|-------------|--------------|-----------|
| Invoice not found (source_file_id is null) | 404 | `{"error": "Not Found", "message": "No source file available for this invoice"}` | Invoice exists but has no linked source file (single-invoice API import) |
| Source file not found in store | 500 | `{"error": "Internal Server Error", "message": "Source file is unavailable"}` | UUID mapping exists but file is missing from disk |
| Invalid invoice ID | 400 | `{"error": "Bad Request", "message": "Invalid invoice id parameter"}` | Path variable is not a positive integer |
| File size exceeds 50MB at upload | 413 | `{"error": "Request Entity Too Large", "message": "File size exceeds 50MB limit"}` | Per NFR-003 |
| Invalid file format at upload | 400 | Existing `InvalidFileFormatResponse` pattern | Consistent with existing intake error handling |

**Security Rule:** Error responses must never include stack traces, SQL error codes, file system paths, or server-internal identifiers. The `source_file_id` UUID must not appear in any error message.

---

## 6. Database Migration

### 6.1 Flyway Migration: `V3__add_source_file_id_to_invoices.sql`

| Column | Type | Constraints | Default | Comment |
|--------|------|-------------|---------|---------|
| `source_file_id` | VARCHAR(64) | NULL | NULL | UUID of the source Excel file the invoice came from |
| `source_filename` | VARCHAR(256) | NULL | NULL | Original filename the client uploaded (sanitised) |

**Migration SQL:**
```sql
ALTER TABLE invoices ADD COLUMN source_file_id VARCHAR(64);
COMMENT ON COLUMN invoices.source_file_id IS 'UUID of the source Excel file the invoice came from';

ALTER TABLE invoices ADD COLUMN source_filename VARCHAR(256);
COMMENT ON COLUMN invoices.source_filename IS 'Original filename the client uploaded, sanitised against header injection';
```

**Constraints:**
- Both columns are nullable. Existing data will have NULL values.
- Existing invoices imported via single-invoice API (not Excel) will have NULL for both columns.
- Migration must be idempotent (Flyway versioning ensures it runs once).

---

## 7. Architectural Decisions

| Decision ID | Description |
|-------------|-------------|
| D-EXCEL-001 | Excel intake store is filesystem-based with configurable path (`gimme.excel-store-path`). Files are stored with UUID filenames. Same pattern as `FileBackedPoCStoreService`. |
| D-EXCEL-002 | Invoice entity gains nullable `sourceFileId` (VARCHAR(64)) and `sourceFilename` (VARCHAR(256)) fields. |
| D-EXCEL-003 | Source file serving endpoint is `GET /api/v1/analyst/invoices/{id}/source-file`. Returns raw file bytes with `Content-Type` and `Content-Disposition` headers. |
| D-EXCEL-004 | Upload flow is extended to persist the original file during existing intake processing. No new upload endpoint is created. |
| D-EXCEL-005 | `AnalystInvoiceDTO` gains `sourceFileId` and `sourceFilename` fields (both nullable strings). |
| D-CA-002 | Analyst API endpoints are unauthenticated for MVP. |
| D-026 | Unauthenticated MVP endpoints. |

---

## 8. Security Requirements

| ID | Requirement | Enforcement |
|----|-------------|-------------|
| S-006 | Error responses must not expose stack traces, SQL errors, file system paths, or server internals. | Implementation review and contract test verification. |
| S-007 | UUID filenames prevent path traversal attacks on the file store. | `FileBackedExcelStoreService` uses UUID-based lookup only. |
| S-008 | `source_filename` must be sanitised to prevent HTTP header injection. | Reject or escape newlines, semicolons, and control characters (ASCII 0-31) at write time. |
| S-009 | `Content-Type` must match the actual stored file format, not derived from user input. | Determine MIME type from file extension stored at upload time. Validate against allowlist (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `text/csv`). |
| S-010 | File size limit: 50MB maximum at upload time. | Reject with 413 status per NFR-003. |

---

## 9. Integration Points

### 9.1 Backend Components

| Component | Path | Change |
|-----------|------|--------|
| `FileBackedExcelStoreService` | New | Service for Excel file persistence and retrieval. Follows `FileBackedPoCStoreService` pattern. |
| `Invoice` entity | `5-backend/.../invoice/entity/Invoice.java` | Add `sourceFileId` (String) and `sourceFilename` (String) fields with JPA annotations. |
| `AnalystInvoiceDTO` | `5-backend/.../analyst/dto/AnalystInvoiceDTO.java` | Add `sourceFileId` (String) and `sourceFilename` (String) fields. |
| `AnalystController` | `5-backend/.../analyst/controller/AnalystController.java` | Add `getSourceFile` endpoint method. Update `getInvoiceDetail` to include new fields. |
| `ExcelIntakeController` | `5-backend/.../intake/ExcelIntakeController.java` | Inject `FileBackedExcelStoreService`. Save uploaded file to store and persist UUID + filename on created Invoice entities. |
| Flyway migration | `5-backend/.../db/migration/V3__add_source_file_id_to_invoices.sql` | New migration adding two columns to `invoices` table. |

### 9.2 Frontend Components

| Component | Path | Change |
|-----------|------|--------|
| `analystApi.js` | `4-frontend/.../api/analystApi.js` | Add `fetchSourceFile(id)` function. Update response parsing to include `sourceFileId` and `sourceFilename` in list and detail responses. |
| `InvoiceDrawer.jsx` | `4-frontend/.../components/InvoiceDrawer.jsx` | Wire "Bekijken" link to `fetchSourceFile`. Enable/disable based on `sourceFileId` presence. |
| `analystApi.test.js` | `4-frontend/.../api/__tests__/analystApi.test.js` | Add tests for `fetchSourceFile` endpoint. |

---

## 10. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-07-09 | Initial contract for WI-CA-003. New endpoint `GET /api/v1/analyst/invoices/{id}/source-file`. Schema extensions to list and detail endpoints. |
