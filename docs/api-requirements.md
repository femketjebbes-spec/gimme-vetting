# API Requirements: Case Analyst Invoice List, Detail & Source File API

**Produced By**: Femke (Frontend Agent)
**Timestamp**: 2026-07-09 14:20
**Source Contract**: `docs/api-contract-wi-ca-003.md` (v1.0.0), `docs/api-contract-wi-ca-001.md` (v1.0.0)
**Work Item**: WI-CA-003 (View Source Excel Files in Analyst Dashboard)
**Status**: Complete

---

## Endpoint 1: Paginated Invoice List

| Property | Value |
|----------|-------|
| HTTP Method | GET |
| Path | `/api/v1/analyst/invoices` |
| Request Parameters | `page` (integer, default 0, >= 0), `size` (integer, default 50, 1-200), `sort` (string, default `id,asc`, format `field,direction`), `status` (string, enum: QUEUED, REJECTED_TYPE_A, REJECTED_TYPE_B), `search` (string, max 256 chars) |
| Expected Response | JSON object with `content` (array of invoice items), `totalElements` (integer), `totalPages` (integer), `currentPage` (integer), `pageSize` (integer) |
| Authentication Required | No |
| Frontend Consumer | `4-frontend/src/business-service/api/analystApi.js` (`fetchInvoiceList`) |

### Response Schema

```json
{
  "content": [
    {
      "id": 1,
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
  ],
  "totalElements": 150,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 50
}
```

### New Fields

| Field | Type | Description |
|-------|------|-------------|
| `sourceFileId` | string (nullable) | UUID of the source Excel file. Null for non-Excel invoices. |
| `sourceFilename` | string (nullable) | Original filename the client uploaded. Null when sourceFileId is null. |

### Error Responses

| Status Code | Description |
|-------------|-------------|
| 400 Bad Request | Invalid query parameters (e.g., page negative, size exceeds 200, search exceeds 256 chars) |

---

## Endpoint 2: Single Invoice Detail

| Property | Value |
|----------|-------|
| HTTP Method | GET |
| Path | `/api/v1/analyst/invoices/{id}` |
| Request Parameters | `id` (integer, required, > 0) -- path variable |
| Expected Response | JSON object with invoice fields including `sourceFileId` and `sourceFilename` |
| Authentication Required | No |
| Frontend Consumer | `4-frontend/src/business-service/api/analystApi.js` (`fetchInvoiceDetail`) |

### Response Schema

```json
{
  "id": 1,
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

### New Fields

| Field | Type | Description |
|-------|------|-------------|
| `sourceFileId` | string (nullable) | UUID of the source Excel file. Null for non-Excel invoices. |
| `sourceFilename` | string (nullable) | Original filename the client uploaded. Null when sourceFileId is null. |

### Error Responses

| Status Code | Description |
|-------------|-------------|
| 404 Not Found | Invoice with requested ID does not exist |
| 400 Bad Request | Invalid invoice ID parameter (not a positive integer) |

---

## Endpoint 3: Source File Download (NEW)

| Property | Value |
|----------|-------|
| HTTP Method | GET |
| Path | `/api/v1/analyst/invoices/{id}/source-file` |
| Request Parameters | `id` (integer, required, > 0) -- path variable |
| Expected Response | Raw file bytes with `Content-Type` and `Content-Disposition` headers |
| Authentication Required | No |
| Frontend Consumer | `4-frontend/src/business-service/api/analystApi.js` (`fetchSourceFile`) |

### Response Headers (200 OK)

| Header | Value |
|--------|-------|
| `Content-Type` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` for .xlsx, `text/csv` for .csv |
| `Content-Disposition` | `inline; filename="<original-filename>"` |
| Body | Raw file bytes |

### Error Responses

| Status Code | Description | Response Body |
|-------------|-------------|---------------|
| 404 Not Found | Invoice has no sourceFileId or file not found in store | `{"error": "Not Found", "message": "No source file available for this invoice"}` |
| 500 Internal Server Error | File exists in DB but missing/corrupted in store | `{"error": "Internal Server Error", "message": "Source file is unavailable"}` |
| 400 Bad Request | Invalid invoice ID | `{"error": "Bad Request", "message": "Invalid invoice id parameter"}` |

---

## Architecture Decisions Applied

- **D-EXCEL-001**: Excel intake store is filesystem-based with configurable path (`gimme.excel-store-path`). Files are stored with UUID filenames. Same pattern as `FileBackedPoCStoreService`.
- **D-EXCEL-002**: Invoice entity gains nullable `sourceFileId` (VARCHAR(64)) and `sourceFilename` (VARCHAR(256)) fields.
- **D-EXCEL-003**: Source file serving endpoint is `GET /api/v1/analyst/invoices/{id}/source-file`. Returns raw file bytes with `Content-Type` and `Content-Disposition` headers.
- **D-EXCEL-004**: Upload flow is extended to persist the original file during existing intake processing. No new upload endpoint is created.
- **D-EXCEL-005**: `AnalystInvoiceDTO` gains `sourceFileId` and `sourceFilename` fields (both nullable strings).
- **D-CA-002**: Analyst API endpoints are unauthenticated for MVP.
- **D-026**: Unauthenticated MVP endpoints.
- **S-006**: Error responses must not expose stack traces, SQL, or server internals.
- **S-007**: UUID filenames prevent path traversal attacks on the file store.
- **S-008**: `source_filename` must be sanitised to prevent HTTP header injection.
- **S-009**: `Content-Type` must match the actual stored file format.
