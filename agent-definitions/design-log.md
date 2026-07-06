# Design Log

This file records architectural decisions regarding agent boundaries and user overrides.

## Entries

_No design decisions recorded yet._

## Entries

### Entry 1: Alignment Agent Creation

- **Date**: 2026-07-02
- **Decision**: Created Alignment Agent with dual verification responsibilities (role boundary compliance and requirements conformance).
- **Rationale**: Both verification tasks share the same trigger pattern (post-artefact submission) and enforcement mechanism (review channel with rejection authority). Combining them avoids duplicating the review infrastructure and ensures consistent compliance evaluation.
- **Boundary safeguard**: Boundary improvement suggestions are logged to the design log rather than directly modifying agent definitions. This prevents irreversible changes to role definitions and ensures boundary adjustments are deliberate and documented.

### Entry 2: Architect Agent Creation

- **Date**: 2026-07-02
- **Decision**: Created Architect agent that combines task delegation and architectural advice into a single role.
- **Rationale**: User requires centralised architectural knowledge. Combining delegation and advisory functions ensures all task assignments are grounded in documented architecture decisions.
- **Boundary safeguard**: Architect does not implement code, define new agent roles, or modify artefacts produced by coding agents. It delegates only.
- **Design override**: User explicitly requested combining two potentially separate functions (delegation and architectural advisory) into one agent despite the risk of broad scope. This is recorded per Ada's monitoring requirements.

### Entry 3: Mandatory Requirements Gate

- **Date**: 2026-07-02
- **Decision**: All feature requests and task triggers must pass through Robbie (Requirements Engineer) before reaching any producing agent.
- **Rationale**: Feature validation and requirements structuring is a prerequisite for architectural decisions and implementation. Bypassing Robbie risks unvalidated features reaching the architecture and implementation stages.
- **Artefact impact**: Updated [`workflow/agent-architecture-flow.mmd`](workflow/agent-architecture-flow.mmd) to replace the direct trigger from UserTask to Archibald with a mandatory flow through Robbie.

### Entry 4: Database Engineer Creation

- **Date**: 2026-07-03
- **Decision**: Created Database Engineer agent responsible for schema design, query optimization, migration management, and index design.
- **Rationale**: User requires a dedicated database expert that translates architectural decisions and functional requirements into concrete database artefacts. The agent operates downstream of Archibald and Robbie.
- **Boundary decision**: Database technology selection explicitly excluded from this agent's scope. This preserves Archibald's authority over technology choices. Major structural changes require explicit Architect approval.
- **Guardrails enforced**: Destructive operation warnings, parameterized query enforcement, naming convention consistency.
