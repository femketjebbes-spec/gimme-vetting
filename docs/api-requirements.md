# API Requirements: Case Analyst Invoice List & Detail API

**Produced By**: Femke (Frontend Agent)
**Timestamp**: 2026-07-09 09:40
**Source Contract**: `docs/api-contract-wi-ca-001.md` (v1.0.0)
**Work Item**: WI-CA-001
**Status**: Complete

---

## Endpoint 1: Paginated Invoice List

| Property | Value |
|----------|-------|
| HTTP Method | GET |
| Path | `/api/v1/analyst/invoices` |
| Request Parameters | `page` (integer, default 0, >= 0), `size` (integer, default 50, 1-200), `sort` (string, default `id,asc`, format `field,direction`), `status` (string, enum: QUEUED, REJECTED_TYPE_A, REJECTED_TYPE_B), `search` (string, max 256 chars) |
| Expected Response | JSON object with `content` (array of invoice items), `totalElements` (integer), `totalPages` (integer), `currentPage` (integer), `pageSize` (integer) |
| Authentication Required | No |
| Frontend Consumer | `4-frontend/src/business-service/api/analystApi.js` (`fetchInvoiceList`) |

### Response Schema

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

### Error Responses

| Status Code | Description |
|-------------|-------------|
| 400 Bad Request | Invalid query parameters (e.g., page negative, size exceeds 200, search exceeds 256 chars) |

---

## Endpoint 2: Single Invoice Detail

| Property | Value |
|----------|-------|
| HTTP Method | GET |
| Path | `/api/v1/analyst/invoices/{id}` |
| Request Parameters | `id` (integer, required, > 0) — path variable |
| Expected Response | JSON object with invoice fields: `id`, `invoiceNumber`, `debtorName`, `address`, `bankAccountNumber`, `phoneNumber`, `status`, `poCStatus`, `rejectionType`, `resubmissionCount` |
| Authentication Required | No |
| Frontend Consumer | `4-frontend/src/business-service/api/analystApi.js` (`fetchInvoiceDetail`) |

### Response Schema

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

### Error Responses

| Status Code | Description |
|-------------|-------------|
| 404 Not Found | Invoice with requested ID does not exist |
| 400 Bad Request | Invalid invoice ID parameter (not a positive integer) |

---

## Architecture Decisions Applied

- **D-CA-001**: Resubmission uses Option A (update existing row, increment count)
- **D-CA-002**: Unauthenticated MVP endpoints
- **D-CA-003**: `resubmissionCount` field added via Flyway migration
- **D-CA-004**: API versioning via `/api/v1/analyst/` path prefix
- **D-CA-005**: React Router routing at `/analyst` path
