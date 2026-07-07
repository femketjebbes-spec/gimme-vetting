# Delegation Plan: Wi-006 Frontend Fix — Vite Entry Point Creation

## Architecture Constraints

- Frontend project is at `4-frontend/`
- Vite requires `index.html` at the project root (`4-frontend/index.html`)
- Vite requires a JavaScript entry file (convention: `src/main.jsx` or `src/index.jsx`)
- The React application renders the `ExcelUpload` component from `src/frontend/components/ExcelUpload.jsx`
- React 18 is the dependency
- No business logic changes — only structural entry point files

## Subtasks

### Subtask 1: Create Vite Entry Files
- **Assigned Agent**: Femke (Frontend Agent)
- **Input Artefact**: `docs/wi-006-frontend-fix-delegation.md` (this delegation plan)
- **Output Artefact**: 
  - `4-frontend/index.html` — Vite HTML entry point
  - `4-frontend/src/main.jsx` — React entry point that renders the app
- **Constraints**: 
  - `index.html` must be at `4-frontend/index.html` (project root, not inside `src/`)
  - `index.html` must include a `<script type="module" src="/src/main.jsx"></script>` tag
  - `src/main.jsx` must import React, ReactDOM, and render a root element
  - No additional dependencies required
  - The app should render the `ExcelUpload` component
- **Security Considerations**: None. This is structural setup only.

## Testing Steps for Femke

After creating the files, execute from `4-frontend/`:
1. `npm run build` — must succeed without errors
2. Verify `dist/` directory is created with production assets

## Pre-existing Context

The frontend already has:
- `package.json` with React 18 and Vite 5 dependencies
- `vite.config.js` with React plugin configured
- `src/frontend/components/ExcelUpload.jsx` — the main upload component
- Jest configuration for tests
- Missing: `index.html` and `src/main.jsx`

## Completion Criteria

The subtask is complete when:
1. `4-frontend/index.html` exists
2. `4-frontend/src/main.jsx` exists
3. `npm run build` succeeds from `4-frontend/`
