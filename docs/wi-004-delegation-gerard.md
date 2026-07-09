# Delegation Plan: WI-004 Return Excel Generation — Gerard Phase

## Architecture Constraints

### D-028: Synchronous Processing
The return Excel download is triggered by a synchronous HTTP request within the same upload response cycle.

### D-029: Apache POI Library
Apache POI is the mandated Excel generation library. Latest patched version required. XML entity expansion must be disabled.

### S-010: Temporary File Storage
Return Excel files are stored in a secure temporary directory with automatic cleanup.

### S-011: Download Link Security
The download link must not expose the server-side file path. A UUID or token-based link is required.

### S-012: Per-Row Error Detail
Per-row error detail must only contain canonical field names. Never expose server-internal identifiers.

## Subtasks

### Subtask 1: Review API Contract for Return Excel Output

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: `docs/api-contract-wi-003.md` (v3.0.0), `re-workspace/work-items/wi-004-return-excel-generation.md` (work item definition)
- **Output Artefact**: Updated `docs/api-contract-wi-003.md` (v3.0.0) with return Excel download section
- **Constraints**: 
  - The contract must document the return Excel download endpoint: GET `/api/v1/intake/excel/download/{filename}`
  - The download response must be `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` for .xlsx files
  - The return Excel must include columns: invoice number, debtor name, address, phone number, bank account number, Issue
  - The Issue column format: "MISSING_FIELDS: field1, field2" for mandatory field failures, "MISSING_POC" for PoC verification failures
  - No version change needed — the contract already defines the response structure via `returnExcelDownloadLink`
- **Security Considerations**: 
  - Download endpoint must validate filename against path traversal
  - Temporary files must have automatic cleanup

### Subtask 2: Contract Submission to Alignment Agent

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: `docs/api-contract-wi-003.md` (v3.0.0)
- **Output Artefact**: Updated `docs/alignment-review-request.md` with Gerard's WI-004 contract review
- **Constraints**: 
  - Gerard must submit the contract review for Alignment Agent review using the standard JSON format
  - The self-certification must confirm compliance with decisions D-028, D-029 and security requirements S-010 through S-012
  - Gerard must not proceed to backend delegation until the Alignment Agent approves
- **Security Considerations**: Alignment Agent review ensures the contract includes all security requirements for the return Excel endpoint.

## Gerard Phase Completion Criteria

Gerard phase is considered complete when:
1. `docs/api-contract-wi-003.md` is reviewed for WI-004 requirements
2. `docs/alignment-review-request.md` contains Alignment Agent approval for Gerard's WI-004 contract review
3. The Alignment Agent sets `greenlightForNextAgent` to `true`

## Parallel Phase Activation

Upon Alignment Agent approval, Archibald will produce a separate parallel delegation plan for Femke (Frontend) and Naut (Backend), both consuming the same `docs/api-contract-wi-003.md` file.

## Dependency Graph

```
Gerard Subtask 1 (Contract Review) ──► Gerard Subtask 2 (Alignment Review) ──► Archibald reads Alignment decision ──► Parallel Phase (Femke + Naut)
```

## References

| Reference | Source |
|-----------|--------|
| WI-004 Definition | `re-workspace/work-items/wi-004-return-excel-generation.md` |
| API Contract | `docs/api-contract-wi-003.md` |
| Requirements Spec | `re-workspace/requirements-spec.md` (RQ-008) |
