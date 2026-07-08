# Work Item: WI-004 — Return Excel Generation

**Parent Requirement:** RQ-008 (Return Excel with Missing Data)
**Parent Work Stream:** W-007 (Excel Batch Intake Pipeline)
**Business Objective:** OPE-001 — Reduce case analyst workload by automating repetitive validation tasks in the vetting intake process.
**Created:** 2026-07-07 [Session 5]
**Status:** Not started
**Estimated Effort:** 1 sprint (TBD by development team)

---

## 1. Requirement Statement

Gimme shall produce a return Excel file containing only the rows that failed validation.

The return Excel file shall include all original column data from each failing row (returned fully, not truncated). An additional column shall indicate the validation issue per row: "MISSING_FIELDS" with a list of missing field names, or "MISSING_POC" for rows that passed mandatory field validation but had no matching PoC file.

The return Excel file shall be available for download via the client portal at the end of the upload request cycle. The return Excel file shall be formatted such that the client can fill in missing fields directly within the file and re-upload.

Rows that passed all validation gates are NOT included in the return Excel file.

---

## 2. Acceptance Criteria (Gherkin)

### Feature: Return Excel Generation

As a client,
I need to receive an Excel file containing only the rows with issues,
So that I can correct the missing data and re-upload.

---

#### Scenario 1: Return Excel contains only failing rows

```gherkin
Given an uploaded Excel file with 10 rows
And 3 rows failed mandatory field validation (WI-003)
And 2 rows failed PoC existence verification (WI-001)
And 5 rows passed all validation gates
When the return Excel is generated
Then the return Excel shall contain exactly 5 rows (3 missing fields + 2 missing PoC)
And the 5 passing rows shall NOT be included in the return Excel
```

**Rationale:** The return Excel contains only failing rows, not all rows. This is more efficient for the client to review (D-012).

---

#### Scenario 2: Return Excel includes all original column data for each failing row

```gherkin
Given a failing row with values:
  | Field              | Value            |
  | invoiceNumber      | "INV-2026-0042"  |
  | debtorName         | "" (empty)       |
  | address            | "Street 1, City" |
  | phoneNumber        | "+31612345678"   |
  | bankAccountNumber  | "NL12BANK0123456789" |
When the return Excel is generated
Then the row shall be returned with all original column data intact
And no column data shall be truncated or omitted
And the empty debtorName field shall be preserved as empty in the return Excel
```

**Rationale:** The client needs to see the original data to understand which row has the issue and to fill in missing data without losing existing information.

---

#### Scenario 3: Return Excel includes an additional issue identification column

```gherkin
Given a failing row with missing fields: debtorName, address
And a failing row with missing PoC: invoiceNumber = "INV-2026-0043"
When the return Excel is generated
Then the return Excel shall include an additional column named "Issue"
And the row with missing debtorName and address shall have "Issue" = "MISSING_FIELDS: debtorName, address"
And the row with missing PoC shall have "Issue" = "MISSING_POC"
```

**Rationale:** The additional column identifies the validation issue per row, enabling the client to understand what needs to be corrected.

---

#### Scenario 4: Return Excel is formatted for client to fill in and re-upload

```gherkin
Given a return Excel file with failing rows
When the return Excel is generated
Then it shall use the same column structure as the upload file (same columns, same order)
And it shall include a header row matching the upload format
And the existing field values shall be preserved in their respective columns
And the client can fill in empty fields and re-upload the file
```

**Rationale:** The return Excel must be compatible with the upload process. Same column structure, same order, header row present. The client fills in missing data and re-uploads the same file.

---

#### Scenario 5: Return Excel is available as a download link in the client portal

```gherkin
Given an Excel upload has been processed
And some rows failed validation
When the processing is complete
Then the client portal shall display a download link for the return Excel file
And the download link shall be available within the same request cycle
And the return Excel file shall be generated in a format consistent with the upload format (.xlsx if upload was .xlsx, .csv if upload was .csv)
```

**Rationale:** The return Excel is accessible via a download link in the client portal (D-013). The format matches the upload format for consistency.

---

#### Scenario 6: Return Excel with no failing rows

```gherkin
Given an uploaded Excel file with 10 rows
And all 10 rows passed all validation gates
When the return Excel is generated
Then the return Excel shall be an empty file (header row only, no data rows)
Or an alternative message shall be displayed to the client indicating all rows passed validation
```

**Rationale:** When all rows pass validation, the return Excel is either empty (header only) or a message indicates success. Either approach is acceptable; the specification does not mandate which.

---

## 3. ISO 29148 Quality Attribute Validation

| Attribute | Assessment | Notes |
|-----------|------------|-------|
| **Unambiguous** | PASS | "Return Excel containing only failing rows" is concrete. Additional column "Issue" is defined. |
| **Complete** | PARTIAL | The requirement specifies the content and format of the return Excel. It does not specify the file naming convention, the download link mechanism, or what happens when all rows pass. These are implementation decisions for the architect. |
| **Consistent** | PASS | No conflict with other requirements. This is the output mechanism for the Excel intake pipeline (RQ-006). |
| **Verifiable** | PASS | All 6 acceptance criteria are directly testable. |
| **Feasible** | PASS | Excel generation is a standard operation with mature libraries (Apache POI, EasyExcel). |
| **Traceable** | PASS | Traces to OPE-001 (business objective) and Session 5 requirement. |

**Quality Verdict:** PASS with minor completeness notes. File naming convention and download link mechanism are delegated to the architect.

---

## 4. Design Decisions (Logged)

| Decision | Rationale | Stakeholder Authority | Assumptions Dependent |
|----------|-----------|----------------------|----------------------|
| D-012: Return Excel contains only failing rows | User explicitly confirmed only rows with issues are returned. | Confirmed by user in Session 5 | None |
| D-013: Return Excel is download link in portal | User confirmed download link, not email. | Confirmed by user in Session 5 | None |

---

## 5. Out of Scope

The following are explicitly excluded from WI-004:

- Excel file parsing — handled by WI-002
- Mandatory field validation — handled by WI-003
- PoC existence verification — handled by WI-001
- Separate PoC upload — handled by WI-005
- Client portal UI (download link rendering) — outside RE scope; architect produces interface design
- Email notification — not part of MVP
- Historical return Excel storage — not part of MVP
- Data format validation beyond emptiness check — only emptiness is checked for MVP

---

## 6. Implementation Notes

### Input Contract

The generation component receives two collections from upstream components:

1. **Failing rows from mandatory field validation (WI-003):**
```java
List<ValidationResult> failedValidationRows;
// Each ValidationResult contains: rowIndex, status=FAILED, missingFields, originalRow
```

2. **Failing rows from PoC verification (WI-001):**
```java
List<String> failedPoCInvoiceNumbers;
// Invoice numbers that passed mandatory validation but had no matching PoC
```

### Output Contract

The return Excel file:

| Attribute | Value |
|-----------|-------|
| Format | Same as upload format (.xlsx or .csv) |
| Columns | All original columns + "Issue" column |
| Header row | Present (matching upload format) |
| Data rows | Only failing rows |
| File naming | To be determined by architect (e.g., `return_INV-2026-0042_<timestamp>.xlsx`) |

### Column Structure

If the upload file had columns: `invoice number`, `debtor name`, `address`, `phone number`, `bank account number`

The return file shall have: `invoice number`, `debtor name`, `address`, `phone number`, `bank account number`, `Issue`

### Issue Column Values

| Condition | Issue Column Value |
|-----------|-------------------|
| Missing mandatory fields | `MISSING_FIELDS: field1, field2, ...` |
| No matching PoC | `MISSING_POC` |
| Multiple missing fields | `MISSING_FIELDS: field1, field2, field3` |

### Excel Generation Options

| Library | Pros | Cons |
|---------|------|------|
| Apache POI | Same library as parsing, consistent API | Higher memory usage |
| EasyExcel | Memory-efficient, Alibaba open-source | Different API from parsing |
| Java CSV writer (for .csv only) | Lightweight | Only handles CSV, not .xlsx |

**Recommendation:** Use the same library as WI-002 (Apache POI) for consistency.

### Return Excel Generation Logic

1. Collect all failing rows from WI-003 (mandatory field validation failures)
2. Collect all failing rows from WI-001 (PoC verification failures) — these rows have passed mandatory validation, so their original data is intact
3. For each failing row, append the "Issue" column value
4. Create the Excel file with header row matching the upload format
5. Add all failing rows as data rows
6. Make the file available for download via the client portal

### Data Model Requirements

No new persistent entities are needed. The `ValidationResult` objects from WI-003 and the PoC verification results are transient. The return Excel file is generated in-memory or in a temporary file and served to the client.

---

## 7. Dependencies

| Dependency | Status | Notes |
|------------|--------|-------|
| WI-001 (PoCStoreService) | Upstream | Provides PoC verification results for each row. |
| WI-002 (Excel Upload and Parsing) | Upstream | Provides original row data for the return Excel. |
| WI-003 (Per-Row Validation) | Upstream | Provides validation failure results (missing fields per row). |
| Gerard (API Agent) | Parallel | Gerard defines the download endpoint API contract. |

---

## 8. Test Strategy

### Unit Tests

- Return Excel with mixed failing rows (mandatory field + missing PoC)
- Return Excel with only mandatory field failures
- Return Excel with only PoC failures
- Return Excel with no failing rows (empty file)
- Return Excel preserves all original column data
- Return Excel includes correct Issue column values
- Return Excel format matches upload format (.xlsx → .xlsx, .csv → .csv)
- Return Excel includes header row

### Integration Tests

- Full pipeline: upload → parse → validate → PoC check → return Excel generation
- Verify download link is available in response
- Verify return Excel can be re-uploaded without format errors

### Test Data

| Test Case | Total Rows | Failed Validation | Failed PoC | Passed | Expected Return Rows |
|-----------|-----------|-------------------|------------|--------|---------------------|
| All fail validation | 5 | 5 | 0 | 0 | 5 |
| All fail PoC | 5 | 0 | 5 | 0 | 5 |
| Mixed | 10 | 3 | 2 | 5 | 5 |
| All pass | 5 | 0 | 0 | 5 | 0 (empty) |
| Single fail | 1 | 1 | 0 | 0 | 1 |

---

## 9. Risks and Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|-----------|------------|
| Return Excel file is very large (many failing rows) | Server memory pressure during generation | Medium | Stream file generation. Use temp file instead of in-memory. Architect should design boundary. |
| Client cannot re-upload the return Excel due to format mismatch | Client confusion, unnecessary support tickets | Low | Ensure return Excel format is identical to upload format (columns, order, header). |
| Issue column values are confusing to client | Client cannot understand what needs correction | Low | Use clear, consistent error messages. Consider adding a legend in the client portal UI. |
| Temporary file cleanup on server | Disk space accumulation | Low | Implement automatic cleanup of temporary return Excel files after a configurable retention period. |

---

## 10. Definition of Done

- [ ] All 6 Gherkin scenarios implemented as automated tests
- [ ] Unit test coverage for Excel generation logic: minimum 80%
- [ ] Integration test for full pipeline: upload → parse → validate → PoC check → return Excel
- [ ] Code review completed
- [ ] No regression in existing business service tests
- [ ] Acceptance criteria reviewed and approved by product owner or case analyst stakeholder

---

**Work Item Author:** Robbie (Requirements Engineer)
**Review Status:** NOT YET APPROVED — Pending architect design review
**Next Step:** Archibald produces delegation plan; Naut implements Excel generation logic
