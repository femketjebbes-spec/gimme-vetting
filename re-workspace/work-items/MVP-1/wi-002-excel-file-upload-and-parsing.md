# Work Item: WI-002 — Excel File Upload and Parsing

**Parent Requirement:** RQ-006 (Excel Batch Intake)
**Parent Work Stream:** W-007 (Excel Batch Intake Pipeline)
**Business Objective:** OPE-001 — Reduce case analyst workload by automating repetitive validation tasks in the vetting intake process.
**Created:** 2026-07-07 [Session 5]
**Status:** Completed
**Completed:** 2026-07-08
**Implemented By:** Naut
**Estimated Effort:** 1–2 sprints (TBD by development team)

---

## 1. Requirement Statement

Gimme shall provide a client portal endpoint where external clients can upload an Excel file containing multiple invoices, one per row.

Each row in the Excel file represents one invoice submission. The Excel file shall use a defined column structure matching the mandatory fields: `invoiceNumber`, `debtorName`, `address`, `phoneNumber`, `bankAccountNumber`. Gimme shall parse each row into a domain object that downstream validation components can consume.

Processing is synchronous: the client uploads, Gimme processes, Gimme returns the result file in the same request cycle.

---

## 2. Acceptance Criteria (Gherkin)

### Feature: Excel File Upload and Parsing

As an external client,
I need to upload an Excel file with multiple invoices,
So that Gimme can process all invoices in a single request.

---

#### Scenario 1: Valid Excel file with multiple rows — all rows parsed

```gherkin
Given a client uploads an Excel file in .xlsx format
And the file contains a header row with column names: "invoice number", "debtor name", "address", "phone number", "bank account number"
And the file contains 10 data rows, each with valid values in all columns
When the upload request is processed
Then Gimme shall parse all 10 rows into domain objects
And each row shall be mapped to the correct field: invoiceNumber, debtorName, address, phoneNumber, bankAccountNumber
And the parsed row objects shall be passed to the downstream validation component (WI-003)
```

**Rationale:** This is the happy path. The file has a header row, correct column names, and all data rows are complete. Parsing succeeds for all rows.

---

#### Scenario 2: Excel file without a header row — column order is used

```gherkin
Given a client uploads an Excel file in .xlsx format
And the file does NOT contain a header row
And the first data row contains values in column order: invoiceNumber, debtorName, address, phoneNumber, bankAccountNumber
When the upload request is processed
Then Gimme shall interpret the first row as data (not a header)
And each value shall be mapped by column position: column 0 = invoiceNumber, column 1 = debtorName, column 2 = address, column 3 = phoneNumber, column 4 = bankAccountNumber
And all data rows shall be parsed into domain objects
```

**Rationale:** The header row is optional (D-018). When absent, column order is the sole mapping mechanism. Column order is: invoiceNumber, debtorName, address, phoneNumber, bankAccountNumber (D-019).

---

#### Scenario 3: CSV file upload

```gherkin
Given a client uploads an Excel file in .csv format
And the file uses comma as the delimiter
And the file contains 5 data rows
When the upload request is processed
Then Gimme shall parse the CSV file successfully
And each row shall be mapped to the correct field by column position
And all 5 rows shall be parsed into domain objects
```

**Rationale:** CSV is supported alongside .xlsx (D-017). The parsing component must handle both formats. CSV uses column position mapping (header optional).

---

#### Scenario 4: Malformed Excel file (corrupted or non-Excel format)

```gherkin
Given a client uploads a file that is corrupted or not a valid Excel/CSV file
When the upload request is processed
Then Gimme shall reject the file with a clear error message
And the error response shall indicate the file format is invalid
And no rows shall be parsed
```

**Rationale:** The component must gracefully handle malformed input. A corrupted file or a file with an unsupported format (e.g., .docx) should produce a structured error response, not a server error.

---

#### Scenario 5: Row with fewer columns than expected

```gherkin
Given a client uploads a valid Excel file
And one data row contains only 3 values (missing columns for phoneNumber and bankAccountNumber)
When the upload request is processed
Then the row shall be parsed successfully as a domain object
And the missing fields shall be stored as null or empty strings
And the row shall be passed to the mandatory field validation component (WI-003) which will flag the missing fields
```

**Rationale:** A row with fewer columns than expected is not a parsing failure — it is a validation failure. The row should be parsed with missing fields as empty/null and passed downstream. WI-003 handles the mandatory field check.

---

#### Scenario 6: Empty rows in the file

```gherkin
Given a client uploads a valid Excel file
And the file contains 3 empty rows interspersed among 10 data rows
When the upload request is processed
Then the empty rows shall be skipped during parsing
And only the 10 non-empty rows shall be passed to the validation component
```

**Rationale:** Empty rows may be introduced by the client Excel editor. They should be silently skipped, not treated as validation failures.

---

## 3. ISO 29148 Quality Attribute Validation

| Attribute | Assessment | Notes |
|-----------|------------|-------|
| **Unambiguous** | PASS | "Parse each row into a domain object" is concrete. Column-to-field mapping is defined. |
| **Complete** | PARTIAL | The requirement specifies the parsing mechanism and column structure. It does not specify the Excel parsing library (Apache POI vs. EasyExcel), character encoding for CSV, or handling of merged cells. The architect must decide these implementation details. |
| **Consistent** | PASS | No conflict with other requirements. This is the intake mechanism that feeds WI-003 (validation). |
| **Verifiable** | PASS | All 6 acceptance criteria are directly testable. |
| **Feasible** | PASS | Excel parsing is a standard operation with mature libraries (Apache POI, EasyExcel, OpenCSV). |
| **Traceable** | PASS | Traces to OPE-001 (business objective) and Session 5 MVP requirement. |

**Quality Verdict:** PASS with minor completeness notes. Implementation details (library selection, character encoding, merged cell handling) are delegated to the architect.

---

## 4. Design Decisions (Logged)

| Decision | Rationale | Stakeholder Authority | Assumptions Dependent |
|----------|-----------|----------------------|----------------------|
| D-017: Excel supports .xlsx and .csv formats | Client may use either format. Both must be supported for MVP. | Confirmed by user in Session 5 | None |
| D-018: Header row is optional | Upstream Excel may or may not include a header row. Column order is used as the fallback. | Confirmed by user in Session 5 | D-019 (column order) |
| D-019: Column order is invoiceNumber / debtorName / address / phoneNumber / bankAccountNumber | Confirmed by user in Session 5. This order applies when no header row is present. | Confirmed by user in Session 5 | None |
| D-020: No authentication for MVP | No auth for the client portal in MVP. | Confirmed by user in Session 5 | Security risk — must be flagged in architect design |
| D-021: No file size limit for MVP | No file size limit for MVP. Architect should design boundary but no enforcement in MVP. | Confirmed by user in Session 5 | If large files cause performance issues, async may be required (AUNV-006) |

---

## 5. Out of Scope

The following are explicitly excluded from WI-002:

- Mandatory field validation — handled by WI-003
- PoC existence verification — handled by WI-001 (existing PoCStoreService)
- Return Excel generation — handled by WI-004
- Separate PoC upload — handled by WI-005
- Client portal UI — outside RE scope; architect produces interface design
- Authentication — no auth for MVP
- File size enforcement — no limit for MVP
- Batch/async processing — synchronous for MVP

---

## 6. Implementation Notes

### Input Contract

The client uploads a single file via a POST endpoint:

| Attribute | Value |
|-----------|-------|
| Format | .xlsx or .csv |
| Maximum file size | No limit for MVP |
| Authentication | None for MVP |
| Content type (xlsx) | application/vnd.openxmlformats-officedocument.spreadsheetml.sheet |
| Content type (csv) | text/csv |

### Column Mapping

When a header row is present, column names are matched case-insensitively:

| Column Name (Header) | Field Name | Position (No Header) |
|----------------------|------------|---------------------|
| "invoice number" | invoiceNumber | 0 |
| "debtor name" | debtorName | 1 |
| "address" | address | 2 |
| "phone number" | phoneNumber | 3 |
| "bank account number" | bankAccountNumber | 4 |

When no header row is present, values are mapped by column position.

### Output Contract

Each parsed row produces a domain object:

```java
public class ExcelInvoiceRow {
    private Integer rowIndex;        // 0-based row index in the source file
    private String invoiceNumber;
    private String debtorName;
    private String address;
    private String phoneNumber;
    private String bankAccountNumber;
    private List<String> parseErrors; // null if no errors
}
```

The list of parsed rows is passed to WI-003 for mandatory field validation.

### Parsing Library Options

| Library | Pros | Cons |
|---------|------|------|
| Apache POI | Industry standard, supports .xlsx and .csv, mature API | Heavy dependency, higher memory usage for large files |
| EasyExcel | Memory-efficient for large files, Alibaba open-source | Less widely adopted, fewer community examples |
| OpenCSV + jxls | Lightweight for CSV, separate handling for .xlsx | Requires two libraries, more code to maintain |

**Recommendation:** Apache POI for MVP simplicity. EasyExcel can be evaluated if large-file performance becomes an issue.

### Data Model Requirements

The following entities are needed (see W-005 for full model):

- **ExcelInvoiceRow** — transient domain object representing one parsed row: rowIndex, invoiceNumber, debtorName, address, phoneNumber, bankAccountNumber

No database persistence is required for this work item alone. The parsed rows are transient objects passed to the validation component.

---

## 7. Dependencies

| Dependency | Status | Notes |
|------------|--------|-------|
| WI-001 (PoCStoreService) | Parallel | Not a direct dependency for WI-002. WI-003 depends on WI-001 output. |
| WI-003 (Per-Row Validation) | Downstream | WI-002 produces row objects consumed by WI-003. |
| WI-004 (Return Excel) | Downstream | WI-002 provides the original row data for the return Excel. |
| Gerard (API Agent) | Parallel | Gerard defines the API contract for the upload endpoint. |
| Upstream Excel format | External | Column structure confirmed by user in Session 5. |

---

## 8. Test Strategy

### Unit Tests

- .xlsx parsing: header row present, header row absent, single row, multiple rows
- .csv parsing: comma-delimited, various encodings
- Malformed file handling: corrupted file, wrong format, empty file
- Column mapping: header-based mapping, position-based mapping
- Empty row handling: skip empty rows, handle rows with fewer columns
- Special characters in data: Unicode characters in debtor names, special characters in addresses

### Integration Tests

- Full upload request flow with mocked file storage
- Verify that parsed rows are passed to the validation component
- Verify error response for malformed files

### Test Data

| Test Case | Format | Header | Rows | Expected Outcome |
|-----------|--------|--------|------|-----------------|
| Happy path | .xlsx | Yes | 10 | All 10 rows parsed |
| No header | .xlsx | No | 5 | All 5 rows parsed by position |
| CSV | .csv | Yes | 3 | All 3 rows parsed |
| Malformed | .docx | N/A | 0 | Error: invalid file format |
| Empty rows | .xlsx | Yes | 10 + 3 empty | 10 rows parsed, 3 skipped |
| Fewer columns | .xlsx | Yes | 1 (3 cols) | Row parsed with null/empty for missing fields |

---

## 9. Risks and Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|-----------|------------|
| Large file memory consumption (no size limit for MVP) | OOM during parsing | Medium | Use streaming API (Apache POI SXSSF) or recommend EasyExcel to architect. |
| Client Excel file contains merged cells | Parsing may skip or misread rows | Low | Document limitation in MVP. Handle merged cells in a future iteration. |
| CSV encoding issues (UTF-8 vs. ISO-8859-1) | Special characters corrupted | Low | Detect encoding from BOM or try multiple encodings with fallback. |
| Column name mismatch (client uses different header names) | Mapping fails, all rows rejected | Medium | Accept multiple header name variants (e.g., "invoice number", "invoiceNo", "invoice"). Map them to the canonical field name. |

---

## 10. Definition of Done

- [ ] All 6 Gherkin scenarios implemented as automated tests
- [ ] Unit test coverage for parsing logic: minimum 80%
- [ ] Integration test for full upload request flow with mocked file storage
- [ ] Apache POI (or chosen library) dependency added to build
- [ ] Code review completed
- [ ] No regression in existing business service tests
- [ ] Acceptance criteria reviewed and approved by product owner or case analyst stakeholder

---

**Work Item Author:** Robbie (Requirements Engineer)
**Review Status:** NOT YET APPROVED — Pending architect design review
**Next Step:** Archibald produces delegation plan; Gerard defines API contract; Naut implements parsing logic
