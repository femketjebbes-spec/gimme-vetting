# Parallel Delegation Plan: WI-001 PoC Existence Verification

## Architecture Constraints

### D-001: Case-Insensitive Filename Matching
Filename matching performs full-string comparison after lowercase normalisation of both the PoC filename and the invoice number. No substring matching, pattern matching, or fuzzy matching is permitted.

### D-002: Multiple PoC Files for One Invoice
Finding at least one matching filename is sufficient. No deduplication or duplicate rejection logic is required.

### D-003: Configurable PoC Store Location
The PoC store location is a configurable path or storage bucket. No assumption about shared filesystem versus object storage.

### S-005: Path Traversal Protection
The PoC filename must be sanitized against path traversal.

### S-006: Error Response Security
Error responses must not expose server internals.

## Shared Contract

`docs/api-contract.md` (v1.0.0)

Both agents consume the SAME versioned API contract file. Femke must implement the frontend to match the contract exactly. Naut must implement the backend to match the contract exactly.

## Subtasks

### Subtask 1: Frontend Implementation

- **Assigned Agent**: Femke (Frontend Agent)
- **Input Artefact**: `docs/api-contract.md` (v1.0.0)
- **Output Artefact**: Frontend code in `4-frontend/src/frontend/`
- **Constraints**: 
  - Frontend must implement an invoice submission form with fields: invoiceNumber, debtorName, address (street, postalCode, city, country), bankAccountNumber, phoneNumber
  - All fields are mandatory — validation on client side before submission
  - On success (200 POC_VERIFIED): display confirmation message
  - On PoC mismatch (400 MISSING_POC): display error message with the invoice number
  - On validation error (400): display structured error listing missing/invalid fields
  - Include JSDoc note about missing authentication
- **Security Considerations**: Client-side validation is UX only — the backend validates all fields server-side.

### Subtask 2: Backend Implementation

- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `docs/api-contract.md` (v1.0.0)
- **Output Artefact**: Java backend source code in `5-backend/business-service/src/main/java/com/gimmevettingsolution/`
- **Constraints**: 
  - Implement POST `/api/v1/intake` endpoint in the business service
  - Request body: JSON with invoiceNumber, debtorName, address, bankAccountNumber, phoneNumber
  - Validate invoiceNumber against pattern `^[A-Za-z0-9\\-_.]+$`
  - Validate address fields: street, postalCode, city, country (ISO 3166-1 alpha-2)
  - Call `PoCStoreService.hasMatchingPoC(invoiceNumber)` for PoC existence verification
  - Response on PoC match: HTTP 200 with `{"status": "POC_VERIFIED", "invoiceNumber": "INV-2026-0042"}`
  - Response on PoC mismatch: HTTP 400 with `{"status": "MISSING_POC", "errorDetail": "No PoC linked to invoice INV-2026-0042"}`
  - Response on validation error: HTTP 400 with structured error listing missing/invalid fields
  - PoC store path is configurable via application.yml
  - Include Javadoc note about missing authentication (D-026)
- **Security Considerations**: 
  - Invoice number sanitization against path traversal (S-005)
  - Error responses must not expose server internals (S-006)
  - PoC store path must not be exposed in error messages

## Parallel Phase Completion Criteria

The parallel phase is considered complete when both Femke and Naut have submitted their respective alignment review requests to the Alignment Agent.

## Parallel Phase Dependencies

Both agents work from the SAME contract file (`docs/api-contract.md`). This contract was produced by Gerard and approved by the Alignment Agent.

The contract was reviewed against:
- RQ-001 (PoC Existence Verification)
- Architecture decisions D-001, D-002, D-003
- Security requirements S-005, S-006
