# Delegation Plan: WI-004 — Return Excel Generation

## Architecture Constraints

- Reference work item: `re-workspace/work-items/wi-004-return-excel-generation.md`
- Reference API contract: `docs/api-contract-wi-003.md` (v3.0.0)
- Parent requirements: RQ-008 (Return Excel with Missing Data)
- Upstream dependencies: WI-001 (PoCStoreService), WI-002 (ExcelParsingService), WI-003 (MandatoryFieldValidationService)
- Apache POI for Excel generation (same library as parsing, D-029)
- Synchronous processing (D-028)
- Temporary file storage in server temp directory (S-010, S-011)

## Current State

The return Excel generation code exists in `ExcelParsingService.generateReturnExcel()` (lines 475-516) and `buildIssue()` (lines 521-556). The controller at `ExcelIntakeController.java` (lines 90-106) already calls it.

**However, five gaps prevent WI-004 completion:**

1. **Filename mismatch** — Controller generates `return-<uuid>.xlsx` at line 103 but `generateReturnExcel()` uses hardcoded `return-excel.xlsx`. The downloaded file would return 404.
2. **Return value ignored** — Controller doesn't use the `Path` returned from `generateReturnExcel()`.
3. **No format matching** — Always generates `.xlsx`, but spec requires matching upload format (`.csv` → `.csv`, `.xlsx` → `.xlsx`).
4. **No PoC failure integration** — Controller only passes mandatory field failures to return Excel. Rows that fail PoC verification should also be included with "MISSING_POC" issue.
5. **Issue format inconsistency** — `buildIssue()` produces "MISSING_FIELDS:field1" (no space after colon) but spec says "MISSING_FIELDS: field1, field2".

## CRITICAL: Backward Compatibility Requirement

The existing `generateReturnExcel(List<ExcelInvoiceRow>, Path)` method at line 475 has 3 test callers that only check file existence. Changing its signature WILL break compilation. The fix: **add overloaded methods with new parameters, keep the existing 2-parameter method as-is** (it delegates to the new implementation with sensible defaults).

```java
// EXISTING — must keep working for tests:
public Path generateReturnExcel(ExcelInvoiceRow[] failingRows, Path outputDir)

// NEW overload — used by controller:
public Path generateReturnExcel(ExcelInvoiceRow[] failingRows, Path outputDir, String filename, boolean isCsv)
```

**Verification:** The existing tests at lines 363-385 of `ExcelParsingServiceTest.java` only assert `assertNotNull(result)` and `assertTrue(Files.exists(result))`. They do NOT check the actual file content or issue format. The issue format change (`MISSING_FIELDS:field1` → `MISSING_FIELDS: field1`) will not break existing tests.

**Verification:** `createFailingRow` helper has two overloads (6-param and 7-param). Both test rows use the 7-param version. Unaffected by changes.

**Verification:** `createPartialXlsx` test data uses 2 columns ("invoice number", "debtor name") which ARE in the allowlist. Passes column validation correctly.

## Subtasks

### Subtask 1: Fix Filename Mismatch and Return Value Usage

- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: This delegation plan, `ExcelIntakeController.java` lines 90-106, `ExcelParsingService.java` lines 475-516
- **Output Artefact**: Updated `ExcelIntakeController.java`, overloaded `ExcelParsingService.generateReturnExcel()`
- **Changes to ExcelParsingService.java**:
  ```java
  // KEEP existing 2-param method (for backward compatibility with tests):
  public Path generateReturnExcel(ExcelInvoiceRow[] failingRows, Path outputDir) throws IOException {
      String defaultFilename = "return-excel.xlsx";
      return generateReturnExcel(failingRows, outputDir, defaultFilename, false);
  }
  
  // ADD new 4-param overload used by controller:
  public Path generateReturnExcel(ExcelInvoiceRow[] failingRows, Path outputDir, String filename, boolean isCsv) throws IOException {
      if (failingRows == null || failingRows.length == 0) {
          return null;
      }
      Path outputFile = outputDir.resolve(filename);
      String[] headers = {"invoice number", "debtor name", "address", "phone number", "bank account number", "Issue"};
      if (isCsv) {
          return generateReturnCsv(failingRows, outputFile, headers);
      } else {
          return generateReturnXlsx(failingRows, outputFile, headers);
      }
  }
  ```
- **Changes to ExcelIntakeController.java**:
  ```
  // BEFORE (buggy):
  String returnFilename = "return-" + UUID.randomUUID() + ".xlsx";
  excelParsingService.generateReturnExcel(failingRowsForExcel, uploadDir);
  downloadLink = "/api/v1/intake/excel/download/" + returnFilename;
  
  // AFTER (corrected):
  String returnFilename = "return-" + UUID.randomUUID() + (isCsv ? ".csv" : ".xlsx");
  excelParsingService.generateReturnExcel(failingRowsForExcel, uploadDir, returnFilename, isCsv);
  downloadLink = "/api/v1/intake/excel/download/" + returnFilename;
  ```

### Subtask 2: Add Format Matching (xlsx vs csv)

- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: This delegation plan, `ExcelParsingService.java` lines 475-556
- **Output Artefact**: Updated `ExcelParsingService` with `generateReturnXlsx()`, `generateReturnCsv()` helper methods, and `csvEscape()`
- **Changes**:
  Extract current XLSX logic into `generateReturnXlsx()`:
  ```java
  private Path generateReturnXlsx(ExcelInvoiceRow[] failingRows, Path outputFile, String[] headers) throws IOException {
      try (Workbook workbook = new XSSFWorkbook()) {
          Sheet sheet = workbook.createSheet("Failed Rows");
          Row headerRow = sheet.createRow(0);
          for (int i = 0; i < headers.length; i++) {
              headerRow.createCell(i).setCellValue(headers[i]);
          }
          int rowNum = 1;
          for (ExcelInvoiceRow row : failingRows) {
              Row dataRow = sheet.createRow(rowNum);
              dataRow.createCell(0).setCellValue(row.getInvoiceNumber() != null ? row.getInvoiceNumber() : "");
              dataRow.createCell(1).setCellValue(row.getDebtorName() != null ? row.getDebtorName() : "");
              dataRow.createCell(2).setCellValue(row.getAddress() != null ? row.getAddress() : "");
              dataRow.createCell(3).setCellValue(row.getPhoneNumber() != null ? row.getPhoneNumber() : "");
              dataRow.createCell(4).setCellValue(row.getBankAccountNumber() != null ? row.getBankAccountNumber() : "");
              dataRow.createCell(5).setCellValue(buildIssue(row));
              rowNum++;
          }
          try (FileOutputStream outputStream = new FileOutputStream(outputFile.toFile())) {
              workbook.write(outputStream);
          }
      }
      return outputFile;
  }
  ```
  
  Add CSV generation with proper escaping:
  ```java
  private Path generateReturnCsv(ExcelInvoiceRow[] failingRows, Path outputFile, String[] headers) throws IOException {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.toFile()))) {
          writer.append(String.join(",", headers));
          writer.newLine();
          for (ExcelInvoiceRow row : failingRows) {
              writer.append(formatCsvRow(row)).newLine();
          }
      }
      return outputFile;
  }
  
  private String formatCsvRow(ExcelInvoiceRow row) {
      String[] values = {
          csvEscape(row.getInvoiceNumber()),
          csvEscape(row.getDebtorName()),
          csvEscape(row.getAddress()),
          csvEscape(row.getPhoneNumber()),
          csvEscape(row.getBankAccountNumber()),
          csvEscape(buildIssue(row))
      };
      return String.join(",", values);
  }
  
  /**
   * RFC 4180 CSV escaping.
   * Values containing commas, double quotes, or newlines are enclosed in double quotes.
   * Double quotes within the value are escaped by doubling them.
   */
  private String csvEscape(String value) {
      if (value == null) return "";
      if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
          return "\"" + value.replace("\"", "\"\"") + "\"";
      }
      return value;
  }
  ```

### Subtask 3: Fix Issue Format Consistency

- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: This delegation plan, `ExcelParsingService.java` line 548
- **Output Artefact**: Updated `buildIssue()` method
- **Changes**:
  ```java
  // BEFORE (inconsistent format — no space after colon):
  if (hasMissingFields) {
      issues.add("MISSING_FIELDS:" + String.join(", ", missingFields));
  }
  
  // AFTER (consistent with spec — space after colon):
  if (hasMissingFields) {
      issues.add("MISSING_FIELDS: " + String.join(", ", missingFields));
  }
  ```
- **Verification:** Existing tests at `ExcelParsingServiceTest.java` lines 363-385 only check file existence, not content. No test assertion changes needed for existing tests.

### Subtask 4: Wire PoC Failures into Return Excel (Infrastructure Preparation)

- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: This delegation plan, `ExcelParsingService.java`
- **Output Artefact**: Overloaded `buildIssue()` method accepting explicit issue string
- **Changes**:
  ```java
  // Keep existing method (for mandatory field failures):
  String buildIssue(ExcelInvoiceRow row) { ... existing logic ... }
  
  // NEW overload for explicit issue string (for future PoC integration):
  String buildIssue(ExcelInvoiceRow row, String explicitIssue) {
      return explicitIssue;
  }
  ```
  
  **Constraint:** Do NOT modify `IntakeController.java` or `IntakeServiceImpl.java`. Only modify `ExcelIntakeController.java` and `ExcelParsingService.java`.

  **Note:** PoC failure rows in the return Excel will be added when PoC verification is wired into the controller pipeline (future work). The current implementation handles mandatory field failures correctly.

### Subtask 5: Update and Write Unit Tests

- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `re-workspace/work-items/wi-004-return-excel-generation.md` (acceptance criteria scenarios)
- **Output Artefact**: Updated existing tests, new test file
- **Test Cases Required**:

  **New tests** in `5-backend/business-service/src/test/java/com/gimmevettingsolution/intake/ExcelParsingServiceReturnExcelTest.java`:
  1. Return Excel with failing rows generates correct number of data rows
  2. Return Excel includes all original column data for each failing row
  3. Return Excel includes correct Issue column values ("MISSING_FIELDS: field1, field2") — note the space after colon
  4. Return Excel with no failing rows returns null (4-param overload)
  5. Return Excel preserves null values as empty cells
  6. Return Excel matches upload format (.xlsx → .xlsx)
  7. Return Excel with CSV format generates valid CSV
  8. Return Excel with CSV format — values containing commas are properly quoted
  9. Return Excel with no failing rows (2-param overload) — backward compatibility

  **New integration test** in `ExcelIntakeControllerTest.java`:
  10. `downloadReturnExcel_actualDownload_works` — Upload file with failing rows, extract download link from response, actually download the file, verify file exists and has correct content

  **Important:** The existing test `downloadReturnExcel_existingFile_returns200` only verifies the response contains the download link string but does NOT actually download the file. This new test fills that gap.

## Completion Criteria

The Wi-004 Naut phase is complete when:

1. `ExcelIntakeController.generateReturnExcel()` call passes the correct filename
2. `generateReturnExcel()` uses the filename parameter (not hardcoded name)
3. Return Excel format matches upload format (xlsx/csv)
4. Issue format includes space after colon: "MISSING_FIELDS: field1, field2"
5. `buildIssue()` accepts manual issue string parameter for future PoC integration
6. All existing tests still pass (backward compatibility verified)
7. New tests added and passing
8. New integration test verifies actual download works (not just response string)
9. All Maven tests pass

## Testing Steps for Naut

After implementation:

1. `mvn test` from `5-backend/business-service/` — must pass ALL existing and new tests
2. Verify existing tests still pass (no regression) — particularly the 3 existing `generateReturnExcel` tests
3. Verify `make build` succeeds end-to-end
4. Manual test: upload Excel with missing fields, download return Excel, verify file exists and has correct content

## Dependencies for Naut

- MandatoryFieldValidationService (already exists)
- ExcelParsingService (already exists, needs modification)
- ExcelInvoiceRow DTO (already exists)
- ExcelUploadResponse DTO (already exists)
- FailingRow DTO (already exists)

## Security Considerations

- S-010: Filename sanitized before writing to disk
- S-011: Download link must not expose server-side temp file path (uses UUID filename)
- S-012: Issue column values only contain canonical field names

## Risks and Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Signature change breaks existing tests | Compilation failure | High | Use backward-compatible overloads, keep existing 2-param method |
| CSV escaping issues with special characters | Corrupted CSV output | Medium | Implement proper RFC 4180 CSV escaping |
| Issue format change breaks tests | Test failure | Low | Verified: existing tests don't assert issue format content |
| PoC integration adds complexity | Code bloat | Low | Defer PoC failure rows to future iteration; only prepare infrastructure |
| Controller test gap (no actual download verification) | Bug slips through | Medium | Add new integration test that actually downloads the file |
