# Contingent Claims Analysis Valuation Platform - Local Dev Runner (PowerShell)
# 1. Builds the frontend
# 2. Runs Spring Boot backend (port 8000) and Angular hot-reload dev server (port 4200) concurrently

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Contingent Claims Analysis (OPM) Platform - Dev Launcher " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Step 1: Build frontend
Write-Host "[1/2] Building frontend production bundle..." -ForegroundColor Yellow
npm --prefix frontend run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "Frontend build failed. Exiting." -ForegroundColor Red
    exit $LASTEXITCODE
}

# Step 2: Run backend and frontend concurrently
Write-Host "[2/2] Launching Spring Boot backend and Angular hot-reload dev server..." -ForegroundColor Green
Write-Host "  -> Backend API:  http://localhost:8000/api" -ForegroundColor Gray
Write-Host "  -> Dev UI:       http://localhost:4200 (hot reload, proxies /api -> 8000)" -ForegroundColor Gray
Write-Host "  -> Unified UI:   http://localhost:8000" -ForegroundColor Gray
Write-Host "Press Ctrl+C to stop both services." -ForegroundColor Magenta

npx --yes concurrently -k -n "BACKEND,FRONTEND" -c "blue.bold,green.bold" `
    "mvn -f backend/pom.xml spring-boot:run" `
    "npm --prefix frontend start"

