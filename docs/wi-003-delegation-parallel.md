# Parallel Delegation Plan: WI-003 Per-Row Mandatory Field Validation

## Architecture Constraints

### D-010: Mandatory Fields
The following fields are mandatory per row: invoiceNumber, debtorName, address, bankAccountNumber, phoneNumber. All provided by the upstream form system.

### D-022: Whitespace-Only Values
Whitespace-only values are treated as empty. A field with only spaces fails mandatory field validation.

### D-023: Null Values
Null values are treated as empty. A null field fails mandatory field validation.

### D-026: No Authentication for MVP
No authentication on the upload endpoint. Both frontend and backend must include Javadoc/JSDoc notes stating authentication is absent for the PoC phase.

### D-027: No File Size Limit for MVP
No file size limit enforced.

### D-028: Synchronous Processing
Client uploads, server processes all rows through parsing and validation, returns result in the same HTTP response cycle.

### D-029: Apache POI Library
Backend must use Apache POI for Excel parsing. Latest patched version required. XML entity expansion must be disabled.

### S-012: Per-Row Error Detail
Per-row error detail must only contain canonical field names. Never expose server-internal identifiers, stack traces, or file paths.

## Shared Contract

`docs/api-contract-wi-003.md` (v3.0.0)

Both agents consume the SAME versioned API contract file. Femke must implement the frontend to match the contract exactly. Naut must implement the backend to match the contract exactly.

## Subtasks

### Subtask 1: Frontend Implementation

- **Assigned Agent**: Femke (Frontend Agent)
- **Input Artefact**: `docs/api-contract-wi-003.md` (v3.0.0)
- **Output Artefact**: Frontend code in `4-frontend/src/frontend/`
- **Constraints**: 
  - Frontend must display per-row failure details from the `failingRows` array in the response
  - Each failing row must show: rowIndex, missingFields (list of field names)
  - Display summary counts: total rows processed, rows passed, rows failed
  - Error handling for: invalid file format (400), column name mismatch (400), internal error (500)
  - Include JSDoc note about missing authentication (D-026)
- **Security Considerations**: 
  - Display only the data provided by the backend API
  - Do not trust client-side data for security decisions
  - Do not expose raw server error messages to users

### Subtask 2: Backend Implementation

- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `docs/api-contract-wi-003.md` (v3.0.0)
- **Output Artefact**: Java backend source code in `5-backend/business-service/src/main/java/com/gimmevettingsolution/`
- **Constraints**: 
  - Implement `MandatoryFieldValidationService` with method `ValidationResult validate(List<ExcelInvoiceRow> rows)`
  - Each field checked with: `value == null || value.trim().isEmpty()`
  - `RowFailure` DTO with `rowIndex` (int) and `missingFields` (List<String>)
  - `ValidationResult` DTO with aggregate counts and failingRows list
  - Update `ExcelUploadResponse` to include `failingRows` field per the v3.0.0 contract
  - Update `ExcelIntakeController` to inject and use `MandatoryFieldValidationService`
  - Replace inline validation logic with service call
  - Must not change endpoint path, method, or existing error response schemas
  - Must not break existing tests — update as needed
  - Include comprehensive unit tests (minimum 10 test cases)
- **Security Considerations**: 
  - `missingFields` must only contain canonical field names (S-012)
  - No server-internal identifiers in error responses
  - Apache POI must use latest patched version with XML entity expansion disabled (D-029)

## Parallel Phase Completion Criteria

The parallel phase is considered complete when both Femke and Naut have submitted their respective alignment review requests to the Alignment Agent.

## Parallel Phase Dependencies

Both agents work from the SAME contract file (`docs/api-contract-wi-003.md`). This contract was produced by Gerard and approved by the Alignment Agent.

The contract was reviewed against:
- RQ-007 (Mandatory Field Validation)
- Architecture decisions D-010, D-022, D-023, D-026, D-027, D-028, D-029
- Security requirements S-007 through S-012
