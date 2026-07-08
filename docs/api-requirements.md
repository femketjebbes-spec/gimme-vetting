# API Requirements Document

**Produced By**: Femke (Frontend Agent)
**Timestamp**: 2026-07-07 13:08
**Source Contract**: `docs/api-contract-wi-002.md` (v2.0.0)
**Frontend Component**: `4-frontend/src/frontend/components/ExcelUpload.jsx`

## Overview

This document specifies the API endpoints required by the ExcelUpload frontend component for the WI-002 Excel File Upload and Batch Processing Pipeline. The frontend consumes a single endpoint to upload Excel or CSV files for batch processing.

## Endpoints

### POST /api/v1/intake/excel

| Attribute | Value |
|-----------|-------|
| HTTP Method | POST |
| Path | `/api/v1/intake/excel` |
| Content-Type | `multipart/form-data` |
| Authentication | No (PoC phase, per D-026) |

#### Request

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | File | Yes | The Excel file to upload. Must be `.xlsx` or `.csv`. |

**File format constraints (client-side, per S-007):**

| Format | Extension | MIME Type |
|--------|-----------|-----------|
| Excel (.xlsx) | `.xlsx` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| CSV | `.csv` | `text/csv` |

#### Expected Response — 200 OK (Processing Complete)

```json
{
  "processingStatus": "COMPLETED",
  "totalRowsProcessed": 10,
  "rowsPassed": 7,
  "rowsFailed": 3,
  "returnExcelDownloadLink": "/api/v1/intake/excel/download/temp-return-excel-abc123.xlsx"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `processingStatus` | string | Always `"COMPLETED"` on success. |
| `totalRowsProcessed` | integer | Total number of non-empty rows parsed. |
| `rowsPassed` | integer | Number of rows that passed all validation gates. |
| `rowsFailed` | integer | Number of rows that failed at least one validation gate. |
| `returnExcelDownloadLink` | string | URL or server-relative path to download the return Excel file. `null` when `rowsFailed` is 0. |

**Frontend behaviour:** When `rowsFailed > 0` and `returnExcelDownloadLink` is non-null, render a download link using the `returnExcelDownloadLink` value as the `href` attribute with the `download` attribute.

#### Expected Response — 400 Bad Request (Invalid File Format)

```json
{
  "status": "INVALID_FILE_FORMAT",
  "errorDetail": "Unsupported MIME type: application/msword"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | `"INVALID_FILE_FORMAT"` |
| `errorDetail` | string | Human-readable description of the file format error. |

**Frontend behaviour:** Display the message "The uploaded file is not a valid Excel or CSV file." in an error alert div.

#### Expected Response — 400 Bad Request (Column Name Mismatch)

```json
{
  "status": "COLUMN_NAME_MISMATCH",
  "unrecognizedColumns": ["invoice id", "client name"]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | `"COLUMN_NAME_MISMATCH"` |
| `unrecognizedColumns` | string[] | List of unrecognized column names found in the header row. |

**Frontend behaviour:** Display the message "Unrecognized column names: {comma-separated list}" in an error alert div.

#### Expected Response — 500 Internal Server Error

```json
{
  "status": "INTERNAL_ERROR",
  "errorDetail": "Unexpected error during Excel processing"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | `"INTERNAL_ERROR"` |
| `errorDetail` | string | Technical description of the failure. |

**Frontend behaviour:** Display the message "An unexpected error occurred during processing." in an error alert div.

## Notes

- Authentication is absent for the PoC phase. This endpoint is unauthenticated and should be protected in a future work item.
- The frontend performs client-side MIME type validation before upload. The backend performs server-side MIME type validation as mandated by S-007.
- The upload button is disabled during processing (loading state) to prevent duplicate submissions.
- The `returnExcelDownloadLink` is only rendered when `rowsFailed > 0`.
