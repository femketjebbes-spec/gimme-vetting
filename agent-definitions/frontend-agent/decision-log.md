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
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 3cacf7e (Bugfix waarbij de template excel niet een excel file was)

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
<<<<<<< HEAD
=======
>>>>>>> 4a4153c (wi-007 af)
=======
>>>>>>> 3cacf7e (Bugfix waarbij de template excel niet een excel file was)
