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

---

## System Output

Gimme produces **debtor dossiers** as its primary output.
- A debtor dossier contains all invoices that have been accepted for a debtor after passing automated validation.
- What happens after a debtor dossier is produced is out of scope.

---

## Rejection Classification

Gimme supports two types of invoice rejection:

**Type A — Incomplete Data:** The invoice was rejected because mandatory data fields are empty or enrichment could not fully complete the debtor information.
- The client may re-submit the invoice at any time, without limit on attempts.
- Re-submitted invoices are queued for batch processing. The case analyst determines during manual acceptance whether the invoice enters the current batch or a later one.

**Type B — Business Rule:** The invoice was rejected because Gimme does not accept the case (Uncooperative Register match, active payment plan).
- The invoice is not re-applicable. The client cannot re-submit the same invoice.

---

## Requirements

### RQ-001: PoC Existence Verification

Gimme shall verify that a Proof of Correspondence (PoC) document exists and is linked to the invoice at the point of intake.
- Verification shall be performed by matching the PoC filename to the invoice number.
- If no PoC is linked, the invoice shall be rejected automatically (Type A — Incomplete Data).

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Source document, line 11 ("Bestandsnaam-matching")
**Verification Method:** Test that submitting an invoice without an associated PoC file results in automatic rejection.
**Priority:** Must have

---

### RQ-002: System Identifier Requirement

Gimme shall reject any invoice that lacks a unique System Identifier.
- The System Identifier field shall be mandatory and cannot be empty.
- The System Identifier is required for database integrity.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Source document, line 65 ("Presence of a unique System Identifier on the invoice is a hard requirement for database integrity")
**Verification Method:** Test that submission of an invoice without a System Identifier results in rejection.
**Priority:** Must have

---

### RQ-003: Uncooperative Register Check

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

### RQ-004: Mandatory Field Validation

Gimme shall reject dossier submissions that contain empty mandatory fields.
- The following fields are mandatory for a debtor to be uniquely identifiable:
  - Debtor name (initials + surname are sufficient; full first name is preferred)
  - Address
  - Bank account number (rekeningnummer)
- The System Identifier field is mandatory (see RQ-002).
- The form interface shall enforce this restriction at the point of entry (preventing empty submissions from being submitted).
- This rejection is Type A (Incomplete Data) — the invoice is re-applicable.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Source document, line 105 ("Het systeem weigert de indiening van dossiers indien verplichte velden leeg zijn"); Session 2 clarification on unique debtor identification fields
**Verification Method:** Test that submitting a dossier with any mandatory field (debtor name, address, bank account number, system identifier) empty results in rejection.
**Priority:** Must have

---

### RQ-005: Automatic Debtor Data Enrichment

Gimme shall automatically retrieve debtor information from internal and external databases when a Debtor ID is provided during dossier submission.
- The data enrichment call shall be triggered automatically upon Debtor ID entry.
- Retrieved data shall be populated into the dossier without manual intervention.
- If enrichment fails to fully complete the debtor information, the invoice shall be rejected as Type A (Incomplete Data) and the client shall receive an overview indicating which invoices could not be auto-completed.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Source document, line 107 ("Triggering Logic voor Verrijking")
**Verification Method:** Test that providing a valid Debtor ID triggers automatic data retrieval and population of debtor fields.
**Priority:** Should have
**Dependency:** Internal and/or external database APIs must be available

---

### RQ-006: Rejection Overview for Clients

Gimme shall provide a client-facing overview listing invoices that were rejected as Type A (Incomplete Data) due to auto-completion failures.
- The overview shall be generated automatically upon rejection.
- The overview shall indicate which invoices could not be fully auto-completed and the specific missing information.
- The overview shall inform the client that they may re-submit the invoice at any time without limit.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Session 2 clarification — client receives an overview of invoices that could not be auto-completed
**Verification Method:** Test that a client submitting invoices that fail enrichment receives an overview listing the affected invoices and missing data.
**Priority:** Must have

---

### RQ-007: Batch Acceptance by Case Analyst

Gimme shall present non-rejected invoices to case analysts for manual acceptance.
- Case analysts shall determine, upon manual acceptance, whether each invoice enters the current processing batch or a later one.
- This acceptance step occurs after automated validation and is separate from the automated checks.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Session 2 clarification — case analysts manually accept invoices and determine batch inclusion
**Verification Method:** Test that non-rejected invoices are presented to case analysts for manual batch acceptance.
**Priority:** Must have

---

### RQ-008: Payment Plan Check

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

### RQ-009: Warning Logging for Unavailable Data Sources

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
| RQ-001 | PoC Existence Verification | Must have | Type A (Incomplete Data) | File upload system |
| RQ-002 | System Identifier Requirement | Must have | Type A (Incomplete Data) | Invoice data model |
| RQ-003 | Uncooperative Register Check | Must have | Type B (Business Rule) | External registry (provided during implementation) |
| RQ-004 | Mandatory Field Validation | Must have | Type A (Incomplete Data) | Form system |
| RQ-005 | Automatic Debtor Data Enrichment | Should have | Type A (Incomplete Data) | Database APIs |
| RQ-006 | Rejection Overview for Clients | Must have | Type A (Incomplete Data) | Reporting system |
| RQ-007 | Batch Acceptance by Case Analyst | Must have | N/A (Post-acceptance) | Case management system |
| RQ-008 | Payment Plan Check | Must have | Type B (Business Rule) | External registry (provided during implementation) |
| RQ-009 | Payment Plan Blocking Rule | Must have | Type B (Business Rule) | Payment plan registry data accuracy |

---

## Open Questions (Remaining)

| ID | Question | Status |
|----|----------|--------|
| OQ-001 | What is the current state of the Uncooperative Register as a data source? | Open |
| OQ-002 | What is the current state of the payment plan database (incassolijst) as a data source? | Open |
| OQ-003 | What fields are mandatory for dossier submission? | Open |
| OQ-004 | What is the downstream workflow for invoices after case analyst batch acceptance (PoC validation and beyond)? | Open |
| OQ-005 | What non-functional requirements apply (security, audit, availability, data retention)? | Open |
| OQ-006 | What is the error handling mechanism when data sources are unavailable during intake? | Open |
