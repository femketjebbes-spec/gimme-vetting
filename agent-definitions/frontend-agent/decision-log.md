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
