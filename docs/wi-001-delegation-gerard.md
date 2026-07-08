# Delegation Plan: WI-001 PoC Existence Verification — Gerard Phase

## Architecture Constraints

### D-001: Case-Insensitive Filename Matching
Filename matching performs full-string comparison after lowercase normalisation of both the PoC filename and the invoice number. No substring matching, pattern matching, or fuzzy matching is permitted.

### D-002: Multiple PoC Files for One Invoice
Finding at least one matching filename is sufficient to pass the PoC existence gate. No deduplication or duplicate rejection logic is required.

### D-003: Configurable PoC Store Location
The PoC store location is a configurable path or storage bucket. No architectural assumption about shared filesystem versus object storage.

### S-005: Path Traversal Protection
The PoC filename must be sanitized against path traversal. The endpoint must not accept a filename that resolves to a path outside the PoC store directory.

### S-006: Error Response Security
Error responses must not expose server internals (stack traces, file paths, directory structures).

## Subtasks

### Subtask 1: API Contract for PoC Existence Verification

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: `re-workspace/work-items/wi-001-poc-existence-verification.md` (work item definition), `agent-definitions/architecture-decisions.md` (architecture decisions D-001, D-002, D-003, S-005, S-006)
- **Output Artefact**: `docs/api-contract.md` — versioned API contract for the PoC existence verification endpoint
- **Constraints**: 
  - The contract must define POST `/api/v1/intake` as the single-invoice submission endpoint
  - Request Content-Type: `application/json`
  - Request body must include: invoiceNumber (mandatory, pattern: alphanumeric + hyphens + underscores + dots), debtorName (mandatory), address (mandatory, structured object with street/postalCode/city/country), bankAccountNumber (mandatory), phoneNumber (mandatory)
  - Response on PoC match: HTTP 200 with `{"status": "POC_VERIFIED", "invoiceNumber": "INV-2026-0042"}`
  - Response on PoC mismatch: HTTP 400 with `{"status": "MISSING_POC", "errorDetail": "No PoC linked to invoice INV-2026-0042"}`
  - Response on validation error: HTTP 400 with structured error listing missing/invalid fields
  - No authentication in the contract (per D-026), but a Javadoc note must be included
- **Security Considerations**: 
  - Path traversal protection for filenames (S-005)
  - Error responses must not expose server internals (S-006)
  - Input validation: invoiceNumber must match pattern `^[A-Za-z0-9\\-_.]+$` to prevent injection

### Subtask 2: API Contract Version Numbering

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: None (first contract for the project)
- **Output Artefact**: Version number `1.0.0` for `docs/api-contract.md`
- **Constraints**: 
  - This is the initial contract version for the PoC phase
  - Version follows semantic versioning: MAJOR.MINOR.PATCH
- **Security Considerations**: None specific to versioning.

### Subtask 3: Contract Submission to Alignment Agent

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: `docs/api-contract.md` (produced in Subtask 1)
- **Output Artefact**: Updated `docs/alignment-review-request.md` with Gerard's WI-001 contract submission
- **Constraints**: 
  - Gerard must submit the contract for Alignment Agent review using the standard JSON format defined in the Alignment Agent specification
  - The self-certification must confirm compliance with architectural decisions D-001, D-002, D-003 and security requirements S-005, S-006
  - Gerard must not proceed to backend delegation until the Alignment Agent approves
- **Security Considerations**: Alignment Agent review ensures the contract includes all security requirements for the intake endpoint.

## Gerard Phase Completion Criteria

Gerard phase is considered complete when:
1. `docs/api-contract.md` is produced and reviewed
2. `docs/alignment-review-request.md` contains Alignment Agent approval for Gerard's WI-001 contract work
3. The Alignment Agent sets `greenlightForNextAgent` to `true`

## Parallel Phase Activation

Upon Alignment Agent approval, Archibald will produce a separate parallel delegation plan for Femke (Frontend) and Naut (Backend), both consuming the same `docs/api-contract.md` file.

## Dependency Graph

```
Gerard Subtask 1 (API Contract) ──► Gerard Subtask 3 (Alignment Review) ──► Archibald reads Alignment decision ──► Parallel Phase (Femke + Naut)
```

## References

| Reference | Source |
|-----------|--------|
| WI-001 Definition | `re-workspace/work-items/wi-001-poc-existence-verification.md` |
| Architecture Decisions | `agent-definitions/architecture-decisions.md` |
| Requirements Spec | `re-workspace/requirements-spec.md` (RQ-001) |
