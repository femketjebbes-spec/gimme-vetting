# Delegation Plan: Wi-003 — Per-Row Mandatory Field Validation (Gerard Phase)

## Architecture Constraints

- Reference: API contract at `docs/api-contract-wi-002.md`
- Current response (Section 5.1) has `rowsFailed` as an integer count only
- Wi-003 requires per-row `missingFields` details so failing rows can be flagged accurately
- Mandatory fields (D-010): `invoiceNumber`, `debtorName`, `address`, `bankAccountNumber`, `phoneNumber`
- Whitespace-only values treated as empty (D-022)
- Null values treated as empty (D-023)
- No authentication changes (D-026)
- No file size limit changes (D-027)

## Subtasks

### Subtask 1: Review and Update API Contract for Wi-003
- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: `docs/wi-003-delegation-gerard.md` (this delegation plan), `docs/api-contract-wi-002.md`, `re-workspace/work-items/wi-003-per-row-mandatory-field-validation.md`
- **Output Artefact**: Updated API contract at `docs/api-contract-wi-003.md`
- **Constraints**: 
  - The contract response for successful upload must include per-row `missingFields` arrays for failing rows
  - The response must NOT change for error responses (400 column mismatch, 400 invalid file format, 500 internal error)
  - Whitespace-only values must be documented as equivalent to empty
  - The version must be incremented to 3.0.0
- **Security Considerations**: Per-row error detail must not expose server internals (stack traces, file paths). Only field names are reported.

### Subtask 2: Submit for Alignment Review
- **Assigned Agent**: Gerard (API-Agent)
- **Input Artefact**: Updated `docs/api-contract-wi-003.md`
- **Output Artefact**: Alignment review request at `docs/alignment-review-request.md`
- **Constraints**: Submit with `pipelineStage: "gerard contract review for wi-003"` and list the updated contract as the produced artefact

## Expected API Contract Change

Current response (Section 5.1 — ExcelUploadResponse):
```json
{
  "processingStatus": "COMPLETED",
  "totalRowsProcessed": 10,
  "rowsPassed": 8,
  "rowsFailed": 2,
  "returnExcelDownloadLink": "/api/v1/intake/excel/download/return-abc123.xlsx"
}
```

New response must include per-row failure details:
```json
{
  "processingStatus": "COMPLETED",
  "totalRowsProcessed": 10,
  "rowsPassed": 8,
  "rowsFailed": 2,
  "returnExcelDownloadLink": "/api/v1/intake/excel/download/return-abc123.xlsx",
  "failingRows": [
    {
      "rowIndex": 3,
      "missingFields": ["debtorName", "address"]
    },
    {
      "rowIndex": 7,
      "missingFields": ["bankAccountNumber"]
    }
  ]
}
```

## Completion Criteria

The Gerard phase is complete when:
1. `docs/api-contract-wi-003.md` exists with updated response schema
2. Alignment review request submitted at `docs/alignment-review-request.md`
