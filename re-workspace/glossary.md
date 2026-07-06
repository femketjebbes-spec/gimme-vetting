# Glossary Log

Records domain-specific terms to construct a consistent ubiquitous language.

Format per entry:
```
[YYYY-MM-DD] [Session N] <term>
Description: <statement>
```

[2026-07-03] [Session 1] PoC (Proof of Correspondence)
Description: Primary evidence document for Gimmo vetting dossiers. Must contain at least two contact moments spaced at least one month apart, with the first contact moment occurring at least two months before the submission date. Used as a gatekeeping document for dossier quality.

[2026-07-03] [Session 1] Vetting Proces
Description: The end-to-end process by which Gimmo evaluates and accepts or rejects invoices/dossiers. Currently manual, proposed for automation across four streams: PoC validation, debtor cooperativeness check, data enrichment, and payment plan verification.

[2026-07-03] [Session 1] Debiteurencontrole
Description: The debtor verification process, including checks against an Uncooperative Register and verification of existing payment arrangements.

[2026-07-03] [Session 1] Backwards Chaining
Description: A modulation technique where a business goal is formulated first, and proposed changes are derived to achieve that goal with minimal disruption to existing business operations. This is the reverse of solution-first approaches where a technical solution is proposed and the business adapts. Clarified by user in Session 1.

[2026-07-03] [Session 1] Case Analyst (Dossierbehandelaar)
Description: The human role performing dossier processing. Under the proposed automation, their role shifts from manual verification to exception handling only.

[2026-07-03] [Session 1] Gimme
Description: The organisation or system being automated. User confirmed in Session 1 that "Gimme" is the correct spelling. All instances of "Gimmo" in source materials should be corrected to "Gimme".

[2026-07-03] [Session 1] System Identifier
Description: A unique identifier on invoices required for database integrity. Mandatory field with no empty values permitted.

[2026-07-03] [Session 1] Uncooperative Register
Description: A database or register of debtors with a history of uncooperative behaviour. Invoices for matched debtors are automatically blocked.

[2026-07-03] [Session 1] Payment Plan (Betalingsregeling)
Description: An existing payment arrangement for a debtor. The system must check for active payment plans before accepting new invoices. New invoices are rejected if a payment plan exists.

[2026-07-03] [Session 1] Submission Date (T=0)
Description: The reference date for a dossier, treated as time zero. Used to calculate temporal constraints on PoC contact moments.

[2026-07-03] [Session 1] OCR (Optical Character Recognition)
Description: Technology proposed for extracting dates and text from uploaded PoC documents. Implied as a dependency for automated date validation.
