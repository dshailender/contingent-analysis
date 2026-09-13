#!/usr/bin/env bash
# Contingent Claims Analysis Valuation Platform - Local Dev Runner (Bash)
# 1. Builds the frontend
# 2. Runs Spring Boot backend (port 8000) and Angular hot-reload dev server (port 4200) concurrently

set -e

echo "=========================================================="
echo " Contingent Claims Analysis (OPM) Platform - Dev Launcher "
echo "=========================================================="

# Step 1: Build frontend
echo "[1/2] Building frontend production bundle..."
npm --prefix frontend run build

# Step 2: Run backend and frontend concurrently
echo "[2/2] Launching Spring Boot backend and Angular hot-reload dev server..."
echo "  -> Backend API:  http://localhost:8000/api"
echo "  -> Dev UI:       http://localhost:4200 (hot reload, proxies /api -> 8000)"
echo "  -> Unified UI:   http://localhost:8000"
echo "Press Ctrl+C to stop both services."

npx --yes concurrently -k -n "BACKEND,FRONTEND" -c "blue.bold,green.bold" \
    "mvn -f backend/pom.xml spring-boot:run" \
    "npm --prefix frontend start"

