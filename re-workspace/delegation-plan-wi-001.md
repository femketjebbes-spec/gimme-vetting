# Delegation Plan: WI-001 — PoC Existence Verification

**Work Item:** [`re-workspace/work-items/wi-001-poc-existence-verification.md`](re-workspace/work-items/wi-001-poc-existence-verification.md)
**Status:** Approved — D-001, D-002, D-003 confirmed
**Date:** 2026-07-07
**Pipeline:** Gerard (API contract) → Alignment Agent gate → Naut (backend implementation)

---

## Phase 1: Gerard — Intake Pipeline API Contract

### Assignment
Gerard shall produce `docs/api-contract.md` defining the intake pipeline endpoint. This endpoint receives invoice submissions and performs PoC existence verification.

### Input
- WI-001 specification: [`re-workspace/work-items/wi-001-poc-existence-verification.md`](re-workspace/work-items/wi-001-poc-existence-verification.md)
- Architecture decisions: [`agent-definitions/architecture-decisions.md`](agent-definitions/architecture-decisions.md) (D-001, D-002, D-003)
- Requirements spec: [`re-workspace/requirements-spec.md`](re-workspace/requirements-spec.md) (RQ-001)

### Output Artefacts
1. `docs/api-contract.md` — Formal API contract specification
2. `docs/alignment-review-request.md` — JSON review request submitted to Alignment Agent

### Contract Requirements

The API contract must define:

**Endpoint:** POST `/api/v1/intake`

**Request Body:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| invoiceNumber | string | Yes | System Identifier. Used for PoC filename matching. |
| debtorName | string | Yes | Debtor full name. |
| address | object | Yes | Street, postal code, city, country. |
| bankAccountNumber | string | Yes | IBAN or national account number. |
| phoneNumber | string | Yes | Contact phone number. |

**Response — 202 Accepted (PoC verified):**
| Field | Type | Description |
|-------|------|-------------|
| status | string | "POC_VERIFIED" |
| nextStep | string | "BUSINESS_RULE_CHECK" |
| invoiceId | string | Generated identifier for the accepted invoice. |

**Response — 400 Bad Request (Type A Rejection):**
| Field | Type | Description |
|-------|------|-------------|
| status | string | "REJECTED_TYPE_A" |
| rejectionReason | string | "No PoC linked to invoice {invoiceNumber}" |
| resubmitAllowed | boolean | true |

**Response — 503 Service Unavailable (PoC store inaccessible):**
| Field | Type | Description |
|-------|------|-------------|
| status | string | "SERVICE_UNAVAILABLE" |
| errorDetail | string | Description of the storage failure. |

### Architectural Constraints

1. The endpoint must use case-insensitive filename matching per D-001. The PoC store path is a configurable value injected at runtime per D-003.
2. Multiple PoC files for one invoice do not trigger rejection per D-002.
3. The endpoint must not perform business rule checks (RQ-002, RQ-003). These are deferred to subsequent work items.
4. The endpoint must not require authentication for submission. This is an assumption to be confirmed by the user.
5. The response must not expose the PoC store path in error messages or logs per D-003 security implications.

### Security Requirements

- The request body must be validated against the contract schema before any backend service is invoked. Invalid or malformed requests receive 400 Bad Request with a structured error body.
- The invoice number field must be sanitised to prevent path traversal attacks (e.g., `../../etc/passwd` as an invoice number).
- The PoC store path must be stored in a secure configuration store, not in version-controlled files.
- The endpoint must enforce a maximum request body size to prevent denial-of-service.

### Alignment Agent Gate

After producing the API contract, Gerard must submit `docs/alignment-review-request.md` to the Alignment Agent. Archibald must read the Alignment Agent decision and confirm `greenlightForNextAgent` is `true` with `nextAgentInPipeline` set to `Naut` before producing Phase 2 delegation.

---

## Phase 2: Naut — Backend Implementation

### Activation Gate

Naut activates only after Archibald reads the Alignment Agent approval from `docs/alignment-review-request.md` and confirms `greenlightForNextAgent` is `true` with `nextAgentInPipeline` set to `Naut`.

### Assignment
Naut shall implement the PoC existence verification logic within the Business Service.

### Input
- `docs/api-contract.md` produced by Gerard
- Archibald's phase 2 delegation plan (produced after Alignment Agent approval)
- Architecture decisions: [`agent-definitions/architecture-decisions.md`](agent-definitions/architecture-decisions.md) (D-001, D-002, D-003)
- WI-001 specification: [`re-workspace/work-items/wi-001-poc-existence-verification.md`](re-workspace/work-items/wi-001-poc-existence-verification.md)

### Output Artefacts
1. JUnit 5 test classes in `5-backend/business-service/src/test/java/com/gimmevettingsolution/`
2. Java production classes in `5-backend/business-service/src/main/java/com/gimmevettingsolution/`
3. `docs/alignment-review-request.md` — JSON review request submitted to Alignment Agent

### Subtasks for Naut

**Subtask N-001: Invoice Entity**
Create the Invoice entity class with fields: invoiceNumber, debtorName, address, bankAccountNumber, phoneNumber, poCStatus, rejectionType, status. The entity must use JPA annotations consistent with the existing Spring Boot configuration (PostgreSQL via Hibernate).

**Subtask N-002: PoC Store Service**
Create a PoCStoreService interface and implementation. The service receives a configurable PoC store path via Spring configuration. It provides a method `boolean hasMatchingPoC(String invoiceNumber)` that performs case-insensitive filename matching per D-001. The implementation must handle path traversal sanitisation of the invoiceNumber parameter.

**Subtask N-003: Intake Controller**
Create a controller handling POST `/api/v1/intake`. The controller receives the request body, invokes the PoCStoreService, and returns the appropriate response (202 accepted or 400 rejected) per the API contract.

**Subtask N-004: Integration Tests**
Create integration tests using mocked PoC store that verify the full intake pipeline: PoC match routes to accepted, PoC mismatch routes to Type A rejection.

### Architectural Constraints

1. Naut must follow the controller-service-repository layering pattern as documented in the architecture decisions file.
2. All code must be implemented using strict TDD: Testing Mode writes tests first, Implementation Mode writes production code.
3. The PoC store path must be injected via `application.yml` configuration. No hardcoded paths.
4. Naut must not modify `docs/api-contract.md`. The contract is owned by Gerard.
5. Naut must not implement business rule checks (RQ-002, RQ-003). These are explicitly out of scope for WI-001.
6. Naut must not add authentication middleware. The endpoint is unauthenticated for the PoC phase. This must be flagged as a future requirement.

### Security Requirements

- Invoice number input must be sanitised to prevent path traversal.
- The PoC store path must not appear in any log output or error response.
- The controller must validate all request body fields against the contract schema before invoking business logic.
- Naut must add a comment flagging that authentication is absent and should be added in a future work item.

---

## Phase 3: Alignment Agent Gate

After Naut completes implementation, Naut submits `docs/alignment-review-request.md` to the Alignment Agent. The Alignment Agent validates Naut's artefacts against Robbie's requirements and Archibald's specs.

Since Naut is the last agent in the initial pipeline sequence, no downstream agent activation follows Alignment Agent approval.

---

## Pipeline Flow

```
[Archibald] → Phase 1: Gerard → [Alignment Agent] → Phase 2: Naut → [Alignment Agent] → Complete
```

---

## Summary Visual

See [`agent-definitions/architect-agent/models/2026-07-07-session1-wi001-delegation.mmd`](agent-definitions/architect-agent/models/2026-07-07-session1-wi001-delegation.mmd) for the Mermaid delegation flow diagram.
