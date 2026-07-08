# Decision Log

[2026-07-07] [Session 1] DECISION: Import path for ExcelUpload component uses relative path from src/main.jsx
Rationale: From `src/main.jsx`, the path `./frontend/components/ExcelUpload` correctly resolves to `src/frontend/components/ExcelUpload.jsx`. The initial path `../frontend/components/ExcelUpload` was incorrect because `..` from `src/` goes to the project root, not `src/frontend/`.
