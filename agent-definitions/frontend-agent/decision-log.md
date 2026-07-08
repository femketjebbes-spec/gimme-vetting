# Decision Log

[2026-07-07] [Session 1] DECISION: Import path for ExcelUpload component uses relative path from src/main.jsx
Rationale: From `src/main.jsx`, the path `./frontend/components/ExcelUpload` correctly resolves to `src/frontend/components/ExcelUpload.jsx`. The initial path `../frontend/components/ExcelUpload` was incorrect because `..` from `src/` goes to the project root, not `src/frontend/`.

[2026-07-08] [Session 2] DECISION: Download Template button uses aria-label matching button text for accessible name resolution
Rationale: Testing Library's getByRole with role "button" resolves the accessible name from either the button text content or aria-label. When aria-label differs from button text, the aria-label takes precedence. The test uses `/download template/i` regex which matched the button text but not the aria-label value "Download Excel template". Setting aria-label to "Download Template" resolves this mismatch.
Affected Tests: `ExcelUpload.test.jsx` - Download Template Button describe block

[2026-07-08] [Session 2] DECISION: Template download uses blob URL pattern with dynamic filename extraction from Content-Disposition header
Rationale: The API contract specifies `Content-Disposition: attachment; filename="invoice-intake-template.xlsx"`. The frontend extracts the filename via regex matching on the response header, falling back to `invoice-intake-template.xlsx`. A temporary blob URL is created, an anchor element triggers the download, then cleanup removes the URL and element. This pattern is consistent with browser-native file download handling without requiring the backend to serve a static URL.
Derived from: `docs/api-contract-wi-007.md` GET /api/v1/intake/excel/template response specification

[2026-07-08] [Session 2] TEST-SPEC: `4-frontend/src/frontend/components/__tests__/ExcelUpload.test.jsx` maps to WI-007 delegation subtask 1 (Frontend Implementation)
Purpose: Validates that the Download Template button renders, triggers GET request to correct endpoint, and uses the correct API path
Derived from: `docs/wi-007-delegation-parallel.md` Subtask 1
