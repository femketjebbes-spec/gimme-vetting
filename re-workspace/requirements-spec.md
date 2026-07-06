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

## Mandatory Field Enforcement (External)

The following fields are enforced as mandatory at the form interface level (outside Gimme). Incomplete submissions are blocked at the form and do not enter the system:
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

## Requirements Summary

| ID | Description | Priority | Type | Dependency |
|----|-------------|----------|------|------------|
| RQ-001 | PoC Existence Verification | Must have | Type A (Missing PoC) | File upload system |
| RQ-002 | Uncooperative Register Check | Must have | Type B (Business Rule) | External registry (provided during implementation) |
| RQ-003 | Payment Plan Check | Must have | Type B (Business Rule) | External registry (provided during implementation) |
| RQ-004 | Batch Acceptance by Case Analyst | Must have | N/A (Post-acceptance) | Case management system |
| RQ-005 | Warning Logging for Unavailable Data Sources | Must have | N/A | System logging infrastructure |

---

## Open Questions (Remaining)

| ID | Question | Status |
|----|----------|--------|
| OQ-001 | What is the current state of the Uncooperative Register as a data source? | Resolved - external entity provides during implementation |
| OQ-002 | What is the current state of the payment plan database (incassolijst) as a data source? | Resolved - external entity provides during implementation |
| OQ-003 | What fields are mandatory for dossier submission? | Resolved - enforced externally by form: name, address, rekeningnummer, phone, invoice number |
| OQ-004 | What is the downstream workflow for invoices after case analyst batch acceptance (PoC validation and beyond)? | Resolved - out of scope; system output is debtor dossiers |
| OQ-005 | What non-functional requirements apply (security, audit, availability, data retention)? | Deferred - NFRs deferred to future session |
| OQ-006 | What is the error handling mechanism when data sources are unavailable during intake? | Resolved - log warning and proceed |
