"""Risk-free rate curves, linear yield curve interpolation, and continuous compounding.
"""

from typing import List, Tuple, Optional
from .models import YieldCurvePoint, RiskFreeRateAnalysis
from .black_scholes import to_continuous_rate
from .date_math import year_fraction, DateLike

# Standard curve tenors
US_TREASURY_TENORS = [
    ("1 Month", 1.0 / 12.0),
    ("2 Month", 2.0 / 12.0),
    ("3 Month", 0.25),
    ("4 Month", 1.0 / 3.0),
    ("6 Month", 0.5),
    ("1 Year", 1.0),
    ("2 Year", 2.0),
    ("3 Year", 3.0),
    ("5 Year", 5.0),
    ("7 Year", 7.0),
    ("10 Year", 10.0),
    ("20 Year", 20.0),
    ("30 Year", 30.0),
]

ECB_TENORS = [
    ("3 Month", 0.25),
    ("6 Month", 0.5),
    ("9 Month", 0.75),
    ("1 Year", 1.0),
    ("2 Year", 2.0),
    ("3 Year", 3.0),
    ("4 Year", 4.0),
    ("5 Year", 5.0),
    ("7 Year", 7.0),
    ("10 Year", 10.0),
    ("15 Year", 15.0),
    ("20 Year", 20.0),
    ("30 Year", 30.0),
]


def interpolate_yield_curve(
    points: List[Tuple[float, float]],
    target_tenor: float
) -> Tuple[float, str]:
    """Linearly interpolates yield between adjacent curve maturities.

    Args:
        points: List of (tenor_years, rate_percent) tuples.
        target_tenor: Target horizon in years.

    Returns:
        (interpolated_rate_percent, interpolation_metadata_str)
    """
    valid = sorted([(t, r) for t, r in points if t > 0.0 and float("-inf") < r < float("inf")], key=lambda x: x[0])
    if not valid:
        return 0.0, "Empty or invalid curve"

    if target_tenor <= valid[0][0]:
        return valid[0][1], f"Clamped to shortest tenor ({valid[0][0]:.2f}y: {valid[0][1]:.2f}%)"

    if target_tenor >= valid[-1][0]:
        return valid[-1][1], f"Clamped to longest tenor ({valid[-1][0]:.2f}y: {valid[-1][1]:.2f}%)"

    for i in range(len(valid) - 1):
        t1, r1 = valid[i]
        t2, r2 = valid[i + 1]
        if t1 <= target_tenor <= t2:
            frac = (target_tenor - t1) / (t2 - t1)
            interp_rate = r1 + frac * (r2 - r1)
            meta = f"Interpolated between {t1:.2f}y ({r1:.2f}%) and {t2:.2f}y ({r2:.2f}%)"
            return interp_rate, meta

    return valid[-1][1], "Fallback"


def build_risk_free_analysis(
    as_of_date: DateLike,
    exit_date: DateLike,
    annual_effective_rate_pct: float,
    source_name: str = "Manual / User Specified",
    curve_points: Optional[List[Tuple[str, float, float]]] = None,
    day_count_basis: int = 1
) -> RiskFreeRateAnalysis:
    """Builds a structured RiskFreeRateAnalysis exhibit model."""
    term = max(0.0, year_fraction(as_of_date, exit_date, day_count_basis))
    r_eff = annual_effective_rate_pct / 100.0
    r_cont = to_continuous_rate(r_eff) if r_eff > -1.0 else 0.0

    points_model: List[YieldCurvePoint] = []
    if curve_points:
        points_model = [
            YieldCurvePoint(tenor_name=p[0], tenor_years=p[1], rate_percent=p[2])
            for p in curve_points
        ]

    return RiskFreeRateAnalysis(
        source_name=source_name,
        as_of_date=str(as_of_date),
        term_years=term,
        interpolated_annual_effective_rate=annual_effective_rate_pct,
        continuous_rate=r_cont * 100.0,
        curve_points=points_model,
        interpolation_metadata=f"Term to liquidity: {term:.2f} years. Continuous rate = ln(1 + {annual_effective_rate_pct:.2f}%) = {r_cont * 100.0:.4f}%."
    )

