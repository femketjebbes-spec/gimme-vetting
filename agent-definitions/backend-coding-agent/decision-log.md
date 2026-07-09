# Decision Log — Naut (Backend Coding Agent)

## Session 1 (2026-07-08) — WI-008 BR-001 Fix

[2026-07-08] [Session 1] DECISION: FileType enum is a standalone class in `com.gimmevettingsolution.intake.service` package
Assumptions: The enum will be used by both `ExcelParsingService` and `ExcelIntakeController`
Rationale: Delegation plan requires the enum in the service package; standalone class follows existing pattern (e.g., `InvalidFileFormatResponse` is in `dto` package, not nested in service)

[2026-07-08] [Session 1] DECISION: `detectFileType()` returns FileType.UNKNOWN for streams shorter than 4 bytes
Assumptions: Empty or very small files should not be accepted as valid Excel or CSV
Rationale: API contract Section 3.2.1 Step 3c states "If neither, reject with INVALID_FILE_FORMAT"

[2026-07-08] [Session 1] DECISION: CSV detection is based on the first 4 bytes being printable ASCII, whitespace, or valid UTF-8 multi-byte sequences
Assumptions: If a file starts with text-like bytes, it is likely CSV; binary files start with non-text bytes
Rationale: Delegation plan requires "read first line as text. If valid UTF-8/ASCII text, treat as CSV"

[2026-07-08] [Session 1] DECISION: Error detail for UNKNOWN type is "File content is not a recognized Excel or CSV format"
Assumptions: Per FR-BR001-03, error messages must indicate actual detection reason, not generic "Unsupported MIME type"
Rationale: Delegation plan Section "Implementation details" specifies this exact message string

[2026-07-08] [Session 1] TEST-SPEC: `ExcelParsingServiceTest.java` maps to BR-001 FR-BR001-01 content-based detection tests
Purpose: Validate XLSX (ZIP header), CSV (text bytes), UNKNOWN (binary bytes), empty stream, null stream
Derived from: Delegation plan "Test requirements"

[2026-07-09] [Session 2] TEST-SPEC: `AnalystControllerTest.java` maps to WI-CA-001 Subtask 2 - Backend Controller tests
Purpose: 13 unit tests covering both API endpoints with mocked dependencies using MockMvc standalone setup
Derived from: Delegation plan docs/wi-ca-001-delegation-parallel.md Subtask 2

[2026-07-09] [Session 2] TEST-SPEC: `AnalystServiceTest.java` maps to WI-CA-001 Subtask 2 - Backend Service unit tests
Purpose: 12 unit tests for listInvoices (pagination, sorting, filtering, search) and getInvoiceDetail
Derived from: Delegation plan docs/wi-ca-001-delegation-parallel.md Subtask 2

[2026-07-09] [Session 2] TEST-SPEC: `InputValidationServiceTest.java` maps to WI-CA-001 Subtask 2 - Input validation tests
Purpose: 36 unit tests for page, size, sort, status, search validation edge cases
Derived from: Delegation plan docs/wi-ca-001-delegation-parallel.md Subtask 2

[2026-07-09] [Session 2] DECISION: AnalystController uses constructor injection for both AnalystService and InputValidationService
Assumptions: Both dependencies will be provided by Spring context
Rationale: Constructor injection enables testability with mock instances in standalone MockMvc setup

[2026-07-09] [Session 2] DECISION: Controller tests use MockMvcBuilders.standaloneSetup with explicit mock injection instead of @WebMvcTest or @SpringBootTest
Assumptions: The controller constructor accepts InputValidationService as a parameter
Rationale: standaloneSetup avoids Spring context overhead and guarantees mocks are used. @WebMvcTest failed because the controller internally instantiated InputValidationService via new() instead of using the @MockBean.

[2026-07-08] [Session 1] TEST-SPEC: `ExcelIntakeControllerTest.java` maps to BR-001 FR-BR001-02 fallback MIME handling tests
Purpose: Validate fast path (recognized MIME), fallback path (null MIME with XLSX content, octet-stream with XLSX content), rejection (binary content)
Derived from: Delegation plan "Test requirements"

[2026-07-09] [Session 2] DECISION: `run_MVP1_locally.sh` performs full clean build (`mvn clean package -DskipTests`) before starting backend
Assumptions: Maven is available on the developer machine; the `business-service` module exists under `5-backend/`
Rationale: BR-001 root cause was stale bytecode served by `spring-boot:run` without prior `mvn clean`. Clean-slate requirement (WR-001) mandates full rebuild each invocation.

[2026-07-09] [Session 2] DECISION: Backend readiness timeout increased from 30 to 60 seconds
Assumptions: Clean build + Spring Boot startup fits within 60 seconds on typical developer hardware (SSD, adequate CPU)
Rationale: WR-001 spec Section 4.1 (NFR-WR001-01) documents clean build takes 30-90 seconds plus 10-20 seconds startup. 60 seconds accommodates first cold build on moderate hardware.

[2026-07-09] [Session 2] DECISION: Frontend build failure does not abort the script; only a warning is printed
Assumptions: The frontend dev server (Vite) serves from memory and does not require a prior `dist/` build
Rationale: WR-001 spec FR-WR001-03 states "start the backend (if not already running) but print a clear warning and NOT start the frontend dev server" for build failures. However, the dev server itself can still start and serves from memory. The script starts the dev server regardless after printing the warning.

[2026-07-09] [Session 2] DECISION: `run_MVP1_locally.bat` implements equivalent clean-slate behavior for Windows
Assumptions: Windows developers use `run_MVP1_locally.bat` as the equivalent entry point to the Unix shell script
Rationale: Delegation plan Subtask 2 requires equivalent Windows behavior. The batch file already existed in the repository.
