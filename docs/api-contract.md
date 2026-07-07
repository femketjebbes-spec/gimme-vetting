# API Contract: Intake Pipeline — PoC Existence Verification

**Work Item:** WI-001 (PoC Existence Verification)
**Endpoint:** POST `/api/v1/intake`
**Version:** 1.0.0
**Date:** 2026-07-07
**Owner:** Gerard (API-Agent)
**Status:** Pending Alignment Agent approval

---

## 1. Overview

This contract defines the intake pipeline endpoint for invoice submission. The endpoint receives invoice data, verifies Proof of Correspondence (PoC) existence by matching the invoice number to a PoC filename in a configurable PoC store, and returns an appropriate response based on the verification result.

---

## 2. Endpoint Definition

| Property | Value |
|----------|-------|
| Path | `/api/v1/intake` |
| Method | `POST` |
| Authentication | None (PoC phase only) |
| Content-Type | `application/json` |
| Max Body Size | 10 KB |

---

## 3. Request Schema

### 3.1 Body

The request body MUST conform to the following JSON schema. All fields are mandatory. The adapter layer validates this schema before invoking any backend service. Requests that fail validation receive a `400 Bad Request` response with a structured error body.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["invoiceNumber", "debtorName", "address", "bankAccountNumber", "phoneNumber"],
  "properties": {
    "invoiceNumber": {
      "type": "string",
      "minLength": 1,
      "maxLength": 128,
      "pattern": "^[A-Za-z0-9\\-_.]+$",
      "description": "System Identifier. Used for PoC filename matching. Alphanumeric characters, hyphens, underscores, and periods only. Sanitised to prevent path traversal."
    },
    "debtorName": {
      "type": "string",
      "minLength": 1,
      "maxLength": 256,
      "description": "Full legal name of the debtor."
    },
    "address": {
      "type": "object",
      "required": ["street", "postalCode", "city", "country"],
      "properties": {
        "street": {
          "type": "string",
          "minLength": 1,
          "maxLength": 256,
          "description": "Street name and house number."
        },
        "postalCode": {
          "type": "string",
          "minLength": 1,
          "maxLength": 16,
          "description": "Postal or ZIP code."
        },
        "city": {
          "type": "string",
          "minLength": 1,
          "maxLength": 128,
          "description": "City name."
        },
        "country": {
          "type": "string",
          "minLength": 2,
          "maxLength": 2,
          "pattern": "^[A-Z]{2}$",
          "description": "ISO 3166-1 alpha-2 country code."
        }
      },
      "additionalProperties": false
    },
    "bankAccountNumber": {
      "type": "string",
      "minLength": 1,
      "maxLength": 34,
      "description": "IBAN or national account number."
    },
    "phoneNumber": {
      "type": "string",
      "minLength": 1,
      "maxLength": 20,
      "pattern": "^[+]?[0-9\\-() ]+$",
      "description": "Contact phone number in E.164 or national format."
    }
  },
  "additionalProperties": false
}
```

### 3.2 Header Constraints

| Header | Required | Value |
|--------|----------|-------|
| `Content-Type` | Yes | `application/json` |
| `Accept` | No | `application/json` |

---

## 4. Response Schemas

### 4.1 202 Accepted — PoC Verified

The invoice passed the PoC existence gate and is queued for business rule check (deferred) and case analyst batch acceptance.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["status", "nextStep", "invoiceId"],
  "properties": {
    "status": {
      "type": "string",
      "enum": ["POC_VERIFIED"],
      "description": "PoC existence verification passed."
    },
    "nextStep": {
      "type": "string",
      "enum": ["BUSINESS_RULE_CHECK"],
      "description": "Invoice queued for business rule check (deferred) and case analyst batch acceptance."
    },
    "invoiceId": {
      "type": "string",
      "pattern": "^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$",
      "description": "UUID v4 identifier generated for the accepted invoice."
    }
  },
  "additionalProperties": false
}
```

**Example Response:**

```json
{
  "status": "POC_VERIFIED",
  "nextStep": "BUSINESS_RULE_CHECK",
  "invoiceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### 4.2 400 Bad Request — Type A Rejection (No PoC Found)

The invoice failed the PoC existence gate. No matching PoC filename was found in the PoC store. The client may re-submit at any time.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["status", "rejectionReason", "resubmitAllowed"],
  "properties": {
    "status": {
      "type": "string",
      "enum": ["REJECTED_TYPE_A"],
      "description": "Type A rejection: missing PoC."
    },
    "rejectionReason": {
      "type": "string",
      "description": "Human-readable rejection reason. Format: No PoC linked to invoice {invoiceNumber}"
    },
    "resubmitAllowed": {
      "type": "boolean",
      "enum": [true],
      "description": "Always true for Type A rejections. Client may re-submit indefinitely."
    }
  },
  "additionalProperties": false
}
```

**Example Response:**

```json
{
  "status": "REJECTED_TYPE_A",
  "rejectionReason": "No PoC linked to invoice INV-2026-0042",
  "resubmitAllowed": true
}
```

### 4.3 400 Bad Request — Validation Error

The request body failed schema validation. Returns a structured error body listing each violated constraint.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["status", "errors"],
  "properties": {
    "status": {
      "type": "string",
      "enum": ["VALIDATION_ERROR"],
      "description": "Request body failed schema validation."
    },
    "errors": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["field", "message"],
        "properties": {
          "field": {
            "type": "string",
            "description": "Dot-notated field path (e.g., address.city)."
          },
          "message": {
            "type": "string",
            "description": "Human-readable validation error description."
          }
        },
        "additionalProperties": false
      },
      "minItems": 1
    }
  },
  "additionalProperties": false
}
```

**Example Response:**

```json
{
  "status": "VALIDATION_ERROR",
  "errors": [
    {
      "field": "invoiceNumber",
      "message": "String length must be between 1 and 128"
    },
    {
      "field": "address.country",
      "message": "String does not match pattern: ^[A-Z]{2}$"
    }
  ]
}
```

### 4.4 503 Service Unavailable — PoC Store Inaccessible

The PoC store is unreachable or the storage layer returned an error. The client may retry.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["status", "errorDetail"],
  "properties": {
    "status": {
      "type": "string",
      "enum": ["SERVICE_UNAVAILABLE"],
      "description": "PoC store is currently inaccessible."
    },
    "errorDetail": {
      "type": "string",
      "description": "Technical description of the storage failure. Must NOT contain the PoC store path."
    }
  },
  "additionalProperties": false
}
```

**Example Response:**

```json
{
  "status": "SERVICE_UNAVAILABLE",
  "errorDetail": "Storage backend connection timeout after 3000ms"
}
```

### 4.5 413 Payload Too Large

The request body exceeds the maximum allowed size (10 KB).

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["status", "errorDetail"],
  "properties": {
    "status": {
      "type": "string",
      "enum": ["PAYLOAD_TOO_LARGE"],
      "description": "Request body exceeds maximum allowed size."
    },
    "errorDetail": {
      "type": "string",
      "description": "Maximum allowed size: 10240 bytes"
    }
  },
  "additionalProperties": false
}
```

---

## 5. Error Mapping Registry

The adapter layer maps internal backend errors to clean API responses using the following registry. Backend error codes are translated to user-friendly responses per security and UX requirements.

| Backend Error | HTTP Status | API Response `status` | Mapping Rationale |
|--------------|-------------|-----------------------|-------------------|
| `SCHEMA_VALIDATION_FAILED` | 400 | `VALIDATION_ERROR` | Request body violated JSON schema constraints. |
| `POC_NOT_FOUND` | 400 | `REJECTED_TYPE_A` | No matching PoC filename. Type A rejection. |
| `POC_STORE_UNAVAILABLE` | 503 | `SERVICE_UNAVAILABLE` | PoC store connection failure. |
| `BODY_SIZE_EXCEEDED` | 413 | `PAYLOAD_TOO_LARGE` | Request body exceeds max size. |
| `PATH_TRAVERSAL_DETECTED` | 400 | `VALIDATION_ERROR` | Invoice number contained path traversal patterns. Treated as validation error. |
| `DB_CONSTRAINT_VIOLATION` | 500 | `INTERNAL_ERROR` | Database constraint violated. Root cause masked for security. |
| `INTERNAL_ERROR` | 500 | `INTERNAL_ERROR` | Unhandled internal error. Detailed message omitted from response. |

---

## 6. Architectural Constraints

| Constraint ID | Requirement | Enforcement |
|---------------|-------------|-------------|
| D-001 | Filename matching is case-insensitive. `poCFileName.toLowerCase() == invoiceNumber.toLowerCase()` | Applied by backend PoC store service. The API contract does not alter this. |
| D-002 | Multiple PoC files for one invoice are not an error. One match suffices. | Applied by backend PoC store service. The API contract does not alter this. |
| D-003 | PoC store path is configurable, injected at runtime. Must NOT appear in error messages or logs. | Enforcement handled by backend configuration. The `503` error response schema explicitly forbids path exposure in `errorDetail`. |
| D-004 | No business rule checks (RQ-002, RQ-003). Deferred to subsequent work items. | The `202` response uses `nextStep: BUSINESS_RULE_CHECK` as a placeholder. The backend does not execute business rule checks for WI-001. |
| D-005 | Endpoint is unauthenticated for the PoC phase. | No authentication middleware applied to POST `/api/v1/intake` in this phase. |

---

## 7. Security Requirements

| # | Requirement | Enforcement Layer |
|---|-------------|-------------------|
| S-001 | Request body validated against JSON schema before any backend service invocation. | Adapter layer (Zod / JSON Schema validator). |
| S-002 | Invoice number sanitised to prevent path traversal attacks. Reject values containing `../`, `..\\`, or absolute path patterns. | Adapter layer request transformer. |
| S-003 | PoC store credentials stored in secure configuration store, not version-controlled files. | Backend configuration (not part of API contract). |
| S-004 | Maximum request body size enforced: 10 KB. | Adapter layer middleware. |
| S-005 | PoC store path never exposed in error responses or logs. | Backend error handling. Contract schema for `503` `errorDetail` field explicitly prohibits path exposure. |
| S-006 | Request body size enforcement prevents denial-of-service. | Adapter layer body size middleware. |

---

## 8. PoC Matching Semantics

The PoC existence verification operates as follows:

1. The adapter receives the request body and validates it against the JSON schema defined in Section 3.1.
2. If validation passes, the adapter forwards the request to the backend.
3. The backend PoC store service performs case-insensitive full-string filename matching: `poCFileName.toLowerCase() == invoiceNumber.toLowerCase()`.
4. If at least one match is found, the service returns success. The adapter responds with `202 Accepted`.
5. If no match is found, the service returns `POC_NOT_FOUND`. The adapter responds with `400 Bad Request` and `REJECTED_TYPE_A`.
6. If the PoC store is inaccessible, the service returns `POC_STORE_UNAVAILABLE`. The adapter responds with `503 Service Unavailable`.

---

## 9. Contract Versioning

| Version | Date | Change |
|---------|------|--------|
| 1.0.0 | 2026-07-07 | Initial contract for WI-001 PoC Existence Verification. |

---

## 10. Dependencies

| Dependency | Source | Notes |
|------------|--------|-------|
| RQ-001 | [`re-workspace/requirements-spec.md`](re-workspace/requirements-spec.md) | PoC Existence Verification requirement. |
| D-001 | [`agent-definitions/architecture-decisions.md`](agent-definitions/architecture-decisions.md) | Case-insensitive filename matching. |
| D-002 | [`agent-definitions/architecture-decisions.md`](agent-definitions/architecture-decisions.md) | Multiple PoC files tolerated. |
| D-003 | [`agent-definitions/architecture-decisions.md`](agent-definitions/architecture-decisions.md) | Configurable PoC store path. |
| WI-001 | [`re-workspace/work-items/wi-001-poc-existence-verification.md`](re-workspace/work-items/wi-001-poc-existence-verification.md) | Formal work item definition. |
