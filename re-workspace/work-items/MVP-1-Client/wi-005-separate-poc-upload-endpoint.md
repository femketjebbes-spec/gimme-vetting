# Work Item: WI-005 — Separate PoC File Upload Endpoint

**Parent Requirement:** RQ-009 (Separate PoC Upload)
**Parent Work Stream:** W-007 (Excel Batch Intake Pipeline)
**Business Objective:** OPE-001 — Reduce case analyst workload by automating repetitive validation tasks in the vetting intake process.
**Created:** 2026-07-07 [Session 5]
**Status:** Completed
**Completed:** 2026-07-08
**Implemented By:** Naut (Backend), Femke (Frontend PoCUpload)
**Estimated Effort:** 0.5–1 sprint (TBD by development team)

---

## 1. Requirement Statement

Gimme shall provide a client portal endpoint for uploading PoC (Proof of Correspondence) files separately from the Excel invoice batch.

The client can upload PoC files (PDF) via the client portal. Each uploaded PoC file must be named according to the convention: the invoice number (System Identifier) matches the PoC filename (case-insensitive, per D-001).

Uploaded PoC files are stored in the same PoC store as the single-invoice intake path (configurable path, per D-003).

The client portal shall display a list of invoice numbers that are missing PoC files (from the return Excel, RQ-008), enabling the client to upload the corresponding PoC files.

PoC upload is independent of the Excel upload. The client receives the return Excel, then uploads the missing PoC files separately, then re-uploads the corrected Excel.

---

## 2. Acceptance Criteria (Gherkin)

### Feature: Separate PoC File Upload

As a client,
I need to upload missing PoC files separately,
So that previously rejected invoices (missing PoC) can pass the PoC existence gate on re-upload.

---

#### Scenario 1: Upload a PoC file with correct filename

```gherkin
Given the return Excel indicated that invoice "INV-2026-0042" is missing a PoC file
And the client uploads a PDF file named "INV-2026-0042.pdf" via the separate PoC upload endpoint
When the upload is processed
Then the file shall be stored in the PoC store
And the filename shall be matched case-insensitively to the invoice number
And a subsequent Excel upload containing "INV-2026-0042" shall pass the PoC existence gate
```

**Rationale:** This is the happy path. The client uploads a PoC file with the correct filename. The file is stored in the same PoC store as the single-invoice intake path (WI-001).

---

#### Scenario 2: Upload a PoC file with incorrect filename

```gherkin
Given the return Excel indicated that invoice "INV-2026-0042" is missing a PoC file
And the client uploads a PDF file named "WRONG-NAME.pdf" via the separate PoC upload endpoint
When the upload is processed
Then the file shall be stored in the PoC store
But the filename shall not match the expected invoice number "INV-2026-0042"
And a subsequent Excel upload containing "INV-2026-0042" shall still fail the PoC existence gate
```

**Rationale:** The client may upload a PoC file with the wrong filename. The file is stored (it is not rejected), but it will not match any invoice number during PoC verification. The client must upload the correctly named file.

**Note:** This behaviour may be debatable. An alternative is to reject the upload with an error message indicating the filename does not match any expected invoice number. The specification does not mandate which approach; the architect must decide or escalate to the user.

---

#### Scenario 3: Upload multiple PoC files for different invoices

```gherkin
Given the return Excel indicated that invoices "INV-2026-0042" and "INV-2026-0043" are missing PoC files
And the client uploads two PDF files:
  - "INV-2026-0042.pdf"
  - "INV-2026-0043.pdf"
When both uploads are processed
Then both files shall be stored in the PoC store
And subsequent Excel uploads containing either invoice number shall pass the PoC existence gate
```

**Rationale:** The client may need to upload multiple PoC files. Each upload is independent and processed immediately.

---

#### Scenario 4: Upload a non-PDF file

```gherkin
Given the return Excel indicated that invoice "INV-2026-0042" is missing a PoC file
And the client uploads a non-PDF file (e.g., "INV-2026-0042.docx") via the separate PoC upload endpoint
When the upload is processed
Then the system shall either reject the file with a clear error message or store it and let it be ignored by PoC matching
```

**Rationale:** The specification does not mandate whether non-PDF files should be rejected outright or accepted and ignored. The architect must decide. PoC matching in WI-001 already handles non-PDF files by stripping `.pdf` extension if present, so non-PDF files would be ignored during matching anyway.

---

#### Scenario 5: Duplicate PoC upload for same invoice

```gherkin
Given the client has already uploaded "INV-2026-0042.pdf" to the PoC store
And the client uploads another PDF file named "INV-2026-0042.pdf" via the separate PoC upload endpoint
When the upload is processed
Then the file shall be stored in the PoC store
And the existing file shall either be overwritten or preserved (architect decision)
And a subsequent Excel upload containing "INV-2026-0042" shall pass the PoC existence gate
```

**Rationale:** Duplicate uploads are not an error condition. D-002 confirms that multiple PoC files for the same invoice are not an error. The architect must decide whether the second upload overwrites the first or is rejected with a message.

---

#### Scenario 6: Client portal displays list of invoice numbers missing PoC

```gherkin
Given the client has uploaded an Excel file and received a return Excel
And the return Excel contains 3 rows with "MISSING_POC" issues
When the client opens the client portal
Then the portal shall display a list of the 3 invoice numbers that are missing PoC files
And each invoice number shall be associated with a PoC upload link or button
```

**Rationale:** The client needs to know which invoice numbers are missing PoC files. The return Excel provides this information, but the client portal should also display it for convenience. This is a UI requirement — the architect produces the interface design.

---

## 3. ISO 29148 Quality Attribute Validation

| Attribute | Assessment | Notes |
|-----------|------------|-------|
| **Unambiguous** | PARTIAL | "The client portal shall display a list of invoice numbers" is clear, but the mechanism for associating uploaded PoC files with specific invoices is implicit (filename matching). The architect must make this explicit in the design. |
| **Complete** | PARTIAL | The requirement specifies the PoC upload endpoint and filename matching. It does not specify file type enforcement, duplicate handling, or the client portal UI for displaying missing PoC invoice numbers. |
| **Consistent** | PASS | No conflict with other requirements. This is the supplementary intake mechanism for PoC files. |
| **Verifiable** | PASS | All 6 acceptance criteria are directly testable. |
| **Feasible** | PASS | File upload and storage is a standard operation. The existing PoCStoreService can be reused. |
| **Traceable** | PASS | Traces to OPE-001 (business objective) and Session 5 requirement. |

**Quality Verdict:** PASS with minor completeness notes. File type enforcement, duplicate handling, and client portal UI are delegated to the architect.

---

## 4. Design Decisions (Logged)

| Decision | Rationale | Stakeholder Authority | Assumptions Dependent |
|----------|-----------|----------------------|----------------------|
| D-014: Separate PoC upload is not integrated into Excel upload | User confirmed the client uploads PoC files separately. The client portal displays which invoice numbers are missing PoC files. | Confirmed by user in Session 5 | None |

---

## 5. Out of Scope

The following are explicitly excluded from WI-005:

- Excel file parsing — handled by WI-002
- Mandatory field validation — handled by WI-003
- Return Excel generation — handled by WI-004
- Client portal UI (list of missing PoC invoice numbers, upload buttons) — outside RE scope; architect produces interface design
- Bulk PoC upload — single file upload only for MVP
- PoC content validation — only filename matching, not content analysis (confirmed out of scope in Session 1)

---

## 6. Implementation Notes

### Input Contract

The client uploads a single file via a POST endpoint:

| Attribute | Value |
|-----------|-------|
| Format | PDF recommended, but architect may allow other formats |
| Filename | Must match invoice number (case-insensitive, per D-001) |
| Maximum file size | No limit for MVP (consistent with D-021) |
| Authentication | None for MVP (consistent with D-020) |
| Endpoint | POST /api/v1/poc-upload (TBD by Gerard) |

### Output Contract

After upload, the system:
1. Stores the file in the PoC store (existing `FileBackedPoCStoreService`)
2. Returns a success or error response
3. The file is immediately available for PoC matching (subsequent Excel uploads will find it)

### Filename Matching

The existing `PoCStoreService.hasMatchingPoC(invoiceNumber)` method performs:
```java
poCFileName.toLowerCase().endsWith(".pdf")
    ? poCFileName.toLowerCase().substring(0, poCFileName.toLowerCase().length() - 4)
    : poCFileName.toLowerCase()
== invoiceNumber.toLowerCase()
```

Uploaded PoC files are stored in the same PoC store. No new matching logic is needed — the existing algorithm applies.

### API Endpoint Design (TBD by Gerard)

Proposed endpoint signature:
```
POST /api/v1/poc-upload
Content-Type: multipart/form-data
Body: file (PDF)
Response: 200 OK with { "status": "UPLOADED", "invoiceNumber": "INV-2026-0042" }
       or 400 Bad Request with { "status": "ERROR", "message": "..." }
```

**Note:** The actual API contract is outside RE scope. Gerard (API Agent) produces the formal API specification.

### Integration with Existing PoCStoreService

WI-005 reuses the existing `FileBackedPoCStoreService` for file storage. The existing service already handles:
- Path traversal protection (D-003)
- Case-insensitive filename matching
- PDF extension stripping
- File system directory operations

No changes to `PoCStoreService` are required for WI-005. The new endpoint simply calls `PoCStoreService.store(file)` (a new method to be designed by the architect).

### Client Portal UI (TBD by Architect)

The client portal shall display:
1. A list of invoice numbers that are missing PoC files (from the return Excel, RQ-008)
2. An upload button or drag-and-drop area for each invoice number
3. Feedback after upload: success or error message

This is a UI requirement — the architect produces the interface design. The frontend agent (Femke) implements the frontend code.

### Data Model Requirements

No new persistent entities are needed. The existing `PoCStoreService` manages PoC file storage. No database changes are required.

---

## 7. Dependencies

| Dependency | Status | Notes |
|------------|--------|-------|
| WI-001 (PoCStoreService) | Upstream | Direct dependency. WI-005 stores files using the existing PoCStoreService. |
| WI-002 (Excel Upload and Parsing) | Parallel | Not a direct dependency. The return Excel from WI-002/WI-004 provides the list of missing PoC invoice numbers. |
| WI-004 (Return Excel) | Upstream | Provides the list of invoice numbers missing PoC files. |
| Gerard (API Agent) | Parallel | Gerard defines the API contract for the PoC upload endpoint. |
| Frontend Agent (Femke) | Parallel | Frontend agent implements the client portal UI for displaying missing PoC invoice numbers and upload buttons. |

---

## 8. Test Strategy

### Unit Tests

- Upload a PoC file with correct filename: stored in PoC store
- Upload a PoC file with incorrect filename: stored but not matched
- Upload a non-PDF file: either rejected or stored and ignored
- Duplicate upload: overwrite or reject (architect decision)
- Filename matching: case-insensitive match after upload

### Integration Tests

- Full flow: Excel upload → return Excel shows missing PoC → PoC upload → Excel re-upload → PoC verification passes
- Verify that a previously Type A rejected invoice now passes PoC existence gate after PoC upload
- Verify that PoCStoreService is correctly reused (no new storage mechanism needed)

### Test Data

| Test Case | Filename | Invoice Number | Expected Outcome |
|-----------|----------|---------------|-----------------|
| Correct filename | "INV-0042.pdf" | "INV-0042" | Stored, matched |
| Wrong filename | "WRONG.pdf" | "INV-0042" | Stored, not matched |
| Case variation | "inv-0042.pdf" | "INV-0042" | Stored, matched (case-insensitive) |
| Duplicate | "INV-0042.pdf" (twice) | "INV-0042" | Stored twice or overwritten |
| Non-PDF | "INV-0042.docx" | "INV-0042" | Rejected or stored/ignored |
| Special chars | "INV-0042-EU.pdf" | "INV-0042-EU" | Stored, matched |

---

## 9. Risks and Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|-----------|------------|
| Client uploads PoC file with wrong filename | PoC not matched, client confusion | Medium | Error message on upload indicating expected filename. Or reject upload with guidance. |
| Client uploads non-PDF files | Storage bloat, confusion | Medium | Reject non-PDF files with clear error message. |
| Duplicate uploads consume storage | Disk space accumulation | Low | Implement automatic cleanup or overwrite behaviour. |
| PoC upload endpoint is slow (large PDF files) | Poor client experience | Low | Implement upload progress indicator. Consider file size limit in future iteration. |

---

## 10. Definition of Done

- [ ] All 6 Gherkin scenarios implemented as automated tests
- [ ] Unit test coverage for PoC upload logic: minimum 80%
- [ ] Integration test for full flow: Excel upload → return Excel → PoC upload → Excel re-upload → PoC pass
- [ ] Code review completed
- [ ] No regression in existing business service tests
- [ ] Acceptance criteria reviewed and approved by product owner or case analyst stakeholder

---

**Work Item Author:** Robbie (Requirements Engineer)
**Review Status:** NOT YET APPROVED — Pending architect design review
**Priority:** Should have (deferred from MVP if necessary, per RQ-009)
**Next Step:** Archibald produces delegation plan; Gerard defines API contract; Naut implements upload endpoint
