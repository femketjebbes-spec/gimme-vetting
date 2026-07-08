# API Contract: Excel File Upload and Batch Processing Pipeline

**Work Item:** WI-002 (Excel File Upload and Parsing)
**Endpoint:** POST `/api/v1/intake/excel`
**Version:** 2.0.0
**Date:** 2026-07-07
**Owner:** Gerard (API-Agent)
**Status:** Pending Alignment Agent approval

---

## 1. Overview

This contract defines the Excel file upload and batch processing pipeline endpoint. The endpoint accepts a multipart file upload containing either `.xlsx` or `.csv` format. The server parses each row, maps columns to domain fields (invoiceNumber, debtorName, address, phoneNumber, bankAccountNumber), performs mandatory field validation, verifies Proof of Correspondence (PoC) existence for each row, and returns a summary result including a download link for a return Excel file containing only the rows that failed validation.

Processing is synchronous: the client uploads a file, the server processes all rows through the full pipeline (parsing, mandatory field validation, PoC existence verification), and returns the result in the same HTTP response cycle.

```
NOTE: Authentication is absent for the PoC phase. This endpoint is unauthenticated and should be protected in a future work item.
```

---

## 2. Endpoint Definition

| Property | Value |
|----------|-------|
| Path | `/api/v1/intake/excel` |
| Method | `POST` |
| Authentication | None (PoC phase only) |
| Content-Type | `multipart/form-data` |
| File Field Name | `file` |
| Processing Model | Synchronous |
| Parsing Library | Apache POI (latest patched version) |

---

## 3. Request Schema

### 3.1 Body

The request body uses `multipart/form-data` with a single file field named `file`.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | File | Yes | The Excel file to upload. Must be `.xlsx` or `.csv`. |

### 3.2 Supported File Formats

| Format | Extension | MIME Type |
|--------|-----------|-----------|
| Excel (.xlsx) | `.xlsx` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| CSV | `.csv` | `text/csv` |

The adapter layer MUST validate the MIME type server-side before any processing. Files with unsupported MIME types receive an immediate `400 Bad Request` response.

### 3.3 Header Constraints

| Header | Required | Value |
|--------|----------|-------|
| `Content-Type` | Yes | `multipart/form-data` |

---

## 4. Parsing Rules

### 4.1 Header Detection

The header row is optional. When present, column names are matched case-insensitively against the allowlist defined in Section 4.2. When absent, column position mapping is applied as defined in Section 4.3.

### 4.2 Column Name Allowlist (Header Row Present)

When the uploaded file contains a header row, column names are matched case-insensitively against the following allowlist:

| Column Name (Header) | Canonical Field Name |
|----------------------|---------------------|
| "invoice number" | invoiceNumber |
| "debtor name" | debtorName |
| "address" | address |
| "phone number" | phoneNumber |
| "bank account number" | bankAccountNumber |

Any column name not matching an entry in this allowlist triggers a `400 Bad Request` response. The error response MUST list all unrecognized column names.

### 4.3 Column Position Mapping (No Header Row)

When the uploaded file does not contain a header row, column positions determine field mapping:

| Column Position | Canonical Field Name |
|-----------------|---------------------|
| 0 | invoiceNumber |
| 1 | debtorName |
| 2 | address |
| 3 | phoneNumber |
| 4 | bankAccountNumber |

### 4.4 Row Processing

| Condition | Handling |
|-----------|----------|
| Empty row | Skipped during parsing. Not counted in `totalRowsProcessed`. |
| Row with fewer columns than expected | Parsed with null/empty for missing fields. Passed downstream. |
| Row with extra columns | Extra columns ignored. |

### 4.5 Parsed Row Domain Object

Each parsed row produces a transient domain object consumed by downstream validation:

```java
public class ExcelInvoiceRow {
    private Integer rowIndex;        // 0-based row index in the source file
    private String invoiceNumber;
    private String debtorName;
    private String address;
    private String phoneNumber;
    private String bankAccountNumber;
    private List<String> parseErrors; // null if no errors
}
```

---

## 5. Response Schemas

### 5.1 200 OK — Processing Complete

The file was parsed successfully and all validation gates completed. The response includes a summary of processing results and a download link for the return Excel file.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["processingStatus", "totalRowsProcessed", "rowsPassed", "rowsFailed", "returnExcelDownloadLink"],
  "properties": {
    "processingStatus": {
      "type": "string",
      "enum": ["COMPLETED"],
      "description": "Processing completed successfully."
    },
    "totalRowsProcessed": {
      "type": "integer",
      "minimum": 0,
      "description": "Total number of non-empty rows parsed from the uploaded file."
    },
    "rowsPassed": {
      "type": "integer",
      "minimum": 0,
      "description": "Number of rows that passed all validation gates (mandatory field validation and PoC existence verification)."
    },
    "rowsFailed": {
      "type": "integer",
      "minimum": 0,
      "description": "Number of rows that failed at least one validation gate."
    },
    "returnExcelDownloadLink": {
      "type": "string",
      "description": "URL or server-relative path to download the return Excel file. The file contains only the rows that failed validation."
    }
  },
  "additionalProperties": false
}
```

**Example Response:**

```json
{
  "processingStatus": "COMPLETED",
  "totalRowsProcessed": 10,
  "rowsPassed": 7,
  "rowsFailed": 3,
  "returnExcelDownloadLink": "/api/v1/intake/excel/download/temp-return-excel-abc123.xlsx"
}
```

### 5.2 400 Bad Request — Invalid File Format

The uploaded file is malformed, corrupted, or has an unsupported MIME type. No rows are parsed.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["status", "errorDetail"],
  "properties": {
    "status": {
      "type": "string",
      "enum": ["INVALID_FILE_FORMAT"],
      "description": "The uploaded file could not be processed."
    },
    "errorDetail": {
      "type": "string",
      "description": "Human-readable description of the file format error."
    }
  },
  "additionalProperties": false
}
```

**Example Response:**

```json
{
  "status": "INVALID_FILE_FORMAT",
  "errorDetail": "Unsupported MIME type: application/msword. Expected application/vnd.openxmlformats-officedocument.spreadsheetml.sheet or text/csv."
}
```

### 5.3 400 Bad Request — Column Name Mismatch

The uploaded file contains a header row with one or more unrecognized column names.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["status", "unrecognizedColumns"],
  "properties": {
    "status": {
      "type": "string",
      "enum": ["COLUMN_NAME_MISMATCH"],
      "description": "One or more column names in the header row are not recognized."
    },
    "unrecognizedColumns": {
      "type": "array",
      "items": {
        "type": "string"
      },
      "minItems": 1,
      "description": "List of unrecognized column names found in the header row."
    }
  },
  "additionalProperties": false
}
```

**Example Response:**

```json
{
  "status": "COLUMN_NAME_MISMATCH",
  "unrecognizedColumns": ["invoice id", "client name"]
}
```

### 5.4 500 Internal Server Error — Processing Failure

An unexpected error occurred during file processing. The client should retry the request.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["status", "errorDetail"],
  "properties": {
    "status": {
      "type": "string",
      "enum": ["INTERNAL_ERROR"],
      "description": "An unexpected error occurred during processing."
    },
    "errorDetail": {
      "type": "string",
      "description": "Technical description of the failure. Must NOT expose server internals."
    }
  },
  "additionalProperties": false
}
```

**Example Response:**

```json
{
  "status": "INTERNAL_ERROR",
  "errorDetail": "Unexpected error during Excel processing"
}
```

---

## 6. Error Mapping Registry

The adapter layer maps internal backend errors to clean API responses using the following registry:

| Backend Error | HTTP Status | API Response `status` | Mapping Rationale |
|--------------|-------------|-----------------------|-------------------|
| `INVALID_MIME_TYPE` | 400 | `INVALID_FILE_FORMAT` | Uploaded file MIME type not in allowlist. |
| `FILE_CORRUPTED` | 400 | `INVALID_FILE_FORMAT` | File cannot be opened by the parser. |
| `UNSUPPORTED_FORMAT` | 400 | `INVALID_FILE_FORMAT` | File extension or MIME type not supported. |
| `COLUMN_NAME_MISMATCH` | 400 | `COLUMN_NAME_MISMATCH` | Header row contains unrecognized column names. |
| `PROCESSING_FAILURE` | 500 | `INTERNAL_ERROR` | Unexpected error during parsing or validation. |
| `PATH_TRAVERSAL_DETECTED` | 400 | `INVALID_FILE_FORMAT` | Uploaded filename contained path traversal patterns. |

---

## 7. Architectural Constraints

| Constraint ID | Requirement | Enforcement |
|---------------|-------------|-------------|
| D-024 | Strict column name matching. Only the five allowed column names are accepted (case-insensitive). Unknown column names trigger a structured 400 rejection. | Adapter layer request transformer validates header names against the allowlist before parsing. |
| D-025 | Supports `.xlsx` and `.csv` formats. Header row is optional. Column position mapping applies when no header row is present. | Parser adapts to file format and header presence. |
| D-026 | No authentication for MVP. Endpoint is unauthenticated. | No authentication middleware applied to POST `/api/v1/intake/excel` in this phase. |
| D-027 | No file size limit enforced for MVP. | No body size middleware applied. Architect recommends designing a size boundary for MVP+1. |
| D-028 | Synchronous processing. Client uploads, server processes, server returns result in the same HTTP response cycle. | Endpoint handler executes the full pipeline (parse → validate → PoC check → generate return Excel) before responding. |
| D-029 | Apache POI is the mandated Excel parsing library. Latest patched version must be used. XML entity expansion must be disabled. | Backend uses Apache POI with XXE protection enabled. |

---

## 8. Security Requirements

| # | Requirement | Enforcement Layer |
|---|-------------|-------------------|
| S-007 | MIME type validation server-side. The adapter layer validates the uploaded file MIME type against the allowlist (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `text/csv`) before any processing. Client-side MIME type checks are insufficient. | Adapter layer file upload middleware. |
| S-008 | Column name allowlist enforcement. Header column names are validated against a fixed allowlist. Unknown column names trigger an immediate 400 rejection with structured error listing unrecognized names. No column name injection vectors exist since column names are validated before parsing. | Adapter layer request transformer. |
| S-009 | Apache POI security. The version used must be the latest patched version to mitigate XML external entity (XXE) and ZIP slip vulnerabilities. XML entity expansion must be disabled in parser configuration. | Backend Apache POI parser configuration. |
| S-010 | File upload path traversal prevention. The uploaded filename is sanitized to prevent path traversal attacks (`../`, `..\\`, absolute path patterns). The file is stored in a secure server-side temporary directory. | Adapter layer file upload handler. |
| S-011 | Return Excel download link security. The download link must not expose the server-side temporary file path. The link must be a controlled URL that the adapter layer resolves to the temporary file with an expiration mechanism. | Adapter layer response transformer. |

---

## 9. Dependencies

| Dependency | Source | Notes |
|------------|--------|-------|
| RQ-006 | [`re-workspace/requirements-spec.md`](re-workspace/requirements-spec.md) | Excel Batch Intake requirement. |
| RQ-007 | [`re-workspace/requirements-spec.md`](re-workspace/requirements-spec.md) | Mandatory Field Validation requirement (downstream). |
| RQ-008 | [`re-workspace/requirements-spec.md`](re-workspace/requirements-spec.md) | Return Excel with Missing Data requirement (downstream). |
| D-024 | [`agent-definitions/architecture-decisions.md`](agent-definitions/architecture-decisions.md) | Strict column name matching. |
| D-025 | [`agent-definitions/architecture-decisions.md`](agent-definitions/architecture-decisions.md) | Format and header support. |
| D-026 | [`agent-definitions/architecture-decisions.md`](agent-definitions/architecture-decisions.md) | No authentication for MVP. |
| D-027 | [`agent-definitions/architecture-decisions.md`](agent-definitions/architecture-decisions.md) | No file size limit for MVP. |
| D-028 | [`agent-definitions/architecture-decisions.md`](agent-definitions/architecture-decisions.md) | Synchronous processing. |
| D-029 | [`agent-definitions/architecture-decisions.md`](agent-definitions/architecture-decisions.md) | Apache POI library. |
| WI-002 | [`re-workspace/work-items/wi-002-excel-file-upload-and-parsing.md`](re-workspace/work-items/wi-002-excel-file-upload-and-parsing.md) | Formal work item definition. |
| WI-003 | [`re-workspace/work-items/wi-003-per-row-mandatory-field-validation.md`](re-workspace/work-items/wi-003-per-row-mandatory-field-validation.md) | Downstream validation output contract. |
| WI-004 | [`re-workspace/work-items/wi-004-return-excel-generation.md`](re-workspace/work-items/wi-004-return-excel-generation.md) | Downstream return Excel output contract. |

---

## 10. Contract Versioning

| Version | Date | Change |
|---------|------|--------|
| 2.0.0 | 2026-07-07 | Initial contract for WI-002 Excel File Upload and Parsing. New endpoint (`/api/v1/intake/excel`), new request format (`multipart/form-data`), new response structure (batch processing summary). Different from WI-001 (1.0.0) which uses `POST /api/v1/intake` with `application/json`. |
