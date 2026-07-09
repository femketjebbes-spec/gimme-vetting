# Parallel Delegation Plan: WI-004 Return Excel Generation

## Architecture Constraints

- Reference API contract: `docs/api-contract-wi-003.md` (v3.0.0)
- Reference work item: `re-workspace/work-items/wi-004-return-excel-generation.md`
- Parent requirement: RQ-008 (Return Excel with Missing Data)
- Apache POI for Excel generation (same library as parsing, D-029)
- Synchronous processing (D-028)
- Temporary file storage in server temp directory (S-010, S-011)
- No authentication (D-026)
- No file size limit (D-027)

## Current State

The return Excel generation code exists in `ExcelParsingService.generateReturnExcel()` and `buildIssue()`. The controller at `ExcelIntakeController.java` already calls it. Five gaps were identified:

1. Filename mismatch — Controller generates `return-<uuid>.xlsx` but `generateReturnExcel()` uses hardcoded `return-excel.xlsx`
2. Return value ignored — Controller does not use the `Path` returned from `generateReturnExcel()`
3. No format matching — Always generates `.xlsx`, but spec requires matching upload format
4. No PoC failure integration — Controller only passes mandatory field failures to return Excel
5. Issue format inconsistency — `buildIssue()` produces "MISSING_FIELDS:field1" (no space after colon) but spec says "MISSING_FIELDS: field1"

## Shared Contract

`docs/api-contract-wi-003.md` (v3.0.0)

## Subtasks

### Subtask 1: Backend Implementation — Return Excel Fixes

- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `docs/wi-004-delegation.md` (delegation plan), `docs/api-contract-wi-003.md`
- **Output Artefact**: Updated `ExcelParsingService.java` and `ExcelIntakeController.java`
- **Constraints**: 
  - Add overloaded `generateReturnExcel()` method with filename and isCsv parameters, keep existing 2-param method for backward compatibility
  - Fix filename generation to use UUID and correct extension
  - Implement format matching: `.csv` uploads produce `.csv` return files, `.xlsx` uploads produce `.xlsx` return files
  - Integrate PoC failure rows into return Excel with "MISSING_POC" issue
  - Fix issue format to "MISSING_FIELDS: field1, field2" (space after colon, comma-space between fields)
  - Add CSV export helper method `generateReturnCsv()`
- **Security Considerations**: Temporary return Excel files must be stored in a secure temporary directory. Filenames must be sanitized against path traversal.

### Subtask 2: Frontend Implementation

- **Assigned Agent**: Femke (Frontend Agent)
- **Input Artefact**: `docs/api-contract-wi-003.md` (v3.0.0)
- **Output Artefact**: Updated `ExcelUpload.jsx` component in `4-frontend/src/frontend/components/`
- **Constraints**: 
  - Display per-row failure details from `failingRows` array when available
  - Download link must use `returnExcelDownloadLink` from response
  - Show count of passing vs failing rows
  - Error handling for 400 (invalid format, column mismatch) and 500 (internal error)
- **Security Considerations**: Client-side validation is UX only. The backend enforces all security constraints.

## Parallel Phase Completion Criteria

The parallel phase is considered complete when both Femke and Naut have submitted their respective alignment review requests to the Alignment Agent.
