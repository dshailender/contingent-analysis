# Contingent Claims Analysis Valuation Platform

## Project map

- `backend/app/engine/` is the pure Python valuation domain layer. Keep financial calculations, models, validation, and orchestration here rather than in route handlers.
- `backend/app/api/` contains thin FastAPI adapters under `/api`; preserve typed request/response models and translate expected `ValueError` failures to HTTP 400 responses.
- `backend/tests/` covers API behavior, boundary conditions, exports, pipeline behavior, and golden-reference reconciliation.
- `frontend/src/app/` is an Angular 22 standalone application. Components own presentation and user interaction; `ValuationStateService` owns shared request/response state and API-driven actions.
- `frontend/src/app/models/valuation.models.ts` mirrors the backend Pydantic contracts. Update both sides when an API contract changes.
- `golden_reference/` contains canonical calculation outputs. Treat numerical changes as intentional only when the model behavior and reconciliation evidence support them.

## Documentation

- Start with [README.md](README.md) for architecture, setup, supported features, and expected reconciliation metrics.
- Use [walkthrough.md](walkthrough.md) for the established frontend visual scale, report geometry, print rules, and prior verification results.
- Do not duplicate those documents in code comments or agent guidance; update them when user-facing setup or architecture changes.

## Commands

Run from the repository root unless noted:

```powershell
python -m pytest backend/tests/ -v
cd frontend
npm test -- --watch=false
npm run build
```

For the integrated app, build the frontend first, then run:

```powershell
.\backend\.venv\Scripts\python.exe -m uvicorn app.main:app --app-dir backend --host 127.0.0.1 --port 8000
```

Angular development runs from `frontend` with `npm start` and proxies `/api` to the backend according to `frontend/proxy.conf.json`.

## Implementation rules

- Preserve the two-date model: calibration inputs/backsolve and valuation inputs/allocation are separate structures and may not have identical securities.
- Preserve existing day-count, compounding, continuous-rate, breakpoint, Black-Scholes, root-finding, waterfall, holdings, and export semantics. Add focused tests for any change to these paths.
- Prefer the existing Pydantic models, engine functions, Angular signals, standalone components, and scoped SCSS patterns over new abstractions.
- Keep route handlers thin and keep UI API calls/state transitions in the existing services. Do not put valuation formulas in Angular templates or components.
- When changing a request or response field, update the Python model, TypeScript model, API usage, and affected tests together.
- Excel and PDF exports are behavior surfaces, not incidental formatting. Run the export tests when changing them; PDF tests may require the installed Playwright browser.
- Keep the dense financial tables and print/report layout stable. Consult `walkthrough.md` before changing typography, table geometry, exhibit headers, or print styles.
- Avoid changing `golden_reference/` to make tests pass. Investigate the calculation or test assumption first.

## Change validation

- Backend changes: run the narrowest relevant test file first, then `python -m pytest backend/tests/ -v` when practical.
- Frontend changes: run `npm test -- --watch=false` and `npm run build` from `frontend`.
- API contract changes: exercise the relevant FastAPI endpoint tests and confirm the Angular models/services still compile.
- Report/export changes: verify workbook sheet names and PDF page/watermark expectations, not only HTTP status codes.