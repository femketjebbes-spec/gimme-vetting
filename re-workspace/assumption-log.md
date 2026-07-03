# Assumption Log

Records every assumption held by the user or Robbie during elicitation or specification.

Format per entry:
```
[YYYY-MM-DD] [Session N] ASSUMPTION: <statement>
Status: Unverified | Verified | Overridden
Rationale (if overridden): <user-provided>
```

[2026-07-03] [Session 1] ASSUMPTION: "Backwards chaining" as described in the document represents genuine goal-driven inference, not merely a collection of independent validation rules.
Status: Overridden
Rationale: User clarified in Session 1 that "backwards chaining" refers to a modulation technique where a business goal is formulated first and changes are derived to achieve it with minimal disruption. It is not a formal AI inference mechanism. The term describes an approach philosophy, not a technical architecture pattern.

[2026-07-03] [Session 1] ASSUMPTION: OCR is a viable and reliable mechanism for extracting dates from PoC documents.
Status: Overridden
Rationale: User clarified in Session 1 that all PoC validation is out of scope. OCR dependency for PoC is therefore not required.

[2026-07-03] [Session 1] ASSUMPTION: An Uncooperative Register already exists as a data source.
Status: Unverified
Notes: The document states the system should check against this register. It is unclear whether the register currently exists as a database, a spreadsheet, or a conceptual idea. If it does not exist, its creation is a prerequisite.

[2026-07-03] [Session 1] ASSUMPTION: A payment plan database or incassolijst already exists as a data source.
Status: Unverified
Notes: Same assessment as above. The document references this as an existing source. Its current state is unknown.

[2026-07-03] [Session 1] ASSUMPTION: "Gimme" and "Gimmo" refer to the same entity.
Status: Unverified
Notes: The document uses both spellings. This is a basic consistency issue that must be resolved before proceeding.

[2026-07-03] [Session 1] ASSUMPTION: The four automation streams described are the complete set of automation requirements.
Status: Unverified
Notes: The document states these are "de vier primaire automatiseringsstromen." This implies completeness, but the user should confirm that no other automation streams are needed.

[2026-07-03] [Session 1] ASSUMPTION: Non-functional requirements (performance, availability, security, auditability) are not in scope for this review.
Status: Unverified
Notes: The document contains no non-functional requirements. Their absence may be intentional (out of scope) or unintentional (overlooked). This is a significant gap if the system handles financial and legal data.

[2026-07-03] [Session 1] ASSUMPTION: The 1-month waiting period before manual enrichment of incomplete dossiers (section 4, line 113) is a business rule, not a system timing mechanism.
Status: Unverified
Notes: The document states a "wachttijd van 1 maand" applies. It is unclear whether the system enforces this as a timer, or whether it is an organisational guideline for case analysts.
