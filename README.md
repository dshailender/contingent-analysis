# Contingent Claims Analysis (OPM) Valuation Platform

A production-ready quantitative financial valuation application implementing the **Option Pricing Model (OPM)**, **Black-Scholes Call Tranches**, **Numerical Root-Finding Equity Backsolve**, **Comparative Liquidation Waterfall**, **Multi-Fund Portfolio Holdings Evaluation (MOIC)**, and **S&P Capital IQ Excel Bridge Integration**.

Implemented on **Java 25** and **Spring Boot 4.x** with full numerical parity, typed REST API, Apache POI Excel bridge, Playwright watermarked PDF generation, and an Angular 22 standalone frontend.

---

## 🏛️ System Architecture

```
contingent-analysis/
├── backend/                             # Java 25 + Spring Boot 4.x Valuation Engine (Maven)
│   ├── pom.xml                          # Dependencies: Spring Boot 4.1.1, Apache POI 5.5.0, Playwright 1.58.0, Commons Math 3.6.1, PDFBox 3.0.4
│   └── src/
│       ├── main/
│       │   ├── java/com/example/contingentanalysis/
│       │   │   ├── api/                 # Spring Web MVC REST Controllers
│       │   │   │   ├── CalculateController.java     # /api/calculate, /api/validate, /api/scenario/default, /api/basis-conventions
│       │   │   │   ├── ExportController.java        # /api/exports/excel, /api/exports/pdf
│       │   │   │   ├── RiskFreeRatesController.java # /api/risk-free-rates/curves, /interpolate
│       │   │   │   ├── CapitalIqController.java     # /api/capital-iq/workbook, /upload
│       │   │   │   ├── HealthController.java        # /api/health
│       │   │   │   ├── FrontendController.java      # Static Angular mount & SPA routing
│       │   │   │   └── ApiExceptionHandler.java     # Global REST exception advice (HTTP 400/500)
│       │   │   ├── config/              # Web MVC, CORS, and Jackson configuration
│       │   │   ├── domain/              # Pure Java Valuation Domain Layer
│       │   │   │   ├── blackscholes/    # Analytical European call, N(d1), continuous compounding
│       │   │   │   ├── breakpoints/     # Seniority LP schedule, conversion, warrant exercise
│       │   │   │   ├── capitaliq/       # Capital IQ plugin template generator & parser
│       │   │   │   ├── capitalization/  # Derivation, simple & compound dividend accruals
│       │   │   │   ├── claims/          # Dollar & percentage claims across breakpoint tiers
│       │   │   │   ├── date/            # 5 Day-count conventions (US 30/360, Act/Act, Act/360, Act/365, Euro 30/360)
│       │   │   │   ├── defaultscenario/ # Canonical pre-populated TADO valuation scenario
│       │   │   │   ├── exports/         # Apache POI Excel workbook & Playwright landscape PDF with watermark
│       │   │   │   ├── holdings/        # Multi-fund position aggregation & MOIC
│       │   │   │   ├── model/           # Strongly-typed domain models matching TypeScript contracts
│       │   │   │   ├── opm/             # Incremental call tranches & Brent's root-finder backsolve
│       │   │   │   ├── pipeline/        # End-to-end execution orchestrator
│       │   │   │   ├── riskfree/        # US Treasury & ECB yield curve interpolation
│       │   │   │   ├── validation/      # Cap table consistency & input validation
│       │   │   │   ├── volatility/      # Merton asset volatility delevering & relevering
│       │   │   │   └── waterfall/       # Sequential liquidation & comparative pro-rata distribution
│       │   │   └── ContingentAnalysisApplication.java # Spring Boot application entrypoint
│       │   └── resources/
│       │       └── application.yml      # Server port 8000, Jackson snake_case property naming
│       └── test/
│           └── java/com/example/contingentanalysis/
│               ├── ApiEndpointsTest.java         # 8 MockMvc integration tests for all REST endpoints
│               ├── BoundaryConditionsTest.java   # 10 Boundary and mathematical edge condition tests
│               ├── ExportsTest.java              # 2 Tests verifying 7-sheet Excel and watermarked PDF exports
│               ├── GoldenReconciliationTest.java # 6 Canonical numerical parity reconciliation tests
│               └── PipelineTest.java             # 1 Full end-to-end pipeline test
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
│   │   │   ├── models/                  # TypeScript data interfaces matching Java domain models
│   │   │   ├── services/                # API client & reactive signal state management
│   │   │   ├── app.ts                   # Main application root component
│   │   │   └── app.html                 # Workspace view switcher & loading overlays
│   │   └── styles.scss                  # Executive visual theme & styling system
└── golden_reference/                    # Canonical JavaScript run outputs (87 KB JSON)
```

---

## 🚀 Quick Start Guide

### Prerequisites
- **Java**: OpenJDK 25+
- **Maven**: 3.9+
- **Node.js**: 20+ (tested on Node.js v24.21.0, npm 11.19.0)

### 1. Backend Test Suite Execution
```powershell
cd backend
mvn clean test
```
All 27 test cases will execute:
- Golden reference numerical parity tests
- Boundary conditions and edge cases
- Apache POI multi-sheet Excel workbook export tests
- Playwright landscape PDF watermarking tests
- Full MockMvc REST API integration tests

### 2. Frontend Development & Build
```powershell
cd frontend

# Run unit tests
npm test -- --watch=false

# Build production bundle
npm run build
```

### 3. Unified Local Development (Backend + Frontend Hot Reload)

To run the full stack locally with a single unified command—building the frontend first, then starting the Spring Boot backend (`mvn spring-boot:run`) concurrently with the Angular dev server in hot-reload mode:

#### Unified Command (Works in both PowerShell and Bash)
From the repository root:
```bash
npm run dev
```
*(or `npm start`)*

Alternatively, as a direct one-liner without using npm scripts:
```bash
npm --prefix frontend run build; npx --yes concurrently -k -n backend,frontend -c blue.bold,green.bold "mvn -f backend/pom.xml spring-boot:run" "npm --prefix frontend start"
```

Dedicated scripts are also available:
- **PowerShell**: `.\run-local.ps1`
- **Bash**: `./run-local.sh`

#### Access Points
- **Hot-Reload Development UI**: [`http://localhost:4200`](http://localhost:4200) (auto-proxies `/api` calls to the Spring Boot backend)
- **Spring Boot Backend API**: [`http://localhost:8000/api`](http://localhost:8000/api)
- **Spring Boot Embedded UI**: [`http://localhost:8000`](http://localhost:8000) (serves compiled Angular production dist)

---

## 🧮 Numerical Reconciliation Results

All calculations match canonical golden reference outputs within strict tolerance:

| Metric | Golden Reference | Java Engine | Reconciled Variance |
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
   - High-precision root-finding equity backsolve via Apache Commons Math `BrentSolver`.
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
