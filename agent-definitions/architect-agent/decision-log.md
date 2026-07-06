# Architect Decision Log

Records decisions made by the Architect agent during task analysis and architecture design sessions.

[2026-07-02] [Session 1] DECISION: The Architect agent combines task delegation and architectural advice into a single role.
Assumptions: Centralised architectural knowledge improves consistency across delegation decisions.
Rationale: User requirement for single source of architectural truth.

[2026-07-02] [Session 1] DECISION: The Architect uses socratic questioning to uncover architectural requirements.
Assumptions: Users may have insufficient architectural knowledge to specify all requirements upfront.
Rationale: User stated they have little knowledge about architectural design.

[2026-07-02] [Session 1] DECISION: Security considerations are explicitly raised during all architectural questioning.
Assumptions: Security is a constraint that shapes architecture, not an afterthought.
Rationale: User explicit requirement for security consideration.

[2026-07-02] [Session 1] DECISION: The Architect activates on direct user task input, not on Robbie's output.
Assumptions: Users initiate architectural planning directly rather than routing through requirements first.
Rationale: User specified direct user trigger.

[2026-07-02] [Session 1] DECISION: Architecture decisions are stored in a separate file at agent-definitions/architecture-decisions.md.
Assumptions: Centralising decisions outside the agent folder keeps them accessible to all agents.
Rationale: Coding agents need to reference architecture decisions during implementation.

[2026-07-06] [Session 3] DECISION: The Architect must read the Alignment Agent compliance decision from docs/alignment-review-request.md before activating Naut. A new monitoring error type (Alignment Agent gate violation) was added to block delegation if the compliance decision is missing or shows REJECTED status for Gerard.
Assumptions: The Alignment Agent appends its decision to docs/alignment-review-request.md and the fields greenlightForNextAgent and nextAgentInPipeline are correctly set.
Rationale: The existing Femke-to-Gerard handover already requires this check. The Gerard-to-Nut handover lacked this explicit gate. Symmetric enforcement ensures consistent quality control across all pipeline transitions.
