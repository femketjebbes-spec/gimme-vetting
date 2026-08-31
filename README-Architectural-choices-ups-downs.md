# Architectural Assessment: Gimme Vetting Solution MVP

*Generated: 2026-07-10*
*Source: Architect agent session logs (Sessions 1-12), architecture-decisions.md, decision-log.md, assumption-log.md, current codebase*

---

## 1. The Most Impactful Decisions

### D1: Single-Service Monolith with Shared Database (Business Service)

The architecture routes all data operations through a single Spring Boot service (`business-service` on port 8082) backed by H2 (local) or PostgreSQL (prod). Both the client upload portal (`/`) and the analyst dashboard (`/analyst`) are React routes served from the same SPA, all proxied to the same backend. The `client-service` is a separate service pointing at a different PostgreSQL database (`gimme_vetting_client`), suggesting a planned but not yet integrated domain boundary.

**Why chosen:** MVP speed. A single Spring Boot service eliminates inter-service communication, distributed transactions, and separate deployment pipelines. The shared database is the simplest shared state mechanism.

---

### D2: Synchronous Upload-Process-Return Model (WI-002)

The Excel upload endpoint processes all rows through parsing, mandatory field validation, and PoC existence verification within a single HTTP request cycle, then returns results (including a download link for failing rows) in the same response.

**Why chosen:** MVP simplicity confirmed by the stakeholder. Eliminates async infrastructure (message queues, job stores, polling endpoints). The return Excel download link is generated in-memory and served immediately.

---

### D3: Content-Based File Validation with MIME Fallback (BR-001 / D-BR001)

File type detection uses magic byte inspection (ZIP signature for .xlsx, text encoding validation for .csv) as the authoritative check, with MIME type as a fast-path optimization only. This was adopted after browser-reported MIME types proved unreliable.

**Why chosen:** Browser MIME types are untrusted metadata. Chrome/Firefox report correct types, but other browsers, files without extensions, or OS-level misassociations cause incorrect types (`application/octet-stream`, `application/zip`, empty string). The stakeholder confirmed this was a real production bug.

---

### D4: Flyway + Hibernate `ddl-auto: create-drop` Dual Schema Management

The database schema is managed by both Flyway migrations (V1, V2, V3 for table creation, resubmission_count column, and source_file_id columns) and Hibernate's `ddl-auto: create-drop` in the default profile. In the test profile, Flyway is disabled and Hibernate owns schema creation entirely. In the prod profile, Flyway is enabled and `ddl-auto` is set to `validate`.

**Why chosen:** The dual approach allows rapid local development (Hibernate auto-recreates on restart) while using Flyway for production schema versioning. The test profile disables Flyway since H2's auto-DDL is sufficient for ephemeral test databases.

---

### D5: Filesystem-Based File Stores with UUID Filenames (D-EXCEL-001, D-003)

Both PoC files and Excel intake files are stored on the local filesystem using a configurable path (`gimme.poc-store-path`, `gimme.excel-store-path`). Files are renamed to UUIDs at storage time. Original filenames are tracked in metadata columns (`source_filename` on the Invoice entity).

**Why chosen:** The PoC store pattern was already proven in `FileBackedPoCStoreService`. Extending it for Excel files avoids introducing a new storage abstraction (S3, blob storage) for MVP. UUID filenames prevent path traversal and collision. The configurable path decouples the store from a shared-fs assumption.

---

## 2. Reversibility & Adaptability: One-Way Doors

Ranked from **hardest to change** to **easiest to change**:

| Rank | Decision | Reversibility |
|------|----------|---------------|
| **1** | D4: Flyway + Hibernate dual schema | **One-way door.** Changing from dual management to Flyway-only (or Hibernate-only) requires migration of all schema definitions, potential downtime for data migration, and code changes across all services. The existing `create-drop` vs `validate` inconsistency across profiles means the production path is already correct but the local path is fragile. |
| **2** | D5: Filesystem-based stores | **Near one-way.** Migrating from filesystem to object storage (S3, Azure Blob) requires changes to `FileBackedPoCStoreService` and `FileBackedExcelStoreService`, the `PoCStoreService`/`ExcelStoreService` interfaces, all consumers, and potentially the data model if file storage moves from local disk paths to URIs. The store interface provides an abstraction layer that makes this *plannable* but *expensive*. |
| **3** | D1: Single-service monolith | **Reversible but costly.** Splitting client-service and business-service into independent services with separate databases would require defining inter-service communication (REST, gRPC, messaging), handling cross-service transactions (PoC matching against a separate client database), and separate deployment pipelines. The current dual-database setup (`gimme_vetting_client` vs `gimme_vetting_business`) already hints at a planned split, making the eventual migration less painful than from a single database. |
| **4** | D3: Content-based file validation | **Reversible.** The detection logic is encapsulated in `ExcelParsingService.detectFileType()`. Reverting to MIME-only validation would remove the content-based fallback path and the `isContentBasedDetectionValid()` method. No schema changes, no data migration. |
| **5** | D2: Synchronous processing | **Reversible.** Converting to async would require adding a job queue (RabbitMQ, SQS), a job status endpoint, and modifying the response model. The existing controller structure with its `ExcelUploadResponse` DTO could be extended. The biggest risk is data consistency if jobs fail mid-processing. |

---

## 3. Trade-off Analysis

### D1: Single-Service Monolith

**Positives:**
- Zero inter-service latency, zero distributed failure modes
- Single deployment unit, single configuration surface
- Shared `InvoiceRepository` between upload and analyst endpoints -- no cross-service queries
- Developer velocity: one codebase, one build, one test suite (236 tests)

**Negatives/Risks:**
- No domain boundary enforcement -- any controller can query any repository
- The `client-service` (port 8081, PostgreSQL) exists but is isolated from `business-service` (port 8082, H2/PostgreSQL) -- data is siloed per service with no shared state mechanism
- Scaling is all-or-nothing: to scale the upload endpoint, you must scale the entire service including the analyst dashboard
- Single point of failure for the entire MVP

### D2: Synchronous Processing

**Positives:**
- Simple error handling: the HTTP response contains success or failure
- No job state management, no polling endpoints, no job stores
- Client gets immediate feedback with the return Excel download link
- No infrastructure dependencies (no message brokers, no background workers)

**Negatives/Risks:**
- HTTP connection holds open for the entire processing duration -- under load this ties up threads
- No file size limit for MVP means a single 50MB+ file can block a thread for many seconds
- If PoC store lookup is slow (filesystem scan, network mount), response times degrade
- No retry semantics: if processing fails mid-way, partial results are lost
- `create-drop` on restart means no durability for queued invoices

### D3: Content-Based Validation

**Positives:**
- More secure than MIME-only: inspects actual file content rather than untrusted metadata
- Matches industry best practice (magic byte inspection is the standard approach)
- Robust across browsers and operating systems
- The fast-path optimization preserves performance for well-behaved browsers

**Negatives/Risks:**
- Added complexity in the validation pipeline: two-tier detection, two failure paths
- The bug fixed on 2026-07-10 (MIME mismatch between controller and file store) demonstrates that this dual logic creates maintenance burden -- any component that validates files must implement the same two-tier strategy
- Without a shared validation service, each component can implement it differently

### D4: Flyway + Hibernate Dual Schema Management

**Positives:**
- Local development is fast: Hibernate recreates the schema on every restart
- Flyway provides versioned migrations for production and audit trail
- Test profile uses a simplified setup (Flyway disabled, Hibernate owns schema)

**Negatives/Risks:**
- **Conflict:** `ddl-auto: create-drop` in the default profile **drops and recreates** the schema after Flyway runs, effectively nullifying Flyway's work. This is architecturally inconsistent -- two tools fighting over the same schema.
- The local development behavior silently diverges from production (prod uses `ddl-auto: validate`). This gives a false sense of correctness.
- Schema changes from Flyway migrations can conflict with Hibernate's entity-driven DDL generation.
- The prod profile already uses the correct configuration, but the inconsistency between local and prod masks this until inspected side-by-side.

### D5: Filesystem-Based Stores

**Positives:**
- Zero infrastructure cost: no S3 buckets, no blob containers, no CDN
- Simple API: `save(file)`, `getFile(id)`, `getFileContent(id)`
- Configurable path allows running without any external dependencies
- UUID filenames provide path traversal protection
- Pattern already proven in `FileBackedPoCStoreService`

**Negatives/Risks:**
- Not horizontally scalable: files are local to one service instance. A load-balanced deployment would require a shared filesystem or a storage migration
- No built-in redundancy: a disk failure loses all uploaded PoC and Excel files
- No CDN or edge caching for file serving
- File size is limited only by disk space (no configurable per-file limit for MVP)
- The store path is exposed via JVM system property (`gimme.excel-store-path=/tmp/excel-store`), which could leak in logs or error messages

---

## 4. Risk Heatmap

| Decision | Technical Debt | Future Cost | Current Risk |
|----------|---------------|-------------|-------------|
| D1: Monolith | Low | Medium | Low (MVP is small) |
| D2: Sync processing | Medium | Medium | **High** (no file size limit) |
| D3: Content validation | Medium | Low | Low (tested, stable) |
| **D4: Dual schema** | **High** | **High** | **High** (inconsistent behavior) |
| D5: Filesystem stores | Low | **High** | Low (single instance is fine) |

---

## 5. Recommendation

**Immediate:** Change `ddl-auto` from `create-drop` to `validate` in the default profile of `5-backend/business-service/src/main/resources/application.yml`, or disable Flyway entirely in that profile. Do not use both together. Choose one authority for schema management and be consistent across all profiles.

**Short-term:** Consider adding a file size limit to the upload endpoint (deferred in MVP but flagged as DoS risk per D-027).

**Long-term:** Plan the split between `client-service` and `business-service` before moving to multi-instance deployment, since the filesystem stores and dual-database pattern will not scale horizontally without changes.
