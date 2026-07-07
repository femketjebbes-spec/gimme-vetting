# Session History

## Session 5 — 2026-07-07 — WI-002 Excel File Upload and Parsing

### What Was Explored

User requested implementation of the next work item. After reviewing work items.md and the completion status of WI-001 (fully approved by Alignment Agent), WI-002 was identified as the correct next item. It is the entry point of the MVP Excel intake pipeline with no upstream dependencies.

Architectural decisions D-024 through D-029 and security requirements S-007 through S-011 were documented in architecture-decisions.md. The user confirmed strict column name matching (no aliases accepted, case-insensitive).

### What Was Decided

1. **D-024**: Strict column name matching — only the five allowlisted column names accepted (case-insensitive). No aliases.
2. **D-025**: Format support (.xlsx, .csv), optional header row, column-position fallback mapping.
3. **D-026**: No authentication for MVP. Javadoc/JSDoc notes required.
4. **D-027**: No file size limit for MVP.
5. **D-028**: Synchronous processing model.
6. **D-029**: Apache POI 5.2.5 mandated with XML entity expansion disabled.
7. **S-007**: Server-side MIME type validation.
8. **S-008**: Column name allowlist enforcement.
9. **S-010**: Filename sanitization against path traversal.
10. **S-011**: Temporary file cleanup policy for return Excel files.

Gerard produced `docs/api-contract-wi-002.md` (version 2.0.0). Alignment Agent approved. Femke produced frontend component `ExcelUpload.jsx` with 16 passing Jest tests. Naut produced backend implementation with 69 passing Maven tests. Alignment Agent approved Naut's backend implementation.

### What Remains Open

- WI-003 (Per-Row Mandatory Field Validation) — downstream of WI-002
- WI-004 (Return Excel Generation) — downstream of WI-002 and WI-003
- WI-005 (Separate PoC Upload Endpoint) — parallel after WI-001

### Assumptions Made

- Apache POI 5.2.5 is sufficient for MVP file size and performance requirements. EasyExcel can be evaluated if large-file performance becomes critical (AUNV-006).
- Synchronous processing is acceptable for MVP. Async processing can be evaluated if file sizes or processing times become problematic.
- Temporary file cleanup for return Excel files uses server-side temporary directory with no explicit retention period for MVP. This is a minor operational gap.
