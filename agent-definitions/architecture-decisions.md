# Architecture Decisions

Records all architectural choices for the project. This file is maintained by the Architect agent and serves as the reference for all coding agents.

## Format

```
[YYYY-MM-DD] [Session N] ARCHITECTURAL DECISION: <statement>
Rationale: <reasoning behind the decision>
Security Implications: <security considerations>
Affected Agents: [list of agents whose work is constrained]
```

## Decisions

[2026-07-06] [Session 1] ARCHITECTURAL DECISION: Structural changes to frontend code during Refactoring Mode must trigger a mandatory API contract verification and a re-evaluation workflow that routes through Gerard before any backend changes.
Rationale: Femke's Refactoring Mode may change public interfaces, variable names, and component structures that Gerard's adapter layer depends on. Without a notification mechanism, Gerard and Naut operate on stale contract information. Naut has the same gap for its own backend Refactoring Mode. The re-evaluation workflow ensures that any frontend structural change is validated against the API contract, delegated to Gerard for backend impact assessment, and implemented by Naut only when Gerard confirms a contract mismatch exists. Naut never receives structural change notifications directly from Femke. The signal chain is Femke produces signal -> Archibald reads signal -> Archibald delegates to Gerard -> Gerard validates and delegates to Naut -> Naut implements -> Gerard verifies -> Gerard signals Archibald complete.
Security Implications: Ensures the API contract remains authoritative and prevents silent divergence between frontend consumption patterns and backend endpoint definitions. Silent divergence can lead to security regressions when authentication requirements, input validation, or error handling change without the adapter layer being updated.
Affected Agents: Femke, Gerard, Naut, Archibald

[2026-07-06] [Session 2] ARCHITECTURAL DECISION: All coding agents (Femke, Naut, Gerard, Database Engineer) must submit a structured JSON review request to the Alignment Agent after producing artefacts or making changes. The Alignment Agent validates artefacts against Robbie's requirements and Archibald's specs before approving pipeline progression. The Alignment Agent overwrites the review request file with its compliance decision. Upon rejection, coding agents must correct violations and resubmit with an incremented review cycle number before activating the next pipeline agent.
Rationale: Previous agent definitions listed "Completed artefact submission" to the Alignment Agent review channel without defining a format. The Alignment Agent definition described receiving artefacts through a review channel without specifying submission format, activation trigger, or pipeline gate enforcement. This gap allowed undefined handovers between agents. The JSON review request format provides a machine-parseable, unambiguous artefact listing and self-certification mechanism. Pipeline gate enforcement ensures no coding agent activates its downstream counterpart without explicit Alignment Agent approval. The iterative review loop (rejection with feedback, correction, resubmission) prevents non-compliant artefacts from progressing through the pipeline.
Security Implications: Mandatory alignment verification at every pipeline stage prevents non-compliant code from reaching downstream agents. This reduces the risk of security regressions when frontend, API, or backend changes introduce vulnerabilities that bypass requirements-based validation.
Affected Agents: Femke, Naut, Gerard, Database Engineer, Alignment Agent, Archibald

[2026-07-06] [Session 3] ARCHITECTURAL DECISION: Femke must invoke `npx jest --config jest.config.js --json --outputFile .jest-results.json` in all three modes (Testing Mode, Implementation Mode, Refactoring Mode) and parse the JSON output fields (`numFailedTests`, `numPassedTests`, `failureMessage`) to determine test state. The configuration file `jest.config.js` must reside at the project root as a canonical location.
Rationale: Naut specifies `mvn test` and `mvn compile` with exact command invocations. Femke previously only stated "runs `jest`" without specifying the command pattern, configuration file location, or result parsing mechanism. This under-specification created ambiguity about how test results are deterministically evaluated. JSON output parsing provides machine-readable, unambiguous test state determination analogous to how Naut parses Maven output.
Security Implications: Deterministic test result parsing prevents agents from incorrectly reporting green state when tests have actually failed. This ensures the TDD red-green discipline is enforced reliably across sessions.
Affected Agents: Femke

[2026-07-06] [Session 4] ARCHITECTURAL DECISION: The handover from Gerard (API-Agent) to Naut (Backend Agent) must follow the same Alignment Agent gate pattern as the Femke-to-Gerard handover. Gerard must submit `docs/alignment-review-request.md` to the Alignment Agent after completing API contract work. The Alignment Agent validates Gerard's artefacts against Robbie's requirements and Archibald's specs. Archibald must read the Alignment Agent decision from `docs/alignment-review-request.md` and confirm `greenlightForNextAgent` is `true` with `nextAgentInPipeline` set to `Naut` before producing a delegation plan for Naut. Naut must not activate until this approval is confirmed in Archibald's delegation plan. This applies symmetrically to the Naut completion phase as well.
Rationale: The existing Femke-to-Gerard handover requires Archibald to read the Alignment Agent decision before activating Gerard. The Gerard-to-Naut handover previously lacked this explicit check -- Archibald only read `docs/gerard-ready-signal.md` without verifying Alignment Agent approval. This asymmetry created a gap where Naut could potentially activate before Gerard's compliance was verified. Symmetric handover gates ensure consistent quality enforcement across all pipeline transitions. Archibald's monitoring layer gains a new violation type (Alignment Agent gate violation) that blocks delegation if the compliance decision is missing or shows REJECTED status.
Security Implications: Prevents backend implementation from proceeding on unverified API contracts. An unverified contract could contain endpoint mismatches, missing authentication requirements, or incorrect error mappings that Naut would then implement incorrectly. The Alignment Agent gate ensures Gerard's contract work is requirements-compliant before Naut begins backend development.
Affected Agents: Gerard, Naut, Archibald, Alignment Agent

[2026-07-07] [Session 4] ARCHITECTURAL DECISION: WI-001 PoC filename matching shall be case-insensitive. The matching algorithm performs full-string comparison after lowercase normalisation of both the PoC filename and the invoice number. No substring matching, pattern matching, or fuzzy matching is permitted.
Rationale: The requirements spec (RQ-001) does not specify case-sensitivity. Upstream systems may produce PoC filenames with varying case conventions. Case-insensitive matching accommodates this variation without requiring a transformation layer. The algorithm is deterministic and unambiguous.
Security Implications: Case-insensitive matching reduces false Type A rejections caused by naming convention mismatches, which prevents clients from being blocked by non-functional issues. The full-string comparison (not substring) prevents matching attacks where a malicious filename could partially match a legitimate invoice number.
Affected Agents: Naut, Gerard

[2026-07-07] [Session 4] ARCHITECTURAL DECISION: WI-001 shall tolerate multiple PoC files for a single invoice number. Finding at least one matching filename is sufficient to pass the PoC existence gate. No deduplication or duplicate rejection logic is required.
Rationale: RQ-001 does not specify behaviour for multiple PoC files. The stakeholder confirmed that one match is sufficient. Deduplication is not a business requirement and would add unnecessary complexity to the intake pipeline.
Security Implications: Tolerating multiple PoC files does not introduce security risk. The PoC files are sourced from the legitimate upstream system, not from the client submitting the invoice.
Affected Agents: Naut

[2026-07-07] [Session 4] ARCHITECTURAL DECISION: The PoC store location shall be a configurable path or storage bucket, with no architectural assumption about shared filesystem versus object storage. The configuration value shall be injected at runtime via application configuration.
Rationale: W-005 (Domain Model) defines the PoC entity but not its storage mechanism. The storage decision depends on external factors (who provides PoC files, where they reside) that are outside WI-001 scope. A configurable path decouples the PoC matching logic from the storage mechanism.
Security Implications: Runtime configuration must not expose the PoC store path in error messages or logs. The storage credentials or access keys must be stored in a secure configuration store, not in version-controlled files. The file system or object storage access layer must enforce access controls.
Affected Agents: Naut, Database Engineer

[2026-07-07] [Session 5] ARCHITECTURAL DECISION: WI-002 Excel upload endpoint accepts exactly the column names from the requirements specification (case-insensitive): "invoice number", "debtor name", "address", "phone number", "bank account number". No aliases or alternative column name variants are accepted. Files with unrecognized column names shall be rejected with a structured error response.
Rationale: The user confirmed strict column name enforcement in Session 5. This simplifies the parsing logic and prevents silent mismapping when a client uses unfamiliar column names. The error response enables the client to correct their Excel template.
Security Implications: Rejecting unknown column names prevents unexpected field mapping that could cause data loss or incorrect invoice processing. No column name injection vectors exist since column names are validated against a fixed allowlist.
Affected Agents: Gerard, Naut

[2026-07-07] [Session 5] ARCHITECTURAL DECISION: WI-002 supports .xlsx and .csv file formats. Header row is optional. When no header row is present, column order determines field mapping: column 0 = invoiceNumber, column 1 = debtorName, column 2 = address, column 3 = phoneNumber, column 4 = bankAccountNumber.
Rationale: Confirmed by user in Session 5. This decision covers both format support and the fallback mapping mechanism when headers are absent.
Security Implications: No direct security implications. File type validation must enforce MIME type checking to prevent upload of executable or malicious files disguised as Excel files.
Affected Agents: Gerard, Naut

[2026-07-07] [Session 5] ARCHITECTURAL DECISION: WI-002 endpoint has no authentication for MVP. No file size limit is enforced for MVP.
Rationale: Confirmed by user in Session 5. The client portal is unauthenticated for MVP purposes. File size limits are deferred to a future iteration.
Security Implications: No authentication on the upload endpoint allows unauthenticated file uploads. This is a documented MVP limitation that must be flagged for remediation in a future iteration. Absence of file size limits creates a denial-of-service risk via large file uploads. The architect recommends designing a size boundary but deferring enforcement to MVP+1.
Affected Agents: Gerard, Naut, Femke

[2026-07-07] [Session 5] ARCHITECTURAL DECISION: WI-002 synchronous processing model. The client uploads an Excel file, the server processes all rows through parsing, mandatory field validation, and PoC existence verification, then returns the result (including a download link for the return Excel) in the same HTTP response cycle.
Rationale: Confirmed by user in Session 5 for MVP. Synchronous processing simplifies the MVP architecture. If large files cause performance issues, async processing can be evaluated later (AUNV-006).
Security Implications: Synchronous processing ties client connections to processing time. Large files held in memory during processing increase the attack surface for memory exhaustion DoS. Streaming processing (Apache POI SXSSF) is recommended to mitigate this risk.
Affected Agents: Gerard, Naut, Femke

[2026-07-07] [Session 5] ARCHITECTURAL DECISION: Apache POI is the recommended Excel parsing library for WI-002 through WI-004.
Rationale: Apache POI is the industry standard for Java Excel processing, supports both .xlsx and .csv formats, and is consistent across the parsing and generation work items. EasyExcel can be evaluated later if large-file performance becomes critical.
Security Implications: Apache POI has had historical vulnerabilities related to XML external entity injection in .xlsx files (ZIP slip vulnerabilities). The version used must be the latest patched version. XML entity expansion must be disabled in the parser configuration.
Affected Agents: Naut

[2026-07-07] [Session 6] ARCHITECTURAL DECISION: Wi-006 shall use a GNU Make Makefile at the project root for build orchestration. Targets: `build`, `test`, `clean`, `backend`, `frontend`, `backend-test`, `frontend-test`, `check-tools`. Backend builds before frontend in the `build` target. `check-tools` validates that `mvn`, `node`, `npm` are installed.
Rationale: Option B (Makefile) was selected over Option A (shell script) and Option C (root package.json) by the WI-006 specification. Make provides incremental build support, clear dependency declarations, and is the standard convention for multi-language projects.
Security Implications: None. Build orchestration does not affect the product security surface.
Affected Agents: Naut

[2026-07-08] [Session 7] ARCHITECTURAL DECISION: WI-005 PoC upload endpoint shall reject non-PDF files with a 400 Bad Request response containing a clear error message. The endpoint validates MIME type is `application/pdf` server-side.
Rationale: Consistent with the existing pattern in ExcelIntakeController which validates MIME types for Excel uploads. Rejecting non-PDF files prevents storage bloat and client confusion. The error message must clearly indicate that only PDF files are accepted.
Security Implications: MIME type validation prevents upload of malicious files disguised as PDF. Server-side validation is mandatory because client-side validation is bypassable. The MIME type must be verified against the actual file content (magic byte check) to prevent upload of executable files with `.pdf` extensions.
Affected Agents: Gerard, Naut

[2026-07-08] [Session 7] ARCHITECTURAL DECISION: WI-005 PoC upload shall overwrite existing files when a duplicate filename is uploaded.
Rationale: Consistent with D-002 (multiple PoC files for same invoice are not an error) and the simplest storage strategy. Overwrite behaviour avoids storage bloat from duplicate files. The client can re-upload a corrected PoC file without needing a cleanup mechanism.
Security Implications: No security risk. The existing path traversal protection in FileBackedPoCStoreService (SAFE_PATTERN) applies to uploads as well. The overwrite is atomic via `Files.move()` with `REPLACE_EXISTING`.
Affected Agents: Naut

[2026-07-08] [Session 8] ARCHITECTURAL DECISION: WI-CA-001 resubmission strategy uses Option A: when a client submits an invoice with an existing invoiceNumber, the existing row is updated and resubmissionCount is incremented by one. No new row is created per submission.
Rationale: The work item wi-ca-001-analyst-api explicitly recommends Option A for MVP simplicity. Creating new rows per submission (Option B) would complicate queries and is not required for the analyst dashboard which only needs to display invoice data with a resubmission counter. This decision applies only to the resubmission logic in the intake pipeline, not to the analyst API itself.
Security Implications: Updating in place means the database always contains a single row per invoiceNumber. The unique constraint on invoice_number column enforces this. Resubmission count provides an audit trail of how many times a client has re-submitted. No sensitive data is exposed by this decision.
Affected Agents: Naut, Gerard

[2026-07-08] [Session 8] ARCHITECTURAL DECISION: WI-CA-001 analyst API endpoints are unauthenticated for MVP, consistent with D-026 (no authentication on client portal endpoints for MVP). This applies to GET /api/v1/analyst/invoices and GET /api/v1/analyst/invoices/{id}. Authentication must be added in a future iteration.
Rationale: Consistency with existing unauthenticated endpoints (Excel upload, PoC upload) established in Session 5. The analyst dashboard is for case analysts who use the same client portal. Adding authentication requires infrastructure decisions (auth provider, session management, token storage) that are out of scope for MVP.
Security Implications: Unauthenticated GET endpoints expose all invoice data including debtor names, addresses, bank account numbers, and phone numbers to anyone with access to the API. This is a documented MVP limitation. The architect recommends implementing simple API key authentication or basic auth in MVP+1. Rate limiting should also be added to prevent bulk data extraction. These are not implemented in MVP but must be flagged.
Affected Agents: Gerard, Naut, Femke

[2026-07-08] [Session 8] ARCHITECTURAL DECISION: WI-CA-001 introduces a Flyway migration V2__add_resubmission_count.sql to add the resubmissionCount column to the invoices table. The column is INTEGER NOT NULL DEFAULT 0.
Rationale: The existing Invoice entity (mapped by V1 migration) does not have a resubmissionCount field. The analyst API requires this field in its response DTO. Adding it via Flyway migration ensures the database schema matches the entity definition.
Security Implications: None. This is a metadata column tracking resubmission count. No sensitive data is introduced.
Affected Agents: Naut, Gerard, Database Engineer

[2026-07-08] [Session 8] ARCHITECTURAL DECISION: WI-CA-001 endpoints are versioned under /api/v1/analyst/ path. This follows the existing /api/v1/ prefix convention established by intake and PoC upload endpoints.
Rationale: API versioning via URL path prefix is consistent with existing endpoints. The /analyst/ sub-path clearly separates analyst-facing endpoints from client-facing endpoints, even though both are under /api/v1/.
Security Implications: Path-based separation does not provide security isolation. Access controls must be implemented at the controller or service layer if authentication is added later.
Affected Agents: Gerard, Naut

[2026-07-08] [Session 8] ARCHITECTURAL DECISION: WI-007 template download endpoint is GET /api/v1/intake/excel/template. The endpoint returns a pre-generated XLSX template file with exactly 5 column headers: `invoice number`, `debtor name`, `address`, `phone number`, `bank account number`. The template contains at least one empty data row as visual guide. The response uses `Content-Disposition: attachment; filename="invoice-intake-template.xlsx"` and `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
Rationale: Users need a correctly-formatted template to avoid upload failures from column name mismatches. XLSX-only format is sufficient for MVP. The empty template (no example data, no validation rules, no formatting) keeps implementation simple and reduces maintenance burden.
Security Implications: The template is a static server-generated resource. No authentication required (MVP). No file injection risk since the template is generated server-side. Response headers must not include server-internal path information.
Affected Agents: Gerard, Naut, Femke

[2026-07-08] [Session 8] ARCHITECTURAL DECISION: WI-007 template generation uses Apache POI (XSSFWorkbook) consistent with existing Excel generation code in `ExcelParsingService`. Column headers are defined as constants (derived from `ALLOWED_COLUMN_NAMES`) to prevent drift from the validation allowlist.
Rationale: Apache POI is already used throughout the project for Excel generation (D-008). Sharing the library and code patterns reduces dependency complexity. Constants prevent silent drift between the template headers and the validation allowlist used during upload.
Security Implications: No direct security implications. The constant-based approach ensures template headers always match the validation allowlist, preventing mismatch issues that could cause user confusion or upload failures.
Affected Agents: Naut

[2026-07-08] [Session 8] ARCHITECTURAL DECISION: BR-001 fixes the Excel upload endpoint by replacing MIME-type-only file validation with content-based file format detection. The system SHALL detect file format by inspecting magic bytes (first 4 bytes for ZIP signature `50 4B 03 04` for .xlsx, text detection for .csv) rather than relying solely on the browser-reported MIME type. MIME-type validation is retained as a fast path for well-behaved browsers but falls back to content-based detection when the MIME type is missing, null, or unrecognized (e.g., `application/octet-stream`, `application/zip`). This decision is consistent with the existing architecture decision in Section 7 that mandates magic byte verification for file uploads.
Rationale: Browser-reported MIME types are unreliable metadata. Chrome and Firefox report the correct MIME type for .xlsx files, but other browsers, operating systems with unusual file associations, or files without extensions may report `application/octet-stream`, `application/zip`, or an empty string. Relying solely on MIME type causes valid Excel files to be rejected. Content-based detection using magic bytes is the industry-standard approach for file type detection and is equally secure.
Security Implications: Content-based detection is MORE secure than MIME-type-only validation because it inspects actual file content rather than untrusted metadata. Magic byte inspection prevents upload of files disguised with correct MIME types but malicious content. The ZIP signature check for .xlsx files prevents non-ZIP files from being accepted. CSV detection via text encoding validation prevents binary files from being misidentified. This does not replace path traversal protection or file size limits (deferred to MVP+1).
Affected Agents: Gerard, Naut

[2026-07-08] [Session 10] ARCHITECTURAL DECISION: The case analyst dashboard is served from the existing 4-frontend/ project using React Router with two routes: the client upload page (existing ExcelUpload component at /) and the analyst dashboard (AnalystDashboard component at /analyst). The main.jsx entry point is refactored to wrap both routes in a BrowserRouter.
Rationale: The existing frontend at 4-frontend/ is the sole frontend project for MVP. The analyst dashboard is a separate view within the same application, sharing the same build pipeline, dependency management, and testing infrastructure. React Router provides client-side routing without requiring a separate server or entry point. This is simpler than adding a second Vite entry point or creating a new frontend project.
Security Implications: Both routes serve from the same origin, so CORS is not a concern. The analyst route must not bypass the existing API. No new attack surface is introduced beyond the analyst dashboard UI itself. Future authentication (required per D-CA-002) must protect the /analyst route.
Affected Agents: Femke

[2026-07-09] [Session 11] ARCHITECTURAL DECISION: WI-CA-003 introduces a persistent Excel intake store (`FileBackedExcelStoreService`) following the same filesystem-based pattern as `FileBackedPoCStoreService`. The store path is configurable via `application.yml` (`gimme.excel-store-path`). Files are stored with UUID filenames to prevent path traversal. Original filenames are tracked separately for download headers.
Rationale: The existing `ExcelIntakeController` creates files in a temp directory that is discarded after upload. The work item requires persistent storage so analysts can retrieve the original Excel later. The `FileBackedPoCStoreService` pattern is already proven in the codebase and provides path traversal protection via `SAFE_PATTERN`. UUID filenames eliminate collision and traversal risks.
Security Implications: UUID filenames prevent path traversal attacks. The store path must not be exposed in error messages or logs. MIME type validation (`.xlsx` and `.csv` only) is enforced at upload time. The store service must reject any file that is not a valid Excel or CSV format on read. No authentication on the serving endpoint is a documented MVP limitation per D-CA-002.
Affected Agents: Gerard, Naut

[2026-07-09] [Session 11] ARCHITECTURAL DECISION: The `Invoice` entity gains a new nullable field `sourceFileId` (VARCHAR(64)) linking each invoice to its source Excel file UUID. A Flyway migration `V3__add_source_file_id_to_invoices.sql` adds the column. The field is null for invoices imported via the single-invoice API (not Excel).
Rationale: Every Excel-imported invoice needs a reference to its source file. Using a nullable VARCHAR(64) UUID accommodates both Excel-imported (non-null) and single-invoice API imported (null) invoices. The 64-character length accommodates standard UUID strings.
Security Implications: The `source_file_id` column stores only UUIDs, not file paths or sensitive data. No security risk from the column itself.
Affected Agents: Naut, Gerard, Database Engineer

[2026-07-09] [Session 11] ARCHITECTURAL DECISION: The source file serving endpoint is `GET /api/v1/analyst/invoices/{id}/source-file`. The endpoint returns the original Excel file bytes with appropriate `Content-Type` and `Content-Disposition` headers. Returns 404 when `source_file_id` is null or the file is missing.
Rationale: The endpoint follows the existing RESTful pattern of nesting resource access under the parent entity (`/invoices/{id}/source-file`). Returning raw file bytes allows the browser to handle the download directly. Consistent with the download behaviour decision in the work item.
Security Implications: UUID-based file lookup prevents path traversal. The endpoint must validate that the `source_file_id` exists before serving. `Content-Type` must match the actual file format. `Content-Disposition` must use the stored original filename, not user input. The endpoint must not return file content types that could be interpreted as executable.
Affected Agents: Gerard, Naut

[2026-07-09] [Session 11] ARCHITECTURAL DECISION: The original filename uploaded by the client is stored alongside the UUID mapping so that the download response uses the original filename. Since the Invoice entity is the natural holder of this data, `sourceFileId` is the sole persistence point for the UUID. The original filename tracking must be resolved -- either stored in a mapping table or as an additional column on Invoice.
Rationale: The work item requires `Content-Disposition: inline; filename="<original-filename>.xlsx"` for UX. Options: (a) add `source_filename` column to invoices table, (b) create a separate mapping table. Option (a) is simpler and keeps the mapping co-located with the invoice.
Decision: Add a second nullable column `source_filename` (VARCHAR(256)) to the invoices table. This avoids a separate table and keeps the UUID-to-filename mapping alongside the invoice entity.
Security Implications: The `source_filename` column stores user-provided filenames. While it is only used in HTTP response headers (not filesystem operations), it must be sanitised to prevent header injection attacks. Filenames containing newlines or semicolons must be rejected or sanitised.
Affected Agents: Naut, Gerard, Database Engineer

[2026-07-09] [Session 11] ARCHITECTURAL DECISION: The `ExcelIntakeController` is modified to save the original uploaded file to the Excel store during the existing upload flow. No new endpoint is created for file persistence -- the upload endpoint is extended.
Rationale: The upload endpoint already receives the file as `MultipartFile`. Adding the persistence step inline avoids a separate API call and keeps the flow atomic. The Excel store service is injected as a dependency.
Security Implications: The file is saved immediately upon receipt, before parsing. This means even files with validation errors are persisted. This is acceptable since persistence is an audit requirement. The file is saved with a UUID name regardless of validation outcome.
Affected Agents: Gerard, Naut

[2026-07-09] [Session 11] ARCHITECTURAL DECISION: Frontend "Bekijken" link uses direct `<a>` tag download via the serving API URL. No blob download or JavaScript-mediated download is required for MVP.
Rationale: The work item decided on download behaviour. Using `<a href={url} download>Bekijken</a>` is the simplest approach. Browsers handle .xlsx downloads natively. The disabled state is determined client-side by checking `sourceFileId` presence.
Security Implications: The `href` attribute must be constructed from a trusted base URL and the invoice ID. No user input is used in the URL construction. The `download` attribute may be ignored by some browsers for cross-origin requests, but since both frontend and API share the same origin, this is not a concern.
Affected Agents: Femke

[2026-07-09] [Session 11] ARCHITECTURAL DECISION: The AnalystInvoiceDTO for the detail endpoint gains two new fields: `sourceFileId` (string, nullable) and `sourceFilename` (string, nullable). These fields support both the enabled/disabled state of the "Bekijken" link and the download filename.
Rationale: The frontend needs `sourceFileId` to determine whether to enable the link, and `sourceFilename` is needed for the download anchor's `download` attribute (some browsers support this). Keeping both fields in the DTO avoids an extra API call for filename lookup.
Security Implications: No new security surface. Both fields are derived from trusted server-side data.
Affected Agents: Gerard, Naut, Femke
