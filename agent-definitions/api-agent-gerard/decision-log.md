# API-Agent (Gerard): Decision Log

Records contract validation decisions with their rationale, the severity classification, and the assumptions they depend on.

## Entries

[2026-07-08] [Session 4] WI-007 API CONTRACT PRODUCTION
DECISION: Template download endpoint contract specifies `GET /api/v1/intake/excel/template` returning a static XLSX template file.
Assumptions: Template generation uses `ExcelParsingService.ALLOWED_COLUMN_NAMES` constants to prevent header drift. No authentication required for MVP.
Rationale: D-028 specifies the endpoint path. D-029 requires Apache POI with constant-based headers. The existing `generateReturnXlsx()` pattern provides a reusable template generation approach.

[2026-07-08] [Session 5] WI-007 ALIGNMENT REVIEW SUBMISSION
DECISION: Alignment review request submitted to Alignment Agent with `pipelineStage: "gerard contract review for wi-007"`.
Assumptions: Alignment Agent will approve the contract before Naut implementation begins.
Rationale: Contract satisfies all requirements from WI-007 spec and architectural decisions D-020, D-026, D-028, D-029.

[2026-07-09] [Session 7] WI-CA-001 CONTRACT PRODUCTION
DECISION: Contract defines two GET endpoints at `/api/v1/analyst/invoices` (paginated list) and `/api/v1/analyst/invoices/{id}` (detail) with no authentication middleware for MVP.
Assumptions: Naut will add `resubmissionCount` field to Invoice entity and create Flyway migration V2. Naut will use JPA Specifications for search to prevent SQL injection.
Rationale: D-CA-002 mandates unauthenticated endpoints for MVP. The response schema matches the work item spec exactly. Error responses are structured without exposing server internals (S-006).

[2026-07-09] [Session 7] WI-CA-001 ALIGNMENT REVIEW SUBMISSION
DECISION: Alignment review request submitted with `pipelineStage: "API contract production"`, `nextAgentInPipeline: "Femke-Naut-parallel"`.
Assumptions: Alignment Agent will approve before Archibald activates parallel phase.
Rationale: Contract satisfies all requirements from WI-CA-001 spec and architectural decisions D-CA-001 through D-CA-004.
