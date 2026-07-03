# Naut Session History

Summary of each session conducted by Naut.

## Session 1 - 2026-07-03

**Explored:** Naut's role boundaries, inputs, outputs, and position in the development pipeline. Established that Naut activates after Robbie produces requirements and Archibald produces architecture decisions and delegation plans. Clarified the strict backend-only constraint.

**Decided:** The agent uses Java with Maven as the build tool and JUnit 5 as the testing framework. The agent follows whatever architectural pattern Archibald defines. Frontend code is strictly off-limits. The agent's trigger is Archibald's delegation plan assigning backend subtasks.

**Remains Open:** Specific Java version, backend directory structure, Spring Boot version, and database technology. These will be defined by Robbie and Archibald before the agent activates on real tasks.

**Assumptions:** The project has a clear separation between backend and frontend code. Archibald will document the architectural pattern before the agent begins implementation work.
