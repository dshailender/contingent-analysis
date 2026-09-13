"""Default valuation scenario generator matching the reference model (TADO GmbH).
"""

from typing import List
from pathlib import Path
import json
from .models import SecurityInput, HoldingInput, ValuationRequest


def _parse_num(v):
    if v is None or v == "" or str(v).strip().upper() == "NA":
        return None
    return float(str(v).replace(",", ""))


def _raw_to_security(raw: dict) -> SecurityInput:
    return SecurityInput(
        security=raw.get("Security", ""),
        security_subtype=raw.get("Security Subtype", "Preferred Stock"),
        shares=float(_parse_num(raw.get("Number of Shares")) or 0.0),
        exercise_price=_parse_num(raw.get("Wtd. Avg. Exercise Price")),
        original_issue_price=_parse_num(raw.get("Original Issue Price")),
        conversion_price=_parse_num(raw.get("Conversion Price")),
        liquidation_multiplier=_parse_num(raw.get("Liquidation Multiplier")) or 1.0,
        participation=raw.get("Participation", "NA"),
        max_participation_cap=raw.get("Max Participation Cap", "NA"),
        seniority=int(_parse_num(raw.get("Seniority")) or 999) if _parse_num(raw.get("Seniority")) else None,
        issue_date=raw.get("Issue Date") or None,
        dividend_rate=_parse_num(raw.get("Annual Dividend Rate")),
        compounding_convention=raw.get("Compounding Convention") or "Annual",
        dividends_paid_to_date=_parse_num(raw.get("Dividends Paid To Date")) or 0.0
    )


def get_default_valuation_request() -> ValuationRequest:
    """Returns the reference pre-populated valuation model for TADO GmbH."""
    golden_path = Path(__file__).resolve().parent.parent.parent.parent / "golden_reference" / "golden_reference_run.json"
    
    with open(golden_path, "r", encoding="utf-8") as f:
        data = json.load(f)
    
    inputs = data["inputs"]
    cal_secs = [_raw_to_security(r) for r in inputs["PREPOP_CAL"]]
    val_secs = [_raw_to_security(r) for r in inputs["PREPOP_VAL"]]
    
    holdings = [
        HoldingInput(fund="S2G Fund I", security="Series F1", units=51, cost=65305),
        HoldingInput(fund="S2G Fund I", security="Series H", units=4000, cost=7000000),
        HoldingInput(fund="S2G Fund I", security="Common Stock", units=300, cost=500000),
        HoldingInput(fund="S2G Fund II", security="Series H", units=2500, cost=4000000),
        HoldingInput(fund="S2G Fund II", security="Common Stock", units=300, cost=450000),
        HoldingInput(fund="S2G Fund III", security="Series H", units=594, cost=1088229),
        HoldingInput(fund="S2G Fund III", security="Common Stock", units=137, cost=195660)
    ]
    
    return ValuationRequest(
        company_name="TADO",
        client_name="S2G Investments",
        report_status="DRAFT - For Discussion Purposes Only",
        report_purpose="Valuation Analysis",
        report_purpose_manual="",
        calibration_date=inputs.get("calDate", "2025-02-26"),
        valuation_date=inputs.get("valDate", "2026-06-30"),
        exit_date=inputs.get("exitDate", "2027-06-30"),
        day_count_basis=inputs.get("basis", 1),
        report_currency="EUR",
        display_units="actual",
        calibration_securities=cal_secs,
        valuation_securities=val_secs,
        calibration_security_name=inputs.get("calSec", "Series I"),
        transaction_price=float(inputs.get("txPrice", 2021.90)),
        rf_calibration=float(inputs.get("rfCal", 0.021)) * 100.0 if inputs.get("rfCal", 0.021) < 1.0 else float(inputs.get("rfCal", 2.1)),
        vol_calibration=float(inputs.get("volCal", 0.35)) * 100.0 if inputs.get("volCal", 0.35) < 1.0 else float(inputs.get("volCal", 35.0)),
        dividend_yield_calibration=float(inputs.get("qCal", 0.0)) * 100.0,
        rf_valuation=float(inputs.get("rfVal", 0.024)) * 100.0 if inputs.get("rfVal", 0.024) < 1.0 else float(inputs.get("rfVal", 2.4)),
        vol_valuation=float(inputs.get("volVal", 0.35)) * 100.0 if inputs.get("volVal", 0.35) < 1.0 else float(inputs.get("volVal", 35.0)),
        dividend_yield_valuation=float(inputs.get("qVal", 0.0)) * 100.0,
        market_adjustment=float(inputs.get("marketAdj", 0.0)),
        company_adjustment=float(inputs.get("compAdj", 0.0)),
        waterfall_equity_source="concluded",
        manual_waterfall_equity=229900000.0,
        holdings=holdings
    )

