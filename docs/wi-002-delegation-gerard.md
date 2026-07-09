# Delegation Plan: WI-002 Excel File Upload and Parsing — Gerard Phase

## Architecture Constraints

### D-024: Strict Column Name Matching
Excel upload endpoint accepts exactly the column names from the requirements specification (case-insensitive): "invoice number", "debtor name", "address", "phone number", "bank account number". No aliases or alternative column name variants are accepted. Files with unrecognized column names shall be rejected with a structured error response.

### D-025: Format and Header Support
WI-002 supports .xlsx and .csv file formats. Header row is optional. When no header row is present, column order determines field mapping: column 0 = invoiceNumber, column 1 = debtorName, column 2 = address, column 3 = phoneNumber, column 4 = bankAccountNumber.

### D-026: No Authentication for MVP
WI-002 endpoint has no authentication for MVP. The endpoint must include a Javadoc note stating: "NOTE: Authentication is absent for the PoC phase. This endpoint is unauthenticated and should be protected in a future work item."

### D-027: No File Size Limit for MVP
No file size limit is enforced for MVP. The architect recommends designing a size boundary but defers enforcement to MVP+1.

### D-028: Synchronous Processing
The endpoint processes the file synchronously. The client uploads, the server parses, validates, and returns the result in the same HTTP response cycle.

### D-029: Apache POI Library
Apache POI is the mandated Excel parsing library for consistency across WI-002 through WI-004. The latest patched version must be used to mitigate known XML external entity vulnerabilities. XML entity expansion must be disabled in parser configuration.

### S-007: MIME Type Validation
The upload endpoint must validate the uploaded file type server-side. MIME type checking must prevent upload of executable or malicious files disguised as Excel files.

### S-008: Column Name Allowlist
Column names must be validated against a fixed allowlist to prevent unexpected field mapping. Unknown column names trigger a structured rejection.

## Subtasks

### Subtask 1: API Contract for Excel Upload Endpoint

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: `re-workspace/work-items/wi-002-excel-file-upload-and-parsing.md` (work item definition), `re-workspace/work-items/wi-003-per-row-mandatory-field-validation.md` (validation output contract), `re-workspace/work-items/wi-004-return-excel-generation.md` (return Excel output contract), `agent-definitions/architecture-decisions.md` (architectural decisions D-024 through D-029, S-007, S-008)
- **Output Artefact**: `docs/api-contract-wi-002.md` — versioned API contract for the Excel upload and batch processing pipeline
- **Constraints**: 
  - The contract must define POST `/api/v1/intake/excel` as the upload endpoint
  - Request Content-Type: `multipart/form-data` with a single file field named `file`
  - Supported file types: `.xlsx` (application/vnd.openxmlformats-officedocument.spreadsheetml.sheet) and `.csv` (text/csv)
  - Response on success: HTTP 200 with a JSON body containing `processingStatus`, `totalRowsProcessed`, `rowsPassed`, `rowsFailed`, and `returnExcelDownloadLink`
  - Response on file format error: HTTP 400 with structured error identifying the validation failure
  - Response on column name mismatch: HTTP 400 with a structured error listing the unrecognized column names
  - The contract must reference the shared domain model: parsed rows contain `rowIndex`, `invoiceNumber`, `debtorName`, `address`, `phoneNumber`, `bankAccountNumber`, `parseErrors`
  - The contract must define the return Excel download response format
  - The contract must define the "Issue" column format for the return Excel: `MISSING_FIELDS: field1, field2` for mandatory field failures, `MISSING_POC` for PoC verification failures
  - No authentication requirement in the contract (per D-026), but a Javadoc note must be included
  - The contract must include a Security Requirements section covering D-029 (Apache POI security), S-007 (MIME type validation), and S-008 (column name allowlist)
  - The contract must reference architecture decisions D-024 through D-029 and security requirements S-007, S-008
- **Security Considerations**: 
  - MIME type validation must be enforced server-side, not client-side
  - Apache POI must be configured to disable XML entity expansion to prevent XXE attacks
  - The file field must be validated to prevent path traversal in the uploaded filename
  - The response must not expose the server-side temporary file path in the download link

### Subtask 2: API Contract Version Numbering

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: Existing contract `docs/api-contract.md` (version 1.0.0 for WI-001)
- **Output Artefact**: Version number `2.0.0` for `docs/api-contract-wi-002.md`
- **Constraints**: 
  - New work item with a new endpoint and different request/response patterns justifies a new major version (2.0.0)
  - The contract versioning table must document the initial version
- **Security Considerations**: None specific to versioning.

### Subtask 3: Contract Submission to Alignment Agent

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: `docs/api-contract-wi-002.md` (produced in Subtask 1)
- **Output Artefact**: Updated `docs/alignment-review-request.md` with Gerard's WI-002 contract submission
- **Constraints**: 
  - Gerard must submit the contract for Alignment Agent review using the standard JSON format defined in the Alignment Agent specification
  - The self-certification must confirm compliance with architectural decisions D-024 through D-029 and security requirements S-007, S-008
  - Gerard must not proceed to backend delegation until the Alignment Agent approves
- **Security Considerations**: Alignment Agent review ensures the contract does not omit security requirements for the upload endpoint.

## Gerard Phase Completion Criteria

Gerard phase is considered complete when:
1. `docs/api-contract-wi-002.md` is produced and reviewed
2. `docs/alignment-review-request.md` contains Alignment Agent approval for Gerard's WI-002 contract work
3. The Alignment Agent sets `greenlightForNextAgent` to `true`

## Parallel Phase Activation

Upon Alignment Agent approval, Archibald will produce a separate parallel delegation plan at `docs/wi-002-delegation-parallel.md` for Femke (Frontend) and Naut (Backend), both consuming the same `docs/api-contract-wi-002.md` file.

## Dependency Graph

```
Gerard Subtask 1 (API Contract) ──► Gerard Subtask 3 (Alignment Review) ──► Archibald reads Alignment decision ──► Parallel Phase (Femke + Naut)
```

## References

| Reference | Source |
|-----------|--------|
| WI-002 Definition | [`re-workspace/work-items/wi-002-excel-file-upload-and-parsing.md`](re-workspace/work-items/wi-002-excel-file-upload-and-parsing.md) |
| WI-003 Definition | [`re-workspace/work-items/wi-003-per-row-mandatory-field-validation.md`](re-workspace/work-items/wi-003-per-row-mandatory-field-validation.md) |
| WI-004 Definition | [`re-workspace/work-items/wi-004-return-excel-generation.md`](re-workspace/work-items/wi-004-return-excel-generation.md) |
| Architecture Decisions | [`agent-definitions/architecture-decisions.md`](agent-definitions/architecture-decisions.md) |
| Parent Requirements | [`re-workspace/requirements-spec.md`](re-workspace/requirements-spec.md) — RQ-006, RQ-007, RQ-008 |
