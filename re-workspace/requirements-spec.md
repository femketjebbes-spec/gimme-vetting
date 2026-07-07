# Functional Requirements Specification: Gimme Vetting Process Automation

**Source Document:** modelling session artifacts/wat NotebookLM te zeggen had.docx
**Framework:** ISO 29148
**Business Objective:** OPE-001 — Reduce case analyst workload by automating repetitive validation tasks in the vetting intake process.
**Session:** 1, 2026-07-03 | Session 2, 2026-07-06

---

## Out of Scope

The following requirements from the source document are explicitly out of scope:
- PoC temporal rule validation (interval between contact moments)
- PoC temporal rule validation (age of first contact moment)
- PoC content status checking (rejected historical records)
- OCR/datetime extraction from PoC documents
- Any PoC content analysis beyond file existence and linkage
- Automatic debtor data enrichment (RQ-005, removed in Session 2)

---

## Mandatory Field Enforcement (Internal)

The following fields are enforced as mandatory by Gimme at the point of intake. Incomplete rows are returned to the client with missing fields flagged for correction. This applies to both the single-invoice API intake (POST /api/v1/intake) and the Excel batch intake (Session 5).

- Debtor name
- Address
- Bank account number (rekeningnummer)
- Phone number
- Invoice number (System Identifier) — used for PoC filename matching (RQ-001)

---

## System Output

Gimme produces **debtor dossiers** as its primary output.
- A debtor dossier contains all invoices that have been accepted for a debtor after passing automated validation.
- What happens after a debtor dossier is produced is out of scope.

---

## Rejection Classification

Gimme supports two types of invoice rejection:

**Type A — Missing PoC:** The invoice entered the system but has no linked Proof of Correspondence (PoC). The invoice number provided by the client is used to match the PoC filename. If no matching PoC is found, the invoice is rejected.
- The client may re-submit the invoice at any time, without limit on attempts.
- Re-submitted invoices are queued for batch processing. The case analyst determines during manual acceptance whether the invoice enters the current batch or a later one.

**Type B — Business Rule:** The invoice was rejected because Gimme does not accept the case (Uncooperative Register match, active payment plan).
- The invoice is not re-applicable. The client cannot re-submit the same invoice.

---

## Requirements

### RQ-001: PoC Existence Verification

Gimme shall verify that a Proof of Correspondence (PoC) document exists and is linked to the invoice at the point of intake.
- Verification shall be performed by matching the PoC filename to the invoice number (System Identifier) provided by the client.
- If no PoC is linked, the invoice shall be rejected automatically (Type A — Missing PoC).

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Source document, line 11 ("Bestandsnaam-matching")
**Verification Method:** Test that submitting an invoice without a matching PoC file results in automatic rejection.
**Priority:** Must have

---

### RQ-002: Uncooperative Register Check

Gimme shall attempt a synchronous check against the Uncooperative Register when an invoice is submitted.
- If the Uncooperative Register is available and the debtor is listed in it, Gimme shall reject the invoice automatically without human intervention.
- The check shall be performed immediately upon invoice receipt.
- This rejection is Type B (Business Rule) — the invoice is not re-applicable.
- If the Uncooperative Register is unavailable or does not exist, Gimme shall log a warning and proceed with the invoice without this check.
- The Uncooperative Register is an external data source provided by a different company entity. Gimme shall accept the registry via a defined interface or mock implementation for testing.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Source document, line 21 ("Systeemcontrole op het register") and line 91-95
**Verification Method:** 
1. Test that submitting an invoice for a debtor listed in the Uncooperative Register results in automatic rejection (when available).
2. Test that submitting an invoice proceeds when the Uncooperative Register is unavailable (with a warning logged).
**Priority:** Must have (when data source available); informational (when unavailable)
**Dependency:** Uncooperative Register provided by external entity during implementation; Gimme must accept registry via defined interface or mock

---

### RQ-003: Payment Plan Check

Gimme shall attempt a check against the payment plan registry when a new dossier is submitted.
- The check shall query whether any active payment plan exists for the debtor associated with the invoice.
- If the payment plan registry is available and an active payment plan exists for the debtor, Gimme shall reject the invoice automatically without human intervention.
- Rejection is Type B (Business Rule) — the invoice is not re-applicable.
- If the payment plan registry is unavailable or does not exist, Gimme shall log a warning and proceed with the invoice without this check.
- The payment plan registry is an external data source provided by a different company entity. Gimme shall accept the registry via a defined interface or mock implementation for testing.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Source document, line 123 ("Conflictcheck") and line 125 ("Blocking Business Rule")
**Verification Method:** 
1. Test that submitting an invoice for a debtor with an active payment plan results in automatic rejection (when available).
2. Test that submitting an invoice proceeds when the payment plan registry is unavailable (with a warning logged).
**Priority:** Must have (when data source available); informational (when unavailable)
**Dependency:** Payment plan registry provided by external entity during implementation; Gimme must accept registry via defined interface or mock

---

### RQ-004: Batch Acceptance by Case Analyst

Gimme shall present non-rejected invoices to case analysts for manual acceptance.
- Case analysts shall determine, upon manual acceptance, whether each invoice enters the current processing batch or a later one.
- This acceptance step occurs after automated validation and is separate from the automated checks.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Session 2 clarification — case analysts manually accept invoices and determine batch inclusion
**Verification Method:** Test that non-rejected invoices are presented to case analysts for manual batch acceptance.
**Priority:** Must have

---

### RQ-005: Warning Logging for Unavailable Data Sources

Gimme shall log a warning whenever an external data source (Uncooperative Register or Payment Plan registry) is unavailable or does not exist during invoice intake.
- The warning shall be logged to an audit trail or system log accessible by administrators.
- The invoice shall continue processing without the unavailable check.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Session 2 clarification — log warning and proceed when external data source unavailable
**Verification Method:** Test that processing an invoice with an unavailable Uncooperative Register or Payment Plan registry results in a logged warning and continued processing.
**Priority:** Must have

---

### RQ-006: Excel Batch Intake

Gimme shall provide a client portal endpoint where external clients can upload an Excel file containing multiple invoices, one per row.

- Each row in the Excel file represents one invoice submission.
- The Excel file shall use a defined column structure matching the mandatory fields: invoiceNumber, debtorName, address, bankAccountNumber, phoneNumber.
- Gimme shall process each row independently, applying mandatory field validation (RQ-007) and PoC existence verification (RQ-001) per row.
- Rows that pass all validation gates are stored as invoices in QUEUED state.
- Rows that fail validation are not stored and are flagged for client correction.
- Processing is synchronous: the client uploads, Gimme processes, Gimme returns the result file in the same request cycle.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Session 5 — MVP client portal requirement
**Verification Method:** Upload an Excel file with known mix of valid and invalid rows; verify that valid rows are stored and invalid rows are returned in the result file.
**Priority:** Must have (MVP)

---

### RQ-007: Mandatory Field Validation (Per-Row)

Gimme shall validate each row of the uploaded Excel file for mandatory field completeness.

- The following fields are mandatory per row: debtorName, address, bankAccountNumber, phoneNumber, invoiceNumber.
- A row is considered incomplete if any of these fields is empty or missing.
- The client shall receive the incomplete rows back in the return Excel file (RQ-008) with the specific missing fields identified.
- Incomplete rows are not stored in the system.
- The client may re-submit corrected rows via the same Excel upload interface.
- Validation errors per row are recorded with the specific field name(s) that are missing.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Session 5 — case analysts do not want to manually check for empty fields
**Verification Method:** Upload an Excel file with rows missing different fields; verify that only incomplete rows appear in the return Excel.
**Priority:** Must have (MVP)

---

### RQ-008: Return Excel with Missing Data

Gimme shall produce a return Excel file containing only the rows that failed validation.

- The return Excel file shall include all original column data from each failing row (returned fully, not truncated).
- The return Excel file shall include an additional column indicating the validation issue per row: "MISSING_FIELDS" with a list of missing field names, or "MISSING_POC" for rows that passed mandatory field validation but had no matching PoC file.
- The return Excel file shall be available for download via the client portal at the end of the upload request cycle.
- The return Excel file shall be formatted such that the client can fill in missing fields directly within the file and re-upload.
- Rows that passed all validation gates are NOT included in the return Excel file.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Session 5 — client needs to see what is missing and re-upload
**Verification Method:** Upload an Excel file with 10 rows, 3 invalid (missing fields), 2 missing PoC, 5 valid. Verify return Excel contains exactly 5 rows (3 missing fields + 2 missing PoC).
**Priority:** Must have (MVP)

---

### RQ-009: Separate PoC Upload Endpoint

Gimme shall provide a client portal endpoint for uploading PoC files separately from the Excel invoice batch.

- The client can upload PoC files (PDF) via the client portal.
- Each uploaded PoC file must be named according to the convention: the invoice number (System Identifier) matches the PoC filename (case-insensitive, per D-001).
- Uploaded PoC files are stored in the same PoC store as the single-invoice intake path (configurable path, per D-003).
- The client portal shall display a list of invoice numbers that are missing PoC files (from the return Excel, RQ-008), enabling the client to upload the corresponding PoC files.
- PoC upload is independent of the Excel upload. The client receives the return Excel, then uploads the missing PoC files separately, then re-uploads the corrected Excel.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Session 5 — client needs separate PoC upload mechanism
**Verification Method:** Upload a PoC file named correctly; verify that a previously rejected invoice (missing PoC) would now pass the PoC existence gate.
**Priority:** Should have (deferred from MVP if necessary)

---

## Requirements Summary

| ID | Description | Priority | Type | Dependency |
|----|-------------|----------|------|------------|
| RQ-001 | PoC Existence Verification | Must have | Type A (Missing PoC) | File upload system |
| RQ-002 | Uncooperative Register Check | Must have | Type B (Business Rule) | External registry (provided during implementation) |
| RQ-003 | Payment Plan Check | Must have | Type B (Business Rule) | External registry (provided during implementation) |
| RQ-004 | Batch Acceptance by Case Analyst | Must have | N/A (Post-acceptance) | Case management system |
| RQ-005 | Warning Logging for Unavailable Data Sources | Must have | N/A | System logging infrastructure |
| RQ-006 | Excel Batch Intake | Must have (MVP) | N/A (Intake mechanism) | Client portal |
| RQ-007 | Mandatory Field Validation (Per-Row) | Must have (MVP) | N/A (Validation) | Excel parsing library |
| RQ-008 | Return Excel with Missing Data | Must have (MVP) | N/A (Output) | Excel generation library |
| RQ-009 | Separate PoC Upload Endpoint | Should have | N/A (Intake mechanism) | File storage system |

---

## Open Questions (Remaining)

| ID | Question | Status |
|----|----------|--------|
| OQ-001 | What is the current state of the Uncooperative Register as a data source? | Resolved - external entity provides during implementation |
| OQ-002 | What is the current state of the payment plan database (incassolijst) as a data source? | Resolved - external entity provides during implementation |
| OQ-003 | What fields are mandatory for dossier submission? | Resolved - enforced by Gimme at intake: name, address, rekeningnummer, phone, invoice number |
| OQ-004 | What is the downstream workflow for invoices after case analyst batch acceptance (PoC validation and beyond)? | Resolved - out of scope; system output is debtor dossiers |
| OQ-005 | What non-functional requirements apply (security, audit, availability, data retention)? | Deferred - NFRs deferred to future session |
| OQ-006 | What is the error handling mechanism when data sources are unavailable during intake? | Resolved - log warning and proceed |
| OQ-007 | What is the Excel file column structure for batch upload? | Resolved — Session 5: invoice number, debtor name, address, phone number, bank account number |
| OQ-008 | Does the Excel file have a header row? What is the column order? | Resolved — Session 5: header row optional, column order as listed above |
| OQ-009 | What Excel file format is required (.xlsx, .xls, .csv)? | Resolved — Session 5: supports .xlsx and .csv |
| OQ-010 | What is the maximum file size for the Excel upload? | Resolved — Session 5: no file size limit for MVP |
| OQ-011 | Does the client portal need authentication? | Resolved — Session 5: no authentication for MVP |

---

## Whiteboard Numbering Resolution

The MoSCoW whiteboard introduced a numbering collision and a redundant requirement. The following clarifications apply:

- **RQ-004 (Batch Acceptance by Case Analyst):** The whiteboard labels this requirement as RQ-005. The spec ID RQ-004 is correct. The whiteboard label is misnumbered. No spec change required.
- **RQ-005 (Warning Logging for Unavailable Data Sources):** The spec ID RQ-005 is correct and matches the whiteboard's "RQ5" label. The whiteboard places this in the Should have column while the spec classifies it as Must have. This discrepancy is noted but not resolved per user instruction.
- **RQ-007 (PoC Existence Verification):** The whiteboard introduces RQ-007 in the Must have column. This requirement is functionally identical to RQ-001 (PoC Existence Verification). RQ-007 is documented as redundant. No new spec entry is created. If RQ-007 was intended to describe a different mechanism (e.g., content-based PoC validation rather than filename matching), it must be re-specified with a distinct description.
