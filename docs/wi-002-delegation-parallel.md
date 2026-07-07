# Parallel Delegation Plan: WI-002 Excel File Upload and Parsing

## Architecture Constraints

### D-024: Strict Column Name Matching
Only the five allowlisted column names are accepted (case-insensitive): "invoice number", "debtor name", "address", "phone number", "bank account number". Unknown column names trigger a 400 error.

### D-025: Format and Header Support
Supports .xlsx and .csv formats. Header row is optional. Column position mapping applies when no header is present: column 0 = invoiceNumber, column 1 = debtorName, column 2 = address, column 3 = phoneNumber, column 4 = bankAccountNumber.

### D-026: No Authentication for MVP
No authentication on the upload endpoint. Both frontend and backend must include Javadoc/JSDoc notes stating authentication is absent for the PoC phase and should be protected in a future work item.

### D-027: No File Size Limit for MVP
No file size limit enforced.

### D-028: Synchronous Processing
Client uploads, server processes all rows through parsing and returns result in the same HTTP response cycle.

### D-029: Apache POI Library
Backend must use Apache POI for Excel parsing and generation. Latest patched version required. XML entity expansion must be disabled.

### S-007: MIME Type Validation
Server-side MIME type validation required. Prevent upload of executable or malicious files disguised as Excel.

### S-008: Column Name Allowlist
Column names validated against fixed allowlist. Unknown column names rejected with structured error.

## Shared Contract
`docs/api-contract-wi-002.md`

Both agents consume the SAME versioned API contract file. Femke must implement the frontend to match the contract exactly. Naut must implement the backend to match the contract exactly.

## Subtasks

### Subtask 1: Frontend Implementation
- **Assigned Agent**: Femke (Frontend Agent)
- **Input Artefact**: `docs/api-contract-wi-002.md`
- **Output Artefact**: Frontend code in `4-frontend/src/frontend/`
- **Constraints**: 
  - Frontend must implement an Excel upload UI component with:
    - File picker accepting .xlsx and .csv files
    - MIME type validation before upload (client-side feedback)
    - Upload progress indication (optional for MVP)
    - Display of processing results: rows processed, rows passed, rows failed
    - Download link for the return Excel file
  - All API calls must target `POST /api/v1/intake/excel` with `multipart/form-data`
  - The upload request must use a file field named `file`
  - Error handling must display structured errors for: invalid file format (400), column name mismatch (400)
  - Include JSDoc note about missing authentication
- **Security Considerations**: 
  - Client-side MIME type validation is a UX convenience only — the backend must also validate (S-007)
  - Do not trust client-side validation for security decisions
  - File preview/preview thumbnail is out of scope for MVP

### Subtask 2: Backend Implementation
- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `docs/api-contract-wi-002.md`
- **Output Artefact**: Java backend source code in `5-backend/business-service/src/main/java/com/gimmevettingsolution/`
- **Constraints**: 
  - Implement `POST /api/v1/intake/excel` endpoint in the business service
  - Use Apache POI (D-029) for .xlsx and .csv parsing
  - Implement header detection: if header row present, match column names case-insensitively against allowlist (D-024, S-008)
  - Implement column position fallback mapping when no header row present (D-025)
  - Skip empty rows during parsing
  - Rows with fewer columns than expected → parsed with null/empty for missing fields
  - Malformed files → reject with 400 Bad Request
  - Unknown column names → reject with 400, structured error listing unrecognized names
  - Success response: HTTP 200 with JSON body containing processingStatus, totalRowsProcessed, rowsPassed, rowsFailed, returnExcelDownloadLink
  - Server-side MIME type validation (S-007)
  - Include Javadoc note about missing authentication (D-026)
  - Do NOT implement authentication middleware (D-026)
  - Do NOT implement file size enforcement (D-027)
  - Do NOT implement business rule checks (RQ-002, RQ-003) — out of scope for WI-002
  - Do NOT modify existing WI-001 code unless required for integration
- **Security Considerations**: 
  - Apache POI must use latest patched version with XML entity expansion disabled
  - Server-side MIME type validation must not rely on client-provided Content-Type
  - Uploaded filename must be sanitized to prevent path traversal
  - Temporary return Excel files must be stored in a secure temporary directory with automatic cleanup

## Parallel Phase Completion Criteria

The parallel phase is considered complete when both Femke and Naut have:
1. Implemented their respective subtasks
2. Written automated tests achieving minimum coverage thresholds (Femke: 80% for UI components; Naut: 80% for parsing and validation logic)
3. Submitted structured JSON review requests to the Alignment Agent via `docs/alignment-review-request.md`

## Parallel Phase Dependencies

Both agents work from the SAME contract file (`docs/api-contract-wi-002.md`). This contract was produced by Gerard and approved by the Alignment Agent (review cycle 1, status: APPROVED).

The contract was reviewed against:
- RQ-006 (Excel Batch Intake)
- RQ-007 (Mandatory Field Validation — output contract alignment)
- RQ-008 (Return Excel — output contract alignment)
- Architecture decisions D-024 through D-029
- Security requirements S-007 through S-011

## References

| Reference | Source |
|-----------|--------|
| Shared Contract | `docs/api-contract-wi-002.md` |
| WI-002 Definition | `re-workspace/work-items/wi-002-excel-file-upload-and-parsing.md` |
| WI-003 Definition | `re-workspace/work-items/wi-003-per-row-mandatory-field-validation.md` |
| WI-004 Definition | `re-workspace/work-items/wi-004-return-excel-generation.md` |
| Architecture Decisions | `agent-definitions/architecture-decisions.md` |
| Alignment Decision | `docs/alignment-review-request.md` |
