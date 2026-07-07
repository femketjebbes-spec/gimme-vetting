# Agent Registry

This file logs all active agents in the ZooCode framework.

## Active Agents

| Agent Name | Primary Responsibility | Consumed Artefacts | Produced Artefacts |
|------------|------------------------|--------------------|--------------------|
| Robbie          | Requirements Engineering | User stories, domain context, project constraints | Structured requirements documentation, acceptance criteria |
| Alignment Agent | Compliance Enforcement   | JSON review requests (docs/alignment-review-request.md), agent definitions, requirements baseline (Robbie), architect specs (Archibald) | Compliance decisions (overwritten in docs/alignment-review-request.md), rejection feedback (docs/alignment-rejection-feedback.md) |
| Archibald       | Project Architecture & Task Delegation | User task descriptions, existing architecture decisions, agent definitions, docs/wi-<NNNN>-contract-ready.md (from Gerard), docs/femke-structural-change-signal.md (from Femke), docs/alignment-review-request.md (compliance approval check for Gerard before parallel activation) | `docs/wi-<NNNN>-delegation-gerard.md` (Gerard delegation plan), `docs/wi-<NNNN>-delegation-parallel.md` (Femke+Naut parallel delegation plan), architecture decisions, security review notes, Gerard re-evaluation delegation plans |
| Database Engineer| Schema Design, Query Optimization, Migration Management | Architectural guidelines, functional requirements, existing schema artefacts | Schema definitions, optimized queries, migration scripts, index documentation |
| Ada         | Agent Definition and Specification          | Agent role descriptions, agent registry, agent definitions | Agent definitions, agent registry updates, architecture flow diagrams, custom mode entries |
| API-Agent (Gerard)      | API Integration Supervisor and Adapter Layer Development | Delegation plans from Archibald, frontend fetch patterns, backend endpoint definitions, docs/femke-structural-change-signal.md (from Archibald via delegation) | docs/api-contract-wi-<NNNN>.md, docs/wi-<NNNN>-contract-ready.md, adapter/gateway code, contract validation reports, error mappings, automated contract tests, gerard-reevaluation-complete-signal.md, docs/alignment-review-request.md |
| Naut                 | Java Backend Implementation | Delegation plans from Archibald (parallel), architecture decisions, requirements baseline, existing backend code, docs/api-contract-wi-<NNNN>.md (from Gerard), docs/alignment-review-request.md (compliance check), backend fix delegation from Gerard via Archibald | Java backend source code, JUnit 5 test code, implementation summaries, docs/alignment-review-request.md |
| Femke                | Frontend Implementation       | Delegation plans from Archibald (parallel), architecture decisions, requirements baseline, existing frontend code, docs/api-requirements.md (self-referential for verification), docs/alignment-review-request.md (compliance check)  | Jest test code, HTML/JS frontend files, CSS files, docs/api-requirements.md, docs/femke-structural-change-signal.md, docs/alignment-review-request.md |

### Agent Definition Files

| Agent | Definition File |
|-------|-----------------|
| Alignment Agent | [`agent-definitions/alignment-agent/agent-definition.md`](agent-definitions/alignment-agent/agent-definition.md) |
| Archibald | [`agent-definitions/architect-agent/agent-definition.md`](agent-definitions/architect-agent/agent-definition.md) |
| Ada | [`agent-definitions/ada/agent-definition.md`](agent-definitions/ada/agent-definition.md) |
| Database Engineer | [`agent-definitions/database-engineer/agent-definition.md`](agent-definitions/database-engineer/agent-definition.md) |
| Requirements Engineer | [`agent-definitions/requirements-engineer/agent-definition.md`](agent-definitions/requirements-engineer/agent-definition.md) |
| API-Agent (Gerard) | [`agent-definitions/api-agent-gerard/agent-definition.md`](agent-definitions/api-agent-gerard/agent-definition.md) |
| Naut                 | [`agent-definitions/backend-coding-agent/agent-definition.md`](agent-definitions/backend-coding-agent/agent-definition.md) |
| Femke                | [`agent-definitions/frontend-agent/agent-definition.md`](agent-definitions/frontend-agent/agent-definition.md) |
