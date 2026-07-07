even # Decision Log

Records requirements decisions with their rationale, the stakeholder authority behind them, and the assumptions they depend on. Traceable to entries in the Assumption Log.

Format per entry:
```
[YYYY-MM-DD] [Session N] DECISION: <statement>
Assumptions: <statement>
Rationale: <user-provided>
```

[2026-07-03] [Session 1] DECISION: The source document is treated as a functional requirements draft, not a complete specification.
Assumptions: Non-functional requirements may be needed but are not in scope for this review session.
Rationale: The document contains no security, performance, availability, or audit requirements. Its structure suggests an AI-generated draft rather than a formal specification. This review focuses on the four functional streams it describes.

[2026-07-03] [Session 1] DECISION: ISO 29148 is selected as the quality framework for this review.
Assumptions: None.
Rationale: The document contains formal-sounding system requirements ("Het systeem dient...", "Het systeem weigert..."). ISO 29148 criteria (unambiguous, complete, consistent, verifiable, feasible, traceable) are the appropriate lens for evaluating this type of specification.

[2026-07-03] [Session 1] DECISION: "Backwards chaining" terminology is flagged for review, not corrected.
Assumptions: The term may be used rhetorically rather than technically (see assumption-log.md entry for session 1).
Rationale: If the user intends a formal backwards chaining implementation, the system architecture and inference engine requirements would be fundamentally different. I flag this rather than assume.

[2026-07-03] [Session 1] DECISION: No decision was made on organisation name spelling.
Assumptions: None.
Rationale: This is a critical consistency issue that the user must resolve. I log it as an open question but make no editorial decision.

[2026-07-03] [Session 1] DECISION: PoC (Proof of Correspondence) content validity checking is out of scope. PoC existence verification is in scope.
Assumptions: None.
Rationale: User clarified in Session 1 that PoC content validation (OCR date extraction, temporal rule checking, correspondence content verification) is out of scope. However, the system must still verify that a PoC file exists and is associated with the invoice. This means file-level linkage (e.g., filename matching) is required, but content-level analysis is not.

[2026-07-06] [Session 2] DECISION: Automatic debtor data enrichment is removed from scope.
Assumptions: None.
Rationale: User clarified in Session 2 that data enrichment is no longer a requirement. Invoices that lack mandatory fields (name, address, rekeningnummer, phone number, invoice number) are blocked at the form interface level before entering the system. These invoices are implicitly Type A rejections but never reach the system.

[2026-07-06] [Session 2] DECISION: Mandatory field enforcement is externalized to the submission form.
Assumptions: None.
Rationale: User clarified in Session 2 that mandatory fields (debtor name, address, bank account number, phone number, invoice number / System Identifier) are enforced at the form level. Incomplete submissions are blocked before entering Gimme. This removes the need for RQ-004 (Mandatory Field Validation) from the system specification.

[2026-07-06] [Session 2] DECISION: Requirements restructured from 9 to 5 requirements.
Assumptions: None.
Rationale: Data enrichment removed, mandatory field validation externalized, rejection overview requirement scope conflict resolved by removal. Remaining requirements: RQ-001 (PoC Existence Verification), RQ-002 (Uncooperative Register Check), RQ-003 (Payment Plan Check), RQ-004 (Batch Acceptance by Case Analyst), RQ-005 (Warning Logging for Unavailable Data Sources).

[2026-07-06] [Session 2] DECISION: RQ-005 (Warning Logging for Unavailable Data Sources) demoted from "Must have" to lower priority.
Assumptions: None.
Rationale: RQ-005 provides no direct stakeholder value. It is an internal system behaviour (audit trail logging) that does not produce observable output for end users. Priority adjusted to lower based on user decision. Diagram may omit this requirement until priority is raised again.

[2026-07-07] [Session 3] DECISION: RQ-007 from the MoSCoW whiteboard is documented as redundant with RQ-001 and not added as a separate requirement.
Assumptions: None.
Rationale: RQ-007 on the whiteboard reads "Invoice RQ existence verification submitted" and is placed in the Must have column. This is functionally identical to RQ-001 (PoC Existence Verification). No new spec entry is created. If RQ-007 was intended to describe a distinct mechanism, it must be re-specified with a different description.

[2026-07-07] [Session 3] DECISION: Whiteboard numbering for Batch Acceptance (labelled RQ-005) is corrected to RQ-004 in the spec.
Assumptions: None.
Rationale: The whiteboard labels Batch Acceptance as RQ-005, creating a numbering collision with the spec's RQ-005 (Warning Logging). The spec ID RQ-004 for Batch Acceptance is correct. The whiteboard label is misnumbered. No spec change required.
