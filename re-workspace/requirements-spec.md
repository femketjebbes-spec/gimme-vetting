# Functional Requirements Specification: Gimme Vetting Process Automation

**Source Document:** modelling session artifacts/wat NotebookLM te zeggen had.docx
**Framework:** ISO 29148
**Business Objective:** OPE-001 — Reduce case analyst workload by automating repetitive validation tasks in the vetting intake process.
**Session:** 1, 2026-07-03

---

## Out of Scope

The following requirements from the source document are explicitly out of scope:
- PoC temporal rule validation (interval between contact moments)
- PoC temporal rule validation (age of first contact moment)
- PoC content status checking (rejected historical records)
- OCR/datetime extraction from PoC documents
- Any PoC content analysis beyond file existence and linkage

---

## Requirements

### RQ-001: PoC Existence Verification

Gimme shall verify that a Proof of Correspondence (PoC) document exists and is linked to the invoice at the point of intake.
- Verification shall be performed by matching the PoC filename to the invoice number.
- If no PoC is linked, the invoice shall be rejected automatically.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Source document, line 11 ("Bestandsnaam-matching")
**Verification Method:** Test that uploading an invoice without an associated PoC file results in automatic rejection.
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

Gimme shall perform a synchronous check against the Uncooperative Register database when an invoice is submitted.
- If the debtor is listed in the Uncooperative Register, Gimme shall reject the invoice automatically without human intervention.
- The check shall be performed immediately upon invoice receipt.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Source document, line 21 ("Systeemcontrole op het register") and line 91-95
**Verification Method:** Test that submitting an invoice for a debtor listed in the Uncooperative Register results in automatic rejection.
**Priority:** Must have
**Dependency:** Uncooperative Register data source must be available and accessible

---

### RQ-004: Mandatory Field Validation

Gimme shall reject dossier submissions that contain empty mandatory fields.
- Mandatory fields shall be defined by the system configuration.
- The form interface shall enforce this restriction at the point of entry (preventing empty submissions from being submitted).
- This restriction applies to all required fields including the System Identifier and Debtor ID.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Source document, line 105 ("Het systeem weigert de indiening van dossiers indien verplichte velden leeg zijn")
**Verification Method:** Test that submitting a dossier with any mandatory field empty results in rejection.
**Priority:** Must have

---

### RQ-005: Automatic Debtor Data Enrichment

Gimme shall automatically retrieve debtor information from internal and external databases when a Debtor ID is provided during dossier submission.
- The data enrichment call shall be triggered automatically upon Debtor ID entry.
- Retrieved data shall be populated into the dossier without manual intervention.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Source document, line 107 ("Triggering Logic voor Verrijking")
**Verification Method:** Test that providing a valid Debtor ID triggers automatic data retrieval and population of debtor fields.
**Priority:** Should have
**Dependency:** Internal and/or external database APIs must be available

---

### RQ-006: Incomplete Debtor Data Handling

Gimme shall handle dossiers where debtor information remains incomplete after automatic enrichment (RQ-005).
- The dossier shall be marked as "incomplete debtor information."
- The dossier shall enter a mandatory 1-month waiting period before manual enrichment or automated information request.
- The waiting period shall be tracked by the system.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Source document, line 109-113
**Verification Method:** Test that a dossier with incomplete debtor data after enrichment is marked appropriately and enters the waiting period.
**Priority:** Should have

---

### RQ-007: Payment Plan Check

Gimme shall perform a check against the payment plan database (incassolijst) when a new dossier is submitted.
- The check shall query whether any active payment plan exists for the debtor associated with the invoice.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Source document, line 123 ("Conflictcheck")
**Verification Method:** Test that submitting a dossier for a debtor with an active payment plan triggers a match.
**Priority:** Must have
**Dependency:** Payment plan database must be available and accessible

---

### RQ-008: Payment Plan Blocking Rule

Gimme shall reject any invoice for a debtor that has an active payment plan.
- Rejection shall be automatic and immediate upon detection of an active payment plan.
- The system shall not require human intervention to enforce this rejection.

**Business Objective:** OPE-001 (Reduce case analyst workload)
**Source:** Source document, line 125 ("Blocking Business Rule")
**Verification Method:** Test that submitting an invoice for a debtor with an active payment plan results in automatic rejection.
**Priority:** Must have
**Dependency:** Payment plan database must contain accurate, up-to-date payment plan status information

---

## Requirements Summary

| ID | Description | Priority | In-Scope | Dependency |
|----|-------------|----------|----------|------------|
| RQ-001 | PoC Existence Verification | Must have | Yes | File upload system |
| RQ-002 | System Identifier Requirement | Must have | Yes | Invoice data model |
| RQ-003 | Uncooperative Register Check | Must have | Yes | Uncooperative Register |
| RQ-004 | Mandatory Field Validation | Must have | Yes | Form system |
| RQ-005 | Automatic Debtor Data Enrichment | Should have | Yes | Database APIs |
| RQ-006 | Incomplete Debtor Data Handling | Should have | Yes | Case management system |
| RQ-007 | Payment Plan Check | Must have | Yes | Payment plan database |
| RQ-008 | Payment Plan Blocking Rule | Must have | Yes | Payment plan database |

## Open Questions (Remaining)

| ID | Question | Status |
|----|----------|--------|
| OQ-001 | What is the current state of the Uncooperative Register as a data source? | Open |
| OQ-002 | What is the current state of the payment plan database (incassolijst) as a data source? | Open |
| OQ-003 | What fields are mandatory for dossier submission? | Open |
| OQ-004 | What is the post-acceptance workflow for dossiers that pass all validation gates? | Open |
| OQ-005 | What non-functional requirements apply (security, audit, availability, data retention)? | Open |
| OQ-006 | What is the error handling mechanism when data sources are unavailable during intake? | Open |
