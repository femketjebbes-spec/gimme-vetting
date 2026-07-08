# Bug Report: WI-007 Template Download — Trailing Underscore in Filename

**Filed By**: Robbie (Requirements Engineer)
**Assigned To**: Femke (Frontend Agent)
**Component**: `4-frontend/src/frontend/components/ExcelUpload.jsx` — `handleDownloadTemplate()`
**Date**: 2026-07-08

## Symptom

When clicking "Download Template", the browser downloads a file named `invoice-intake-template.xlsx_` instead of `invoice-intake-template.xlsx`. The trailing underscore causes the OS/browser to not recognize the file as an Excel file.

## Root Cause

The filename extraction regex on **line 128** of `ExcelUpload.jsx` is:

```javascript
const filenameMatch = disposition.match(/filename="?(.+)"?$/i);
```

The `$` anchor in JavaScript regex matches end-of-string **or** right before a trailing newline. If the `Content-Disposition` header contains trailing whitespace, `\r`, or `\n` (which can happen through the Vite proxy at `http://localhost:8082`), the `.+` group greedily captures the underscore character before the `$` anchor matches.

The regex also does not account for the semicolon and parameter separator. The full header value from the backend is:

```
attachment; filename="invoice-intake-template.xlsx"
```

But the regex `/"?(.+)"?$/` will match greedily and may capture unexpected characters.

**Existing code (line 125-132):**

```javascript
const disposition = response.headers.get('content-disposition');
let filename = 'invoice-intake-template.xlsx';
if (disposition) {
  const filenameMatch = disposition.match(/filename="?(.+)"?$/i);
  if (filenameMatch && filenameMatch[1]) {
    filename = filenameMatch[1];
  }
}
```

## Fix Required

1. **Trim the filename** after extraction to remove trailing whitespace/characters.
2. **Tighten the regex** to match only the filename value, not trailing characters.
3. **Use the correct MIME type** for the blob to ensure proper browser handling — the current code does not set the blob type from the response Content-Type.

## Suggested Code Change

```javascript
const disposition = response.headers.get('content-disposition');
let filename = 'invoice-intake-template.xlsx';
if (disposition) {
  // Match filename value with or without quotes, non-greedy
  const filenameMatch = disposition.match(/filename="?([^";]+)"?/i);
  if (filenameMatch && filenameMatch[1]) {
    filename = filenameMatch[1].trim();
  }
}
```

Additionally, use the response Content-Type to set the blob type:

```javascript
const blob = await response.blob().then(b => 
  new Blob([b], { type: response.headers.get('content-type') || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
);
```

Or simply:

```javascript
const contentType = response.headers.get('content-type');
const blob = await response.blob();
// The browser should auto-set blob.type from Content-Type, but be explicit:
const downloadBlob = contentType ? new Blob([blob], { type: contentType }) : blob;
```

## Test Update Needed

The test mock at `ExcelUpload.test.jsx:458-479` needs to return a proper blob with type, since the current mock returns `new Blob()` with no type and no content. The test should verify the blob has the correct MIME type.

## Files to Edit

| File | Lines | Change |
|------|-------|--------|
| `4-frontend/src/frontend/components/ExcelUpload.jsx` | 114-149 | Fix filename regex and add explicit blob MIME type |
| `4-frontend/src/frontend/components/__tests__/ExcelUpload.test.jsx` | 458-499 | Update mocks to include blob with correct type |

## Verification Steps

After fixing:
1. Run `npm start` in `4-frontend/`
2. Run the backend on port 8082
3. Click "Download Template"
4. Verify the downloaded file is named `invoice-intake-template.xlsx` (no trailing underscore)
5. Open with Excel — should show 5 column headers: `invoice number`, `debtor name`, `address`, `phone number`, `bank account number`
