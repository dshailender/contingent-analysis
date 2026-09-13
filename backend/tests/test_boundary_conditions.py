"""Boundary, edge case, and stress tests for the valuation engine.
"""

import pytest
from app.engine.models import SecurityInput, DerivedSecurity, BreakpointTier
from app.engine.capitalization import derive_capitalization, parse_participation_cap
from app.engine.breakpoints import generate_breakpoints
from app.engine.claims import allocate_claims_by_tier
from app.engine.opm import allocate_opm, backsolve_equity
from app.engine.waterfall import allocate_waterfall
from app.engine.black_scholes import black_scholes_call, to_continuous_rate
from app.engine.date_math import year_fraction


def test_conversion_price_zero():
    """Non-converting preferred: conversion price = 0 -> ratio = 0, FD = 0."""
    sec = SecurityInput(
        security="Series NonConvert",
        security_subtype="Preferred Stock",
        shares=1000,
        original_issue_price=10.0,
        conversion_price=0.0,
        liquidation_multiplier=1.0,
        participation="No",
        seniority=1
    )
    derived = derive_capitalization([sec], "2027-01-01")
    assert derived[0].conversion_ratio == 0.0
    assert derived[0].fully_diluted_shares == 0.0
    assert derived[0].total_liquidation_preference == 10000.0


def test_pari_passu_seniority_ties():
    """Two securities sharing the same seniority rank receive pro-rata liquidation claims."""
    s1 = SecurityInput(
        security="Series A-1",
        security_subtype="Preferred Stock",
        shares=1000,
        original_issue_price=10.0,
        conversion_price=10.0,
        seniority=1
    )
    s2 = SecurityInput(
        security="Series A-2",
        security_subtype="Preferred Stock",
        shares=2000,
        original_issue_price=10.0,
        conversion_price=10.0,
        seniority=1
    )
    derived = derive_capitalization([s1, s2], "2027-01-01")
    bps = generate_breakpoints(derived)

    # Tier 1 should fund both Series A-1 ($10k) and Series A-2 ($20k) simultaneously
    assert len(bps) >= 1
    tier1 = bps[0]
    assert tier1.end_equity == pytest.approx(30000.0)
    assert "Series A-1" in tier1.claimants_description
    assert "Series A-2" in tier1.claimants_description

    claims = allocate_claims_by_tier(derived, bps)
    t1_claims = claims[0]
    assert t1_claims.sharing_percentages["Series A-1"] == pytest.approx(10000.0 / 30000.0)
    assert t1_claims.sharing_percentages["Series A-2"] == pytest.approx(20000.0 / 30000.0)


def test_zero_dividend_vs_compounding():
    """Verify simple vs annual vs quarterly compounding conventions."""
    base_pref = SecurityInput(
        security="Series DivTest",
        security_subtype="Preferred Stock",
        shares=100,
        original_issue_price=100.0,
        conversion_price=100.0,
        seniority=1,
        issue_date="2025-01-01",
        dividend_rate=10.0,  # 10%
        compounding_convention="Simple Interest"
    )

    # 2 years under 30/360
    exit_date = "2027-01-01"
    d_simple = derive_capitalization([base_pref], exit_date, day_count_basis=0)[0]
    # Simple: 100 * 0.10 * 2 = 20 per share
    assert d_simple.per_share_dividend == pytest.approx(20.0, abs=1e-2)

    # Annual compounding: 100 * (1.10^2 - 1) = 21.0 per share
    base_pref.compounding_convention = "Annual"
    d_annual = derive_capitalization([base_pref], exit_date, day_count_basis=0)[0]
    assert d_annual.per_share_dividend == pytest.approx(21.0, abs=1e-2)

    # Quarterly compounding: 100 * ((1 + 0.10/4)^8 - 1) = 21.840289 per share
    base_pref.compounding_convention = "Quarterly"
    d_quarterly = derive_capitalization([base_pref], exit_date, day_count_basis=0)[0]
    expected_q = 100.0 * ((1.0 + 0.10 / 4.0) ** 8.0 - 1.0)
    assert d_quarterly.per_share_dividend == pytest.approx(expected_q, abs=1e-2)


def test_equity_below_first_breakpoint_waterfall():
    """If equity is below first breakpoint, senior preferred receives all, junior receives 0."""
    s1 = SecurityInput(
        security="Series Senior",
        security_subtype="Preferred Stock",
        shares=1000,
        original_issue_price=10.0,
        seniority=1
    )
    s2 = SecurityInput(
        security="Common",
        security_subtype="Common Stock",
        shares=1000
    )
    derived = derive_capitalization([s1, s2], "2027-01-01")
    bps = generate_breakpoints(derived)

    # Total senior preference is $10,000. Scenario equity = $4,000.
    wf = allocate_waterfall(derived, bps, 4000.0)
    p_map = {item.security: item.proceeds for item in wf.distribution}
    assert p_map["Series Senior"] == pytest.approx(4000.0)
    assert p_map["Common"] == pytest.approx(0.0)


def test_equity_far_above_final_breakpoint():
    """If equity is well above final breakpoint, residual is split fully diluted."""
    s1 = SecurityInput(
        security="Series Senior",
        security_subtype="Preferred Stock",
        shares=1000,
        original_issue_price=10.0,
        conversion_price=10.0,
        participation="Yes",
        seniority=1
    )
    s2 = SecurityInput(
        security="Common",
        security_subtype="Common Stock",
        shares=1000
    )
    derived = derive_capitalization([s1, s2], "2027-01-01")
    bps = generate_breakpoints(derived)

    # Total LP is 10k. 50/50 fully diluted.
    wf = allocate_waterfall(derived, bps, 100000.0)
    p_map = {item.security: item.proceeds for item in wf.distribution}
    # Senior gets 10k LP + 50% of (100k - 10k) = 10k + 45k = 55k
    # Common gets 50% of 90k = 45k
    assert p_map["Series Senior"] == pytest.approx(55000.0)
    assert p_map["Common"] == pytest.approx(45000.0)
    assert wf.total_proceeds == pytest.approx(100000.0)


def test_black_scholes_boundary_conditions():
    """Test boundary conditions for call pricing."""
    r = to_continuous_rate(0.05)
    # Zero strike: C = S * exp(-q * t)
    c_zero_k = black_scholes_call(100.0, 0.0, r, 0.3, 1.0)
    assert c_zero_k == pytest.approx(100.0)

    # Zero volatility or zero time: C = max(0, S - K*exp(-rt))
    c_zero_t = black_scholes_call(120.0, 100.0, r, 0.3, 0.0)
    assert c_zero_t == pytest.approx(20.0)

    # S = 0 -> C = 0
    c_zero_s = black_scholes_call(0.0, 100.0, r, 0.3, 1.0)
    assert c_zero_s == 0.0

