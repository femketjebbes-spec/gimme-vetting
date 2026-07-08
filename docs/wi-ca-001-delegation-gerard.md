# Delegation Plan: WI-CA-001 — Case Analyst Invoice List & Detail API

## Architecture Constraints

### D-001: Case-Insensitive Filename Matching
Filename matching performs full-string comparison after lowercase normalisation. This does not directly apply to the analyst API but is relevant for the resubmission logic that Gerard will delegate to Naut.

### D-026: No Authentication for MVP
All client-facing endpoints are unauthenticated for MVP. This applies to the analyst API endpoints. The architect recommends implementing authentication in a future iteration.

### D-CA-001: Resubmission Strategy (Option A)
When a client submits an invoice with an existing invoiceNumber, the existing row is updated and resubmissionCount is incremented by one. No new row is created per submission. Confirmed by stakeholder in Session 8.

### D-CA-002: Unauthenticated Analyst API
Analyst API endpoints (GET /api/v1/analyst/invoices and GET /api/v1/analyst/invoices/{id}) are unauthenticated for MVP, consistent with D-026. This is a documented security limitation.

### D-CA-003: Resubmission Count Column
Flyway migration V2__add_resubmission_count.sql adds an INTEGER NOT NULL DEFAULT 0 column to the invoices table.

### D-CA-004: API Versioning
Endpoints use /api/v1/analyst/ path prefix, consistent with existing /api/v1/ convention.

## Subtasks

### Subtask 1: Produce the Versioned API Contract for WI-CA-001
- **Assigned Agent**: API-Agent (Gerard)
- **Input Artefact**: `re-workspace/work-items/MVP-1-Case-analyst/wi-ca-001-analyst-api.md` (work item definition)
- **Output Artefact**: `docs/api-contract-wi-ca-001.md` (versioned API contract) and `docs/wi-ca-001-contract-ready.md` (contract readiness signal)
- **Constraints**:
  - The contract must define two endpoints: GET /api/v1/analyst/invoices (paginated list) and GET /api/v1/analyst/invoices/{id} (single invoice detail)
  - Both endpoints use the response format defined in the work item. The list endpoint returns content array with totalElements, totalPages, currentPage, pageSize metadata. The detail endpoint returns a single object.
  - Each response object includes: id, invoiceNumber, debtorName, address, bankAccountNumber, phoneNumber, status, poCStatus, rejectionType, resubmissionCount
  - The list endpoint accepts query parameters: page (integer, default 0), size (integer, default 50, max 200), sort (string, default "id,asc"), status (string, comma-separated filter), search (string, free-text partial match)
  - The detail endpoint returns 404 when the ID does not exist
  - Authentication: none (per D-CA-002). Add Javadoc/JSDoc note flagging this as an MVP limitation requiring future remediation.
  - Version: 1.0.0
- **Security Considerations**:
  - No authentication on read-only endpoints exposes invoice data to unauthenticated callers. This is a documented MVP limitation.
  - Search parameter must be sanitized against SQL injection. The backend must use parameterised queries or JPA Specifications, never string concatenation.
  - The search is a case-insensitive partial match across invoiceNumber, debtorName, and address fields. This is a database-level LIKE operation, not a full-text search engine.
  - Rate limiting is out of scope for MVP but must be noted as a future requirement to prevent bulk data extraction.
- **Acceptance Criteria**:
  - Contract is syntactically valid OpenAPI-compatible markdown
  - All response fields documented with types
  - All query parameters documented with types, defaults, constraints
  - Error responses documented (404 for detail endpoint)
  - Security notes included per D-CA-002

### Subtask 2: Delegate Backend Implementation to Naut (after Gerard submits alignment review)
- **Assigned Agent**: Naut (Backend Agent) — delegated by Gerard via Archibald pipeline
- **Input Artefact**: `docs/api-contract-wi-ca-001.md` (produced by Gerard)
- **Output Artefact**: Java backend source code in `5-backend/business-service/src/main/java/com/gimmevettingsolution/analyst/`
- **Constraints**:
  - Controller: `AnalystInvoiceController` in package `com.gimmevettingsolution.analyst`
  - Service: `AnalystInvoiceService` in package `com.gimmevettingsolution.analyst.service`
  - DTO: `AnalystInvoiceDTO` (separate from Invoice entity)
  - Add `resubmissionCount` field to `Invoice` entity (INTEGER, default 0, column name `resubmission_count`)
  - Create Flyway migration `V2__add_resubmission_count.sql`: `ALTER TABLE invoices ADD COLUMN resubmission_count INTEGER NOT NULL DEFAULT 0;`
  - Implement pagination using Spring Data `Pageable`
  - Implement dynamic filtering using JPA `Specification` for status filter and free-text search
  - Search must be case-insensitive partial match across invoiceNumber, debtorName, and address (database-level LIKE with `%` wildcards)
  - Resubmission logic (in the intake service, per D-CA-001): when a new invoice has an invoiceNumber that already exists in the database, increment resubmissionCount by 1 and update the existing row
  - Return 404 for non-existent invoice ID in the detail endpoint
  - No authentication layer (per D-CA-002). Add Javadoc note flagging this as MVP limitation.
- **Security Considerations**:
  - All database queries must use parameterised JPA Specifications. No string concatenation for SQL.
  - Search input must be bounded (max length) to prevent excessively long LIKE queries.
  - The resubmissionCount field is metadata only. No security implications.
  - Error responses must not expose stack traces or database internals.
- **Dependencies**: Requires the existing `Invoice` entity and `InvoiceRepository` to be present. Requires the Flyway migration to be applied before the service layer queries resubmissionCount.

## Gerard Pipeline Steps

1. Gerard produces `docs/api-contract-wi-ca-001.md` based on the work item specification and this delegation plan.
2. Gerard submits alignment review request to the Alignment Agent via `docs/alignment-review-request.md`.
3. Archibald reads the Alignment Agent decision from `docs/alignment-review-request.md`.
4. Upon Alignment Agent approval (`greenlightForNextAgent: true`), Archibald will produce the parallel delegation plan for Femke and Naut.

## Parallel Phase Notes

After Gerard completes and receives Alignment Agent approval, Archibald will produce `docs/wi-ca-001-delegation-parallel.md` assigning:
- Femke: Frontend dashboard implementation consuming the versioned API contract
- Naut: Backend implementation of the analyst endpoints and resubmission logic

Both parallel agents will consume the same `docs/api-contract-wi-ca-001.md` file.
