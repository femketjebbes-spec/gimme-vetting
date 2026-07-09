# Gerard Decision Log

[2026-07-08] [Session WI-008] DECISION: Content-based detection is the authoritative file format detection mechanism. MIME type is supplementary only. Detection precedence: (1) MIME type in allowlist → accept directly. (2) MIME type missing/unrecognized → inspect content via magic bytes. (3) Content matches XLSX (ZIP signature `50 4B 03 04`) or CSV (valid text) → proceed. (4) Content does not match → reject with `INVALID_FILE_FORMAT` and specific reason.
Assumptions: The adapter layer has read access to file content (first 4 bytes minimum). Apache POI is already configured for XLSX parsing.
Rationale: BR-001 requires replacing MIME-type-only validation with content-based detection. This decision directly implements FR-BR001-01, FR-BR001-02, and FR-BR001-03 from the bug report.

[2026-07-08] [Session WI-008] DECISION: Architecture decision D-BR001 exists at line 106 of `agent-definitions/architecture-decisions.md`. The entry correctly describes content-based detection, MIME type fallback, and magic byte constants. No blocker identified.
Assumptions: Archibald has already recorded the BR-001 decision as part of the delegation plan preparation.
Rationale: Subtask 2 requires verification only. The decision is present and accurate. No further action required.

[2026-07-08] [Session WI-008] DECISION: Contract version incremented from 2.0.0 to 2.1.0. This is a minor version bump reflecting new functionality (content-based detection) without breaking existing API surface. Endpoint path, method, request schema, and response schema are unchanged.
Assumptions: Semantic versioning applies. Backward compatibility is maintained from the frontend perspective.
Rationale: The delegation plan specifies version 2.1.0. The changes are additive (new detection logic, new error detail precision) and do not alter the contract surface that frontend and backend agents implement against.
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

[2026-07-09] [Session 8] WI-CA-001 ALIGNMENT REVIEW REJECTED — CYCLE 1
DECISION: Alignment Agent rejected review request ALIGN-WI-CA-001-001 (status: REJECTED) due to 5 format violations in the JSON review request. The API contract itself was substantively compliant. Corrections required: (1) Remove residual Naut content. (2) Add reviewRequest wrapper key. (3) Change artefactsProduced to object array. (4) Remove redundant markdown alignment notes. (5) Remove decision-output fields (status, greenlightForNextAgent).
Assumptions: The contract docs/api-contract-wi-ca-001.md requires no modification. Only the review request format needs correction.
Rationale: Alignment Agent definition requires strict JSON format conformance. Pipeline gate enforcement blocks Femke-Naut-parallel activation until approval is granted.

[2026-07-09] [Session 8] WI-CA-001 ALIGNMENT REVIEW RESUBMISSION — CYCLE 2
DECISION: Resubmitted corrected review request at reviewCycle 2. All 5 format violations corrected. reviewRequest wrapper applied. artefactsProduced converted to object array. Residual content removed. Pipeline gate awaiting Alignment Agent approval.
Assumptions: Alignment Agent will approve the corrected format and set greenlightForNextAgent to true, enabling Archibald to produce the parallel delegation plan.
Rationale: All corrections from rejection feedback ALIGN-WI-CA-001-001 have been applied. The contract artefact is unchanged and substantively compliant.
