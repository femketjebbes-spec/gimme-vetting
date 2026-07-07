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

---

[2026-07-07] [Session 4] DECISION: First implementation work item is WI-001 (PoC Existence Verification, RQ-001 subset of W-002).
Assumptions: None.
Rationale: RQ-001 has zero dependency on unverified external registries (AUNV-001, AUNV-002). It delivers observable value in one iteration. It validates the intake pipeline architecture without being blocked by unknown external systems. RQ-002 and RQ-003 are additive gates on the same pipeline structure.

[2026-07-07] [Session 4] DECISION: D-001 — PoC filename matching is case-insensitive.
Assumptions: Pending user confirmation.
Rationale: Accommodates varying file naming conventions from upstream systems. Matching algorithm: poCFileName.toLowerCase() == invoiceNumber.toLowerCase(). Requires user confirmation before implementation.

[2026-07-07] [Session 4] DECISION: D-002 — Multiple PoC files for one invoice number are not an error condition.
Assumptions: Pending user confirmation.
Rationale: One match is sufficient. Deduplication is not required. If the business rule changes (e.g., duplicate PoCs must be flagged), this must be escalated before implementation.

[2026-07-07] [Session 4] DECISION: D-003 — PoC store location is a configurable path or storage bucket.
Assumptions: None.
Rationale: No assumption made about shared filesystem vs object storage. The architect must discover the actual storage mechanism. WI-001 uses an interface abstraction that can be implemented with any storage backend.

[2026-07-07] [Session 4] DECISION: Framework selection — Gherkin for acceptance criteria, ISO 29148 for quality validation.
Assumptions: None.
Rationale: RQ-001 specifies discrete, testable system behaviour with deterministic outcomes. Given-When-Then structure enforces the observable behaviour contract required by the development team. ISO 29148 quality attributes applied to verify the original requirement text before decomposition.

[2026-07-07] [Session 4] DECISION: Minimum viable Invoice entity for WI-001.
Assumptions: W-005 (Domain Model) will be produced in parallel.
Rationale: WI-001 only needs: invoiceNumber (String, mandatory), poCStatus (enum: VERIFIED / MISSING), status (enum: QUEUED / REJECTED). The full entity model (W-005) includes all fields from mandatory field enforcement but WI-001 does not depend on those fields.

[2026-07-07] [Session 5] DECISION: Mandatory field enforcement moved from external to internal.
Assumptions: Verified by user in Session 5.
Rationale: User clarified that upstream Excel may contain incomplete rows. The external form enforcement assumption was incorrect. Gimme now validates mandatory fields per row at intake for both single-invoice API and Excel batch intake.

[2026-07-07] [Session 5] DECISION: Excel batch intake is synchronous (upload-process-return in one request).
Assumptions: Pending user confirmation on file size and processing time.
Rationale: User described synchronous flow. If large files or slow processing require async approach, this decision must be revisited.

[2026-07-07] [Session 5] DECISION: Return Excel contains only failing rows (not all rows).
Assumptions: None.
Rationale: User explicitly confirmed that only rows with issues are returned. This is more efficient for the client to review.

[2026-07-07] [Session 5] DECISION: Return Excel is download link in portal, not email.
Assumptions: None.
Rationale: User confirmed download link. This may change in later iterations.

[2026-07-07] [Session 5] DECISION: PoC upload is a separate endpoint, not integrated into the Excel upload.
Assumptions: None.
Rationale: User confirmed the client uploads PoC files separately. The client portal displays which invoice numbers are missing PoC files.

[2026-07-07] [Session 5] DECISION: RQ-006 (Excel Batch Intake), RQ-007 (Mandatory Field Validation), RQ-008 (Return Excel), RQ-009 (Separate PoC Upload) added to specification.
Assumptions: None.
Rationale: These requirements are necessary for the MVP client portal. RQ-006 is the entry point for batch processing. RQ-007 and RQ-008 handle the validation-return loop. RQ-009 handles PoC file upload.

[2026-07-07] [Session 5] DECISION: RQ-002 and RQ-003 deferred from MVP.
Assumptions: AUNV-001, AUNV-002 remain unverified.
Rationale: User confirmed MVP only includes RQ-001 and RQ-007 (mandatory field validation). RQ-002 (Uncooperative Register) and RQ-003 (Payment Plan) are deferred to a later increment.

[2026-07-07] [Session 5] DECISION: Excel file supports .xlsx and .csv formats.
Assumptions: None.
Rationale: User confirmed both formats for MVP. The architect should implement parsing for both or select one and document the justification.

[2026-07-07] [Session 5] DECISION: Excel header row is optional (column order matters, header presence does not).
Assumptions: None.
Rationale: User said header row "ought not matter." The parser must handle both with and without headers. If the header row is present, it uses the standard names: invoice number, debtor name, address, phone number, bank account number.

[2026-07-07] [Session 5] DECISION: Excel column order is invoice number / debtor name / address / phone number / bank account number.
Assumptions: None.
Rationale: User confirmed this order. The architect must map these column names to the internal field names.

[2026-07-07] [Session 5] DECISION: No authentication for client portal MVP.
Assumptions: None.
Rationale: User confirmed no auth for MVP. This is a security risk that must be documented in the architect design and flagged for future sessions.

[2026-07-07] [Session 5] DECISION: No maximum file size for Excel upload MVP.
Assumptions: None.
Rationale: User confirmed no limit for MVP. The architect should document the performance risk of unlimited file sizes and flag it for NFRs.
