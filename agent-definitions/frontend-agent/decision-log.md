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

[2026-07-08] [Session 3] DECISION: Changed Content-Disposition filename regex from greedy `.+` to negated character class `[^";]+`
Assumptions: The proxy server may append trailing whitespace or carriage return characters to the Content-Disposition header value. The greedy `.+` in the original regex `/filename="?(.+)"?$/i` captured these trailing characters, producing filenames like `invoice-intake-template.xlsx_`.
Rationale: The negated character class `[^";]+` stops matching at the first `;` or `"` character, preventing trailing characters from being captured. The `.trim()` call removes any remaining whitespace. This fix ensures the filename extracted from the header matches the expected value exactly.
Derived from: Bug report at `docs/bug-wi-007-download-filename-underscore.md`

[2026-07-08] [Session 3] DECISION: Blob MIME type is explicitly set from the response Content-Type header during download
Rationale: The original code passed `blob` directly without explicitly creating a new Blob with the correct MIME type. When the proxy modifies the response, the blob may lose its type metadata. Creating `new Blob([blob], { type: contentType })` ensures the downloaded file has the correct type.
Derived from: `docs/api-contract-wi-007.md` GET /api/v1/intake/excel/template response Content-Type specification

[2026-07-08] [Session 3] TEST-SPEC: `4-frontend/src/frontend/components/__tests__/ExcelUpload.test.jsx` Download Template Button describe block includes two regression tests
Purpose: Test 1 validates that trailing whitespace in Content-Disposition header does not pollute the filename. Test 2 validates that a filename with trailing underscore character in the header is correctly extracted without stripping.
Derived from: Bug report at `docs/bug-wi-007-download-filename-underscore.md`

[2026-07-08] [Session 4] DECISION: Removed client-side MIME type validation from ExcelUpload.jsx handleFileChange()
Assumptions: The contract (`docs/api-contract-wi-002.md` v2.1.0, Section 3.2.1) makes MIME type a supplementary hint. Browsers/OSes may report unrecognized MIME types (e.g., `application/octet-stream`, `application/zip`) for valid `.xlsx` files. The backend performs authoritative content-based detection via magic byte inspection.
Rationale: The original code blocked uploads when the file's MIME type was not in the accepted set, preventing users from uploading valid files whose browsers reported unrecognized MIME types. This created a UX regression that the BR-001 fix aims to resolve. Removing the client-side check allows all files to reach the backend, which handles authoritative validation.
Derived from: `docs/wi-008-delegation-parallel.md` Subtask 2 exception clause, `docs/api-contract-wi-002.md` v2.1.0 Section 3.2.1

[2026-07-08] [Session 4] DECISION: HTML accept attribute retained on file input
Assumptions: The `accept=".xlsx,.csv"` attribute on the file input affects only the file picker dialog filter, not actual file rejection.
Rationale: The accept attribute is UI convenience for the file picker dialog. It does not block uploads programmatically and does not contradict the contract. Removing it would reduce UX clarity without any functional benefit.

[2026-07-08] [Session 4] TEST-SPEC: `4-frontend/src/frontend/components/__tests__/ExcelUpload.test.jsx` test "validates MIME type before upload for non-matching types" (line 71) is now obsolete
Purpose: This test validated the client-side MIME type blocking behavior that was removed per BR-001. The test expects the frontend to reject files with non-matching MIME types, which contradicts the contract.
Derived from: `docs/wi-008-delegation-parallel.md` Subtask 2
Status: Requires regeneration by Testing Mode with Archibald authorization

[2026-07-09] [Session 5] DECISION: Manual URL construction to avoid URLSearchParams comma encoding
Assumptions: URLSearchParams encodes commas to %2C (e.g., `id,asc` becomes `id%2Casc`). The backend expects unencoded sort parameters.
Rationale: Replaced URLSearchParams with manual URL construction using array join to produce clean query strings like `sort=id,asc`.
Derived from: `docs/api-contract-wi-ca-001.md` Section 2.2 sort parameter specification

[2026-07-09] [Session 5] DECISION: TextEncoder/TextDecoder polyfill added to setupTests.js
Assumptions: react-router-dom v7 requires TextEncoder/TextDecoder which jsdom does not provide natively.
Rationale: Added `import { TextEncoder, TextDecoder } from 'util'` and `global.TextEncoder`/`global.TextDecoder` to setupTests.js to prevent "TextEncoder not defined" errors during test execution.

[2026-07-09] [Session 5] DECISION: StatusBadge uses separate function declaration and named/default export to avoid duplicate export error
Assumptions: `export function Name {}` creates an implicit named export. Adding a second `export { Name }` creates a duplicate.
Rationale: Changed to `function Name {}` (no export keyword on declaration) then `export { Name }` and `export default Name` to allow both named and default imports in tests.

[2026-07-09] [Session 5] DECISION: AnalystDashboard tests use MemoryRouter wrapper for react-router-dom context
Assumptions: AnalystDashboard uses `<Link>` from react-router-dom which requires router context via BrowserRouter/MemoryRouter.
Rationale: Created `renderWithRouter` helper that wraps component renders in `<MemoryRouter>` to provide router context for Link components in tests.

[2026-07-09] [Session 5] DECISION: Invoice table rendering test provides at least one invoice in mock response
Assumptions: InvoiceTable renders either a `<table>` element (with data) or an empty state `<p>` (without data).
Rationale: The test for table rendering must include at least one invoice in the mock response so InvoiceTable renders the actual table element rather than the empty state message.

[2026-07-09] [Session 5] DECISION: Search/filter re-fetch tests track call count instead of clearing mock
Assumptions: `mockFetch.mockClear()` loses context of subsequent fetch calls in async React state update cycles.
Rationale: Track initial call count before user interaction, then assert the last call in `mock.calls` contains the expected query parameter after interaction.

[2026-07-09] [Session 5] TEST-SPEC: `4-frontend/src/business-service/api/__tests__/analystApi.test.js` maps to WI-CA-001 subtask (fetch list, fetch detail)
Purpose: Validates fetchInvoiceList URL construction, fetchInvoiceDetail path variable, input validation, error handling
Derived from: `docs/wi-ca-001-delegation-parallel.md` Femke subtask, `docs/api-contract-wi-ca-001.md` v1.0.0

[2026-07-09] [Session 5] TEST-SPEC: `4-frontend/src/business-service/components/__tests__/StatusBadge.test.jsx` maps to WI-CA-001 subtask (status badge rendering)
Purpose: Validates QUEUED and REJECTED status color classes and default to QUEUED
Derived from: `docs/wi-ca-001-delegation-parallel.md` status enums: QUEUED/REJECTED_TYPE_A/REJECTED_TYPE_B

[2026-07-09] [Session 5] TEST-SPEC: `4-frontend/src/business-service/components/__tests__/InvoiceTable.test.jsx` maps to WI-CA-001 subtask (invoice table display, pagination)
Purpose: Validates invoice row rendering, status badge display, pagination controls, empty state
Derived from: `docs/wi-ca-001-delegation-parallel.md` invoice list pagination and status filter

[2026-07-09] [Session 5] TEST-SPEC: `4-frontend/src/business-service/components/__tests__/InvoiceDrawer.test.jsx` maps to WI-CA-001 subtask (detail drawer)
Purpose: Validates drawer open/close, overlay dismiss, all 10 invoice fields rendered
Derived from: `docs/wi-ca-001-delegation-parallel.md` single invoice detail endpoint

[2026-07-09] [Session 5] TEST-SPEC: `4-frontend/src/business-service/components/__tests__/AnalystDashboard.test.jsx` maps to WI-CA-001 subtask (main dashboard integration)
Purpose: Validates search input, status filter, table rendering, re-fetch on search/filter, header, navigation link
Derived from: `docs/wi-ca-001-delegation-parallel.md` analyst dashboard with search and status filter
