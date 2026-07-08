# Parallel Delegation Plan: BR-001 MIME-Type-Based File Validation Fix (WI-008)

## Architecture Constraints

- [D-BR001](agent-definitions/architecture-decisions.md): Content-based file format detection replaces MIME-type-only validation. Magic byte inspection (ZIP signature `50 4B 03 04` for .xlsx, text detection for .csv) is the authoritative detection method.
- [D-007](agent-definitions/architecture-decisions.md): File upload security requires magic byte verification against actual file content.
- [D-005](agent-definitions/architecture-decisions.md): No authentication, no file size limit for MVP.
- [D-009](agent-definitions/architecture-decisions.md): Apache POI is the Excel parsing library.

## Shared Contract

`docs/api-contract-wi-002.md` (version 2.1.0)

## Subtasks

### Subtask 1: Backend Implementation — Content-Based File Detection

- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `docs/api-contract-wi-002.md` (version 2.1.0), `re-workspace/bug-reports/BR-001-mime-type-based-file-validation.md`
- **Output Artefact**: Modified `ExcelParsingService.java` with content-based detection, modified `ExcelIntakeController.java` with fallback flow, updated unit tests
- **Constraints**:

  The implementation MUST replace the current MIME-type-only validation in `ExcelIntakeController.isSupportedMimeType()` check (line 58-65) with a fallback-to-content approach:

  1. **Fast path** (current behavior preserved): If `mimeType` is exactly `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` or `text/csv`, proceed with parsing using the MIME type directly. No content inspection needed.

  2. **Fallback path**: If `mimeType` is `null`, empty, or unrecognized (e.g., `application/octet-stream`, `application/zip`), call the new `ExcelParsingService.detectFileType(InputStream)` method to inspect file content:
     - Read first 4 bytes of the file
     - If bytes match `50 4B 03 04` (ZIP local file header / "PK\x03\x04"), treat as XLSX
     - If not ZIP, read first line as text. If valid UTF-8/ASCII text, treat as CSV
     - If neither, return `FileType.UNKNOWN`

  3. **Rejection**: If `detectFileType()` returns `UNKNOWN`, reject with `INVALID_FILE_FORMAT` response including the specific detection failure reason (not generic message).

  **Implementation details:**
  - Create a `FileType` enum: `XLSX`, `CSV`, `UNKNOWN` in package `com.gimmevettingsolution.intake.service`
  - Add `FileType detectFileType(InputStream)` method to `ExcelParsingService`
  - Modify `ExcelIntakeController.uploadExcel()` to use fallback flow:
    - Lines 57-68 must be restructured to: check MIME type fast path → if not supported, call `detectFileType()` → proceed based on result
  - The `isCsv` boolean (currently line 68) must be determined from the result of MIME-type check OR content-based detection
  - Error messages for `INVALID_FILE_FORMAT` responses MUST indicate the actual reason (per BR-001 FR-BR001-03). Example: "File content is not a recognized Excel or CSV format" instead of "Unsupported MIME type: application/octet-stream"

  **Files to modify:**
  - `5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/service/ExcelParsingService.java`
  - `5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/ExcelIntakeController.java`

  **Test requirements:**
  - Add unit tests for `detectFileType()` covering: valid XLSX bytes, valid CSV text, UNKNOWN content, empty stream
  - Update `ExcelIntakeControllerTest` to test fallback flow when MIME type is `application/octet-stream` but file content is valid XLSX
  - Test that non-Excel files (PDF, JPG) are correctly rejected regardless of MIME type

- **Security Considerations**: Content-based detection is MORE secure than MIME-type-only validation. The magic byte check prevents upload of files with correct MIME type but malicious content. The ZIP signature check for XLSX prevents non-ZIP files from being accepted as Excel. CSV text detection prevents binary files from being misidentified. Path traversal protection (existing `isSafeFilename()` check) remains unchanged and applies before detection.

### Subtask 2: Frontend Review — No Changes Required

- **Assigned Agent**: Femke (Frontend Agent)
- **Input Artefact**: `docs/api-contract-wi-002.md` (version 2.1.0), `docs/wi-008-delegation-parallel.md`
- **Output Artefact**: Confirmation that no frontend changes are required, or updated `ExcelUpload.jsx` if needed
- **Constraints**:

  The BR-001 fix is entirely a backend implementation change. The API endpoint (`POST /api/v1/intake/excel`), request body schema (`multipart/form-data` with field `file`), and response schema remain **identical** in version 2.1.0 of the contract. The frontend does not need any code changes.

  Femke MUST:
  1. Review `docs/api-contract-wi-002.md` (version 2.1.0) to confirm no frontend-facing changes exist
  2. Confirm `ExcelUpload.jsx` requires no modifications
  3. Submit a review request to the Alignment Agent at `docs/alignment-review-request.md` indicating `agentName: "Femke"`, `artefactsProduced: []` (or `["confirmations/no-frontend-changes-needed.md"]`), `nextAgentInPipeline: null`

  **Exception:** If Femke discovers any frontend code that performs client-side MIME type validation that contradicts the new contract (e.g., rejecting files before upload), Femke MUST update the frontend to remove client-side MIME validation or add appropriate error handling. The contract states MIME type is "supplementary hint only" — client-side checks that block uploads based on MIME type would create a UX regression.

- **Security Considerations**: Frontend client-side validation is a UX convenience only (documented in BR-001 notes). Removal of client-side MIME validation does not introduce security risk because the backend performs authoritative server-side content-based detection.

## Parallel Phase Completion Criteria

The parallel phase is considered complete when both Femke and Naut have:
1. Completed their respective subtasks
2. Submitted alignment review requests to the Alignment Agent (even if Femke's request confirms no changes)
3. Received Alignment Agent approval

## Notes

- This fix addresses BR-001: users on browsers/OSes that report unrecognized MIME types (e.g., `application/octet-stream`, `application/zip`) can now upload valid `.xlsx` files.
- Frontend code (`ExcelUpload.jsx`) requires no changes per the delegation plan because the API contract endpoint and response schema are unchanged.
- The `FileType` enum and `detectFileType()` method are new backend additions that do not affect the API surface.
