# Work Items: Gimme Vetting Process Automation

**Source:** re-workspace/requirements-spec.md (ISO 29148 specification)
**Created:** 2026-07-06
**Framework:** Work items decomposed by architectural concern, not by requirement ID. Dependencies are explicit.

---

## W-001: External Registry Interface Definition

**Type:** Interface specification
**Priority:** Must have
**Dependencies:** None
**Status:** Not started

### Purpose
Define the interface contract for two external data sources that Gimme queries during invoice intake. Both sources are provided by different company entities. Gimme does not own or maintain these registries.

### Scope
The architect shall design an interface abstraction that supports:
- Synchronous query against Uncooperative Register (used by RQ-002)
- Synchronous query against Payment Plan registry (used by RQ-003)
- Graceful degradation when either registry is unavailable: the interface must return a failure indicator that triggers the warning logged by RQ-005, and the invoice must continue processing without the check
- A mock or stub implementation for testing purposes

### Input
- RQ-002 requirement text (re-workspace/requirements-spec.md, lines 69-85)
- RQ-003 requirement text (re-workspace/requirements-spec.md, lines 89-104)
- RQ-005 requirement text (re-workspace/requirements-spec.md, lines 122-130)
- Stakeholder decision: registries are external, provided by different company entities, Gimme accepts via defined interface or mock

### Output
- Interface definition (API contract, data structures, error responses)
- Protocol and communication pattern selection justification
- Mock implementation specification
- Integration points documented in architectural model

### Assumptions to Verify (from Assumption Log)
- AUNV-001: Uncooperative Register exists as a data source (Status: Unverified)
- AUNV-002: Payment plan registry exists as a data source (Status: Unverified)
- The architect must confirm whether these are live systems to integrate with, or future systems requiring mock-only design for now

### Architectural Decisions Required
- Synchronous versus asynchronous query pattern (RQ-002 explicitly states synchronous; RQ-003 does not specify — architect must justify)
- Timeout policy for registry calls
- Retry strategy (if any) when a registry is temporarily unavailable
- Format of the query payload: what debtor identifiers are sent (name? account number? both?)
- Format of the response payload: what data is returned to trigger rejection

---

## W-002: Invoice Intake and PoC Matching Engine

**Type:** Core processing component
**Priority:** Must have
**Dependencies:** None (but depends on file upload system being available externally)
**Status:** Not started

### Purpose
Implement the invoice intake pipeline that verifies Proof of Correspondence existence and applies business rule checks (Uncooperative Register, Payment Plan) before presenting accepted invoices to case analysts.

### Scope
The architect shall design a component or pipeline that:
- Accepts an invoice submission (with invoice number as System Identifier, debtor name, address, bank account number, phone number — all provided by the upstream form system)
- Verifies PoC existence by matching the invoice number to a PoC filename (RQ-001)
- On PoC match: proceeds to business rule checks (RQ-002, RQ-003)
- On PoC mismatch: rejects the invoice as Type A (Missing PoC)
- On business rule rejection (Uncooperative Register match or active Payment Plan): rejects the invoice as Type B (not re-applicable)
- On no business rule rejection: queues the invoice for case analyst batch acceptance (RQ-004)

### Input
- RQ-001 requirement text (re-workspace/requirements-spec.md, lines 56-66)
- RQ-002 requirement text (re-workspace/requirements-spec.md, lines 69-85)
- RQ-003 requirement text (re-workspace/requirements-spec.md, lines 89-104)
- RQ-004 requirement text (re-workspace/requirements-spec.md, lines 108-117)

### Output
- Component design for invoice intake pipeline
- PoC filename matching strategy (exact match, pattern matching, case sensitivity)
- Rejection routing logic (Type A versus Type B)
- Integration points with W-001 (External Registry Interface)
- Data flow diagram showing invoice state transitions

### Architectural Decisions Required
- PoC filename matching algorithm (exact, partial, regex)
- File storage location for PoC documents (shared filesystem? object storage?)
- How the intake pipeline receives invoice data from the upstream form system
- Internal data model for invoices (what fields are stored, what state transitions are tracked)

---

## W-003: Case Analyst Batch Acceptance Interface

**Type:** User-facing interface
**Priority:** Must have
**Dependencies:** W-002 (PoC Matching Engine produces accepted invoices)
**Status:** Not started

### Purpose
Design the mechanism through which case analysts receive and manually accept invoices for batch inclusion.

### Scope
The architect shall design:
- A mechanism (UI, API, or internal queue) that presents non-rejected invoices to case analysts
- Case analyst decision logic: whether each invoice enters the current processing batch or a later one
- The output mechanism that produces debtor dossiers (the system's primary output per the specification)

### Input
- RQ-004 requirement text (re-workspace/requirements-spec.md, lines 108-117)
- Specification statement: "Gimme produces debtor dossiers as its primary output. A debtor dossier contains all invoices that have been accepted for a debtor after passing automated validation."

### Output
- Interface design (or integration specification if this is a front-end task for a separate team)
- Data structure for batch assignment
- Debtor dossier output format and delivery mechanism

### Architectural Decisions Required
- Is this a web UI, a CLI tool, or an API consumed by another system?
- How are debtor dossiers assembled (by debtor key? what constitutes the debtor key?)
- How and where are debtor dossiers delivered or stored after production
- What happens to rejected invoices (Type A re-submitted invoices are queued for batch processing)

### Out of Scope (confirmed by requirements specification)
- Post-acceptance workflow beyond dossier production
- Automatic debtor data enrichment

---

## W-004: Warning Logging Infrastructure

**Type:** Internal system capability
**Priority:** Must have (per spec, though Decision Log notes demotion consideration)
**Dependencies:** W-001 (External Registry Interface)
**Status:** Not started

### Purpose
Provide audit trail logging when external data sources are unavailable during invoice intake.

### Scope
The architect shall design:
- A logging mechanism that records warnings when Uncooperative Register or Payment Plan registry is unavailable
- Log destination: audit trail or system log accessible by administrators
- Integration point with W-001 to receive availability failure signals

### Input
- RQ-005 requirement text (re-workspace/requirements-spec.md, lines 122-130)
- Decision Log note: priority adjusted to lower by user, may be omitted until priority is raised

### Output
- Logging infrastructure design (structured log format, destination, retention)
- Integration points
- Decision on whether this is a shared logging framework or a targeted solution

### Notes
This requirement produces no direct stakeholder-visible output. It is an internal audit mechanism. The architect may recommend leveraging an existing logging infrastructure rather than building a new one.

---

## W-005: Domain Model and Ubiquitous Language

**Type:** Conceptual design
**Priority:** Must have
**Dependencies:** None
**Status:** Not started

### Purpose
Establish the domain model and data structures that underpin all other work items.

### Scope
The architect shall produce:
- Entity model for: Invoice, Debtor, PoC (Proof of Correspondence), Uncooperative Register entry, Payment Plan entry
- Entity relationships and cardinality
- State model for invoices (submitted, PoC verified, business rule check passed, rejected Type A, rejected Type B, accepted by analyst, included in batch, part of debtor dossier)
- Alignment with glossary terms in re-workspace/glossary.md

### Input
- re-workspace/glossary.md (11 domain terms)
- re-workspace/models/invoice-state-diagram.mmd (existing state diagram — architect should review for consistency)
- All requirement texts

### Output
- Class diagram or entity-relationship diagram
- State transition model for invoice entity
- Ubiquitous language alignment document (confirming terminology consistency with glossary)

---

## W-006: Assumption Verification and Risk Register

**Type:** Discovery and risk assessment
**Priority:** Must have before implementation begins
**Dependencies:** None
**Status:** Not started

### Purpose
Systematically verify all unverified assumptions from the requirement elicitation phase and produce a risk register for the architect.

### Items to Verify

**AUNV-001: Uncooperative Register existence**
- Status: Unverified
- Question: Is this a live, queryable system? What is its current interface?
- Risk if false: W-001 and W-002 cannot proceed with real integration; mock-only design required

**AUNV-002: Payment plan registry existence**
- Status: Unverified
- Question: Is this a live, queryable system? What is its current interface?
- Risk if false: W-001 and W-002 cannot proceed with real integration; mock-only design required

### Input
- re-workspace/assumption-log.md (two unverified entries)

### Output
- Verification status for each assumption
- Risk register with mitigation strategies
- Impact assessment on work item scheduling

---

## Work Item Dependency Graph

```
W-005 (Domain Model) ──────────────────────────────────────────────────────────────┐
                                                                                     │
W-006 (Assumption Verification) ────────────────────────────────────────────────────┤
                                                                                     │
W-001 (External Registry Interface) ────────────────────┐                            │
                                                          │                            │
W-002 (PoC Matching Engine) ────────────────────────────┤                            │
                                                          ▼                            │
W-004 (Warning Logging) ◄─────────────────────────────────┤                            │
                                                              │                        │
W-003 (Batch Acceptance Interface) ◄────────────────────────┼────────────────────────┤
```

W-005 and W-006 are independent and can proceed in parallel from project start.
W-001 depends only on W-005 domain model completion (to know what identifiers to send in queries).
W-002 depends on W-001 (needs the registry interface) and W-005.
W-004 depends on W-001 (needs the registry availability signal).
W-003 depends on W-002 (needs accepted invoices from the pipeline).

---

## Open Items for the Architect

| ID | Item | Source | Action Required |
|----|------|--------|-----------------|
| OQ-005 | Non-functional requirements are deferred | re-workspace/open-questions.md, line 44-46 | Architect should flag NFR gaps to user before design proceeds. Security, audit, availability, and data retention are likely necessary for financial/legal data. |
| OQ-007 | Rejection overview requirement (RQ-006) was removed | re-workspace/assumption-log.md, line 44-46 | Confirm with user whether a rejection overview is still needed. The assumption log marks this as moot. |
| OQ-008 | Priority of RQ-005 (Warning Logging) was demoted | re-workspace/decision-log.md, line 44-46 | Architect should confirm whether to include this in the current design scope. |
| OQ-009 | Form system that enforces mandatory fields is external to Gimme | re-workspace/requirements-spec.md, lines 23-31 | Architect must design the intake interface to match whatever format the form system produces. Interface contract must be discovered. |

---

## Constraints on Architectural Design

1. The architect must not add requirements beyond those in RQ-001 through RQ-005. Any additional capability must be flagged as scope expansion.
2. The architect must not assume the existence of external data sources. W-006 verification must complete before real integration is designed.
3. The architect must not design post-acceptance workflows. The system output is debtor dossiers. What happens after is out of scope.
4. The architect must not design mandatory field validation. That is enforced at the form level, external to Gimme.
5. Non-functional requirements are deferred. The architect should document NFR assumptions and flag gaps but must not design NFR capabilities without user confirmation.
