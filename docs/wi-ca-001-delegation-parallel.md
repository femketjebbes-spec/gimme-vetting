# Parallel Delegation Plan: WI-CA-001 — Case Analyst Invoice List & Detail

## Architecture Constraints

- **D-CA-001**: Resubmission uses Option A (update existing row, increment count). The `resubmissionCount` field in responses reflects the current submission count.
- **D-CA-002**: Analyst API endpoints are unauthenticated for MVP. This applies to the dashboard frontend which assumes no authentication layer.
- **D-CA-003**: Resubmission Count column added via Flyway migration `V2__add_resubmission_count.sql`. The column is `INTEGER NOT NULL DEFAULT 0`.
- **D-CA-004**: API endpoints use `/api/v1/analyst/` path prefix.
- **D-020** (from Session 5): No authentication on client portal endpoints for MVP.
- **D-026**: Unauthenticated MVP endpoints.

## Shared Contract

`docs/api-contract-wi-ca-001.md` (v1.0.0)

This contract defines two endpoints:

1. `GET /api/v1/analyst/invoices` — paginated list with filtering, sorting, and search
2. `GET /api/v1/analyst/invoices/{id}` — single invoice detail

Both endpoints return 10 fields per invoice: `id`, `invoiceNumber`, `debtorName`, `address`, `bankAccountNumber`, `phoneNumber`, `status`, `poCStatus`, `rejectionType`, `resubmissionCount`.

## Subtasks

### Subtask 1: Frontend — Case Analyst Dashboard

- **Assigned Agent**: Femke (Frontend Agent)
- **Input Artefact**: `docs/api-contract-wi-ca-001.md` (versioned API contract)
- **Output Artefact**: Frontend code in `4-frontend/src/business-service/`
- **Constraints**:
  - All frontend code must be placed in `4-frontend/src/business-service/` as requested by the user.
  - The AnalystDashboard component must consume both endpoints from the shared contract.
  - The list endpoint (`GET /api/v1/analyst/invoices`) supports query parameters: `page` (integer, 0-indexed, default 0), `size` (integer, 1-200, default 50), `sort` (string, format `field,direction`), `status` (string, comma-separated), `search` (string, max 256 characters).
  - The detail endpoint (`GET /api/v1/analyst/invoices/{id}`) takes a path variable `id` (Long, positive).
  - Both endpoints are unauthenticated. No auth headers, no token management, no session handling.
  - All fetch calls must target endpoints declared in the contract. All response parsing must conform to the JSON schema defined in the contract.
  - The `status` filter accepts: `QUEUED`, `REJECTED_TYPE_A`, `REJECTED_TYPE_B`.
  - The `poCStatus` field accepts: `VERIFIED`, `MISSING`, `PENDING`.
  - Error responses at 400 and 404 are defined in the contract and must be handled.
  - The existing frontend project uses React 18 with Vite build pipeline. Jest testing is configured via `jest.config.js`.
  - React Router is not currently a dependency. Adding routing for the `/analyst` path requires adding `react-router-dom` to `package.json` and modifying `main.jsx`. If adding React Router, the AnalystDashboard must be at route `/analyst` as specified in architectural decision line 134 of architecture-decisions.md.
- **Security Considerations**:
  - The search input (`search` parameter) must be bounded to 256 characters client-side before being sent to the API, consistent with the contract constraint.
  - The `id` path parameter in the detail endpoint must be validated as a positive integer before constructing the URL.
  - No authentication handling is required per D-CA-002.
  - Error messages displayed to the user must not expose raw API error details (stack traces, SQL, server internals) per S-006 and contract section 5.

### Subtask 2: Backend — Analyst API Endpoints

- **Assigned Agent**: Naut (Backend Agent)
- **Input Artefact**: `docs/api-contract-wi-ca-001.md` (versioned API contract)
- **Output Artefact**: Java backend source code in `5-backend/business-service/src/main/java/com/gimmevettingsolution/analyst/`
- **Constraints**:
  - Controller must be placed in `com.gimmevettingsolution.analyst` package.
  - Two endpoints must be implemented exactly as defined in the contract:
    1. `GET /api/v1/analyst/invoices` — paginated list with dynamic filtering via JPA `Specification`
    2. `GET /api/v1/analyst/invoices/{id}` — single invoice detail
  - The `Invoice` entity already exists in `5-backend/business-service/src/main/java/com/gimmevettingsolution/invoice/entity/Invoice.java`. A `resubmissionCount` field must be added to this entity.
  - Flyway migration `V2__add_resubmission_count.sql` must be created in `5-backend/business-service/src/main/resources/db/migration/`.
  - Query parameters must be validated: `page >= 0`, `1 <= size <= 200`, `sort` field must be from the allowlisted set (`id`, `invoiceNumber`, `debtorName`, `status`, `poCStatus`, `rejectionType`, `resubmissionCount`), `status` comma-separated values must be valid enums, `search` max 256 characters.
  - The `search` parameter must use parameterised JPA Specifications or native queries with bound parameters. String concatenation for SQL is prohibited.
  - Response DTO (`AnalystInvoiceDTO`) must match the contract schema exactly with all 10 required fields.
  - `status` field enum: `QUEUED`, `REJECTED_TYPE_A`, `REJECTED_TYPE_B`.
  - `poCStatus` field enum: `VERIFIED`, `MISSING`, `PENDING`.
  - `rejectionType` field: nullable, enum values `REJECTED_TYPE_A`, `REJECTED_TYPE_B`, or `null`.
  - Error responses must conform to the contract: 400 Bad Request with details array for validation errors, 404 Not Found for missing invoices.
  - Error responses must NOT expose stack traces, SQL, file paths, or server internals per contract section 5.4 (S-006).
  - Pagination uses Spring Data `Pageable`. Response wraps content in `Page<T>` structure with `content`, `totalElements`, `totalPages`, `currentPage`, `pageSize`.
- **Security Considerations**:
  - SQL injection prevention: The `search` parameter must never use string concatenation for SQL queries. All search operations must use parameterised JPA Specifications or native queries with bound parameters.
  - Input bounding: `search` bounded to 256 characters. `size` bounded to max 200. These prevent DoS via excessively long queries or result sets.
  - Error response security: Error responses must not expose stack traces, file paths, database internals, or SQL queries.
  - No authentication is implemented per D-CA-002 (documented MVP limitation).

## Parallel Phase Completion Criteria

The parallel phase is considered complete when both Femke and Naut have submitted their respective alignment review requests to the Alignment Agent.
