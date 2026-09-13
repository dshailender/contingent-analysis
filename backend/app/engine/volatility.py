"""Volatility calculations, guideline public company statistics, Merton asset delevering,
and subject company relevering.
"""

import math
from typing import List, Optional
from .models import VolatilityStats
from .black_scholes import normal_cdf


def quantile(values: List[float], q: float) -> float:
    """Computes sample quantile matching reference JavaScript calculation.

    Reference formula:
    p = (n - 1) * q
    b = floor(p), d = p - b
    returns a[b] + (a[b+1] if b+1 < n else a[b]) * d
    """
    valid = sorted([v for v in values if math.isfinite(v)])
    if not valid:
        return float("nan")
    n = len(valid)
    if n == 1:
        return valid[0]

    p = (n - 1) * q
    b = int(math.floor(p))
    d = p - b
    next_val = valid[b + 1] if (b + 1 < n) else valid[b]
    # Replicates reference JS: a[b] + (a[b+1] ?? a[b]) * d
    return valid[b] + next_val * d


def standard_quantile(values: List[float], q: float) -> float:
    """Standard statistical linear quantile: a[b] + (a[b+1] - a[b]) * d."""
    valid = sorted([v for v in values if math.isfinite(v)])
    if not valid:
        return float("nan")
    n = len(valid)
    if n == 1:
        return valid[0]

    p = (n - 1) * q
    b = int(math.floor(p))
    d = p - b
    next_val = valid[b + 1] if (b + 1 < n) else valid[b]
    return valid[b] + (next_val - valid[b]) * d


def calculate_volatility_stats(values: List[float]) -> Optional[VolatilityStats]:
    """Computes summary statistics: min, q1, median, mean, q3, max."""
    valid = [v for v in values if math.isfinite(v)]
    if not valid:
        return None

    return VolatilityStats(
        min=float(min(valid)),
        q1=float(quantile(valid, 0.25)),
        median=float(quantile(valid, 0.50)),
        mean=float(sum(valid) / len(valid)),
        q3=float(quantile(valid, 0.75)),
        max=float(max(valid))
    )


def merton_asset_volatility(
    equity_vol: float,
    equity_market_cap: float,
    total_debt: float,
    risk_free_rate: float,
    term: float
) -> float:
    """Delevers observed guideline company equity volatility to Merton asset volatility.

    Formula:
    total = E + D
    d1 = (ln(total / D) + (rf + 0.5 * sigma_E^2) * t) / (sigma_E * sqrt(t))
    sigma_A = sigma_E * (E / total) / N(d1)
    """
    if not math.isfinite(equity_vol) or equity_vol <= 0.0 or equity_market_cap <= 0.0:
        return float("nan")
    if total_debt <= 0.0:
        return equity_vol

    t = max(1e-6, float(term))
    rf = float(risk_free_rate or 0.0)
    total = equity_market_cap + total_debt

    d1 = (math.log(total / total_debt) + (rf + 0.5 * equity_vol * equity_vol) * t) / (equity_vol * math.sqrt(t))
    nd1 = normal_cdf(d1)

    if nd1 > 1e-9:
        return equity_vol * (equity_market_cap / total) / nd1
    return float("nan")


def relever_equity_volatility(
    asset_vol: float,
    subject_equity_value: float,
    subject_debt: float,
    subject_preferred: float
) -> float:
    """Relevers asset volatility to subject company capital structure.

    Formula:
    D/E = (Debt + Preferred) / Equity
    sigma_E = sigma_A * (1 + D/E)
    """
    if not math.isfinite(asset_vol) or asset_vol <= 0.0 or subject_equity_value <= 0.0:
        return float("nan")

    de = (subject_debt + subject_preferred) / subject_equity_value
    return asset_vol * (1.0 + de)

