# Open Questions Log

Records questions that need resolution before requirements can be finalised.

Format per entry:
```
[YYYY-MM-DD] [Session N] QUESTION: <statement>
Raised by: <user/Robbie>
Status: Open | Resolved
Answer: <if resolved>
```

[2026-07-03] [Session 1] QUESTION: What is the correct canonical name: "Gimme" or "Gimmo"?
Raised by: Robbie
Status: Resolved
Answer: "Gimme" is confirmed as the correct spelling. All instances of "Gimmo" should be corrected.

[2026-07-03] [Session 1] QUESTION: Is "backwards chaining" used as a formal inference mechanism or as a rhetorical label for conditional acceptance logic?
Raised by: Robbie
Status: Resolved
Answer: Clarified in Session 1: backwards chaining is a modulation technique (goal-first, minimal disruption to existing business). It is not a technical architecture pattern.

[2026-07-03] [Session 1] QUESTION: What are the business objectives for this project?
Raised by: Robbie
Status: Resolved
Answer: OPE-001 (reduce case analyst workload) is the confirmed primary objective. OPE-002 through OPE-005 are confirmed as subsumed under OPE-001 and will be listed as derived/secondary objectives. All requirements will trace to OPE-001.

[2026-07-03] [Session 1] QUESTION: What OCR technology is intended or required for PoC date extraction?
Raised by: Robbie
Status: Resolved
Answer: Content OCR (date extraction from PoC documents) is out of scope. However, PoC existence verification remains in scope, which may require file-level linkage (e.g., filename matching) rather than content OCR. The file matching mechanism must be specified separately.

[2026-07-03] [Session 1] QUESTION: What is the current state of the data sources referenced in the requirements (Uncooperative Register, payment plan database, incassolijst)?
Raised by: Robbie
Status: Open
Notes: Each is described as an existing system to query. Their existence, accessibility, and data quality directly determine feasibility.

[2026-07-03] [Session 1] QUESTION: Which OCR technology is intended or required for PoC date extraction?
Raised by: Robbie
Status: Open
Notes: The document proposes OCR but specifies no technology, accuracy threshold, or fallback mechanism for failed extractions. This is a critical technical dependency.

[2026-07-03] [Session 1] QUESTION: Are there non-functional requirements that should be documented in parallel with these functional requirements?
Raised by: Robbie
Status: Open
Notes: The system handles financial and legal data. Security, audit logging, data retention, performance, and availability are likely necessary. The source document contains none.

[2026-07-03] [Session 1] QUESTION: Who are the stakeholders for this system?
Raised by: Robbie
Status: Open
Notes: The document mentions case analysts implicitly. Other stakeholders (debtor, client/submitter, management, legal/compliance) are not documented. A stakeholder map is needed.

[2026-07-03] [Session 1] QUESTION: What happens after the four validation gates? The document describes acceptance and rejection but not the downstream workflow for accepted dossiers.
Raised by: Robbie
Status: Open
Notes: The requirements cover only the intake/acceptance gate. Post-acceptance processing is undefined.

[2026-07-03] [Session 1] QUESTION: What constitutes an "uitzondering" (special case / business outlier) that requires case analyst intervention, and what is the handling workflow for such cases?
Raised by: Robbie
Status: Resolved
Answer: "Uitzonderingen" refers to exceptional PoC formats requiring case analyst intervention. This is out of scope since PoC validation is out of scope (Session 1 clarification).
