"""Option Pricing Method (OPM) allocation and numerical root-finding backsolve.

Implements incremental Black-Scholes call tranche valuation and exact backsolve
matching the reference CCA model.
"""

import math
from typing import List, Dict, Optional
from scipy.optimize import root_scalar, brentq

from .models import (
    DerivedSecurity,
    BreakpointTier,
    OpmTranche,
    OpmAllocationResult
)
from .date_math import year_fraction, DateLike
from .black_scholes import black_scholes_call, to_continuous_rate
from .claims import allocate_claims_by_tier


def allocate_opm(
    rows: List[DerivedSecurity],
    breakpoints: List[BreakpointTier],
    equity_value: float,
    valuation_date: DateLike,
    exit_date: DateLike,
    rf_effective: float,
    volatility: float,
    dividend_yield: float = 0.0,
    day_count_basis: int = 1
) -> OpmAllocationResult:
    """Allocates total equity value using incremental Black-Scholes call option tranches."""
    t = max(0.0, year_fraction(valuation_date, exit_date, day_count_basis))
    rf_continuous = to_continuous_rate(rf_effective) if rf_effective > -1.0 else 0.0

    # Strikes are 0 followed by all breakpoint upper bounds
    strikes = [0.0] + [bp.end_equity for bp in breakpoints]
    calls = [
        black_scholes_call(equity_value, k, rf_continuous, volatility, t, dividend_yield)
        for k in strikes
    ]

    # Incremental call tranche values
    incs = []
    for i in range(len(calls) - 1):
        incs.append(max(0.0, calls[i] - calls[i + 1]))
    # Final call value above the last breakpoint (Thereafter slice)
    last_call = calls[-1] if calls else 0.0
    incs.append(last_call)

    # Build tranches metadata
    tranches: List[OpmTranche] = []
    for i, bp in enumerate(breakpoints):
        tranches.append(OpmTranche(
            tier=bp.tier,
            strike_low=bp.start_equity,
            strike_high=bp.end_equity,
            call_low=calls[i],
            call_high=calls[i + 1],
            incremental_call=incs[i]
        ))

    # Calculate claim allocations per tier
    tier_claims = allocate_claims_by_tier(rows, breakpoints)

    allocated: Dict[str, float] = {r.security: 0.0 for r in rows}

    for i, tr in enumerate(tier_claims):
        tot = sum(tr.dollar_claims.values())
        if tot > 0.0:
            for sec, val in tr.dollar_claims.items():
                allocated[sec] += incs[i] * (val / tot)

    # Allocate Thereafter slice pro-rata by fully diluted shares
    total_fd = sum(r.fully_diluted_shares for r in rows)
    if total_fd > 0.0:
        for r in rows:
            allocated[r.security] += last_call * (r.fully_diluted_shares / total_fd)

    # Per share values and percentages
    per_share: Dict[str, float] = {}
    pct_alloc: Dict[str, float] = {}
    total_alloc = sum(allocated.values())

    for r in rows:
        sec = r.security
        tot_val = allocated[sec]
        sh = r.shares
        per_share[sec] = (tot_val / sh) if sh > 0.0 else 0.0
        pct_alloc[sec] = (tot_val / total_alloc) if total_alloc > 0.0 else 0.0

    return OpmAllocationResult(
        equity_value=equity_value,
        term=t,
        risk_free_rate_effective=rf_effective,
        risk_free_rate_continuous=rf_continuous,
        volatility=volatility,
        dividend_yield=dividend_yield,
        tranches=tranches,
        allocated_values=allocated,
        per_share_values=per_share,
        percent_allocations=pct_alloc,
        total_allocated=total_alloc
    )


def backsolve_equity(
    rows: List[DerivedSecurity],
    breakpoints: List[BreakpointTier],
    calibration_date: DateLike,
    exit_date: DateLike,
    rf_effective: float,
    volatility: float,
    dividend_yield: float,
    calibration_security_name: str,
    target_price: float,
    day_count_basis: int = 1
) -> float:
    """Numerically backsolves for total enterprise equity value such that
    the per-share value of calibration_security_name equals target_price.
    """
    cal_row = next((r for r in rows if r.security == calibration_security_name), None)
    if not cal_row or cal_row.shares <= 0.0 or target_price <= 0.0:
        raise ValueError(f"Invalid calibration security '{calibration_security_name}' or target price {target_price}")

    sh = cal_row.shares

    def objective(s: float) -> float:
        if s <= 0.0:
            return -target_price
        alloc_res = allocate_opm(
            rows=rows,
            breakpoints=breakpoints,
            equity_value=s,
            valuation_date=calibration_date,
            exit_date=exit_date,
            rf_effective=rf_effective,
            volatility=volatility,
            dividend_yield=dividend_yield,
            day_count_basis=day_count_basis
        )
        sec_val = alloc_res.allocated_values.get(calibration_security_name, 0.0)
        return (sec_val / sh) - target_price

    # Bracket search
    lo = 1e-6
    hi = max(1e6, target_price * sh * 5.0)

    # Expand upper bound until objective(hi) >= 0
    max_expansion = 100
    for _ in range(max_expansion):
        if objective(hi) >= 0.0:
            break
        hi *= 2.0
    else:
        raise RuntimeError("Failed to bracket root for OPM backsolve (upper bound expansion exhausted)")

    # Solve using Brent's method with fallback to bisection
    try:
        sol = brentq(objective, lo, hi, xtol=1e-8, maxiter=200)
        return float(sol)
    except Exception:
        # Robust bisection fallback
        for _ in range(160):
            mid = lo + (hi - lo) / 2.0
            fm = objective(mid)
            if abs(fm) <= 1e-12:
                return mid
            if fm > 0.0:
                hi = mid
            else:
                lo = mid
            if (hi - lo) <= max(1e-7, abs(mid) * 1e-14):
                break
        return lo + (hi - lo) / 2.0

