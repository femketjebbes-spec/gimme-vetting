# Work Item: WI-CA-002 — Case Analyst Read-Only Dashboard Frontend

**Parent Requirement:** RQ-010 (Case Analyst Read-Only Dashboard)
**Work Stream:** Frontend — Analyst Read-Only View
**Business Objective:** OPE-002 — Give case analysts visibility into the invoice pipeline
**Created:** 2026-07-08
**Status:** Not started
**Priority:** Must have
**Estimated Effort:** 1–2 sprints

---

## 1. Requirement Statement

Build a **read-only dashboard** for case analysts that displays invoices from the API (`GET /api/v1/analyst/invoices`) in a high-density, full-screen table. The dashboard supports:

- Paginated invoice list with filtering (status) and search
- Clicking a row opens a slide-over drawer with full invoice details
- Status badges with color coding
- Resubmission count indicator (dark blue arrow icon on resubmitted invoices)

**Key design constraint:** Read-only. No write actions, no validation buttons, no return/reject modals.

**Design reference:** [`docs/dashboard-specs-autovetting.md`](docs/dashboard-specs-autovetting.md) provides the full design spec. For MVP, implement the core table + drawer only — skip stats tab, settings tab, and archived "Dossiers" tab.

---

## 2. UI Components

### 2.1 Sidebar (Minimal)

| Tab | Icon | Visibility |
|-----|------|------------|
| Invoices | `fa-file-invoice` | Active (default) |
| Statistics | `fa-chart-line` | Hidden for MVP |
| Settings | `fa-gear` | Hidden for MVP |

The "Dossiers" (archived) tab is hidden — out of scope for MVP.

### 2.2 Invoice Table

Full-screen, compact table with the following columns:

| Column | Field | Width | Notes |
|--------|-------|-------|-------|
| Status | `status` | Auto | Colored badge |
| Invoice Number | `invoiceNumber` | Fixed | Clickable row trigger |
| Debtor Name | `debtorName` | Fixed | — |
| Address | `address` | Fixed | Truncated with ellipsis |
| PO Status | `poCStatus` | Fixed | VERIFIED / PENDING / FAILED |
| Resubmit | — | 32px | Dark blue arrow icon if `resubmissionCount > 0` |

**Row height:** ≤ 36px (per NFR-002)
**Column styling:** Per design spec — Poppins for headers, Arimo for data, Slate color palette.

### 2.3 Status Badges

| Status | Badge Color | Icon |
|--------|-------------|------|
| QUEUED | Amber/Gold (`#C59B27`) | `fa-circle-exclamation` |
| REJECTED_TYPE_A | Red (`#DC2626`) | `fa-circle-xmark` |
| REJECTED_TYPE_B | Slate (`#64748B`) | `fa-circle-xmark` |
| VALIDATED | Green (`#22C55E`) | `fa-circle-check` |

### 2.4 Slide-Over Drawer (Detail)

When clicking a table row, a drawer slides in from the right over the table:

**Drawer content:**
- Debtor name (large heading)
- Debtor address
- IBAN / bank account number
- Phone number
- Invoice number
- Status badge
- PoC status
- Rejection reason (if applicable, from `rejectionType`)
- "Bekijken" link/button for document view (placeholder — no actual document viewing in MVP)

**No action buttons** in the drawer for MVP — no "Validatie afronden", no "Terugsturen", no "Afwijzen".

### 2.5 Search & Filter Bar

Above the table:
- Search input (free-text, debounced)
- Status filter dropdown (multi-select: QUEUED, REJECTED_TYPE_A, REJECTED_TYPE_B)
- Clear filters button

---

## 3. State Management

| State | Source | Description |
|-------|--------|-------------|
| `invoices` | API response | Paginated list from `GET /api/v1/analyst/invoices` |
| `selectedInvoice` | User click | Full invoice object from `GET /api/v1/analyst/invoices/{id}` |
| `drawerOpen` | User click | Boolean, controls drawer visibility |
| `filters` | User input | `{ status: string[], search: string }` |
| `pagination` | API response | `{ currentPage, pageSize, totalPages, totalElements }` |

---

## 4. Acceptance Criteria (Gherkin)

### Feature: Read-Only Analyst Dashboard

#### Scenario 1: Dashboard loads and displays invoices

```gherkin
Given the backend API is running and returns 25 invoices
When the case analyst opens the dashboard
Then the invoice table displays the first page (50 items or fewer)
And each row shows: status badge, invoice number, debtor name, address, PO status
And QUEUED invoices show an amber badge with exclamation icon
```

#### Scenario 2: Search filters invoices

```gherkin
Given 100 invoices are loaded in the dashboard
When the analyst types "de Vries" in the search field
And the search debounces for 300ms
Then the table displays only invoices matching "de Vries" in invoiceNumber, debtorName, or address
```

#### Scenario 3: Status filter

```gherkin
Given 100 invoices: 30 QUEUED, 20 REJECTED_TYPE_A, 50 REJECTED_TYPE_B
When the analyst selects "QUEUED" in the status filter
Then the table displays only the 30 QUEUED invoices
```

#### Scenario 4: Drawer shows invoice details

```gherkin
Given an invoice table is displayed
When the analyst clicks on a row with invoiceNumber "INV-2026-0042"
Then a slide-over drawer opens from the right
And the drawer shows: debtor name, address, bank account, phone number, status, PoC status
And no action buttons are visible in the drawer
```

#### Scenario 5: Resubmission indicator

```gherkin
Given an invoice with resubmissionCount = 2 exists
When the dashboard displays this invoice
Then a dark blue curved arrow icon appears in the resubmit column
```

#### Scenario 6: Pagination

```gherkin
Given 150 invoices are in the system
When the dashboard loads
Then the first page shows 50 invoices
And pagination controls show page 1 of 3
When the analyst clicks "next"
Then page 2 shows invoices 51-100
```

---

## 5. Design Tokens (from dashboard-specs-autovetting.md)

**Colors:**
- Ocher: `#C59B27` (brand), `#FDF9ED` (light bg), `#EEDCA8` (muted border)
- Dark: `#0F172A` (primary text), `#64748B` (muted text)
- Light bg: `#F8FAFC` (table headers)
- Border: `#E2E8F0`

**Fonts:**
- Headers: Poppins
- Body: Arimo

**Icons:** FontAwesome 6 (see dashboard spec for full list)

**Resubmit SVG** (inline):
```svg
<svg class="w-4 h-4 text-brand-sidebar inline-block ml-2 shrink-0 align-middle" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" title="Opnieuw ingediend">
    <path d="M9 14L4 9l5-5"/>
    <path d="M4 9h10.5a5.5 5.5 0 0 1 5.5 5.5v0a5.5 5.5 0 0 1-5.5 5.5H11"/>
</svg>
```

---

## 6. Implementation Notes

- Use existing frontend project at `4-frontend/`
- New component: `AnalystDashboard.jsx`
- New component: `InvoiceTable.jsx`
- New component: `InvoiceDrawer.jsx`
- New component: `StatusBadge.jsx`
- New component: `SearchFilterBar.jsx`
- API service: `analystApi.js` (wraps fetch calls to backend)
- No auth layer needed for MVP (per resolved question: no authentication for MVP)

---

## 7. Out of Scope for MVP (Deferred to MVP-2+)

- Validate, Return, Reject action buttons
- Reason modal for returns/rejections
- Statistics/analytics tab
- Settings tab
- Archived invoices ("Dossiers" tab)
- Authentication / login
- Export functionality
- Document preview ("Bekijken") — placeholder only

---

## 8. Dependencies

- Depends on: WI-CA-001 (backend API endpoints)
- Depends on: design spec [`docs/dashboard-specs-autovetting.md`](docs/dashboard-specs-autovetting.md)
- Part of: MVP-1 — Case Analyst Read-Only View
