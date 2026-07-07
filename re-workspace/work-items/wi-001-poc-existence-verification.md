# Work Item: WI-001 — PoC Existence Verification

**Parent Requirement:** RQ-001 (re-workspace/requirements-spec.md)
**Work Stream:** W-002 (Invoice Intake and PoC Matching Engine)
**Business Objective:** OPE-001 — Reduce case analyst workload by automating repetitive validation tasks in the vetting intake process.
**Created:** 2026-07-07 [Session 4]
**Status:** Ready for implementation
**Estimated Effort:** 1 sprint (TBD by development team)

---

## 1. Requirement Statement

Gimme shall verify that a Proof of Correspondence (PoC) document exists and is linked to the invoice at the point of intake.

Verification shall be performed by matching the PoC filename to the invoice number (System Identifier) provided by the client. If no PoC is linked, the invoice shall be rejected automatically (Type A — Missing PoC).

---

## 2. Acceptance Criteria (Gherkin)

### Feature: PoC Existence Verification

As a system intake pipeline,
I must verify PoC existence for every invoice,
So that only invoices with valid evidence proceed to the case analyst queue.

---

#### Scenario 1: PoC filename matches invoice number — invoice proceeds

```gherkin
Given an invoice submission with invoice number "INV-2026-0042"
And a PoC file named "INV-2026-0042.pdf" is available in the PoC store
When the invoice is submitted to the intake pipeline
Then the system shall match the invoice number to the PoC filename
And the invoice shall pass the PoC existence gate
And the invoice shall be queued for case analyst batch acceptance
```

**Rationale:** This is the happy path. The filename match confirms PoC linkage. The invoice proceeds to the next gate (business rule checks) and ultimately the case analyst queue.

---

#### Scenario 2: No PoC file exists for the invoice number — Type A rejection

```gherkin
Given an invoice submission with invoice number "INV-2026-0042"
And no PoC file with a matching name exists in the PoC store
When the invoice is submitted to the intake pipeline
Then the system shall fail the PoC existence gate
And the invoice shall be rejected as Type A (Missing PoC)
And the rejection reason shall be recorded as "No PoC linked to invoice INV-2026-0042"
And the client may re-submit this invoice at any time
```

**Rationale:** Type A rejection is not final. The client receives no limit on re-submission attempts. This is critical for client experience.

---

#### Scenario 3: Multiple PoC files exist for one invoice number — first match suffices

```gherkin
Given an invoice submission with invoice number "INV-2026-0042"
And PoC files named "INV-2026-0042.pdf" and "INV-2026-0042-copy.pdf" exist in the PoC store
When the invoice is submitted to the intake pipeline
Then the system shall find at least one matching PoC filename
And the invoice shall pass the PoC existence gate
And the invoice shall be queued for case analyst batch acceptance
```

**Rationale:** Multiple PoC files for the same invoice do not constitute an error. One match is sufficient. The system does not need to deduplicate or reject duplicate PoCs. This is an assumption pending verification.

---

#### Scenario 4: Case-insensitive filename matching

```gherkin
Given an invoice submission with invoice number "INV-2026-0042"
And a PoC file named "inv-2026-0042.pdf" (lowercase) exists in the PoC store
When the invoice is submitted to the intake pipeline
Then the system shall perform case-insensitive filename comparison
And the invoice shall pass the PoC existence gate
```

**Rationale:** Filename matching must be case-insensitive to accommodate varying file naming conventions from upstream systems. This is a design decision that requires user confirmation.

---

#### Scenario 5: Invoice number contains special characters

```gherkin
Given an invoice submission with invoice number "INV-2026-0042-EU"
And a PoC file named "INV-2026-0042-EU.pdf" exists in the PoC store
When the invoice is submitted to the intake pipeline
Then the system shall match the full invoice number including special characters
And the invoice shall pass the PoC existence gate
```

**Rationale:** System Identifiers may contain hyphens or other special characters. The matching algorithm must handle the full identifier string, not just alphanumeric prefix.

---

## 3. ISO 29148 Quality Attribute Validation

| Attribute | Assessment | Notes |
|-----------|------------|-------|
| **Unambiguous** | PASS | "Matching the PoC filename to the invoice number" is a concrete, deterministic operation. No vague terms present. |
| **Complete** | PARTIAL | The requirement specifies the matching mechanism (filename) and the rejection behaviour (Type A). It does not specify case-sensitivity, special character handling, or multi-match behaviour. These are addressed in acceptance criteria but should be confirmed by the user. |
| **Consistent** | PASS | No conflict with other requirements. RQ-002 and RQ-003 add business rule gates but do not modify the PoC gate behaviour. |
| **Verifiable** | PASS | All 5 acceptance criteria are directly testable. Given-When-Then structure ensures deterministic test cases. |
| **Feasible** | PASS | Filename matching is a standard file system operation. No unknown technology required. |
| **Traceable** | PASS | Traces to OPE-001 (business objective) and source document line 11 ("Bestandsnaam-matching"). |

**Quality Verdict:** PASS with minor completeness notes. The acceptance criteria resolve the partial completeness gaps, but the user should confirm the case-sensitivity and multi-match assumptions before implementation.

---

## 4. Design Decisions (Logged)

| Decision | Rationale | Stakeholder Authority | Assumptions Dependent |
|----------|-----------|----------------------|----------------------|
| D-001: Filename matching is case-insensitive | Accommodates varying file naming conventions from upstream systems. | Pending user confirmation | None (this is a new assumption for verification) |
| D-002: Multiple PoC files for one invoice are not an error condition | One match is sufficient. Deduplication is not required. | Pending user confirmation | None (this is a new assumption for verification) |
| D-003: The PoC store location is a configurable path or storage bucket | No assumption made about shared filesystem vs object storage yet. | Architect decision (within WI-001 scope) | W-005 (Domain Model) must define the PoC entity but not its storage location |

---

## 5. Out of Scope

The following are explicitly excluded from WI-001:

- PoC content validation (OCR date extraction, temporal rule checking, correspondence content verification) — confirmed out of scope in Session 1
- PoC file storage mechanism (who provides the PoC files, where they are stored) — external dependency
- Business rule checks (RQ-002 Uncooperative Register, RQ-003 Payment Plan) — handled in subsequent iterations
- Mandatory field validation — enforced at the form interface level, external to Gimme
- Case analyst batch acceptance interface (RQ-004) — handled in W-003

---

## 6. Implementation Notes

### Input Contract

The intake pipeline receives from the upstream form system:
- Invoice number (System Identifier) — mandatory, used for PoC matching
- Debtor name
- Address
- Bank account number (rekeningnummer)
- Phone number

All fields are guaranteed mandatory by the upstream form. Gimme does not validate their presence.

### Output Contract

After PoC existence verification, the invoice is in one of two states:
1. **PoC verified** — queued for case analyst batch acceptance (proceeds to business rule checks)
2. **Type A rejected** — rejection recorded, client may re-submit

### Matching Algorithm

The matching algorithm performs:
```
poCFileName.toLowerCase() == invoiceNumber.toLowerCase()
```

This is a full-string match after case normalisation. No substring matching, no pattern matching, no fuzzy matching.

### Data Model Requirements

The following entities are needed (see W-005 for full model):

- **Invoice** — contains: invoiceNumber (System Identifier), debtorName, address, bankAccountNumber, phoneNumber, poCStatus (VERIFIED / MISSING), rejectionType (A / B / NONE), status (QUEUED / REJECTED / ACCEPTED)
- **PoC** — contains: filename, linkedInvoiceNumber

No database persistence is required for this work item alone. The PoC store is accessed as a file system or object storage resource.

---

## 7. Dependencies

| Dependency | Status | Notes |
|------------|--------|-------|
| W-005 (Domain Model) | Parallel | The Invoice entity definition is needed but can proceed in parallel. The minimum entity definition (fields: invoiceNumber, poCStatus) is sufficient for WI-001. |
| W-006 (Assumption Verification) | Parallel | No dependency. WI-001 does not touch external registries. |
| Upstream form system | External | Produces invoice submissions with mandatory fields. Interface contract must be discovered by the architect. |
| PoC file storage | External | Produces PoC files. Location and access mechanism must be discovered. |

---

## 8. Test Strategy

### Unit Tests

- Filename matching: exact match, case-insensitive match, no match, special character match
- Type A rejection: rejection reason recorded correctly, client re-submission allowed
- Invoice state transitions: Received → PoC verified → Queued; Received → Type A rejected

### Integration Tests

- Full intake pipeline with mocked PoC store
- Verify that a PoC-missed invoice does not reach the case analyst queue
- Verify that a PoC-passed invoice reaches the case analyst queue

### Test Data

Required test data:
| Test Case | Invoice Number | PoC File Available | Expected Outcome |
|-----------|---------------|-------------------|-----------------|
| Happy path | INV-2026-0042 | INV-2026-0042.pdf | PoC verified, queued |
| No PoC | INV-2026-0043 | none | Type A rejected |
| Case variation | INV-2026-0044 | inv-2026-0044.pdf | PoC verified, queued |
| Special chars | INV-2026-0045-EU | INV-2026-0045-EU.pdf | PoC verified, queued |
| Multi-match | INV-2026-0046 | INV-2026-0046.pdf, INV-2026-0046-copy.pdf | PoC verified, queued |

---

## 9. Risks and Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|-----------|------------|
| PoC files use a different naming convention than invoice numbers | WI-001 matching fails, causing false Type A rejections | Medium | Confirm PoC naming convention with the team. If a transformation or mapping is needed, this must be escalated to the architect before implementation. |
| PoC file names contain path separators or special characters | Filename matching may break if the matching algorithm does not sanitise input | Low | The matching algorithm uses exact string comparison after case normalisation. If PoC filenames include directory paths, the algorithm must extract the filename component only. |
| Upstream form system sends invoice numbers with leading/trailing whitespace | Matching fails on whitespace mismatch | Low | Trim whitespace from invoice number before matching. This is a minor defensive coding practice. |

---

## 10. Definition of Done

- [ ] All 5 Gherkin scenarios implemented as automated tests
- [ ] Unit test coverage for filename matching logic: minimum 80%
- [ ] Integration test for full intake pipeline with mocked PoC store
- [ ] Invoice entity model defined (W-005 parallel task)
- [ ] Code review completed
- [ ] No regression in existing business service tests
- [ ] Acceptance criteria reviewed and approved by product owner or case analyst stakeholder

---

**Work Item Author:** Robbie (Requirements Engineer)
**Review Status:** APPROVED — All design decisions D-001, D-002, D-003 confirmed by stakeholder
**Next Step:** Archibald produces delegation plan; Naut implements backend logic
