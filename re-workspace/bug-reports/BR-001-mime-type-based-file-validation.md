# Bug Report BR-001: MIME-Type-Based File Validation Rejects Valid Excel Files

- **Document ID**: BR-001
- **Version**: 1.0
- **Last Updated**: 2026-07-08
- **Status**: Open
- **Severity**: High (blocks core functionality — Excel upload)
- **Component**: Excel Intake (`ExcelIntakeController`, `ExcelParsingService`)
- **Related Work Items**: [WI-002](re-workspace/work-items/MVP-1/wi-002-excel-file-upload-and-parsing.md)

---

## 1. Problem Statement

When users upload a valid `.xlsx` Excel file through the frontend, they receive the error:

> "The uploaded file is not a valid Excel or CSV file."

This occurs even though the file content is a properly formatted Excel spreadsheet that Apache POI (the library used by the backend) can parse without issues.

---

## 2. Root Cause

The backend validates uploaded files **exclusively by MIME type** before attempting any content inspection. The relevant code is in [`ExcelParsingService.isSupportedMimeType()`](5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/service/ExcelParsingService.java:60), which only accepts exactly two MIME types:

- `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` (`.xlsx`)
- `text/csv` (`.csv`)

This approach is fragile because **browsers report MIME types inconsistently**:

| Browser / Scenario | Reported MIME Type | Result |
|---|---|---|
| Chrome on standard `.xlsx` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | Accepts |
| Firefox on standard `.xlsx` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | Accepts |
| Some browsers / corrupted file association | `application/octet-stream` | **Rejected** |
| macOS with unusual file associations | `application/zip` | **Rejected** |
| File with no extension | `""` (empty string) | **Rejected** |
| Legacy `.xls` file | `application/vnd.ms-excel` | **Rejected** |

When the MIME type doesn't match the hardcoded allowlist, [`ExcelIntakeController`](5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/ExcelIntakeController.java:58) returns `INVALID_FILE_FORMAT` at line 58–64 — **before** Apache POi ever gets a chance to verify the file content.

### Why This Matters

MIME types are **metadata provided by the client (browser)**. They are not reliably enforced or accurate. Relying on them for file validation is a known anti-pattern. The correct approach is **content-based validation** (also known as "magic bytes" or "file signature" detection).

---

## 3. Impact

- **User-facing**: Users on certain browsers, operating systems, or file configurations cannot use the core Excel upload feature.
- **Security**: MIME-type validation alone is not a security control — it can be bypassed by simply changing a header. Content-based validation is equally (or more) secure while being more reliable.
- **Correctness**: The system rejects valid files, which is worse than accepting invalid ones (fail-safe vs. fail-open for valid content).

---

## 4. Required Fix (Functional Requirements)

### FR-BR001-01: Content-Based File Format Detection

The system SHALL detect the uploaded file format by inspecting file content (magic bytes / file signature), not by relying solely on the MIME type reported by the browser.

**Acceptance Criteria:**

1. `.xlsx` files SHALL be detected by their ZIP file signature (first 4 bytes: `50 4B 03 04`).
2. `.csv` files SHALL be detected by checking if the file content is valid UTF-8 / ASCII text.
3. The file extension (if present) SHOULD be used as a supplementary hint but MUST NOT override content-based detection.
4. Files whose content does not match any supported format SHALL be rejected with `INVALID_FILE_FORMAT`.

### FR-BR001-02: Fallback MIME Type Handling

When the browser reports an unrecognized or empty MIME type, the system SHALL NOT immediately reject the file. Instead, it SHALL fall back to content-based detection.

**Acceptance Criteria:**

1. If `mimeType` is `null` or empty, the system SHALL proceed to content-based detection.
2. If `mimeType` is present but not in the supported list (e.g., `application/octet-stream`), the system SHALL proceed to content-based detection.
3. If `mimeType` is explicitly in the supported list (e.g., `text/csv`), the system MAY skip content-based detection for performance.

### FR-BR001-03: Error Message Accuracy

When a file is rejected, the error message SHALL indicate the actual reason (e.g., "not a recognized Excel format") rather than a generic "not a valid Excel or CSV file."

---

## 5. Recommended Implementation Approach

> **Note**: This is a recommendation from the Requirements Engineer. Implementation details are left to the coding agents.

### Step 1: Create a `FileTypeDetector` Service

A new service (or an extension of `ExcelParsingService`) that determines file type by content:

```
FileTypeDetector.detectFileType(InputStream) -> FileType enum
  - FileType.XLSX: if ZIP signature (PK\x03\x04) is found
  - FileType.CSV: if content is valid text
  - FileType.UNKNOWN: otherwise
```

### Step 2: Modify `ExcelIntakeController`

Replace the current MIME-type-only check with:

```
1. If mimeType is supported (current check) -> skip content detection, use mimeType.
2. If mimeType is null/empty/unsupported -> call FileTypeDetector to detect format.
3. If detected format is XLSX or CSV -> proceed to parse.
4. Otherwise -> reject with INVALID_FILE_FORMAT.
```

### Step 3: Update `ExcelParsingService`

Add a `detectFileType(InputStream)` method that:
- Reads the first 4 bytes of the input stream
- Checks for ZIP signature (`50 4B 03 04` → "PK\x03\x04")
- If ZIP, attempts to open as XLSX via Apache POI (already handles this)
- If not ZIP, reads first line as text and checks if it parses as CSV

### Step 4: Add Tests

- Test with `.xlsx` files where browser reports `application/octet-stream`
- Test with `.xlsx` files where browser reports `application/zip`
- Test with `.csv` files where browser reports `text/csv; charset=utf-8`
- Test with non-Excel files (e.g., `.pdf`, `.jpg`) — must be rejected
- Test with empty files — must be rejected
- Test with corrupted `.xlsx` files — must be rejected with clear message

---

## 6. Related Files

| File | Path | Relevance |
|------|------|-----------|
| `ExcelIntakeController.java` | [`5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/ExcelIntakeController.java`](5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/ExcelIntakeController.java) | Controller that performs MIME-type check |
| `ExcelParsingService.java` | [`5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/service/ExcelParsingService.java`](5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/service/ExcelParsingService.java) | Contains `isSupportedMimeType()` and parsing logic |
| `ExcelUpload.jsx` | [`4-frontend/src/frontend/components/ExcelUpload.jsx`](4-frontend/src/frontend/components/ExcelUpload.jsx) | Frontend component displaying the error |
| `InvalidFileFormatResponse.java` | [`5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/dto/InvalidFileFormatResponse.java`](5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/dto/InvalidFileFormatResponse.java) | Error response DTO |

---

## 7. Notes

- This bug does NOT affect `.xls` (legacy Excel) files — those are a separate limitation (Apache POI HSSFWorkbook would be needed). This bug report is focused on `.xlsx` files failing due to MIME type mismatch.
- The frontend MIME type check at [`ExcelUpload.jsx:41`](4-frontend/src/frontend/components/ExcelUpload.jsx:41) is already documented as "convenience only, backend enforces server-side." This means the backend is the authoritative check and must be fixed.
