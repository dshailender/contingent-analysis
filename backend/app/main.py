"""FastAPI application entrypoint for the Contingent Claims Analysis valuation application.
"""

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from .api.routes_health import router as health_router
from .api.routes_calculate import router as calculate_router
from .api.routes_risk_free import router as risk_free_router
from .api.routes_capital_iq import router as capital_iq_router
from .api.routes_exports import router as exports_router

app = FastAPI(
    title="Contingent Claims Analysis Valuation Engine",
    description="Production-grade Contingent Claims Analysis (OPM) valuation engine with Black-Scholes tranches, backsolve, Capital IQ integration, and watermarked PDF exhibits.",
    version="1.0.0"
)

# Enable CORS for Angular frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:4200",
        "http://127.0.0.1:4200",
        "http://localhost:8000",
        "http://127.0.0.1:8000",
        "*"
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

from pathlib import Path
from fastapi.staticfiles import StaticFiles

# Register routers under /api
app.include_router(health_router, prefix="/api", tags=["Health"])
app.include_router(calculate_router, prefix="/api", tags=["Valuation"])
app.include_router(risk_free_router, prefix="/api", tags=["Risk-Free Rates"])
app.include_router(capital_iq_router, prefix="/api", tags=["Capital IQ Bridge"])
app.include_router(exports_router, prefix="/api", tags=["Exports"])

frontend_dist = Path(__file__).resolve().parent.parent.parent / "frontend" / "dist" / "frontend" / "browser"
if frontend_dist.exists():
    app.mount("/", StaticFiles(directory=str(frontend_dist), html=True), name="frontend")
else:
    @app.get("/")
    def root():
        return {
            "app": "Contingent Claims Analysis Valuation Engine",
            "version": "1.0.0",
            "docs": "/docs"
        }

