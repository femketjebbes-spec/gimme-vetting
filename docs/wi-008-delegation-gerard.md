# Delegation Plan: BR-001 MIME-Type-Based File Validation (WI-008)

## Architecture Constraints

- [D-BR001](agent-definitions/architecture-decisions.md): BR-001 replaces MIME-type-only file validation with content-based file format detection. Magic byte inspection (ZIP signature `50 4B 03 04` for .xlsx, text detection for .csv) is the authoritative detection method. MIME-type validation is retained as a fast path for well-behaved browsers but falls back to content-based detection when the MIME type is missing, null, or unrecognized.
- [D-007](agent-definitions/architecture-decisions.md): File upload security requires magic byte verification against actual file content to prevent upload of executable files disguised as Excel files.
- [D-005](agent-definitions/architecture-decisions.md): WI-002 endpoint has no authentication for MVP. No file size limit is enforced for MVP.
- [D-009](agent-definitions/architecture-decisions.md): Apache POI is the recommended Excel parsing library.
- Existing API contract: [`docs/api-contract-wi-002.md`](docs/api-contract-wi-002.md) version 2.0.0.

## Subtasks

### Subtask 1: Update API Contract — Document Content-Based Detection

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: `docs/api-contract-wi-002.md` (version 2.0.0), `re-workspace/bug-reports/BR-001-mime-type-based-file-validation.md`, `docs/wi-008-delegation-gerard.md`
- **Output Artefact**: Updated `docs/api-contract-wi-002.md` (version 2.1.0), `docs/wi-008-contract-ready.md`
- **Constraints**:
  - Increment contract version to 2.1.0
  - Section 3.2 (Supported File Formats) MUST be updated to state that file format detection uses content-based magic byte inspection. MIME type is listed as a supplementary hint only.
  - Section 3.2 MUST document the detection precedence: (1) If MIME type is one of the supported types (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` or `text/csv`), use it directly. (2) If MIME type is missing, null, or unrecognized, inspect file content. (3) If content inspection yields .xlsx (ZIP signature) or CSV (text encoding), proceed. (4) If content inspection fails, reject with `INVALID_FILE_FORMAT`.
  - The contract MUST specify the magic byte constants: XLSX = first 4 bytes `50 4B 03 04` (ZIP local file header signature), CSV = valid UTF-8/ASCII text content.
  - Error messages for `INVALID_FILE_FORMAT` responses MUST indicate the actual detection reason (per BR-001 FR-BR001-03).
  - The endpoint path, method, request schema, response schema, and all other sections remain unchanged.
- **Security Considerations**: Content-based detection is more secure than MIME-type-only validation. The contract must mandate that the implementation uses magic byte inspection as the authoritative check, not just as a cosmetic change.

### Subtask 2: Update Architecture Decision File

- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: `agent-definitions/architecture-decisions.md` (already updated by Archibald)
- **Output Artefact**: No file modification by Gerard. Archibald has already documented the BR-001 decision. Gerard must verify the decision entry is present and correctly references this delegation plan.
- **Constraints**: Gerard reads the architecture decisions file and confirms the BR-001 decision entry exists with the correct content. If absent, Gerard flags it as a blocker.
- **Security Considerations**: N/A (verification only).

## Gerard Completion Criteria

Gerard is considered complete when:
1. The versioned API contract (`docs/api-contract-wi-002.md`) is updated to version 2.1.0 with content-based detection documented.
2. Gerard submits the JSON review request to the Alignment Agent at `docs/alignment-review-request.md`.
3. The Alignment Agent approves with `greenlightForNextAgent: true` and `nextAgentInPipeline` set to `Femke-Naut-parallel`.
4. Gerard produces `docs/wi-008-contract-ready.md` confirming the contract is ready for parallel implementation.

## Notes

- This work item (WI-008) addresses BR-001 which is a bug fix for the existing Excel upload endpoint (POST `/api/v1/intake/excel`).
- The API endpoint path, HTTP method, request body schema, and response schema do NOT change. Only the internal file format detection mechanism changes.
- Frontend code (`ExcelUpload.jsx`) does not require changes because the endpoint contract remains identical from the frontend perspective.
- The content-based detection logic (magic byte inspection) is an implementation detail within the backend service layer, not an API surface change.
