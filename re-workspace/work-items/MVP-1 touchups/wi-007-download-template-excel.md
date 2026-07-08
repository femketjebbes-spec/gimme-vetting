# Work Item 007: Download Template Excel Sheet

- **Work Item ID**: WI-007
- **Version**: 1.0.0
- **Last Updated**: 2026-07-08
- **Status**: Completed
- **Related Work Items**: [WI-002](wi-002-excel-file-upload-and-parsing.md)

## Overview
Users uploading Excel files for invoice intake need a pre-formatted template file they can download. This template will contain the correct column headers so users can fill in their invoice data in the expected format before uploading.

## Background
The existing Excel intake system ([WI-002](wi-002-excel-file-upload-and-parsing.md)) requires files with exactly five mandatory columns (case-insensitive matching):
1. `invoice number`
2. `debtor name`
3. `address`
4. `phone number`
5. `bank account number`

Users currently have no way to obtain a correctly-formatted template file, leading to upload failures due to column name mismatches or missing fields.

## Functional Requirements

### FR-001: Template Download Endpoint
- **Description**: The backend shall expose a REST endpoint that serves a pre-generated template Excel file (.xlsx) containing the correct column headers.
- **Priority**: High
- **Acceptance Criteria**:
  - A GET endpoint exists at `/api/v1/intake/excel/template`
  - The endpoint returns an HTTP 200 OK with the template file as the response body
  - The response includes `Content-Disposition: attachment` header with filename `invoice-intake-template.xlsx`
  - The response includes `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
  - The template file contains exactly 5 columns with headers: `invoice number`, `debtor name`, `address`, `phone number`, `bank account number`
  - The template contains at least one empty data row as a visual guide
  - The endpoint returns HTTP 500 if template generation fails

### FR-002: Template File Content
- **Description**: The template Excel file shall be a valid .xlsx file with proper column headers matching the validation allowlist.
- **Priority**: High
- **Acceptance Criteria**:
  - The template is a valid Apache POI-generated XLSX file
  - Column headers exactly match (case-sensitive in the file): `invoice number`, `debtor name`, `address`, `phone number`, `bank account number`
  - Headers are in row 0 (first row), columns A through E (indices 0-4)
  - The sheet name is "Template" or "Invoice Intake Template"
  - Column headers are visually distinguishable (bold font, optional)

### FR-003: Frontend Download Button
- **Description**: The frontend shall provide a visible download button that triggers the template file download.
- **Priority**: Medium
- **Acceptance Criteria**:
  - A download button labeled "Download Template" or similar is displayed near the Excel upload component
  - Clicking the button makes a GET request to `/api/v1/intake/excel/template`
  - The browser initiates a file download with the filename `invoice-intake-template.xlsx`
  - The button is accessible and follows existing UI patterns

## Non-Functional Requirements

### NFR-001: Performance
- **Description**: Template generation and download shall be fast.
- **Metrics**:
  - Template endpoint response time < 500ms
  - Template file size < 100KB

### NFR-002: Maintainability
- **Description**: The template generation logic shall be encapsulated and reusable.
- **Metrics**:
  - Template generation logic resides in a dedicated service method or the existing `ExcelParsingService`
  - Column headers are defined as a constant to prevent drift from the validation allowlist

## Technical Notes
- The backend already uses Apache POI (`XSSFWorkbook`) for Excel generation (see [`ExcelParsingService.generateReturnXlsx()`](5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/service/ExcelParsingService.java:516))
- The column headers are defined in [`ExcelParsingService.ALLOWED_COLUMN_NAMES`](5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/service/ExcelParsingService.java:28)
- A new endpoint should be added to [`ExcelIntakeController`](5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/ExcelIntakeController.java) following the existing pattern
- The template generation can share logic with [`ExcelParsingService.generateReturnXlsx()`](5-backend/business-service/src/main/java/com/gimmevettingsolution/intake/service/ExcelParsingService.java:516) but without the "Issue" column (the return Excel has 6 columns; the template should have only 5)

## Resolved Open Questions

1. **Template data**: The template is empty (no example data). It contains only headers and at least one empty data row.
2. **File format**: XLSX only. No CSV template variant for MVP.
3. **Data validation rules**: None. The template is minimal — headers and empty rows only, no dropdown lists, no cell formatting, no input restrictions.

## Status

**Current**: Proposed
**Resolved**: 2026-07-08 (Session 8)
