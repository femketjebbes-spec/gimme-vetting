# Architecture Decisions

Records all architectural choices for the project. This file is maintained by the Architect agent and serves as the reference for all coding agents.

## Format

```
[YYYY-MM-DD] [Session N] ARCHITECTURAL DECISION: <statement>
Rationale: <reasoning behind the decision>
Security Implications: <security considerations>
Affected Agents: [list of agents whose work is constrained]
```

## Decisions

[2026-07-06] [Session 1] ARCHITECTURAL DECISION: Structural changes to frontend code during Refactoring Mode must trigger a mandatory API contract verification and a re-evaluation workflow that routes through Gerard before any backend changes.
Rationale: Femke's Refactoring Mode may change public interfaces, variable names, and component structures that Gerard's adapter layer depends on. Without a notification mechanism, Gerard and Naut operate on stale contract information. Naut has the same gap for its own backend Refactoring Mode. The re-evaluation workflow ensures that any frontend structural change is validated against the API contract, delegated to Gerard for backend impact assessment, and implemented by Naut only when Gerard confirms a contract mismatch exists. Naut never receives structural change notifications directly from Femke. The signal chain is Femke produces signal -> Archibald reads signal -> Archibald delegates to Gerard -> Gerard validates and delegates to Naut -> Naut implements -> Gerard verifies -> Gerard signals Archibald complete.
Security Implications: Ensures the API contract remains authoritative and prevents silent divergence between frontend consumption patterns and backend endpoint definitions. Silent divergence can lead to security regressions when authentication requirements, input validation, or error handling change without the adapter layer being updated.
Affected Agents: Femke, Gerard, Naut, Archibald

[2026-07-06] [Session 2] ARCHITECTURAL DECISION: All coding agents (Femke, Naut, Gerard, Database Engineer) must submit a structured JSON review request to the Alignment Agent after producing artefacts or making changes. The Alignment Agent validates artefacts against Robbie's requirements and Archibald's specs before approving pipeline progression. The Alignment Agent overwrites the review request file with its compliance decision. Upon rejection, coding agents must correct violations and resubmit with an incremented review cycle number before activating the next pipeline agent.
Rationale: Previous agent definitions listed "Completed artefact submission" to the Alignment Agent review channel without defining a format. The Alignment Agent definition described receiving artefacts through a review channel without specifying submission format, activation trigger, or pipeline gate enforcement. This gap allowed undefined handovers between agents. The JSON review request format provides a machine-parseable, unambiguous artefact listing and self-certification mechanism. Pipeline gate enforcement ensures no coding agent activates its downstream counterpart without explicit Alignment Agent approval. The iterative review loop (rejection with feedback, correction, resubmission) prevents non-compliant artefacts from progressing through the pipeline.
Security Implications: Mandatory alignment verification at every pipeline stage prevents non-compliant code from reaching downstream agents. This reduces the risk of security regressions when frontend, API, or backend changes introduce vulnerabilities that bypass requirements-based validation.
Affected Agents: Femke, Naut, Gerard, Database Engineer, Alignment Agent, Archibald

[2026-07-06] [Session 3] ARCHITECTURAL DECISION: Femke must invoke `npx jest --config jest.config.js --json --outputFile .jest-results.json` in all three modes (Testing Mode, Implementation Mode, Refactoring Mode) and parse the JSON output fields (`numFailedTests`, `numPassedTests`, `failureMessage`) to determine test state. The configuration file `jest.config.js` must reside at the project root as a canonical location.
Rationale: Naut specifies `mvn test` and `mvn compile` with exact command invocations. Femke previously only stated "runs `jest`" without specifying the command pattern, configuration file location, or result parsing mechanism. This under-specification created ambiguity about how test results are deterministically evaluated. JSON output parsing provides machine-readable, unambiguous test state determination analogous to how Naut parses Maven output.
Security Implications: Deterministic test result parsing prevents agents from incorrectly reporting green state when tests have actually failed. This ensures the TDD red-green discipline is enforced reliably across sessions.
Affected Agents: Femke

[2026-07-06] [Session 4] ARCHITECTURAL DECISION: The handover from Gerard (API-Agent) to Naut (Backend Agent) must follow the same Alignment Agent gate pattern as the Femke-to-Gerard handover. Gerard must submit `docs/alignment-review-request.md` to the Alignment Agent after completing API contract work. The Alignment Agent validates Gerard's artefacts against Robbie's requirements and Archibald's specs. Archibald must read the Alignment Agent decision from `docs/alignment-review-request.md` and confirm `greenlightForNextAgent` is `true` with `nextAgentInPipeline` set to `Naut` before producing a delegation plan for Naut. Naut must not activate until this approval is confirmed in Archibald's delegation plan. This applies symmetrically to the Naut completion phase as well.
Rationale: The existing Femke-to-Gerard handover requires Archibald to read the Alignment Agent decision before activating Gerard. The Gerard-to-Naut handover previously lacked this explicit check — Archibald only read `docs/gerard-ready-signal.md` without verifying Alignment Agent approval. This asymmetry created a gap where Naut could potentially activate before Gerard's compliance was verified. Symmetric handover gates ensure consistent quality enforcement across all pipeline transitions. Archibald's monitoring layer gains a new violation type (Alignment Agent gate violation) that blocks delegation if the compliance decision is missing or shows REJECTED status.
Security Implications: Prevents backend implementation from proceeding on unverified API contracts. An unverified contract could contain endpoint mismatches, missing authentication requirements, or incorrect error mappings that Naut would then implement incorrectly. The Alignment Agent gate ensures Gerard's contract work is requirements-compliant before Naut begins backend development.
Affected Agents: Gerard, Naut, Archibald, Alignment Agent

[2026-07-07] [Session 4] ARCHITECTURAL DECISION: WI-001 PoC filename matching shall be case-insensitive. The matching algorithm performs full-string comparison after lowercase normalisation of both the PoC filename and the invoice number. No substring matching, pattern matching, or fuzzy matching is permitted.
Rationale: The requirements spec (RQ-001) does not specify case-sensitivity. Upstream systems may produce PoC filenames with varying case conventions. Case-insensitive matching accommodates this variation without requiring a transformation layer. The algorithm is deterministic and unambiguous.
Security Implications: Case-insensitive matching reduces false Type A rejections caused by naming convention mismatches, which prevents clients from being blocked by non-functional issues. The full-string comparison (not substring) prevents matching attacks where a malicious filename could partially match a legitimate invoice number.
Affected Agents: Naut, Gerard

[2026-07-07] [Session 4] ARCHITECTURAL DECISION: WI-001 shall tolerate multiple PoC files for a single invoice number. Finding at least one matching filename is sufficient to pass the PoC existence gate. No deduplication or duplicate rejection logic is required.
Rationale: RQ-001 does not specify behaviour for multiple PoC files. The stakeholder confirmed that one match is sufficient. Deduplication is not a business requirement and would add unnecessary complexity to the intake pipeline.
Security Implications: Tolerating multiple PoC files does not introduce security risk. The PoC files are sourced from the legitimate upstream system, not from the client submitting the invoice.
Affected Agents: Naut

[2026-07-07] [Session 4] ARCHITECTURAL DECISION: The PoC store location shall be a configurable path or storage bucket, with no architectural assumption about shared filesystem versus object storage. The configuration value shall be injected at runtime via application configuration.
Rationale: W-005 (Domain Model) defines the PoC entity but not its storage mechanism. The storage decision depends on external factors (who provides PoC files, where they reside) that are outside WI-001 scope. A configurable path decouples the PoC matching logic from the storage mechanism.
Security Implications: Runtime configuration must not expose the PoC store path in error messages or logs. The storage credentials or access keys must be stored in a secure configuration store, not in version-controlled files. The file system or object storage access layer must enforce access controls.
Affected Agents: Naut, Database Engineer

[2026-07-07] [Session 5] ARCHITECTURAL DECISION: WI-002 Excel upload endpoint accepts exactly the column names from the requirements specification (case-insensitive): "invoice number", "debtor name", "address", "phone number", "bank account number". No aliases or alternative column name variants are accepted. Files with unrecognized column names shall be rejected with a structured error response.
Rationale: The user confirmed strict column name enforcement in Session 5. This simplifies the parsing logic and prevents silent mismapping when a client uses unfamiliar column names. The error response enables the client to correct their Excel template.
Security Implications: Rejecting unknown column names prevents unexpected field mapping that could cause data loss or incorrect invoice processing. No column name injection vectors exist since column names are validated against a fixed allowlist.
Affected Agents: Gerard, Naut

[2026-07-07] [Session 5] ARCHITECTURAL DECISION: WI-002 supports .xlsx and .csv file formats. Header row is optional. When no header row is present, column order determines field mapping: column 0 = invoiceNumber, column 1 = debtorName, column 2 = address, column 3 = phoneNumber, column 4 = bankAccountNumber.
Rationale: Confirmed by user in Session 5. This decision covers both format support and the fallback mapping mechanism when headers are absent.
Security Implications: No direct security implications. File type validation must enforce MIME type checking to prevent upload of executable or malicious files disguised as Excel files.
Affected Agents: Gerard, Naut

[2026-07-07] [Session 5] ARCHITECTURAL DECISION: WI-002 endpoint has no authentication for MVP. No file size limit is enforced for MVP.
Rationale: Confirmed by user in Session 5. The client portal is unauthenticated for MVP purposes. File size limits are deferred to a future iteration.
Security Implications: No authentication on the upload endpoint allows unauthenticated file uploads. This is a documented MVP limitation that must be flagged for remediation in a future iteration. Absence of file size limits creates a denial-of-service risk via large file uploads. The architect recommends designing a size boundary but deferring enforcement to MVP+1.
Affected Agents: Gerard, Naut, Femke

[2026-07-07] [Session 5] ARCHITECTURAL DECISION: WI-002 synchronous processing model. The client uploads an Excel file, the server processes all rows through parsing, mandatory field validation, and PoC existence verification, then returns the result (including a download link for the return Excel) in the same HTTP response cycle.
Rationale: Confirmed by user in Session 5 for MVP. Synchronous processing simplifies the MVP architecture. If large files cause performance issues, async processing can be evaluated later (AUNV-006).
Security Implications: Synchronous processing ties client connections to processing time. Large files held in memory during processing increase the attack surface for memory exhaustion DoS. Streaming processing (Apache POI SXSSF) is recommended to mitigate this risk.
Affected Agents: Gerard, Naut, Femke

[2026-07-07] [Session 5] ARCHITECTURAL DECISION: Apache POI is the recommended Excel parsing library for WI-002 through WI-004.
Rationale: Apache POI is the industry standard for Java Excel processing, supports both .xlsx and .csv formats, and is consistent across the parsing and generation work items. EasyExcel can be evaluated later if large-file performance becomes critical.
Security Implications: Apache POI has had historical vulnerabilities related to XML external entity injection in .xlsx files (ZIP slip vulnerabilities). The version used must be the latest patched version. XML entity expansion must be disabled in the parser configuration.
Affected Agents: Naut

[2026-07-07] [Session 6] ARCHITECTURAL DECISION: Wi-006 shall use a GNU Make Makefile at the project root for build orchestration. Targets: `build`, `test`, `clean`, `backend`, `frontend`, `backend-test`, `frontend-test`, `check-tools`. Backend builds before frontend in the `build` target. `check-tools` validates that `mvn`, `node`, `npm` are installed.
Rationale: Option B (Makefile) was selected over Option A (shell script) and Option C (root package.json) by the WI-006 specification. Make provides incremental build support, clear dependency declarations, and is the standard convention for multi-language projects.
Security Implications: None. Build orchestration does not affect the product security surface.
Affected Agents: Naut
