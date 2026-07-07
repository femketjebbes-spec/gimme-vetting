# Femke: Decision Log

This file records implementation decisions with their rationale and the constraints they derive from. Traceable to Archibald's architecture decisions. Records test-to-spec mappings produced during Testing Mode. Format per entry:
```

[2026-07-06] [Session 1] DECISION: Femke communicates API readiness to Archibald via a structured completion signal file at `docs/api-ready-signal.md`. Archibald monitors for this file as the trigger to activate Gerard. Gerard completes its work and signals Archibald via `docs/gerard-ready-signal.md`. Archibald reads Gerard's signal before assigning backend subtasks to Naut.
Assumptions: Archibald actively monitors for signal files. Gerard produces both the API contract and the completion signal. Naut waits for Gerard's signal before activating.
Rationale: File-based signals provide an unambiguous, machine-readable handover mechanism. Each agent writes its own signal, eliminating ambiguity about completion state. Archibald acts as the central dispatcher, enforcing sequential workflow compliance.
```

### API-Ready Signal Decision Log Entry

This decision log also records each API-ready signal produced by Femke. Format per signal entry:

```
[YYYY-MM-DD] [Session N] API-SIGNAL: `docs/api-ready-signal.md` produced
**Endpoints Defined**: [count]
**API Requirements Document**: `docs/api-requirements.md`
**Gerard Activated**: [yes/no]
**Gerard Delegation Sent**: [timestamp or pending]
```
[YYYY-MM-DD] [Session N] DECISION: <statement>
Assumptions: <statement>
Rationale: <user-provided or derived from delegation plan>

[YYYY-MM-DD] [Session N] TEST-SPEC: <test file path> maps to <specification or delegation subtask reference>
Purpose: <what behaviour the test validates>
Derived from: <delegation plan subtask ID or Robbie requirement ID>
```

[2026-07-06] [Session 3] DECISION: Femke invokes `npx jest --config jest.config.js --json --outputFile .jest-results.json` in Testing Mode, Implementation Mode, and Refactoring Mode. Test state is determined by parsing the JSON output fields: `numFailedTests` and `failureMessage` for red state, `numPassedTests` and `numFailedTests` for green state.
Assumptions: Jest is installed as a project dependency accessible via `npx`. The `jest.config.js` file exists at the project root before Femke activates. The JSON reporter is available as a built-in Jest reporter.
Rationale: Resolves the under-specification of Femke's test execution. Matches Naut's level of command precision (`mvn test`, `mvn compile`). JSON parsing provides deterministic, machine-readable test state evaluation.

[2026-07-07] [Session 6] TEST-SPEC: `4-frontend/src/frontend/components/__tests__/ExcelUpload.test.jsx` maps to WI-002 Delegation Plan Subtask 1 (Frontend Implementation)
Purpose: Validates file picker rendering, MIME type validation, upload button API calls, success response display, error response handling, and download link rendering.
Derived from: docs/wi-002-delegation-parallel.md Subtask 1, docs/api-contract-wi-002.md

[2026-07-07] [Session 6] TEST-SPEC: File picker test maps to requirement "Accept .xlsx and .csv files with MIME type validation"
Purpose: Ensures the file input accepts correct MIME types and rejects invalid ones before upload.
Derived from: docs/api-contract-wi-002.md Section 3.2 (Supported File Formats), docs/wi-002-delegation-parallel.md Subtask 1

[2026-07-07] [Session 6] TEST-SPEC: Upload button test maps to requirement "POST to /api/v1/intake/excel with FormData field named file"
Purpose: Ensures the correct endpoint is called with FormData containing a `file` field.
Derived from: docs/api-contract-wi-002.md Section 2 (Endpoint Definition), docs/wi-002-delegation-parallel.md Subtask 1

[2026-07-07] [Session 6] TEST-SPEC: Success response test maps to requirement "Display processingStatus, totalRowsProcessed, rowsPassed, rowsFailed, and download link when rowsFailed > 0"
Purpose: Ensures the success response body fields are displayed and the download link renders conditionally.
Derived from: docs/api-contract-wi-002.md Section 5.1 (200 OK Response Schema), docs/wi-002-delegation-parallel.md Subtask 1

[2026-07-07] [Session 6] TEST-SPEC: Error handling tests map to requirements "HTTP 400 Invalid File Format, HTTP 400 Column Name Mismatch, HTTP 500 Generic Error"
Purpose: Ensures each error response type is handled with appropriate user-facing messages.
Derived from: docs/api-contract-wi-002.md Sections 5.2, 5.3, 5.4 (Error Response Schemas)

[2026-07-07] [Session 6] TEST-SPEC: Download link test maps to requirement "Render download link as <a> element when rowsFailed > 0"
Purpose: Ensures the return Excel download link is rendered only when rowsFailed is greater than zero.
Derived from: docs/api-contract-wi-002.md Section 5.1, docs/wi-002-delegation-parallel.md Subtask 1
