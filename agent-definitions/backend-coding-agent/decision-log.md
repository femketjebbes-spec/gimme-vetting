# Naut Decision Log

Records implementation decisions made by Naut during coding sessions.

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
