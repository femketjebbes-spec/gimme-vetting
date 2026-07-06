# Stakeholder Map

Records stakeholders, their roles, authority levels, domain expertise, and interests.

Format per entry:
```
[YYYY-MM-DD] [Session N] STAKEHOLDER: <name/role>
Authority: <high/medium/low>
Domain Knowledge: <description>
Interests/Concerns: <description>
```

[2026-07-03] [Session 1] STAKEHOLDER: Case Analyst (Dossierbehandelaar)
Authority: Medium
Domain Knowledge: High - performs the manual vetting process currently. Understands the workflow, pain points, and validation rules.
Interests/Concerns: Role transformation from manual verification to exception handling. Job security and skill relevance post-automation.

[2026-07-03] [Session 1] STAKEHOLDER: Gimmo/Gimme Management
Authority: High
Domain Knowledge: Medium - owns the business strategy for automation but may not understand technical feasibility constraints.
Interests/Concerns: Workload reduction, operational efficiency, scalability, cost savings.

[2026-07-03] [Session 1] STAKEHOLDER: Debtor (Debituur)
Authority: Low
Domain Knowledge: N/A - subject of the process.
Interests/Concerns: Fair treatment, transparent decision-making, absence of errors in automated blocking decisions.

[2026-07-03] [Session 1] STAKEHOLDER: Client/Submitter (inzender)
Authority: Medium
Domain Knowledge: Medium - submits invoices and PoC documents. Affected by acceptance/rejection decisions.
Interests/Concerns: Faster processing times, clear rejection reasons, minimal manual follow-up required.

[2026-07-03] [Session 1] STAKEHOLDER: Legal/Compliance
Authority: High (inferred)
Domain Knowledge: High - PoC is described as the "primaire poortwachter voor de dossierkwaliteit binnen Gimmo" with "juridische en financiële zekerheid."
Interests/Concerns: Legal defensibility of automated decisions, audit trail, data protection, regulatory compliance.
Notes: This stakeholder group is inferred from context. They are not explicitly mentioned in the document but are critical given the legal/financial nature of the domain.

[2026-07-03] [Session 1] STAKEHOLDER: IT/Development Team
Authority: Low (in decision-making), High (in technical feasibility)
Domain Knowledge: High - responsible for implementation.
Interests/Concerns: Feasibility of OCR, integration with existing data sources, system architecture, testing requirements.
Notes: Identified as an inferred stakeholder. The document references "ontwikkelaars" but does not explicitly position them as stakeholders in the requirements process.
