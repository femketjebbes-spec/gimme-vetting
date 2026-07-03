# Decision Log

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
