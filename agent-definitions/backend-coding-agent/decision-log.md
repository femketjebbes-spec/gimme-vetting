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
