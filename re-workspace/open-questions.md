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
Status: Open
Notes: Both spellings appear in the source document. A single name must be adopted.

[2026-07-03] [Session 1] QUESTION: Is "backwards chaining" used as a formal inference mechanism or as a rhetorical label for conditional acceptance logic?
Raised by: Robbie
Status: Open
Notes: If formal, the system architecture must implement goal-driven inference. If rhetorical, the term should be replaced with a clearer description (e.g., "acceptance-gate logic").

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

[2026-07-03] [Session 1] QUESTION: What is the error handling and escalation path when automated validation fails?
Raised by: Robbie
Status: Open
Notes: The document mentions that case analysts handle exceptions (e.g., when OCR fails). The specific scenarios and escalation procedures are not defined.
