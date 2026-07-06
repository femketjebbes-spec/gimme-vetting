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
