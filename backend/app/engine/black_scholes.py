"""Analytical Black-Scholes European call option pricing and statistical distributions.
"""

import math
from typing import Union


def normal_cdf(x: float) -> float:
    """Standard normal cumulative distribution function."""
    if math.isnan(x):
        return float("nan")
    if math.isinf(x):
        return 1.0 if x > 0 else 0.0
    return 0.5 * math.erfc(-x / math.sqrt(2.0))


def to_continuous_rate(annual_effective_rate: float) -> float:
    """Convert annual effective rate to continuously compounded rate.

    Formula: r_c = ln(1 + r_eff)
    """
    if annual_effective_rate <= -1.0:
        raise ValueError("Annual effective rate cannot be <= -100%")
    return math.log(1.0 + annual_effective_rate)


def from_continuous_rate(continuous_rate: float) -> float:
    """Convert continuously compounded rate to annual effective rate.

    Formula: r_eff = exp(r_c) - 1
    """
    return math.exp(continuous_rate) - 1.0


def black_scholes_call(
    s: float,
    k: float,
    r: float,
    vol: float,
    t: float,
    q: float = 0.0
) -> float:
    """European call option price using analytical Black-Scholes formula.

    Args:
        s: Underlying asset / equity value ($).
        k: Strike price / breakpoint ($).
        r: Continuously compounded risk-free rate.
        vol: Annualized volatility (decimal, e.g. 0.35).
        t: Time to expiration / liquidity in years.
        q: Continuously compounded dividend yield (decimal).

    Returns:
        Call option price ($).
    """
    if s <= 0.0 or vol <= 0.0 or t <= 0.0:
        intrinsic = s * math.exp(-q * t) - k * math.exp(-r * t)
        return max(0.0, intrinsic)

    if k <= 0.0:
        return s * math.exp(-q * t)

    v_sqrt = vol * math.sqrt(t)
    d1 = (math.log(s / k) + (r - q + 0.5 * vol * vol) * t) / v_sqrt
    d2 = d1 - v_sqrt

    call_price = (
        s * math.exp(-q * t) * normal_cdf(d1)
        - k * math.exp(-r * t) * normal_cdf(d2)
    )
    return max(0.0, call_price)

