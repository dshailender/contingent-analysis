"""Valuation execution pipeline orchestrating capitalization derivation, breakpoints,
OPM backsolve, allocation, comparative waterfall, and client holdings evaluation.
"""

from typing import Optional
from .models import (
    ValuationRequest,
    ValuationResponse,
    RiskFreeRateAnalysis,
)
from .capitalization import derive_capitalization
from .breakpoints import generate_breakpoints
from .claims import allocate_claims_by_tier
from .opm import backsolve_equity, allocate_opm
from .waterfall import allocate_waterfall, build_comparative_waterfall
from .holdings import evaluate_client_holdings
from .risk_free_rates import build_risk_free_analysis
from .date_math import year_fraction, BASIS_NAMES
from .black_scholes import to_continuous_rate
from .validation import validate_valuation_request


def calculate_valuation(request: ValuationRequest) -> ValuationResponse:
    """Executes the complete valuation pipeline for a ValuationRequest.

    Raises:
        ValueError: If input validation fails with specific issue descriptions.
    """
    issues = validate_valuation_request(request)
    if issues:
        raise ValueError("; ".join(issues))

    # Terms and continuous rates
    term_cal = year_fraction(request.calibration_date, request.exit_date, request.day_count_basis)
    term_val = year_fraction(request.valuation_date, request.exit_date, request.day_count_basis)
    rf_cal_cont = to_continuous_rate(request.rf_calibration / 100.0) * 100.0
    rf_val_cont = to_continuous_rate(request.rf_valuation / 100.0) * 100.0

    # Display scaling
    display_scale = 1.0
    if request.display_units == "millions":
        display_scale = 1e-6
    elif request.display_units == "thousands":
        display_scale = 1e-3

    # Capitalization derivation
    cal_derived = derive_capitalization(request.calibration_securities, request.exit_date, request.day_count_basis)
    val_derived = derive_capitalization(request.valuation_securities, request.exit_date, request.day_count_basis)

    # Breakpoint schedules
    cal_bps = generate_breakpoints(cal_derived)
    val_bps = generate_breakpoints(val_derived)

    # Claim tier allocations
    cal_claims = allocate_claims_by_tier(cal_derived, cal_bps)
    val_claims = allocate_claims_by_tier(val_derived, val_bps)

    # Calibration backsolve
    cal_solved_eq = backsolve_equity(
        rows=cal_derived,
        breakpoints=cal_bps,
        calibration_date=request.calibration_date,
        exit_date=request.exit_date,
        rf_effective=request.rf_calibration / 100.0,
        volatility=request.vol_calibration / 100.0,
        dividend_yield=request.dividend_yield_calibration / 100.0,
        calibration_security_name=request.calibration_security_name,
        target_price=request.transaction_price,
        day_count_basis=request.day_count_basis
    )

    # Calibration OPM allocation
    cal_opm = allocate_opm(
        rows=cal_derived,
        breakpoints=cal_bps,
        equity_value=cal_solved_eq,
        valuation_date=request.calibration_date,
        exit_date=request.exit_date,
        rf_effective=request.rf_calibration / 100.0,
        volatility=request.vol_calibration / 100.0,
        dividend_yield=request.dividend_yield_calibration / 100.0,
        day_count_basis=request.day_count_basis
    )

    # Valuation concluded equity value
    market_adj = request.market_adjustment / 100.0
    company_adj = request.company_adjustment / 100.0
    concluded_equity = cal_solved_eq * (1.0 + market_adj) * (1.0 + company_adj)

    # Valuation OPM allocation
    val_opm = allocate_opm(
        rows=val_derived,
        breakpoints=val_bps,
        equity_value=concluded_equity,
        valuation_date=request.valuation_date,
        exit_date=request.exit_date,
        rf_effective=request.rf_valuation / 100.0,
        volatility=request.vol_valuation / 100.0,
        dividend_yield=request.dividend_yield_valuation / 100.0,
        day_count_basis=request.day_count_basis
    )

    # Comparative waterfall
    waterfall_equity = (
        concluded_equity if request.waterfall_equity_source == "concluded" else request.manual_waterfall_equity
    )
    val_waterfall = allocate_waterfall(val_derived, val_bps, waterfall_equity)
    cal_waterfall = allocate_waterfall(cal_derived, cal_bps, cal_solved_eq)
    comp_wf = build_comparative_waterfall(cal_derived, val_derived, cal_waterfall, val_waterfall)

    # Client holdings
    holdings = evaluate_client_holdings(request.holdings, val_derived, val_opm.per_share_values)

    # Risk-free rate analyses
    cal_rf = build_risk_free_analysis(
        as_of_date=request.calibration_date,
        exit_date=request.exit_date,
        annual_effective_rate_pct=request.rf_calibration,
        source_name="Calibration Risk-Free Rate",
        day_count_basis=request.day_count_basis
    )
    val_rf = build_risk_free_analysis(
        as_of_date=request.valuation_date,
        exit_date=request.exit_date,
        annual_effective_rate_pct=request.rf_valuation,
        source_name="Valuation Risk-Free Rate",
        day_count_basis=request.day_count_basis
    )

    report_purpose_str = request.report_purpose
    if request.report_purpose == "Other (Specify Below)" and request.report_purpose_manual:
        report_purpose_str = request.report_purpose_manual

    return ValuationResponse(
        company_name=request.company_name,
        client_name=request.client_name,
        report_status=request.report_status,
        report_purpose=report_purpose_str,
        report_currency=request.report_currency,
        display_units=request.display_units,
        display_scale=display_scale,
        firm_logo_base64=request.firm_logo_base64,
        show_secondary_currency=request.show_secondary_currency,
        secondary_currency=request.secondary_currency,
        secondary_fx_rate=request.secondary_fx_rate,
        calibration_date=request.calibration_date,
        valuation_date=request.valuation_date,
        exit_date=request.exit_date,
        day_count_basis=request.day_count_basis,
        day_count_name=BASIS_NAMES.get(request.day_count_basis, "Actual/Actual"),
        term_calibration=term_cal,
        term_valuation=term_val,
        rf_calibration_effective=request.rf_calibration,
        rf_calibration_continuous=rf_cal_cont,
        rf_valuation_effective=request.rf_valuation,
        rf_valuation_continuous=rf_val_cont,
        calibration_security_name=request.calibration_security_name,
        calibration_derived_securities=cal_derived,
        calibration_breakpoints=cal_bps,
        calibration_claims=cal_claims,
        calibration_solved_equity=cal_solved_eq,
        calibration_opm=cal_opm,
        calibration_rf_analysis=cal_rf,
        valuation_derived_securities=val_derived,
        valuation_breakpoints=val_bps,
        valuation_claims=val_claims,
        concluded_equity_value=concluded_equity,
        valuation_opm=val_opm,
        valuation_rf_analysis=val_rf,
        waterfall=val_waterfall,
        calibration_waterfall=cal_waterfall,
        comparative_waterfall=comp_wf,
        holdings=holdings,
        calibration_volatility=None,
        valuation_volatility=None
    )

