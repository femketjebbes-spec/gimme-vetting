# Naut Decision Log

Records implementation decisions made by Naut during coding sessions. Records test-to-spec mappings produced during Testing Mode. Format per entry:

```
[YYYY-MM-DD] [Session N] DECISION: <statement>
Assumptions: <statement>
Rationale: <user-provided or derived from delegation plan>

[YYYY-MM-DD] [Session N] TEST-SPEC: <test file path> maps to <specification or delegation subtask reference>
Purpose: <what behaviour the test validates>
Derived from: <delegation plan subtask ID or Robbie requirement ID>
```

Existing entries:

[2026-07-03] [Session 1] DECISION: Naut is a Java-specific implementation agent.
Assumptions: The project uses Java for backend development.
Rationale: User requirement for Java backend.

[2026-07-03] [Session 1] DECISION: Maven is the exclusive build tool for Naut.
Assumptions: Maven is available in the project environment.
Rationale: User requirement for Maven build tool.

[2026-07-03] [Session 1] DECISION: JUnit 5 is the exclusive testing framework for Naut.
Assumptions: JUnit 5 is compatible with the Java version used in the project.
Rationale: User requirement for JUnit 5 testing framework.

[2026-07-03] [Session 1] DECISION: Naut is strictly confined to backend code modifications.
Assumptions: The project has a clear separation between backend and frontend directories.
Rationale: User explicit requirement to leave frontend code untouched.

[2026-07-03] [Session 1] DECISION: Naut follows the architectural pattern defined by Archibald.
Assumptions: Archibald will document the architectural pattern in the architecture decisions file before Naut activates.
Rationale: User requirement for Archibald to decide the pattern.

[2026-07-03] [Session 1] DECISION: Naut activates only after Robbie and Archibald have completed their work.
Assumptions: Robbie produces requirements documentation and Archibald produces a delegation plan with architecture decisions.
Rationale: User specified sequential pipeline: Robbie first, then Archibald, then Naut.

[2026-07-07] [Session 1] DECISION: WI-002 Excel upload endpoint uses Apache POI 5.2.5 for .xlsx and .csv parsing.
Assumptions: Apache POI 5.2.5 is the latest patched version with XXE protections.
Rationale: Architectural decision D-029 mandates Apache POI with latest patched version.

[2026-07-07] [Session 1] DECISION: Column position mapping applies when no header row is present.
Assumptions: Files without headers always use the same column order: invoiceNumber, debtorName, address, phoneNumber, bankAccountNumber.
Rationale: Architectural decision D-025 confirms position mapping behavior.

[2026-07-07] [Session 1] DECISION: Return Excel generation uses XSSFWorkbook for small failing-row sets.
Assumptions: Failing rows will be small enough to fit in memory without SXSSF streaming.
Rationale: MVP scope does not require streaming for return Excel generation.

[2026-07-07] [Session 1] TEST-SPEC: ExcelParsingServiceTest.java maps to delegation subtask 2 (backend parsing logic)
Purpose: Validates XLSX with header, XLSX without header, CSV parsing, column name validation, empty rows, partial rows, MIME type validation, filename validation, return Excel generation
Derived from: docs/wi-002-delegation-parallel.md subtask 2, docs/api-contract-wi-002.md section 4

[2026-07-07] [Session 1] TEST-SPEC: ExcelIntakeControllerTest.java maps to delegation subtask 2 (controller endpoint)
Purpose: Validates full upload flow, MIME type rejection, column name mismatch rejection, path traversal rejection, return Excel download
Derived from: docs/wi-002-delegation-parallel.md subtask 2, docs/api-contract-wi-002.md sections 2, 3, 5

[2026-07-07] [Session 2] DECISION: Makefile placed at project root with all eight targets as specified in WI-006 delegation plan.
Assumptions: GNU Make is installed on the build machine. Maven 3.x and Node.js 18+ are installed.
Rationale: Delegation plan (docs/wi-006-delegation.md) mandates GNU Make with POSIX compliance and GNU Make extensions.

[2026-07-07] [Session 2] DECISION: Backend build uses `mvn clean package -DskipTests` in 5-backend/ directory.
Assumptions: Multi-module Maven reactor at 5-backend/pom.xml correctly orders modules.
Rationale: Delegation plan specifies `mvn clean package -DskipTests` for backend build target.

[2026-07-07] [Session 2] DECISION: Frontend build uses `npm run build` in 4-frontend/ directory.
Assumptions: Vite build configuration exists in 4-frontend/vite.config.js. Node dependencies are installed.
Rationale: Delegation plan specifies `npm run build` for frontend build target.

[2026-07-07] [Session 2] DECISION: check-tools target verifies mvn, node, npm availability before build targets execute.
Assumptions: Standard POSIX `command -v` is available for tool detection.
Rationale: NFR-002 from WI-006 requires clear error messages for missing tools.

[2026-07-08] [Session 1] DECISION: `ExcelParsingService.generateReturnExcel()` uses 4-param overload instead of changing existing 2-param signature.
Assumptions: All 3 existing test callers use only the 2-param method and check only file existence.
Rationale: Delegation plan mandates backward compatibility. Changing the 2-param signature breaks compilation.

[2026-07-08] [Session 1] DECISION: CSV output uses `writer.write()` + `writer.newLine()` as separate statements.
Assumptions: `BufferedWriter.append()` returns `Writer`, not `BufferedWriter`, so `.newLine()` cannot be chained.
Rationale: Java standard library behavior. Chaining causes compilation error.

[2026-07-08] [Session 1] DECISION: Issue format uses `"MISSING_FIELDS: "` (with space) instead of `"MISSING_FIELDS:"` (without space).
Assumptions: Existing tests check file existence only, not issue content format.
Rationale: Delegation plan spec requires space after colon for consistency.

[2026-07-08] [Session 1] TEST-SPEC: `ExcelParsingServiceReturnExcelTest.java` maps to WI-004 Subtask 5 (all 12 tests).
Purpose: Validates return Excel generation: row count, column data, issue format, null handling, XLSX format, CSV format, CSV escaping (commas, double quotes), backward compatibility.
Derived from: WI-004 delegation plan, RQ-008, D-012, D-013, D-028, D-029

[2026-07-08] [Session 1] TEST-SPEC: `ExcelIntakeControllerTest.java` integration test maps to WI-004 Subtask 5 (Test 10).
Purpose: End-to-end upload -> download flow verification.
Derived from: WI-004 delegation plan Subtask 5

[2026-07-08] [Session 2] DECISION: Invoice number extracted from uploaded filename by stripping `.pdf` extension (case-insensitive) and lowercasing the result.
Assumptions: The frontend sends the file with the invoice number encoded in the filename.
Rationale: Per D-001 case-insensitive matching and the delegation plan. Consistent with `hasMatchingPoC()` algorithm which also lowercases during comparison.

[2026-07-08] [Session 2] DECISION: `SecurityException` thrown from `store()` for path traversal, caught at controller level and mapped to 400 response.
Assumptions: The SAFE_PATTERN regex is the same as used in `hasMatchingPoC()`.
Rationale: Delegation plan specifies catching `SecurityException` from filename validation.

[2026-07-08] [Session 2] DECISION: `RuntimeException` wraps `IOException` from `Files.copy()` in `store()`.
Assumptions: `IOException` during file storage should propagate as a 500 response.
Rationale: Clean separation between validation errors (400) and storage errors (500). Controller catches all exceptions and maps to appropriate HTTP status.

[2026-07-08] [Session 2] TEST-SPEC: `PoCStoreServiceTest.java` store() unit tests map to delegation plan Subtask 1 (store() implementation).
Purpose: Validates file storage, overwrite behaviour, path traversal rejection, directory creation.
Derived from: WI-005 delegation plan Subtask 1, D-001, D-003, D-016

[2026-07-08] [Session 2] TEST-SPEC: `PoCUploadControllerTest.java` integration tests map to delegation plan Subtask 1 (controller implementation).
Purpose: Validates POST /api/v1/poc-upload endpoint: successful PDF upload (200), non-PDF rejection (400), path traversal rejection (400), duplicate upload (200), filename without extension (200).
Derived from: WI-005 delegation plan Subtask 1, docs/api-contract-wi-005.md

[2026-07-08] [Session 3] DECISION: `ALLOWED_COLUMN_NAMES` visibility changed from `private` to `public static final`.
Assumptions: Test class resides in `com.gimmevettingsolution.intake` while the constant is in `com.gimmevettingsolution.intake.service`. Cross-package access requires public visibility.
Rationale: API contract mandates template headers reference `ALLOWED_COLUMN_NAMES` constants directly.

[2026-07-08] [Session 3] DECISION: `TEMPLATE_COLUMN_HEADERS` ordered list used for template generation instead of `ALLOWED_COLUMN_NAMES` Set.
Assumptions: `Set.of()` has no ordering guarantee; template headers must appear in exact contract order.
Rationale: Contract specifies header at column 0 = "invoice number", column 1 = "debtor name", etc. `Set.toArray()` produces unpredictable order.

[2026-07-08] [Session 3] TEST-SPEC: `TemplateDownloadServiceTest.java` maps to WI-007 Subtask 2 (service template generation).
Purpose: Validates 10 tests: non-null bytes, PK header, single sheet, 5 columns, header values in ALLOWED_COLUMN_NAMES, empty data row, no example data, file size under 100KB, correct header order, no extra rows.
Derived from: WI-007 delegation plan Subtask 2, docs/api-contract-wi-007.md

[2026-07-08] [Session 3] TEST-SPEC: `ExcelIntakeControllerTest.java` template tests map to WI-007 Subtask 2 (controller endpoint).
Purpose: Validates 7 tests: 200 status, XLSX content type, Content-Disposition header, non-empty bytes, PK header format, file size under 100KB.
Derived from: WI-007 delegation plan Subtask 2, docs/api-contract-wi-007.md
<<<<<<< HEAD

[2026-07-08] [Session 4] DECISION: Added @Autowired to FileBackedPoCStoreService Spring constructor to resolve Spring bean instantiation failure.
Assumptions: The class has two constructors — a public Spring constructor and a package-private testing constructor. Spring cannot unambiguously resolve which constructor to use without explicit annotation.
Rationale: User-reported bug: "Failed to instantiate FileBackedPoCStoreService: No default constructor found". Standard Spring fix for multi-constructor beans.
=======
>>>>>>> 4a4153c (wi-007 af)
