# Delegation Plan: Wi-003 — Per-Row Mandatory Field Validation (Naut Implementation)

## Architecture Constraints

- Reference API contract: `docs/api-contract-wi-003.md` (v3.0.0)
- Current Excel intake controller at `5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/ExcelIntakeController.java`
- Mandatory fields (D-010): `invoiceNumber`, `debtorName`, `address`, `bankAccountNumber`, `phoneNumber`
- Whitespace-only values treated as empty (D-022)
- Null values treated as empty (D-023)
- Apache POI for parsing (D-029)
- Synchronous processing (D-028)
- No authentication (D-026)
- No file size limit (D-027)
- Security requirement S-012: per-row error detail must only contain canonical field names, never server-internal identifiers

## Current State

The mandatory field validation logic exists inline in `ExcelIntakeController.java` (lines 82-125) but has three deficiencies:
1. No whitespace-only check — only checks `null` and `isEmpty()`
2. Duplicated logic (two identical loops: one for counting, one for collecting failing rows)
3. No per-row `missingFields` detail — response only has integer `rowsFailed` count

## Subtasks

### Subtask 1: Create MandatoryFieldValidationService
- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `docs/wi-003-delegation-naut.md` (this delegation plan), `docs/api-contract-wi-003.md`, `re-workspace/work-items/wi-003-per-row-mandatory-field-validation.md`
- **Output Artefact**: New service `5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/service/MandatoryFieldValidationService.java`
- **Implementation Details**:
  - Service method `ValidationResult validate(List<ExcelInvoiceRow> rows)` that returns:
    - `List<ExcelInvoiceRow> passingRows` — rows with all five fields non-empty and non-whitespace
    - `List<RowFailure> failingRows` — rows with per-row `rowIndex` and `missingFields` (array of canonical field names)
    - `int totalRowsProcessed`
    - `int rowsPassed`
    - `int rowsFailed`
  - Each field checked with: `value == null || value.trim().isEmpty()`
  - `RowFailure` is a simple DTO with `rowIndex` (int) and `missingFields` (List<String>)
  - `ValidationResult` is a simple DTO with the aggregate counts above
- **Constraints**:
  - Service must be a Spring `@Service`
  - Must handle null, empty, and whitespace-only consistently
  - `missingFields` array must use canonical field names only
  - Must include comprehensive unit tests (minimum 10 test cases)
- **Security Considerations**: `missingFields` must only contain canonical field names. No server-internal identifiers, stack traces, or file paths.

### Subtask 2: Update ExcelIntakeController to Use New Service
- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `docs/api-contract-wi-003.md`
- **Output Artefact**: Updated `ExcelIntakeController.java` — remove inline validation logic, inject `MandatoryFieldValidationService`, produce `failingRows` in response
- **Implementation Details**:
  - Inject `MandatoryFieldValidationService` into controller
  - Replace the two inline validation loops (lines 82-125) with a single call to the service
  - Update `ExcelUploadResponse` to include the new `failingRows` field per the v3.0.0 contract
  - May need to update `ExcelUploadResponse` DTO to add `failingRows` field
- **Constraints**:
  - Must not change endpoint path, method, or existing error response schemas
  - `failingRows` may be `null` or omitted when no rows failed (for backward compatibility)
  - Must not break existing tests — update as needed

### Subtask 3: Update ExcelUploadResponse DTO
- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `docs/api-contract-wi-003.md` Section 6.1
- **Output Artefact**: Updated `5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/dto/ExcelUploadResponse.java`
- **Implementation Details**:
  - Add `List<FailingRow> failingRows` field
  - Add `FailingRow` class (or inner class) with `rowIndex` (Integer) and `missingFields` (List<String>)
  - Use Lombok or manual getters/setters as consistent with existing codebase style
- **Constraints**: New field must be optional (no `@JsonProperty(required = true)`) to maintain backward compatibility

### Subtask 4: Write Unit Tests
- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `re-workspace/work-items/wi-003-per-row-mandatory-field-validation.md` (acceptance criteria scenarios)
- **Output Artefact**: Tests in `5-backend/business-service/src/test/java/com/gimmevettingsolution/intake/MandatoryFieldValidationServiceTest.java`
- **Test Cases Required** (minimum 10):
  1. All fields present — row passes
  2. Missing debtorName only — row fails with correct missingFields
  3. Multiple fields missing — all listed in missingFields
  4. Missing invoiceNumber — row fails
  5. Whitespace-only debtorName — treated as empty
  6. Null address — treated as empty
  7. All rows pass — failingRows is empty/null
  8. All rows fail — all rows in failingRows
  9. Mixed pass/fail — correct split
  10. Empty field with only spaces — treated as empty

### Subtask 5: Submit Alignment Review
- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: All modified and new production and test files
- **Output Artefact**: Alignment review request at `docs/alignment-review-request.md`
- **Constraints**: Submit with `pipelineStage: "parallel backend implementation wi-003"`

## Completion Criteria

The Naut phase is complete when:
1. `MandatoryFieldValidationService.java` exists and is tested
2. `ExcelIntakeController.java` uses the service (no inline validation)
3. `ExcelUploadResponse.java` includes `failingRows` field
4. `FailingRow.java` or inner class exists with `rowIndex` and `missingFields`
5. All Maven tests pass
6. Alignment review request submitted

## Testing Steps for Naut

After implementation:
1. `mvn test` from `5-backend/` — must pass
2. Verify existing tests still pass (no regression)
3. Manual test: upload Excel with missing fields, verify `failingRows` in response
