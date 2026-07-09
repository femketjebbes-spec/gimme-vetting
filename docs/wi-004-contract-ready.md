# Contract Ready Signal: WI-004

**Working Item**: WI-004 (Return Excel Generation)
**Signal Produced By**: Gerard (API-Agent)
**Timestamp**: 2026-07-08
**Versioned Contract File**: `docs/api-contract-wi-003.md` (v3.0.0)

## Contract

WI-004 does not introduce a new API contract. It extends the existing WI-003 contract by ensuring the return Excel download endpoint produces correctly formatted Excel files with per-row issue identification. The versioned contract remains `docs/api-contract-wi-003.md` (v3.0.0), which defines the response schema including `returnExcelDownloadLink`.

## Self-Certification

I certify that the WI-003 contract (v3.0.0) covers all output requirements for WI-004:
- RQ-008 (Return Excel with Missing Data) from `re-workspace/requirements-spec.md`
- The contract response at Section 6.1 includes `returnExcelDownloadLink` for downloading the return Excel
- The per-row `failingRows` array in the response supports per-row issue identification

## Completion Status

The Gerard phase for WI-004 is complete. The contract has been reviewed and is ready for Alignment Agent review.
