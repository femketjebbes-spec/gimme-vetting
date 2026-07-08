# Delegation Plan: WI-007 — Download Template Excel Sheet (Gerard Phase)

## Architecture Constraints

- Reference: `re-workspace/work-items/MVP-1 touchups/wi-007-download-template-excel.md` (work item specification)
- FR-001: Template download endpoint at `GET /api/v1/intake/excel/template`
- FR-002: Template file is a valid .xlsx with 5 column headers matching the validation allowlist
- FR-003: Frontend download button (handled in parallel phase)
- NFR-001: Response time < 500ms, file size < 100KB
- NFR-002: Template generation logic in dedicated service method or existing `ExcelParsingService`
- Column headers must be defined as constants to prevent drift from validation allowlist
- D-020 / D-026: No authentication for MVP
- Existing `ExcelParsingService.ALLOWED_COLUMN_NAMES` at `5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/service/ExcelParsingService.java:28`
- Existing `ExcelParsingService.generateReturnXlsx()` at `5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/service/ExcelParsingService.java:516` provides a reusable pattern
- Existing `ExcelIntakeController` at `5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/ExcelIntakeController.java` provides the controller pattern
- Resolved: Template is empty (no example data), headers only, XLSX format only, no data validation rules, no formatting beyond headers

## Subtasks

### Subtask 1: Define API Contract for Template Download Endpoint

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: `docs/wi-007-delegation-gerard.md` (this delegation plan), `re-workspace/work-items/MVP-1 touchups/wi-007-download-template-excel.md`, `agent-definitions/architecture-decisions.md`
- **Output Artefact**: API contract at `docs/api-contract-wi-007.md`
- **Constraints**:
  - The contract must specify `GET /api/v1/intake/excel/template` endpoint
  - Response: HTTP 200 OK with the template file as the response body
  - Response headers must include `Content-Disposition: attachment; filename="invoice-intake-template.xlsx"`
  - Response headers must include `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
  - The template file must contain exactly 5 columns with headers: `invoice number`, `debtor name`, `address`, `phone number`, `bank account number`
  - The template must contain at least one empty data row as a visual guide
  - The template must be a valid Apache POI-generated XLSX file
  - No authentication required (MVP)
  - The contract must specify that the column headers are defined as constants in the backend code to prevent drift from the validation allowlist
  - The contract must specify that the template generation logic resides in a dedicated service method (either a new method in `ExcelParsingService` or a dedicated `TemplateService`)
- **Security Considerations**: No authentication required (MVP). No file injection risk since the template is a static resource generated server-side. The response must not include any server-internal path information.

### Subtask 2: Submit for Alignment Review

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: `docs/api-contract-wi-007.md`
- **Output Artefact**: Alignment review request at `docs/alignment-review-request.md`
- **Constraints**: Submit with `pipelineStage: "gerard contract review for wi-007"` and list the contract as the produced artefact

## Expected API Contract Structure

The contract at `docs/api-contract-wi-007.md` should follow the format of `docs/api-contract-wi-005.md` with these sections:

```
# API Contract: Wi-007 — Download Template Excel Sheet

Version: 7.0.0
Base Path: /api/v1

## GET /intake/excel/template

### Description

Download a pre-formatted Excel template file for invoice intake. The template contains
the correct column headers so users can fill in their invoice data in the expected
format before uploading via the Excel upload endpoint (WI-002).

### Request

**Method**: `GET`
**Path**: `/api/v1/intake/excel/template`
**Authentication**: None (MVP, per D-020)

### Response — 200 OK (Template Download)

**Content-Type**: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
**Content-Disposition**: `attachment; filename="invoice-intake-template.xlsx"`

Binary response: a valid .xlsx Excel file with the following structure:

| Row | Column A | Column B | Column C | Column D | Column E |
|-----|----------|----------|----------|----------|----------|
| 0 (header) | invoice number | debtor name | address | phone number | bank account number |
| 1 (empty) | | | | | |

**Template file constraints:**
- Valid Apache POI-generated XLSX file
- Column headers in row 0 (first row), columns A through E (indices 0-4)
- Headers are case-sensitive: `invoice number`, `debtor name`, `address`, `phone number`, `bank account number`
- At least one empty data row provided as visual guide
- No data validation rules or dropdown lists
- No formatting beyond standard headers
- File size < 100KB

### Response — 500 Internal Server Error

**Content-Type**: `application/json`

```json
{
  "status": "ERROR",
  "errorDetail": "Template generation failed"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | `"ERROR"` |
| `errorDetail` | string | Human-readable error description. |
```
