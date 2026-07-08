# Work Items: Gimme Vetting Process Automation

**Source:** re-workspace/requirements-spec.md (ISO 29148 specification)
**Created:** 2026-07-06
**Framework:** Work items decomposed by architectural concern, not by requirement ID. Dependencies are explicit.

---

## Folder Structure

Work items are organized into two MVP tracks:

| Folder | Track | Scope |
|--------|-------|-------|
| [`MVP-1-Client/`](re-workspace/work-items/MVP-1-Client/) | Client-facing intake | Excel upload, PoC verification, mandatory field validation, return Excel, PoC upload |
| [`MVP-1-Case-analyst/`](re-workspace/work-items/MVP-1-Case-analyst/) | Case analyst view | Read-only invoice list, detail view, dashboard frontend |

---

## Case Analyst Track: MVP-1-Case-analyst

A fully organised work item definition exists at:
- **Folder:** [re-workspace/work-items/MVP-1-Case-analyst/](re-workspace/work-items/MVP-1-Case-analyst/)
- **WI-CA-001:** [Analyst Invoice List & Detail API](re-workspace/work-items/MVP-1-Case-analyst/wi-ca-001-analyst-api.md)
- **WI-CA-002:** [Analyst Read-Only Dashboard Frontend](re-workspace/work-items/MVP-1-Case-analyst/wi-ca-002-frontend-dashboard.md)

**Scope:** Read-only invoice listing with filtering, search, and detail drawer. No write actions for MVP.

---

## Client Track: MVP-1-Client

### First Implementation Work Item: WI-001 (PoC Existence Verification)

**MVP:** [MVP-1](work-items/MVP-1/)

A fully formalised work item definition exists at:
<<<<<<< HEAD
- **File:** [re-workspace/work-items/MVP-1-Client/wi-001-poc-existence-verification.md](re-workspace/work-items/MVP-1-Client/wi-001-poc-existence-verification.md)
=======
- **File:** [re-workspace/work-items/MVP-1/wi-001-poc-existence-verification.md](work-items/MVP-1/wi-001-poc-existence-verification.md)
>>>>>>> 52e0cf2 (work items iets aangepast)
- **Parent Requirement:** RQ-001 (PoC Existence Verification)
- **Parent Work Stream:** W-002 (Invoice Intake and PoC Matching Engine)
- **Status:** Ready for implementation
- **Gherkin Scenarios:** 5 (2 pass scenarios, 3 edge-case scenarios)
- **Quality Validation:** ISO 29148 — PASS (with minor completeness notes resolved by acceptance criteria)

This work item is the recommended starting point for implementation. See the formalised definition for full details on acceptance criteria, design decisions, test strategy, and risks.

---

## WI-006: Project Build Orchestration

**MVP:** [MVP-1](work-items/MVP-1/)

A fully formalised work item definition exists at:
<<<<<<< HEAD
- **File:** [re-workspace/work-items/MVP-1-Client/wi-006-project-build-orchestration.md](re-workspace/work-items/MVP-1-Client/wi-006-project-build-orchestration.md)
=======
- **File:** [re-workspace/work-items/MVP-1/wi-006-project-build-orchestration.md](work-items/MVP-1/wi-006-project-build-orchestration.md)
>>>>>>> 52e0cf2 (work items iets aangepast)
- **Parent Requirement:** None (cross-cutting infrastructure requirement)
- **Type:** Build infrastructure
- **Priority:** High
- **Dependencies:** None
- **Status:** Not started

This work item addresses the absence of a root-level build orchestration mechanism. The project has two independently-built subprojects (frontend via Vite/npm, backend via Maven multi-module) but no single entry point to build, test, or clean the entire solution.

**Recommended approach:** GNU Make Makefile at the project root with targets for `build`, `test`, `clean`, `backend`, `frontend`, and `check-tools`.

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

## MVP Excel Intake Work Items (Session 5)

These work items address the Excel batch intake pipeline required for the MVP client portal.

### WI-002: Excel File Upload and Parsing

**Parent Requirement:** RQ-006 (Excel Batch Intake)
**Work Stream:** W-007 (Excel Batch Intake Pipeline)
**Status:** Not started
**Dependencies:** None
**Priority:** Must have (MVP)

**Purpose:** Define the Excel file upload interface and parsing logic that reads invoice data from an Excel file.

**Scope:**
- POST endpoint for Excel file upload (single file, defined column structure)
- Excel parsing using a library (Apache POI or similar) to read rows into domain objects
- Column-to-field mapping: invoiceNumber, debtorName, address, bankAccountNumber, phoneNumber
- Validation that each row contains the expected number of columns
- Maximum file size enforcement
- Error handling for malformed Excel files (corrupted, non-Excel format)

**Output:**
- API contract for Excel upload endpoint (to be produced by Gerard)
- Excel file column schema (to be confirmed with user: column names, order, header row presence)
- Excel format specification (.xlsx, .xls, or .csv)
- Parsing logic design

**Assumptions to Verify:**
- Excel format: .xlsx vs .xls vs .csv
- Whether the file has a header row
- Maximum file size
- Column order and names

---

### WI-003: Per-Row Mandatory Field Validation

**Parent Requirement:** RQ-007 (Mandatory Field Validation)
**Work Stream:** W-007 (Excel Batch Intake Pipeline)
**Status:** Not started
**Dependencies:** WI-002 (Excel parsing produces row objects)
**Priority:** Must have (MVP)

**Purpose:** Validate each row of the parsed Excel file for mandatory field completeness.

**Scope:**
- For each row, check: debtorName, address, bankAccountNumber, phoneNumber, invoiceNumber are non-empty
- Record which specific fields are missing per failing row
- Do NOT store failing rows in the database
- Group failing rows for the return Excel (RQ-008)
- Pass passing rows to the next validation gate (PoC existence, RQ-001)

**Output:**
- Validation logic implementation
- Per-row validation result data structure (row index, list of missing field names)
- Unit tests for each validation scenario

**Note:** This requirement was previously external. The specification has been updated (Session 5) to move it internal.

---

### WI-004: Return Excel Generation

**Parent Requirement:** RQ-008 (Return Excel with Missing Data)
**Work Stream:** W-007 (Excel Batch Intake Pipeline)
**Status:** Not started
**Dependencies:** WI-002 (parsing), WI-003 (validation results)
**Priority:** Must have (MVP)

**Purpose:** Generate a return Excel file containing only the rows that failed validation, with missing fields identified.

**Scope:**
- Return Excel contains only failing rows (not all rows)
- Each failing row is returned with all original column data intact
- An additional column identifies the validation issue: "MISSING_FIELDS" with field names listed, or "MISSING_POC" for rows that passed mandatory field validation but had no matching PoC file
- Excel format matches the upload format (same column structure)
- File is returned to the client as a download link in the client portal
- Passing rows are NOT included in the return Excel

**Output:**
- Excel generation logic (Apache POI or similar)
- Return Excel format specification
- Download link mechanism in the client portal response

---

### WI-005: PoC File Upload Endpoint

**Parent Requirement:** RQ-009 (Separate PoC Upload)
**Work Stream:** W-007 (Excel Batch Intake Pipeline)
**Status:** Not started
**Dependencies:** WI-001 (existing PoCStoreService), W-002 (PoC Matching Engine already has PoC store)
**Priority:** Should have (deferred from MVP if necessary)

**Purpose:** Provide a client portal endpoint for uploading PoC files separately from the Excel invoice batch.

**Scope:**
- POST endpoint for PoC file upload (PDF)
- Filename validation: must match invoice number format per D-001 (case-insensitive)
- Store in the same PoC store as the single-invoice intake path (configurable path per D-003)
- Display list of invoice numbers missing PoC files (from return Excel, RQ-008) in the client portal UI

**Output:**
- API contract for PoC upload endpoint
- PoC file storage integration with existing FileBackedPoCStoreService
- Client portal UI specification for PoC upload screen

**Note:** The client portal UI is outside Robbie's RE scope. The architect will produce the interface design.

---

### MVP Work Item Dependency Graph

```
WI-002 (Excel Upload and Parsing) ──┐
                                       │
WI-003 (Per-Row Validation) ◄─────────┤
                                       ├──► WI-004 (Return Excel Generation)
WI-001 (PoCStoreService) ────────────────────────►
                                       │
WI-005 (PoC Upload Endpoint) ───────────────► (parallel, after WI-001)
```

WI-002 is the entry point. WI-003 depends on WI-002 output. WI-004 depends on both WI-002 and WI-003. WI-005 can proceed in parallel with WI-002 through WI-004 once WI-001 is complete.

---

## Open Items for the Architect

| ID | Item | Source | Action Required |
|----|------|--------|-----------------|
| OQ-005 | Non-functional requirements are deferred | re-workspace/open-questions.md, line 44-46 | Architect should flag NFR gaps to user before design proceeds. Security, audit, availability, and data retention are likely necessary for financial/legal data. |
| OQ-007 | Rejection overview requirement (RQ-006) was removed | re-workspace/assumption-log.md, line 44-46 | Confirm with user whether a rejection overview is still needed. The assumption log marks this as moot. |
| OQ-008 | Priority of RQ-005 (Warning Logging) was demoted | re-workspace/decision-log.md, line 44-46 | Architect should confirm whether to include this in the current design scope. |
| OQ-009 | Excel file column schema | re-workspace/requirements-spec.md, OQ-007 through OQ-010 | Architect must confirm column names, order, header row presence, and file format (.xlsx/.xls/.csv) before design proceeds. |
| OQ-010 | Client portal authentication | re-workspace/requirements-spec.md, OQ-011 | Architect must confirm whether authentication is required for the client portal before design proceeds. |

---

## Constraints on Architectural Design

1. The architect must not add requirements beyond those in RQ-001 through RQ-009. Any additional capability must be flagged as scope expansion.
2. The architect must not assume the existence of external data sources (RQ-002, RQ-003). W-006 verification must complete before real integration is designed.
3. The architect must not design post-acceptance workflows. The system output is debtor dossiers. What happens after is out of scope.
4. The architect must design mandatory field validation internally. The specification was updated in Session 5: mandatory field enforcement is no longer external.
5. Non-functional requirements are deferred. The architect should document NFR assumptions and flag gaps but must not design NFR capabilities without user confirmation.
6. The architect must produce UI specifications for the client portal as integration with the frontend agent. The architect does not implement frontend code.
