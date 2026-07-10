# API-Ready Signal

**Produced By**: Femke (Frontend Agent)
**Timestamp**: 2026-07-09 14:21
**API Requirements Document**: `docs/api-requirements.md`
**Endpoints Defined**: 3
**Status**: Complete

## Endpoints

| HTTP Method | Path | Description | Auth Required |
|-------------|------|-------------|---------------|
| GET | /api/v1/analyst/invoices | Paginated invoice list with sourceFileId and sourceFilename fields | no |
| GET | /api/v1/analyst/invoices/{id} | Single invoice detail with sourceFileId and sourceFilename fields | no |
| GET | /api/v1/analyst/invoices/{id}/source-file | Source Excel file download (raw bytes) | no |
