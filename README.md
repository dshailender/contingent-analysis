# Contingent Claims Analysis (OPM) Valuation Platform

A production-ready quantitative financial valuation application implementing the **Option Pricing Model (OPM)**, **Black-Scholes Call Tranches**, **Numerical Root-Finding Equity Backsolve**, **Comparative Liquidation Waterfall**, **Multi-Fund Portfolio Holdings Evaluation (MOIC)**, and **S&P Capital IQ Excel Bridge Integration**.

Designed and reconciled directly against the canonical reference application with pure Python calculation engine, typed FastAPI backend, and latest Angular standalone frontend.

---

## 🏛️ System Architecture

```
contingent-analysis/
├── backend/                             # Python 3.14+ FastAPI Valuation Engine
│   ├── app/
│   │   ├── api/                         # REST API Endpoints
│   │   │   ├── routes_calculate.py      # /api/calculate, /api/validate, /api/scenario/default
│   │   │   ├── routes_exports.py        # /api/exports/excel, /api/exports/pdf
│   │   │   ├── routes_risk_free.py      # /api/risk-free-rates/curves, /interpolate
│   │   │   ├── routes_capital_iq.py     # /api/capital-iq/workbook, /upload
│   │   │   └── routes_health.py         # /api/health
│   │   ├── engine/                      # 100% Pure Python Valuation Library
│   │   │   ├── black_scholes.py         # Analytical European call, N(d1), continuous discount
│   │   │   ├── date_math.py             # 5 Day-count conventions (US 30/360, Act/Act, etc.)
│   │   │   ├── capitalization.py        # Derivation, simple & compound dividend accruals
│   │   │   ├── breakpoints.py           # Seniority LP schedule, conversion, warrant exercise
│   │   │   ├── claims.py                # Dollar & percentage claims across breakpoint tiers
│   │   │   ├── opm.py                   # Incremental call tranches & Brent's root-finder backsolve
│   │   │   ├── waterfall.py             # Sequential liquidation & pro-rata recovery
│   │   │   ├── holdings.py              # Multi-fund position aggregation & MOIC
│   │   │   ├── risk_free_rates.py       # US Treasury / ECB yield curve interpolation
│   │   │   ├── volatility.py            # Merton asset volatility delevering & relevering
│   │   │   ├── capital_iq.py            # Capital IQ plugin template generator & parser
│   │   │   ├── exports_excel.py         # Presentation-grade multi-tab OpenPyXL workbook
│   │   │   ├── exports_pdf.py           # Landscape Playwright PDF with "HIGHLY CONFIDENTIAL" watermark
│   │   │   ├── validation.py            # Cap table consistency & input validation
│   │   │   └── pipeline.py              # End-to-end execution orchestrator
│   │   └── main.py                      # FastAPI application & static Angular mount
│   └── tests/                           # 22 Comprehensive automated test suites
├── frontend/                            # Angular 22 Standalone Application
│   ├── src/
│   │   ├── app/
│   │   │   ├── components/              # Executive UI Components
│   │   │   │   ├── header/              # Branding, navigation tabs, run/export buttons
│   │   │   │   ├── engagement-settings/ # Company, client, dates, day-count basis, currency
│   │   │   │   ├── cap-table-editor/    # 15-column spreadsheet grid with context N/A logic
│   │   │   │   ├── opm-controls/        # Calibration class, observed price, vol, rf rates
│   │   │   │   ├── client-holdings-editor/# Multi-fund portfolio editor & subtotal summaries
│   │   │   │   ├── capital-iq-modal/    # CIQ bridge template generator & file upload dropzone
│   │   │   │   └── report-exhibits/     # Executive exhibits 1.0 through 11.0
│   │   │   ├── models/                  # TypeScript data interfaces matching Pydantic
│   │   │   ├── services/                # API client & reactive signal state management
│   │   │   ├── app.ts                   # Main application root component
│   │   │   └── app.html                 # Workspace view switcher & loading overlays
│   │   └── styles.scss                  # Executive visual theme & styling system
├── golden_reference/                    # Canonical JavaScript run outputs (87 KB JSON)
└── pytest.ini                           # Pytest configuration
```

---

## 🚀 Quick Start Guide

### Prerequisites
- **Python**: 3.11+ (tested on Python 3.14.7)
- **Node.js**: 20+ (tested on Node.js v24.21.0, npm 11.19.0)

### 1. Backend Setup & Test Suite Execution
```bash
# Activate virtual environment
.\backend\.venv\Scripts\Activate.ps1

# Run all 22 test suites (reconciliation, boundary conditions, exports, API)
python -m pytest backend/tests/ -v
```

### 2. Frontend Development & Build
```bash
cd frontend

# Run unit tests
npm test -- --watch=false

# Build production bundle
npm run build
```

### 3. Running the Unified Server
When the frontend is built, FastAPI automatically serves the Angular application at the root (`/`) while exposing the REST API at (`/api`):
```bash
.\backend\.venv\Scripts\python.exe -m uvicorn app.main:app --app-dir backend --host 127.0.0.1 --port 8000
```
Open your browser to: **`http://localhost:8000`**

To run Angular in live development mode with hot reload:
```bash
cd frontend
npm start
# App running at http://localhost:4200 (proxies /api to http://localhost:8000)
```

---

## 🧮 Numerical Reconciliation Results

All calculations match canonical reference outputs within strict tolerance:

| Metric | Golden Reference | Python Engine | Reconciled Variance |
| :--- | :--- | :--- | :--- |
| **Calibration Solved Equity** | €287,252,502.92 | €287,252,502.92 | **€0.00** |
| **Series I Backsolved Value** | €2,021.9000 | €2,021.9000 | **€0.0000** |
| **Series I Valuation Per Share**| €1,992.50 | €1,992.50 | **€0.00** |
| **Series H Valuation Per Share**| €2,391.29 | €2,391.29 | **€0.00** |
| **Common Stock Per Share** | €740.15 | €740.15 | **€0.00** |
| **Common Options A Per Share** | €739.33 | €739.33 | **€0.00** |
| **Client Portfolio Fair Value**| €17,568,066.01 | €17,568,066.01 | **€0.00** |
| **Series F1 (Fund I) MOIC** | 0.90x | 0.90x | **0.00x** |
| **Series H (Fund I) MOIC** | 1.37x | 1.37x | **0.00x** |
| **Consolidated Portfolio MOIC**| 1.32x | 1.32x | **0.00x** |

---

## 📑 Core Features

1. **15-Column Capitalization Tables**:
   - Manages Preferred Stock, Common Stock, Options, and Warrants.
   - Context-sensitive N/A field disabling based on security subtype.
   - Seniority tier ranking, compounding conventions (Simple, Annual, Semi-Annual, Quarterly, Monthly), and conversion threshold calculation.
2. **Option Pricing Model & Breakpoint Generation**:
   - Analytical Black-Scholes call option pricing with continuous compounding.
   - Automated breakpoint scheduling with exact warrant handling logic.
   - High-precision root-finding equity backsolve via `scipy.optimize.brentq`.
3. **Multi-Fund Client Holdings (Exhibit 1.0)**:
   - Aggregates positions across funds/vehicles.
   - Computes individual class ownership %, fully diluted ownership %, fair market value, and MOIC.
4. **S&P Capital IQ Bridge**:
   - Generates Excel templates with live Capital IQ formulas.
   - Validates and parses uploaded refreshed workbooks.
   - Calculates Merton distance-to-default asset volatility and relevers to subject company capital structure.
5. **Server-Side Watermarked PDF Generation**:
   - Headless Playwright Chromium rendering of landscape executive exhibit report.
   - Semi-transparent diagonal **"HIGHLY CONFIDENTIAL"** watermark on every page.

