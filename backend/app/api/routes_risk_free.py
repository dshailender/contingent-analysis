"""Risk-free rate curves and yield curve interpolation API endpoints.
"""

from typing import List, Optional
from pydantic import BaseModel
from fastapi import APIRouter, HTTPException
from ..engine.risk_free_rates import (
    US_TREASURY_TENORS,
    ECB_TENORS,
    interpolate_yield_curve,
    build_risk_free_analysis
)
from ..engine.models import RiskFreeRateAnalysis

router = APIRouter()


class InterpolateRequest(BaseModel):
    as_of_date: str
    exit_date: str
    day_count_basis: int = 1
    source_name: str = "Interpolated Curve"
    points: List[List[float]]  # [[tenor_years, rate_percent], ...]


@router.get("/risk-free-rates/curves")
def get_supported_curves():
    """Returns standard curve structures for US Treasury and ECB curves."""
    return {
        "us_treasury": [{"name": t[0], "years": t[1]} for t in US_TREASURY_TENORS],
        "ecb": [{"name": t[0], "years": t[1]} for t in ECB_TENORS]
    }


@router.post("/risk-free-rates/interpolate", response_model=RiskFreeRateAnalysis)
def interpolate_rate(req: InterpolateRequest):
    """Interpolates yield curve to maturity and computes continuous compounding rate."""
    from ..engine.date_math import year_fraction
    term = year_fraction(req.as_of_date, req.exit_date, req.day_count_basis)
    if term <= 0.0:
        raise HTTPException(status_code=400, detail="Exit date must be after as-of date.")

    points_tuples = [(p[0], p[1]) for p in req.points]
    rate_pct, meta = interpolate_yield_curve(points_tuples, term)
    
    # Format curve points
    curve_points_tuples = [(f"{p[0]:.2f}y", p[0], p[1]) for p in req.points]
    return build_risk_free_analysis(
        as_of_date=req.as_of_date,
        exit_date=req.exit_date,
        annual_effective_rate_pct=rate_pct,
        source_name=req.source_name,
        curve_points=curve_points_tuples,
        day_count_basis=req.day_count_basis
    )

