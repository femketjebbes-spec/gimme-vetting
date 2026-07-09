# Work Item: WI-CA-003 — View Source Excel Files in Analyst Dashboard

**Parent Requirement:** RQ-010 (Case Analyst Read-Only Dashboard)
**Parent Work Stream:** Business-Side Source File Viewing
**Business Objective:** OPE-002 — Give case analysts full visibility into the invoice pipeline, including the original Excel source files clients submitted
**Created:** 2026-07-09
**Status:** Not started
**Priority:** Must have
**Estimated Effort:** 0.5–1 sprint

---

## 1. Requirement Statement

Gimme shall persist the original Excel files uploaded by clients on the client-side and make them viewable by case analysts on the business-side analyst dashboard.

When a client uploads an Excel file via the client intake endpoint (`POST /api/v1/intake/excel`, WI-002), the file is currently parsed row-by-row: valid rows become `Invoice` entities stored in the database, invalid rows are returned in a result file, and then **the original file is discarded** (stored only in a temp directory that is not persisted). WI-CA-003 changes this so that the original Excel file is **persisted in a dedicated Excel intake store** and linked to the `Invoice` records created from it.

When a case analyst opens the detail drawer for an invoice (WI-CA-002), clicking the "Bekijken" (View) link shall retrieve and display the original Excel source file that the client uploaded.

**Context:**
- Excel files (.xlsx, .csv) are uploaded via [`ExcelIntakeController`](5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/ExcelIntakeController.java:45)
- The controller creates a temp directory (`Files.createTempDirectory("excel-upload-" + UUID.randomUUID())`) but does **not** persist files long-term
- Valid rows from the Excel are stored as `Invoice` entities in the database (see [`Invoice` entity](5-backend/business-service/src/main/java/com/gimmevettingsolution/invoice/entity/Invoice.java))
- The existing [`Invoice` entity](5-backend/business-service/src/main/java/com/gimmevettingsolution/invoice/entity/Invoice.java) has no field linking it to a source file — a new field is needed
- The analyst dashboard [`wi-ca-002-frontend-dashboard.md`](re-workspace/work-items/MVP-1-Case-analyst/wi-ca-002-frontend-dashboard.md:78) currently has a placeholder "Bekijken" link with explicit note: "no actual document viewing in MVP"

---

## 2. Scope

### In Scope
- Persist uploaded Excel files to a dedicated store (filesystem-based, configurable path)
- Add a `sourceFileId` field to the `Invoice` entity linking each invoice to its source Excel file
- Backend API endpoint to serve the source Excel file by invoice ID
- Frontend integration: "Bekijken" link opens file viewer/download in the detail drawer
- Handling of edge cases (file not found, file corrupted, re-submissions)

### Out of Scope
- Authentication/authorization for file access (deferred — current endpoints are unauthenticated per D-020)
- Editing or modifying source files
- Viewing files other than Excel (.xlsx, .csv)
- Batch file viewing or download
- File metadata display (upload date, file size, filename) — deferred as UX polish
- Archiving or purging old files
- Versioning (multiple uploads for same invoice tracked via `resubmissionCount`, but only the latest file is kept)

---

## 3. Functional Requirements

### FR-001: Persist Source Excel Files

When an Excel file is uploaded via the intake endpoint, the **original file** shall be saved to a persistent storage location.

**Storage design:**
- Filesystem-based store (consistent with existing `FileBackedPoCStoreService` pattern)
- Configurable via `application.yml`: `${gimme.excel-store-path}` (default: a persistent directory, not temp)
- Files stored with a generated UUID filename to avoid collisions: `<uuid>.xlsx` or `<uuid>.csv`
- A mapping table tracks which file UUID corresponds to which set of invoices

**Database changes:**
- Add `source_file_id` column to the `invoices` table
- Type: VARCHAR(64), nullable, stores the UUID of the source Excel file

**Flyway migration:** Add `V3__add_source_file_id_to_invoices.sql`:
```sql
ALTER TABLE invoices ADD COLUMN source_file_id VARCHAR(64);
```

**Upload flow update:**
1. Client uploads Excel via `POST /api/v1/intake/excel`
2. Generate UUID for the source file
3. Save the original file to the Excel store: `<excel-store-path>/<uuid>.xlsx`
4. For each valid row parsed from the Excel, set `source_file_id = <uuid>` when persisting the `Invoice` entity
5. Return the processing result as before (valid rows stored, invalid rows flagged)

**Acceptance Criteria (Gherkin):**

```gherkin
Scenario: Upload Excel and persist source file
  Given a client uploads an Excel file named "batch-001.xlsx" via POST /api/v1/intake/excel
  When the file is processed successfully
  Then the original file is saved to the Excel store with a UUID filename
  And each valid Invoice row created from the file has source_file_id set to that UUID
  And invalid rows are returned in the result file with their failure reasons

Scenario: Re-upload same Excel with corrections
  Given an Excel file was previously uploaded with UUID "abc123"
  And some rows were rejected and corrected by the client
  When the client re-uploads the corrected file
  Then a new UUID is generated and assigned to the new source file
  And the re-submitted invoices get the new source_file_id (existing invoices are updated per RQ on resubmission)
```

---

### FR-002: Source File Serving API

The business service shall expose a REST API endpoint that serves the original Excel source file by invoice ID.

| Property | Value |
|----------|-------|
| HTTP Method | GET |
| Path | `/api/v1/analyst/invoices/{id}/source-file` |
| Path Variable | `id` (Long, required) — invoice ID matching the `Invoice` entity |
| Content-Type Response | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` for .xlsx, `text/csv` for .csv |
| Authentication | None (unauthenticated, consistent with D-020) |

**Response (200 OK):**
- The original Excel file bytes
- `Content-Disposition` header: `inline; filename="<original-filename>.xlsx"`
- `Content-Type` header: appropriate MIME type for the file format

**Response (404 Not Found):**
- When the invoice has no `source_file_id` (e.g., imported via single-invoice API, not Excel)
- JSON body: `{"code": "SOURCE_FILE_NOT_FOUND", "message": "No source file available for this invoice"}`

**Response (400 Bad Request):**
- When the invoice ID is invalid
- JSON body: `{"code": "INVALID_INVOICE_ID", "message": "Invalid invoice ID"}`

**Acceptance Criteria (Gherkin):**

```gherkin
Scenario: Analyst views source file for Excel-imported invoice
  Given an invoice with ID 42 exists and has source_file_id = "abc123"
  And the Excel file "abc123.xlsx" exists in the Excel store
  When the analyst requests GET /api/v1/analyst/invoices/42/source-file
  Then the response status is 200 OK
  And the response Content-Type matches the file format (.xlsx or .csv)
  And the response body contains the original Excel file content

Scenario: Analyst requests source file for non-Excel invoice
  Given an invoice with ID 99 exists but has source_file_id = null
  When the analyst requests GET /api/v1/analyst/invoices/99/source-file
  Then the response status is 404 Not Found
  And the response body contains a JSON error with code "SOURCE_FILE_NOT_FOUND"

Scenario: Analyst requests source file with invalid invoice ID
  When the analyst requests GET /api/v1/analyst/invoices/99999999/source-file
  And invoice 99999999 does not exist
  Then the response status is 404 Not Found
```

---

### FR-003: Excel File Viewer in Detail Drawer

The analyst dashboard frontend shall display the source Excel file when the "Bekijken" link is clicked in the detail drawer.

**Implementation approach:**
- Replace the current placeholder "Bekijken" link with a functional Excel file viewer
- For MVP: open the file in a new browser tab via the serving API URL (simplest approach, browser handles Excel if extensions/plugins are available)
- Alternative: download the file to the analyst's machine via the "Bekijken" link (uses `Content-Disposition: attachment` for download behavior)
- For MVP, use **download behavior** (more reliable across browsers since Excel files don't render in-browser natively)

**Drawer layout update:**
```
┌─────────────────────────────────────────────────┐
│ Debtor Name (large heading)                      │
│ Debtor Address                                   │
│ Bank Account Number                              │
│ Phone Number                                     │
│ Invoice Number                                   │
│ Status Badge   PoC Status   Resubmit Icon        │
│ [Bekijken] link → triggers file download        │
├─────────────────────────────────────────────────┤
│ (No embedded viewer for Excel in MVP —          │
│  clicking "Bekijken" downloads the source file) │
└─────────────────────────────────────────────────┘
```

**Acceptance Criteria (Gherkin):**

```gherkin
Scenario: Analyst clicks "Bekijken" for Excel-imported invoice
  Given the detail drawer is open for an invoice with a source file
  When the analyst clicks "Bekijken"
  Then the source Excel file downloads to the analyst's machine
  And the downloaded file contains the original data the client uploaded

Scenario: Analyst clicks "Bekijken" for non-Excel invoice
  Given the detail drawer is open for an invoice without a source file (source_file_id = null)
  When the analyst clicks "Bekijken"
  Then a message displays: "No source file available for this invoice"
  And no download is triggered
```

---

### FR-004: Source File Availability Indicator

The detail drawer shall indicate whether a source Excel file is available for the invoice.

**Behavior:**
- When `source_file_id` is NOT null, the "Bekijken" link shall be enabled and clickable
- When `source_file_id` is null (e.g., invoice imported via single-invoice API), the "Bekijken" link shall be disabled with an appropriate tooltip

**Database schema impact:**
- The `source_file_id` column on `invoices` must be included in the `AnalystController` detail response (or a separate field `hasSourceFile` boolean)

**Acceptance Criteria (Gherkin):**

```gherkin
Scenario: Enabled "Bekijken" for invoice with source file
  Given the drawer is open for an invoice with source_file_id not null
  Then the "Bekijken" link is enabled and clickable

Scenario: Disabled "Bekijken" for invoice without source file
  Given the drawer is open for an invoice with source_file_id = null
  Then the "Bekijken" link is disabled
  And a tooltip indicates "No source file available"
```

---

## 4. Non-Functional Requirements

### NFR-001: Performance

| Metric | Target |
|--------|--------|
| Source file save time during upload | ≤ 200ms (file copy to persistent store) |
| Source file serve time (p95) | ≤ 500ms for files under 25MB |
| Drawer open time (with source file indicator) | ≤ 50ms additional (only a nullable field check) |

**Rationale:** Excel files are typically small (1-5MB for invoice batches). The 500ms p95 target accounts for disk I/O for larger files.

---

### NFR-002: Security

| Requirement | Description |
|-------------|-------------|
| Path traversal protection | Generated UUID filenames prevent path traversal. Original filenames are stored only for download response headers, never used as filesystem paths |
| No sensitive data in logs | Excel store path must not appear in error messages or logs |
| No authentication (MVP) | File access remains unauthenticated during MVP, consistent with D-020. This is a known risk to be addressed in a future work item |
| File type validation | Only .xlsx and .csv files accepted. Rejected at upload time with 400 Bad Request |

---

### NFR-003: File Handling

| Requirement | Description |
|-------------|-------------|
| MIME-type correctness | Response `Content-Type` must match the file format: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` for .xlsx, `text/csv` for .csv |
| Original filename preserved | Response `Content-Disposition` header uses the original filename the client uploaded (e.g., `batch-001.xlsx`) |
| File size limit | Files larger than 50MB shall be rejected at upload time with 413 Request Entity Too Large |
| Corrupted file handling | If a stored file becomes corrupted/unreadable, serve 500 Internal Server Error with message: "Source file is unavailable" |

---

### NFR-004: Storage

| Requirement | Description |
|-------------|-------------|
| Storage location | Configurable via `application.yml`: `gimme.excel-store-path` |
| Storage retention | Files retained as long as linked invoices exist in the database |
| Storage monitoring | Not in scope for MVP, but design should allow for future cleanup jobs |

---

## 5. Integration Points

### 5.1 Backend Integration

| Component | Reference | Integration |
|-----------|-----------|-------------|
| `ExcelIntakeController` | [`5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/ExcelIntakeController.java`](5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/ExcelIntakeController.java) | Modify to save uploaded file to persistent store instead of temp directory |
| `InvoiceRepository` | [`5-backend/business-service/src/main/java/com/gimmevettingsolution/invoice/repository/InvoiceRepository.java`](5-backend/business-service/src/main/java/com/gimmevettingsolution/invoice/repository/InvoiceRepository.java) | Add `source_file_id` to persisted entities |
| `Invoice` entity | [`5-backend/business-service/src/main/java/com/gimmevettingsolution/invoice/entity/Invoice.java`](5-backend/business-service/src/main/java/com/gimmevettingsolution/invoice/entity/Invoice.java) | Add `sourceFileId` field |
| `AnalystController` | [`5-backend/business-service/src/main/java/com/gimmevettingsolution/analyst/controller/AnalystController.java`](5-backend/business-service/src/main/java/com/gimmevettingsolution/analyst/controller/AnalystController.java) | Add new endpoint method and include `sourceFileId` in detail response |

**Suggested implementation — Excel store service:**
```java
@Service
public class FileBackedExcelStoreService {
    private final Path excelStorePath;
    
    // Save file with UUID name, returns the UUID
    public String save(MultipartFile file) { ... }
    
    // Get file by UUID
    public Resource getById(String uuid) { ... }
    
    // Get original filename for a UUID (stored in a mapping)
    public String getOriginalFilename(String uuid) { ... }
}
```

**Suggested implementation — Entity change:**
```java
@Column(name = "source_file_id", length = 64)
private String sourceFileId;
```

**Suggested implementation — New endpoint:**
```java
@GetMapping("/invoices/{id}/source-file")
public ResponseEntity<Resource> getSourceFile(@PathVariable Long id) { ... }
```

---

### 5.2 Frontend Integration

| Component | Reference | Integration |
|-----------|-----------|-------------|
| Analyst Dashboard Drawer | [`4-frontend/src/business-service/components/`](4-frontend/src/business-service/components/) | Wire "Bekijken" link to source file download endpoint |
| API layer | [`4-frontend/src/business-service/api/analystApi.js`](4-frontend/src/business-service/api/analystApi.js) | Include `sourceFileId` (or `hasSourceFile`) in invoice detail response |

**Suggested implementation:**
- The "Bekijken" link's `href` is set to `/api/v1/analyst/invoices/${selectedInvoice.id}/source-file`
- For download: `<a href={...} download>Bekijken</a>` or fetch as blob and create download link
- For disabled state: check if `selectedInvoice.sourceFileId` is truthy before enabling the link

---

## 6. Database Schema Changes

### Flyway Migration: V3__add_source_file_id_to_invoices.sql

```sql
ALTER TABLE invoices ADD COLUMN source_file_id VARCHAR(64);
COMMENT ON COLUMN invoices.source_file_id IS 'UUID of the source Excel file the invoice came from';
```

### Invoice Entity Update

```java
@Column(name = "source_file_id", length = 64)
private String sourceFileId;
```

---

## 7. Traceability

| Requirement | Traceability |
|-------------|--------------|
| FR-001 (Persist Source Files) | Supports RQ-006 (Excel Batch Intake) — preserves audit trail of original client uploads |
| FR-002 (Source File Serving API) | Supports RQ-010 (Case Analyst Read-Only Dashboard) — extends invoice detail with source file access |
| FR-003 (Excel Viewer) | Supports RQ-010 — analysts can verify original client data |
| FR-004 (Source File Indicator) | Supports RQ-010 — clear UX for invoices without source files |
| NFR-001 (Performance) | Supports OPE-002 — fast file access for high-volume analyst review |
| NFR-002 (Security) | Supports existing security posture — UUID filenames prevent path traversal |

---

## 8. Open Questions

| ID | Question | Status |
|----|----------|--------|
| OQ-001 | Should the "Bekijken" link download the file or open it in a new tab? | Decided: Download (more reliable since Excel files don't render in-browser natively) |
| OQ-002 | Should we store the original filename or always use a generic name like "source-file.xlsx"? | Decided: Store original filename for better UX in downloads |
| OQ-003 | Will there be future need to track multiple source files per invoice (e.g., re-submissions)? | Deferred — current design keeps latest; re-submissions get new UUID |
| OQ-004 | Should single-invoice API imports also have a source file field (or null)? | Decided: `source_file_id` is nullable — null for single-invoice API imports |

---

## 9. Acceptance Criteria Summary

| # | Criterion | Status |
|---|-----------|--------|
| 1 | Uploaded Excel files are persisted to the Excel store with UUID filenames | Pending |
| 2 | Each invoice from the Excel has `source_file_id` set to the file's UUID | Pending |
| 3 | GET `/api/v1/analyst/invoices/{id}/source-file` returns the source Excel file | Pending |
| 4 | GET returns 404 when invoice has no source file (single-invoice API import) | Pending |
| 5 | Analyst dashboard "Bekijken" link downloads the source file | Pending |
| 6 | "Bekijken" link disabled when `source_file_id` is null | Pending |
| 7 | No path traversal vulnerability in Excel file storage | Pending |
| 8 | Correct MIME types served for .xlsx and .csv files | Pending |
