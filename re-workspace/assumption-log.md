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
Status: Overridden
Rationale: User clarified in Session 2 that Type A (incomplete data) rejections can be re-submitted whenever without any waiting period. The 1-month waiting period is removed from the specification.

[2026-07-06] [Session 2] ASSUMPTION: RQ-006 (Rejection Overview) generates an overview for all Type A rejections.
Status: Moot
Rationale: Requirements were restructured in Session 2. RQ-006 (Rejection Overview) was removed. Data enrichment (RQ-005) was removed from scope. The current specification (RQ-001 through RQ-005) does not include a rejection overview requirement. If a rejection overview is needed, it must be specified as a new requirement.

[2026-07-07] [Session 5] ASSUMPTION: Mandatory field enforcement is performed internally by Gimme, not by an external form.
Status: Verified
Rationale: User clarified in Session 5 that the upstream Excel may contain incomplete rows. Case analysts do not want to manually check for empty fields. Gimme validates mandatory fields at intake and returns incomplete rows with missing fields flagged.

[2026-07-07] [Session 5] ASSUMPTION: Excel batch intake is synchronous (upload, process, return in one request cycle).
Status: Unverified
Rationale: User described synchronous flow. No file size limit for MVP means large files are possible. If processing time becomes unacceptable, the architect may recommend async with download link, which would require a specification change.

[2026-07-07] [Session 5] ASSUMPTION: Excel file uses .xlsx or .csv format, header row is optional, column order is invoice number / debtor name / address / phone number / bank account number.
Status: Verified
Rationale: User confirmed in Session 5. No authentication for MVP. No file size limit.

[2026-07-07] [Session 5] ASSUMPTION: Client portal has no authentication for MVP.
Status: Verified
Rationale: User confirmed in Session 5. This is a security risk that must be flagged in the architect design.
