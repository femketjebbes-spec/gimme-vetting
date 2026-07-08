# Parallel Delegation Plan: WI-007 — Download Template Excel Sheet

## Architecture Constraints

- Reference: `re-workspace/work-items/MVP-1 touchups/wi-007-download-template-excel.md` (work item specification)
- D-020: No authentication for MVP
- D-026: No file size limit for MVP
- D-028: Template download endpoint is `GET /api/v1/intake/excel/template`
- D-029: Template generation uses Apache POI with constant-based column headers
- Template is empty (no example data), XLSX only, no data validation rules or formatting
- Column headers must match `ExcelParsingService.ALLOWED_COLUMN_NAMES` exactly

## Shared Contract

`docs/api-contract-wi-007.md`

## Subtasks

### Subtask 1: Frontend Implementation
- **Assigned Agent**: Femke (Frontend Agent)
- **Input Artefact**: `docs/api-contract-wi-007.md`
- **Output Artefact**: Frontend download button component in `4-frontend/src/frontend/`
- **Constraints**: 
  - Add a "Download Template" button near the existing Excel upload component
  - Button triggers a GET request to `/api/v1/intake/excel/template`
  - Browser initiates file download with filename `invoice-intake-template.xlsx`
  - Button is accessible and follows existing UI patterns
  - No authentication handling required (MVP, per D-020)
- **Security Considerations**: No authentication required (MVP). Response handling must not expose server error messages to the user.

### Subtask 2: Backend Implementation
- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `docs/api-contract-wi-007.md`
- **Output Artefact**: Java backend source code in `5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/`
- **Constraints**:
  - Add `GET /api/v1/intake/excel/template` endpoint to `ExcelIntakeController`
  - Generate template using Apache POI (`XSSFWorkbook`) with headers from `ExcelParsingService.ALLOWED_COLUMN_NAMES`
  - Response includes `Content-Disposition: attachment; filename="invoice-intake-template.xlsx"`
  - Response includes `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
  - Template contains exactly 5 column headers: `invoice number`, `debtor name`, `address`, `phone number`, `bank account number`
  - Template contains at least one empty data row
  - No example data, no validation rules, no formatting beyond standard headers
  - Response time target < 500ms, file size < 100KB
  - IOException → 500 Internal Server Error
- **Security Considerations**: No authentication required (MVP). Response must not include server-internal path information in error messages.

## Parallel Phase Completion Criteria

The parallel phase is considered complete when both Femke and Naut have submitted their respective alignment review requests to the Alignment Agent.
