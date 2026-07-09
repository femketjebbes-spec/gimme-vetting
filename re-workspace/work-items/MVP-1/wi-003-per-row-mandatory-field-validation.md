# Work Item: WI-003 — Per-Row Mandatory Field Validation

**Parent Requirement:** RQ-007 (Mandatory Field Validation)
**Parent Work Stream:** W-007 (Excel Batch Intake Pipeline)
**Business Objective:** OPE-001 — Reduce case analyst workload by automating repetitive validation tasks in the vetting intake process.
**Created:** 2026-07-07 [Session 5]
**Status:** Completed
**Completed:** 2026-07-08
**Implemented By:** Naut
**Estimated Effort:** 0.5–1 sprint (TBD by development team)

---

## 1. Requirement Statement

Gimme shall validate each row of the uploaded Excel file for mandatory field completeness.

The following fields are mandatory per row: `invoiceNumber`, `debtorName`, `address`, `bankAccountNumber`, `phoneNumber`. A row is considered incomplete if any of these fields is empty or missing.

Rows that pass all mandatory field checks proceed to PoC existence verification (WI-001 / RQ-001). Rows that fail mandatory field validation are not stored in the system and are flagged for client correction.

**Note:** Mandatory field enforcement was previously externalized to the submission form. Session 5 specification change (D-010) moved this requirement internal to Gimme.

---

## 2. Acceptance Criteria (Gherkin)

### Feature: Per-Row Mandatory Field Validation

As the intake pipeline,
I must validate each row for mandatory field completeness,
So that only complete rows proceed to PoC verification and storage.

---

#### Scenario 1: All mandatory fields present — row passes

```gherkin
Given a parsed Excel row with values:
  | Field              | Value            |
  | invoiceNumber      | "INV-2026-0042"  |
  | debtorName         | "Acme BV"        |
  | address            | "Street 1, City" |
  | phoneNumber        | "+31612345678"   |
  | bankAccountNumber  | "NL12BANK0123456789" |
When the mandatory field validation gate is applied
Then the row shall pass all mandatory field checks
And the row shall be passed to the PoC existence verification gate (RQ-001)
```

**Rationale:** This is the happy path. All mandatory fields contain non-empty values. The row proceeds to the next validation gate.

---

#### Scenario 2: Missing debtor name — row fails

```gherkin
Given a parsed Excel row with values:
  | Field              | Value            |
  | invoiceNumber      | "INV-2026-0043"  |
  | debtorName         | "" (empty)       |
  | address            | "Street 2, City" |
  | phoneNumber        | "+31612345679"   |
  | bankAccountNumber  | "NL13BANK0123456790" |
When the mandatory field validation gate is applied
Then the row shall fail the debtorName mandatory field check
And the validation error shall be recorded as: "MISSING_FIELDS: debtorName"
And the row shall NOT be passed to PoC existence verification
And the row shall be marked for inclusion in the return Excel (RQ-008)
```

**Rationale:** A single missing field causes the row to fail. The error record identifies the specific missing field(s).

---

#### Scenario 3: Multiple fields missing — row fails with all field names listed

```gherkin
Given a parsed Excel row with values:
  | Field              | Value     |
  | invoiceNumber      | "INV-2026-0044" |
  | debtorName         | "" (empty) |
  | address            | "" (empty) |
  | phoneNumber        | "+31612345680" |
  | bankAccountNumber  | "" (empty) |
When the mandatory field validation gate is applied
Then the row shall fail three mandatory field checks: debtorName, address, bankAccountNumber
And the validation error shall be recorded as: "MISSING_FIELDS: debtorName, address, bankAccountNumber"
And the row shall NOT be passed to PoC existence verification
And the row shall be marked for inclusion in the return Excel (RQ-008)
```

**Rationale:** Multiple missing fields are aggregated into a single error record. The client can see all missing fields at once and correct them in one re-upload.

---

#### Scenario 4: Missing invoice number — row fails and is blocked even before PoC check

```gherkin
Given a parsed Excel row with values:
  | Field              | Value            |
  | invoiceNumber      | "" (empty)       |
  | debtorName         | "Acme BV"        |
  | address            | "Street 3, City" |
  | phoneNumber        | "+31612345681"   |
  | bankAccountNumber  | "NL14BANK0123456801" |
When the mandatory field validation gate is applied
Then the row shall fail the invoiceNumber mandatory field check
And the row shall NOT be passed to PoC existence verification
And the row shall be marked for inclusion in the return Excel (RQ-008)
```

**Rationale:** invoiceNumber is used for PoC filename matching. A row without an invoice number cannot proceed to PoC verification. It must fail at the mandatory field gate.

---

#### Scenario 5: Whitespace-only values treated as empty

```gherkin
Given a parsed Excel row with values:
  | Field              | Value            |
  | invoiceNumber      | "INV-2026-0045"  |
  | debtorName         | "   " (whitespace only) |
  | address            | "Street 4, City" |
  | phoneNumber        | "+31612345682"   |
  | bankAccountNumber  | "NL15BANK0123456812" |
When the mandatory field validation gate is applied
Then the row shall fail the debtorName mandatory field check
And whitespace-only values shall be treated as empty
```

**Rationale:** Whitespace-only values are functionally equivalent to empty. The client needs to provide actual data, not spaces.

---

#### Scenario 6: Null values treated as empty

```gherkin
Given a parsed Excel row with values:
  | Field              | Value            |
  | invoiceNumber      | "INV-2026-0046"  |
  | debtorName         | "Acme BV"        |
  | address            | null (missing column) |
  | phoneNumber        | "+31612345683"   |
  | bankAccountNumber  | "NL16BANK0123456823" |
When the mandatory field validation gate is applied
Then the row shall fail the address mandatory field check
And null values shall be treated as empty
```

**Rationale:** Null (from missing column or explicit null) is functionally equivalent to empty. This aligns with Scenario 5 in WI-002 where rows with fewer columns are parsed with null/empty for missing fields.

---

## 3. ISO 29148 Quality Attribute Validation

| Attribute | Assessment | Notes |
|-----------|------------|-------|
| **Unambiguous** | PASS | "Mandatory field check" is deterministic. Empty, null, and whitespace-only are all defined as failures. |
| **Complete** | PASS | All five mandatory fields are listed. Edge cases (whitespace, null, multiple missing) are addressed. |
| **Consistent** | PASS | No conflict with other requirements. This is a validation gate in the Excel intake pipeline (RQ-006) that feeds PoC verification (RQ-001). |
| **Verifiable** | PASS | All 6 acceptance criteria are directly testable. |
| **Feasible** | PASS | String emptiness check is a trivial operation. No unknown technology required. |
| **Traceable** | PASS | Traces to OPE-001 (business objective) and Session 5 requirement. |

**Quality Verdict:** PASS. This requirement is clear, complete, and testable.

---

## 4. Design Decisions (Logged)

| Decision | Rationale | Stakeholder Authority | Assumptions Dependent |
|----------|-----------|----------------------|----------------------|
| D-010: Mandatory field enforcement moved from external to internal | Session 5 confirmed upstream Excel may contain incomplete rows. Gimme validates at intake. | Confirmed by user in Session 5 | None |
| D-022: Whitespace-only values are treated as empty | Whitespace-only is functionally equivalent to empty. | Default (no user override) | None |
| D-023: Null values are treated as empty | Null from missing column or explicit null is functionally equivalent to empty. | Default (no user override) | None |

---

## 5. Out of Scope

The following are explicitly excluded from WI-003:

- PoC existence verification — handled by WI-001 (existing PoCStoreService)
- Excel file parsing — handled by WI-002
- Return Excel generation — handled by WI-004
- Separate PoC upload — handled by WI-005
- Data format validation (e.g., is the phone number a valid phone? is the bank account a valid IBAN?) — only emptiness is checked for MVP
- Database persistence — failing rows are not stored; passing rows are stored by downstream components

---

## 6. Implementation Notes

### Input Contract

The validation component receives a list of `ExcelInvoiceRow` objects produced by WI-002:

```java
public class ExcelInvoiceRow {
    private Integer rowIndex;        // 0-based row index
    private String invoiceNumber;
    private String debtorName;
    private String address;
    private String phoneNumber;
    private String bankAccountNumber;
    private List<String> parseErrors;
}
```

### Output Contract

The validation component produces a `ValidationResult` for each row:

```java
public class ValidationResult {
    private Integer rowIndex;
    private RowStatus status;        // PASSED or FAILED
    private List<String> missingFields; // null if PASSED, contains field names if FAILED
    private ExcelInvoiceRow originalRow; // reference to original row data
}

public enum RowStatus {
    PASSED,
    FAILED
}
```

Rows with `RowStatus.PASSED` are passed to PoC existence verification. Rows with `RowStatus.FAILED` are collected for the return Excel (WI-004).

### Mandatory Fields List

```java
private static final List<String> MANDATORY_FIELDS = List.of(
    "invoiceNumber",
    "debtorName",
    "address",
    "phoneNumber",
    "bankAccountNumber"
);
```

### Validation Logic

For each row and each mandatory field:
1. Get the field value from the row object
2. If the value is null, treat as empty
3. If the value is an empty string or contains only whitespace, treat as empty
4. Record the field name in the `missingFields` list if empty
5. After checking all fields, set `status` to `FAILED` if `missingFields` is non-empty, otherwise `PASSED`

### Data Model Requirements

No new entities are needed. The existing `ExcelInvoiceRow` domain object (produced by WI-002) is the input. The `ValidationResult` (defined above) is a transient output.

---

## 7. Dependencies

| Dependency | Status | Notes |
|------------|--------|-------|
| WI-001 (PoCStoreService) | Downstream | Passing rows are validated against PoCStoreService. |
| WI-002 (Excel Upload and Parsing) | Upstream | Direct dependency. WI-003 consumes parsed row objects from WI-002. |
| WI-004 (Return Excel) | Downstream | Failing rows from WI-003 are passed to WI-004 for inclusion in the return Excel. |
| Gerard (API Agent) | Parallel | Not a direct dependency for WI-003. |

---

## 8. Test Strategy

### Unit Tests

- All mandatory fields present: row passes
- Single missing field: row fails with correct field name
- Multiple missing fields: row fails with all field names listed
- Whitespace-only value: treated as empty
- Null value: treated as empty
- Empty string: treated as empty
- invoiceNumber missing: row fails and is NOT passed to PoC verification

### Integration Tests

- Full pipeline: WI-002 parses rows → WI-003 validates → passing rows to PoCStoreService, failing rows collected
- Verify that failing rows are NOT stored in the database
- Verify that passing rows ARE passed to the PoC verification component

### Test Data

| Test Case | invoiceNumber | debtorName | address | phoneNumber | bankAccountNumber | Expected Outcome |
|-----------|--------------|------------|---------|-------------|-------------------|-----------------|
| All present | "INV-001" | "Acme BV" | "St 1" | "+316..." | "NL12..." | PASSED |
| Missing debtor | "INV-002" | "" | "St 2" | "+316..." | "NL13..." | FAILED: debtorName |
| Missing address | "INV-003" | "Acme BV" | "" | "+316..." | "NL14..." | FAILED: address |
| Missing phone | "INV-004" | "Acme BV" | "St 4" | "" | "NL15..." | FAILED: phoneNumber |
| Missing IBAN | "INV-005" | "Acme BV" | "St 5" | "+316..." | "" | FAILED: bankAccountNumber |
| Missing invoice | "INV-006" | "Acme BV" | "St 6" | "+316..." | "NL16..." | FAILED: invoiceNumber |
| Multiple missing | "INV-007" | "" | "" | "+316..." | "" | FAILED: debtorName, address, bankAccountNumber |
| Whitespace only | "INV-008" | "   " | "St 8" | "+316..." | "NL17..." | FAILED: debtorName |
| Null value | "INV-009" | "Acme BV" | null | "+316..." | "NL18..." | FAILED: address |

---

## 9. Risks and Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|-----------|------------|
| Client considers some fields optional that we treat as mandatory | Unnecessary rejections, client confusion | Low | Mandatory fields were confirmed by user in Session 5. Reconfirm with stakeholder if client disputes. |
| Blank cells in Excel parsed as empty string vs. null | Validation may behave differently depending on parsing library | Low | Handle both null and empty string consistently (D-022, D-023). |
| Client provides invoiceNumber with only whitespace | InvoiceNumber fails mandatory check, but may be confusing to client | Medium | Include field name in error message. Client can see the issue in return Excel. |

---

## 10. Definition of Done

- [ ] All 6 Gherkin scenarios implemented as automated tests
- [ ] Unit test coverage for validation logic: minimum 90%
- [ ] Integration test for full pipeline: WI-002 → WI-003 → PoCStoreService
- [ ] Code review completed
- [ ] No regression in existing business service tests
- [ ] Acceptance criteria reviewed and approved by product owner or case analyst stakeholder

---

**Work Item Author:** Robbie (Requirements Engineer)
**Review Status:** NOT YET APPROVED — Pending architect design review
**Next Step:** Archibald produces delegation plan; Naut implements validation logic
