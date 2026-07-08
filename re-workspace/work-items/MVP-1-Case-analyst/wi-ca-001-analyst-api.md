# Work Item: WI-CA-001 — Case Analyst Invoice List & Detail API

**Parent Requirement:** RQ-010 (Case Analyst Read-Only Dashboard)
**Work Stream:** Backend — Analyst Read-Only View
**Business Objective:** OPE-002 — Give case analysts visibility into the invoice pipeline
**Created:** 2026-07-08
**Status:** Not started
**Priority:** Must have
**Estimated Effort:** 1 sprint

---

## 1. Requirement Statement

Create two REST API endpoints that allow the case analyst dashboard to fetch invoice data:

- `GET /api/v1/analyst/invoices` — paginated list with filtering and sorting
- `GET /api/v1/analyst/invoices/{id}` — single invoice detail

The API leverages the existing `Invoice` entity and `InvoiceRepository`.

---

## 2. API Contract

### GET /api/v1/analyst/invoices

**Request Parameters:**

| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 50 | Items per page (max 200) |
| sort | String | No | id,asc | Sort field(s) and direction, e.g., `status,asc` or `debtorName,asc` |
| status | String | No | — | Filter by status (comma-separated: `QUEUED,REJECTED_TYPE_A`) |
| search | String | No | — | Free-text search across invoiceNumber, debtorName, address |

**Response (200 OK):**

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

### GET /api/v1/analyst/invoices/{id}

**Path Variable:** `id` (Long, required)

**Response (200 OK):**

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

**Response (404 Not Found):** When ID does not exist.

---

## 3. Domain Model Changes

### Resubmission Count

The existing `Invoice` entity does **not** have a `resubmissionCount` field. Add:

```java
@Column(name = "resubmission_count", nullable = false)
private Integer resubmissionCount = 0;
```

**Migration:** Flyway migration `V2__add_resubmission_count.sql`:
```sql
ALTER TABLE invoices ADD COLUMN resubmission_count INTEGER NOT NULL DEFAULT 0;
```

### Resubmission Logic

When a new invoice is submitted with an `invoiceNumber` that already exists in the database:
1. Find the existing invoice by `invoiceNumber`
2. Increment `resubmissionCount` by 1
3. Update the existing record (or create a new one with incremented count — TBD by developer, architect decision needed)

**Open Question:** Should resubmissions create new rows or update the existing row? 
- **Option A (update):** Same row, count increments. Simpler history tracking but loses individual submission state.
- **Option B (new row):** Each submission is a new row, linked by invoiceNumber. More data but more complex queries.
- **Recommendation for MVP:** Option A — update the existing row and increment the counter.

---

## 4. Acceptance Criteria (Gherkin)

### Feature: Invoice List Endpoint

#### Scenario 1: Default list returns first page of invoices

```gherkin
Given there are 75 invoices in the database
When I request GET /api/v1/analyst/invoices
Then the response status is 200
And the response contains 50 invoices (default page size)
And the response totalPages is 2
And the response currentPage is 0
```

#### Scenario 2: Filter by status

```gherkin
Given there are 75 invoices: 30 QUEUED, 20 REJECTED_TYPE_A, 25 REJECTED_TYPE_B
When I request GET /api/v1/analyst/invoices?status=QUEUED
Then the response contains exactly 30 invoices
And all invoices have status "QUEUED"
```

#### Scenario 3: Search by invoice number

```gherkin
Given an invoice with invoiceNumber "INV-2026-0042" exists
When I request GET /api/v1/analyst/invoices?search=INV-2026-0042
Then the response contains exactly 1 invoice
And the invoice has invoiceNumber "INV-2026-0042"
```

#### Scenario 4: Search is case-insensitive partial match

```gherkin
Given an invoice with debtorName "Jan de Vries" exists
When I request GET /api/v1/analyst/invoices?search=de%20v
Then the response contains the invoice with debtorName "Jan de Vries"
```

#### Scenario 5: Non-existent invoice ID returns 404

```gherkin
Given no invoice with ID 9999 exists
When I request GET /api/v1/analyst/invoices/9999
Then the response status is 404
```

---

## 5. Implementation Notes

- Controller goes in `business-service` module: `com.gimmevettingsolution.analyst.AnalystInvoiceController`
- Service layer: `com.gimmevettingsolution.analyst.service.AnalystInvoiceService`
- Repository can leverage Spring Data JPA `Specification` for dynamic filtering
- Pagination uses Spring Data `Pageable`
- DTO class: `AnalystInvoiceDTO` (separate from `Invoice` entity for clean API contract)

---

## 6. Dependencies

- Depends on: existing `Invoice` entity and `InvoiceRepository` in `business-service`
- Depends on: Excel intake (WI-002 in MVP-1-Client) producing invoices in the database
- Blocks: frontend dashboard implementation
