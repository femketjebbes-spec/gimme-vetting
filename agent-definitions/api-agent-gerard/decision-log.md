# Gerard Decision Log

## WI-CA-003 Decisions

[2026-07-09] [Session WI-CA-003] DECISION: API contract version 1.0.0 for source file viewing endpoint
Assumptions: Backend will implement FileBackedExcelStoreService following FileBackedPoCStoreService pattern. Frontend will add fetchSourceFile API function and wire Bekijken link in InvoiceDrawer.
Rationale: Contract follows the same structural pattern as wi-ca-001 v1.0.0 for consistency. New endpoint returns raw file bytes (not base64) to allow browser-native download handling. Schema extensions use nullable strings for sourceFileId and sourceFilename to accommodate both Excel-imported and single-invoice-API invoices.

[2026-07-09] [Session WI-CA-003] DECISION: Error response for missing source file uses 404 Not Found with JSON body
Assumptions: The frontend checks for 404 status to display "No source file available" message in the detail drawer.
Rationale: Consistent with the existing error pattern used by AnalystController for invoice not found. The JSON body matches the format specified in the delegation plan.

[2026-07-09] [Session WI-CA-003] DECISION: Error response for missing file in store uses 500 Internal Server Error
Assumptions: A missing file in the store indicates a system issue (file deletion, disk failure) rather than a client error.
Rationale: The UUID mapping exists in the database but the file is gone from disk. This is a server-side problem that may require administrative intervention. The message "Source file is unavailable" is user-friendly and does not expose the store path.

[2026-07-09] [Session WI-CA-003] DECISION: source_filename column added alongside source_file_id on invoices table
Assumptions: Naut will add source_filename VARCHAR(256) column to the Flyway migration V3.
Rationale: The original filename is needed for Content-Disposition header in the serving endpoint. Storing it on the Invoice entity (rather than a separate mapping table) keeps the data co-located and simplifies queries. Both columns are nullable for single-invoice API imports.

[2026-07-09] [Session WI-CA-003] DECISION: Next agent in pipeline is Femke-Naut-parallel
Assumptions: Alignment Agent will approve this contract review, and Archibald will produce a parallel delegation plan for Femke and Naut.
Rationale: WI-CA-003 requires both backend implementation (Naut: FileBackedExcelStoreService, entity changes, new endpoint) and frontend implementation (Femke: fetchSourceFile API, Bekijken link wiring). Both can proceed in parallel once the contract is stable.
