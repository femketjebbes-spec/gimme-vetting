# Work Items Directory

Structured work items for the Gimme Vetting Solution, organized by implementation phase.

## Directory Structure

```
work-items/
├── README.md                    # This file
├── MVP-1/                       # MVP 1 Core Work Items
│   ├── wi-001-poc-existence-verification.md
│   ├── wi-002-excel-file-upload-and-parsing.md
│   ├── wi-003-per-row-mandatory-field-validation.md
│   ├── wi-004-return-excel-generation.md
│   ├── wi-005-separate-poc-upload-endpoint.md
│   └── wi-006-project-build-orchestration.md
└── MVP-1 touchups/              # MVP 1 Touchups (convenience features)
    └── wi-007-download-template-excel.md
```

## MVP-1 Scope

MVP-1 delivers the core invoice intake and validation pipeline for the Gimme client portal. All work items in this phase are designed to be implemented together as a cohesive MVP.

### Work Items

| ID | Title | Parent Requirement | Priority | Dependencies |
|----|-------|-------------------|----------|--------------|
| WI-001 | PoC Existence Verification | RQ-001 | Must have | None |
| WI-002 | Excel File Upload and Parsing | RQ-006 | Must have | None |
| WI-003 | Per-Row Mandatory Field Validation | RQ-007 | Must have | WI-002 |
| WI-004 | Return Excel Generation | RQ-008 | Must have | WI-002, WI-003 |
| WI-005 | Separate PoC Upload Endpoint | RQ-009 | Should have | WI-001 |
| WI-006 | Project Build Orchestration | N/A (infrastructure) | High | None |

### Dependency Graph

```
WI-001 ────────────────────────────────────────┐
                                                 │
WI-002 ──► WI-003 ──► WI-004                    │
                                                 ├──► Full Pipeline
WI-005 ────────────────────────────────────────┐ │
                                                 │ │
WI-006 (Build Orchestration) ────────────────────┘ ┘
```

**Critical Path:** WI-002 → WI-003 → WI-004

**Parallel Tracks:**
- WI-001 can proceed independently
- WI-005 can proceed in parallel with WI-002 through WI-004
- WI-006 is independent and should be completed first to enable all other work

---

## MVP-1 Touchups

Touchups are convenience features that improve the user experience but are not critical to the core MVP pipeline.

### WI-007: Download Template Excel Sheet

| ID | Title | Parent Requirement | Priority | Dependencies |
|----|-------|-------------------|----------|--------------|
| WI-007 | Download Template Excel Sheet | RQ-006 | Should have | None |

**Purpose:** Provide a downloadable Excel template file with correct column headers so users can fill in their invoice data before uploading.

**File:** [wi-007-download-template-excel.md](MVP-1%20touchups/wi-007-download-template-excel.md)

## Master Reference

For the complete work item catalog including future work streams (W-001 through W-007), see [re-workspace/work-items.md](../work-items.md).

---

**Last Updated:** 2026-07-08
**Document ID:** RE-WI-README
**Version:** 1.0
