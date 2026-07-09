# API Contract: Case Analyst Invoice List & Detail API

**Work Item:** WI-CA-001 (Case Analyst Invoice List & Detail API)
**Endpoints:** `GET /api/v1/analyst/invoices`, `GET /api/v1/analyst/invoices/{id}`
**Version:** 1.0.0
**Date:** 2026-07-09
**Owner:** Gerard (API-Agent)
**Status:** Submitted to Alignment Agent for review

---

## 1. Overview

This contract defines two REST API endpoints that allow the case analyst dashboard to fetch invoice data from the intake pipeline. Both endpoints are unauthenticated for MVP (per D-CA-002) and return invoice data from the existing `invoices` table.

**Architectural Decisions Applied:**
- D-CA-001: Resubmission uses Option A (update existing row, increment count). The `resubmissionCount` field in responses reflects the current submission count.
- D-CA-002: No authentication on analyst API endpoints. Documented as MVP limitation.
- D-CA-003: Resubmission Count column added via Flyway migration `V2__add_resubmission_count.sql`.
- D-CA-004: API versioning uses `/api/v1/analyst/` path prefix.

---

## 2. Endpoint 1: Paginated Invoice List

### 2.1 Definition

| Property | Value |
|----------|-------|
| Path | `/api/v1/analyst/invoices` |
| Method | `GET` |
| Authentication | None (MVP limitation per D-CA-002) |
| Content-Type | `application/json` |

### 2.2 Query Parameters

| Parameter | Type | Required | Default | Constraints | Description |
|-----------|------|----------|---------|-------------|-------------|
| `page` | integer | No | `0` | `>= 0` | Page number (0-indexed) |
| `size` | integer | No | `50` | `1 <= size <= 200` | Items per page |
| `sort` | string | No | `id,asc` | Format: `field,direction` where field is one of `id`, `invoiceNumber`, `debtorName`, `status`, `poCStatus`, `rejectionType`, `resubmissionCount` and direction is `asc` or `desc` | Sort field(s) and direction |
| `status` | string | No | none | Comma-separated list of valid statuses: `QUEUED`, `REJECTED_TYPE_A`, `REJECTED_TYPE_B` | Filter by invoice status |
| `search` | string | No | none | Max length: 256 characters. Case-insensitive partial match across `invoiceNumber`, `debtorName`, and `address` fields. | Free-text search |

**Security Note:** The `search` parameter must be sanitised against SQL injection. The backend must use parameterised JPA Specifications or native queries with bound parameters. String concatenation for SQL is prohibited. Search input must be bounded to 256 characters to prevent excessively long LIKE queries.

### 2.3 Response Schema — 200 OK

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["content", "totalElements", "totalPages", "currentPage", "pageSize"],
  "properties": {
    "content": {
      "type": "array",
      "items": {
        "$ref": "#/definitions/AnalystInvoiceItem"
      },
      "description": "Array of invoice items for the requested page."
    },
    "totalElements": {
      "type": "integer",
      "description": "Total number of invoices matching the query across all pages."
    },
    "totalPages": {
      "type": "integer",
      "description": "Total number of pages available."
    },
    "currentPage": {
      "type": "integer",
      "description": "Current page number (0-indexed)."
    },
    "pageSize": {
      "type": "integer",
      "description": "Number of items per page."
    }
  },
  "definitions": {
    "AnalystInvoiceItem": {
      "type": "object",
      "required": ["id", "invoiceNumber", "debtorName", "address", "bankAccountNumber", "phoneNumber", "status", "poCStatus", "rejectionType", "resubmissionCount"],
      "properties": {
        "id": {
          "type": "integer",
          "format": "int64",
          "description": "Database primary key."
        },
        "invoiceNumber": {
          "type": "string",
          "maxLength": 128,
          "pattern": "^[A-Za-z0-9\\-_.]+$",
          "description": "System Identifier. Used for PoC filename matching."
        },
        "debtorName": {
          "type": "string",
          "maxLength": 256,
          "description": "Full legal name of the debtor."
        },
        "address": {
          "type": "string",
          "maxLength": 512,
          "description": "Concatenated address: street, postal code, city (as stored in the database)."
        },
        "bankAccountNumber": {
          "type": "string",
          "maxLength": 34,
          "description": "IBAN or national account number."
        },
        "phoneNumber": {
          "type": "string",
          "maxLength": 20,
          "description": "Contact phone number."
        },
        "status": {
          "type": "string",
          "enum": ["QUEUED", "REJECTED_TYPE_A", "REJECTED_TYPE_B"],
          "description": "Current invoice processing status."
        },
        "poCStatus": {
          "type": "string",
          "enum": ["VERIFIED", "MISSING", "PENDING"],
          "description": "Proof of Correspondence verification status."
        },
        "rejectionType": {
          "type": ["string", "null"],
          "enum": [null, "REJECTED_TYPE_A", "REJECTED_TYPE_B"],
          "description": "Rejection type, if applicable. Null for non-rejected invoices."
        },
        "resubmissionCount": {
          "type": "integer",
          "description": "Number of times this invoice has been resubmitted by the client."
        }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false
}
```

### 2.4 Example Response — 200 OK

```json
{
  "content": [
    {
      "id": 1,
      "invoiceNumber": "INV-2026-0042",
      "debtorName": "Jan de Vries",
      "address": "Voorbeeldstraat 1, 1234AB Amsterdam",
      "bankAccountNumber": "NL12BUNQ0123456789",
      "phoneNumber": "+31612345678",
      "status": "QUEUED",
      "poCStatus": "VERIFIED",
      "rejectionType": null,
      "resubmissionCount": 0
    }
  ],
  "totalElements": 150,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 50
}
```

### 2.5 Error Response — 400 Bad Request

Returned when query parameters are invalid (e.g., `page` is negative, `size` exceeds 200, `sort` references an unsupported field, `search` exceeds 256 characters).

```json
{
  "error": "Bad Request",
  "message": "Parameter validation failed",
  "details": [
    {
      "field": "search",
      "violation": "max",
      "limit": 256,
      "message": "Search parameter must not exceed 256 characters"
    }
  ]
}
```

---

## 3. Endpoint 2: Single Invoice Detail

### 3.1 Definition

| Property | Value |
|----------|-------|
| Path | `/api/v1/analyst/invoices/{id}` |
| Method | `GET` |
| Authentication | None (MVP limitation per D-CA-002) |
| Content-Type | `application/json` |
| Path Variable | `id` (Long, required, `> 0`) |

### 3.2 Response Schema — 200 OK

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["id", "invoiceNumber", "debtorName", "address", "bankAccountNumber", "phoneNumber", "status", "poCStatus", "rejectionType", "resubmissionCount"],
  "properties": {
    "id": {
      "type": "integer",
      "format": "int64",
      "description": "Database primary key."
    },
    "invoiceNumber": {
      "type": "string",
      "maxLength": 128,
      "pattern": "^[A-Za-z0-9\\-_.]+$",
      "description": "System Identifier. Used for PoC filename matching."
    },
    "debtorName": {
      "type": "string",
      "maxLength": 256,
      "description": "Full legal name of the debtor."
    },
    "address": {
      "type": "string",
      "maxLength": 512,
      "description": "Concatenated address: street, postal code, city (as stored in the database)."
    },
    "bankAccountNumber": {
      "type": "string",
      "maxLength": 34,
      "description": "IBAN or national account number."
    },
    "phoneNumber": {
      "type": "string",
      "maxLength": 20,
      "description": "Contact phone number."
    },
    "status": {
      "type": "string",
      "enum": ["QUEUED", "REJECTED_TYPE_A", "REJECTED_TYPE_B"],
      "description": "Current invoice processing status."
    },
    "poCStatus": {
      "type": "string",
      "enum": ["VERIFIED", "MISSING", "PENDING"],
      "description": "Proof of Correspondence verification status."
    },
    "rejectionType": {
      "type": ["string", "null"],
      "enum": [null, "REJECTED_TYPE_A", "REJECTED_TYPE_B"],
      "description": "Rejection type, if applicable. Null for non-rejected invoices."
    },
    "resubmissionCount": {
      "type": "integer",
      "description": "Number of times this invoice has been resubmitted by the client."
    }
  },
  "additionalProperties": false
}
```

### 3.3 Example Response — 200 OK

```json
{
  "id": 1,
  "invoiceNumber": "INV-2026-0042",
  "debtorName": "Jan de Vries",
  "address": "Voorbeeldstraat 1, 1234AB Amsterdam",
  "bankAccountNumber": "NL12BUNQ0123456789",
  "phoneNumber": "+31612345678",
  "status": "QUEUED",
  "poCStatus": "VERIFIED",
  "rejectionType": null,
  "resubmissionCount": 0
}
```

### 3.4 Error Response — 404 Not Found

Returned when the requested invoice ID does not exist in the database.

```json
{
  "error": "Not Found",
  "message": "Invoice with id {id} not found"
}
```

### 3.5 Error Response — 400 Bad Request

Returned when the `id` path variable is not a valid positive long integer.

```json
{
  "error": "Bad Request",
  "message": "Invalid invoice id parameter"
}
```

---

## 4. Error Mapping Registry

| Backend Error | API Response | Status Code | Notes |
|--------------|--------------|-------------|-------|
| `EntityNotFoundException` for Invoice | 404 Not Found with message | 404 | Detail endpoint only |
| `ConstraintViolationException` for query params | 400 Bad Request with details array | 400 | List endpoint only |
| `DataIntegrityViolationException` (unique constraint on invoice_number) | 409 Conflict with message | 409 | May occur during resubmission via intake endpoint |
| Generic `Exception` | 500 Internal Server Error with generic message | 500 | Error responses must not expose stack traces, SQL, or server internals (per S-006) |

---

## 5. Security Considerations

1. **No Authentication (D-CA-002):** Both endpoints are unauthenticated for MVP. This exposes all invoice data including debtor names, addresses, bank account numbers, and phone numbers to any caller. This is a documented MVP limitation. Future remediation: implement API key authentication or basic auth, plus rate limiting.

2. **SQL Injection Prevention:** The `search` parameter must never be used in string concatenation for SQL queries. All search operations must use parameterised JPA Specifications or native queries with bound parameters.

3. **Input Bounding:** The `search` parameter is bounded to 256 characters. The `size` query parameter is bounded to max 200. These bounds prevent DoS via excessively long queries or result sets.

4. **Error Response Security (S-006):** Error responses must not expose stack traces, file paths, database internals, or SQL queries.

---

## 6. Data Model Reference

The response schema maps to the `invoices` table managed by Flyway migration `V1__create_invoices_table.sql` and the `resubmission_count` column added by `V2__add_resubmission_count.sql`.

### Existing `Invoice` Entity Fields

| Field | DB Column | Type | Notes |
|-------|-----------|------|-------|
| `id` | `id` | `BIGINT` (PK, auto-generated) | Primary key |
| `invoiceNumber` | `invoice_number` | `VARCHAR(128)` | Unique constraint |
| `debtorName` | `debtor_name` | `VARCHAR(256)` | |
| `address` | `address` | `VARCHAR(512)` | Single column storage |
| `bankAccountNumber` | `bank_account_number` | `VARCHAR(34)` | |
| `phoneNumber` | `phone_number` | `VARCHAR(20)` | |
| `poCStatus` | `poc_status` | `VARCHAR(32)` | |
| `rejectionType` | `rejection_type` | `VARCHAR(32)` | |
| `status` | `status` | `VARCHAR(32)` | |

### New Field (per D-CA-003)

| Field | DB Column | Type | Default | Notes |
|-------|-----------|------|---------|-------|
| `resubmissionCount` | `resubmission_count` | `INTEGER` | `0` | NOT NULL |

---

## 7. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-07-09 | Initial contract for WI-CA-001. Defines paginated list and single invoice detail endpoints. |
