"""Reconciliation test suite comparing the Python calculation engine
against the JavaScript golden reference run.
"""

import json
import sys
from pathlib import Path
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.engine.models import SecurityInput, HoldingInput
from app.engine.capitalization import derive_capitalization
from app.engine.breakpoints import generate_breakpoints
from app.engine.claims import allocate_claims_by_tier
from app.engine.opm import allocate_opm, backsolve_equity
from app.engine.waterfall import allocate_waterfall
from app.engine.holdings import evaluate_client_holdings


@pytest.fixture
def golden_data():
    fixture_path = Path(__file__).resolve().parent.parent.parent / "golden_reference" / "golden_reference_run.json"
    with open(fixture_path, "r", encoding="utf-8") as f:
        return json.load(f)


def _map_raw_to_input(raw: dict) -> SecurityInput:
    def _num(v):
        if v is None or v == "" or str(v).strip().upper() == "NA":
            return None
        return float(str(v).replace(",", ""))

    return SecurityInput(
        security=raw.get("Security", ""),
        security_subtype=raw.get("Security Subtype", "Preferred Stock"),
        shares=float(_num(raw.get("Number of Shares")) or 0.0),
        exercise_price=_num(raw.get("Wtd. Avg. Exercise Price")),
        original_issue_price=_num(raw.get("Original Issue Price")),
        conversion_price=_num(raw.get("Conversion Price")),
        liquidation_multiplier=_num(raw.get("Liquidation Multiplier")) or 1.0,
        participation=raw.get("Participation", "NA"),
        max_participation_cap=raw.get("Max Participation Cap", "NA"),
        seniority=int(_num(raw.get("Seniority")) or 999) if _num(raw.get("Seniority")) else None,
        issue_date=raw.get("Issue Date") or None,
        dividend_rate=_num(raw.get("Annual Dividend Rate")),
        compounding_convention=raw.get("Compounding Convention") or "Annual",
        dividends_paid_to_date=_num(raw.get("Dividends Paid To Date")) or 0.0
    )


def test_calibration_derivation(golden_data):
    inputs = golden_data["inputs"]
    cal_raw = inputs["PREPOP_CAL"]
    cal_inputs = [_map_raw_to_input(r) for r in cal_raw]

    cal_derived = derive_capitalization(cal_inputs, inputs["exitDate"], inputs["basis"])
    assert len(cal_derived) == len(cal_raw)

    golden_cal_rows = golden_data["calibration"]["rows"]
    for py_row, js_row in zip(cal_derived, golden_cal_rows):
        assert py_row.security == js_row["Security"]
        assert py_row.shares == pytest.approx(float(js_row["shares"]), rel=1e-5)
        assert py_row.total_liquidation_preference == pytest.approx(float(js_row["pref"]), abs=1e-2)
        assert py_row.fully_diluted_shares == pytest.approx(float(js_row["fd"]), rel=1e-5)


def test_calibration_breakpoints(golden_data):
    inputs = golden_data["inputs"]
    cal_inputs = [_map_raw_to_input(r) for r in inputs["PREPOP_CAL"]]
    cal_derived = derive_capitalization(cal_inputs, inputs["exitDate"], inputs["basis"])

    bps = generate_breakpoints(cal_derived)
    golden_bps = golden_data["calibration"]["breakpoints"]

    assert len(bps) == len(golden_bps)
    for py_bp, js_bp in zip(bps, golden_bps):
        assert py_bp.tier == js_bp[0]
        assert py_bp.start_equity == pytest.approx(float(js_bp[1]), abs=1e-2)
        assert py_bp.end_equity == pytest.approx(float(js_bp[2]), abs=1e-2)


def test_calibration_backsolve_and_allocation(golden_data):
    inputs = golden_data["inputs"]
    cal_inputs = [_map_raw_to_input(r) for r in inputs["PREPOP_CAL"]]
    cal_derived = derive_capitalization(cal_inputs, inputs["exitDate"], inputs["basis"])
    cal_bps = generate_breakpoints(cal_derived)

    solved_eq = backsolve_equity(
        rows=cal_derived,
        breakpoints=cal_bps,
        calibration_date=inputs["calDate"],
        exit_date=inputs["exitDate"],
        rf_effective=inputs["rfCal"],
        volatility=inputs["volCal"],
        dividend_yield=inputs["qCal"],
        calibration_security_name=inputs["calSec"],
        target_price=inputs["txPrice"],
        day_count_basis=inputs["basis"]
    )

    expected_solved = golden_data["calibration"]["solvedEquity"]
    # Verify equity within $0.05
    assert solved_eq == pytest.approx(expected_solved, abs=0.05)

    # Verify OPM allocation for Calibration Date
    opm_res = allocate_opm(
        rows=cal_derived,
        breakpoints=cal_bps,
        equity_value=solved_eq,
        valuation_date=inputs["calDate"],
        exit_date=inputs["exitDate"],
        rf_effective=inputs["rfCal"],
        volatility=inputs["volCal"],
        dividend_yield=inputs["qCal"],
        day_count_basis=inputs["basis"]
    )

    # Series I per-share must match target price 2021.90 exactly
    assert opm_res.per_share_values["Series I"] == pytest.approx(inputs["txPrice"], abs=1e-4)

    golden_alloc = golden_data["calibration"]["opmAllocation"]["alloc"]
    for sec_name, golden_val in golden_alloc.items():
        assert opm_res.allocated_values[sec_name] == pytest.approx(float(golden_val), abs=1.0)


def test_valuation_allocation(golden_data):
    inputs = golden_data["inputs"]
    val_inputs = [_map_raw_to_input(r) for r in inputs["PREPOP_VAL"]]
    val_derived = derive_capitalization(val_inputs, inputs["exitDate"], inputs["basis"])
    val_bps = generate_breakpoints(val_derived)

    adj_eq = golden_data["valuation"]["adjustedEquity"]

    opm_val = allocate_opm(
        rows=val_derived,
        breakpoints=val_bps,
        equity_value=adj_eq,
        valuation_date=inputs["valDate"],
        exit_date=inputs["exitDate"],
        rf_effective=inputs["rfVal"],
        volatility=inputs["volVal"],
        dividend_yield=inputs["qVal"],
        day_count_basis=inputs["basis"]
    )

    golden_alloc = golden_data["valuation"]["opmAllocation"]["alloc"]
    for sec_name, golden_val in golden_alloc.items():
        assert opm_val.allocated_values[sec_name] == pytest.approx(float(golden_val), abs=1.0)

    # Verify per-share concluded fair values
    assert opm_val.per_share_values["Series I"] == pytest.approx(1992.5034, abs=0.01)
    assert opm_val.per_share_values["Series H"] == pytest.approx(2391.2852, abs=0.01)
    assert opm_val.per_share_values["Series G2"] == pytest.approx(1463.5913, abs=0.01)
    assert opm_val.per_share_values["Common Stock"] == pytest.approx(740.1469, abs=0.01)
    assert opm_val.per_share_values["Common Stock Options A"] == pytest.approx(739.3276, abs=0.01)
    assert opm_val.per_share_values["Common Stock Options B"] == pytest.approx(254.0772, abs=0.01)
    assert opm_val.per_share_values["Series H Warrants"] == pytest.approx(740.1469, abs=0.01)


def test_waterfall_allocation(golden_data):
    inputs = golden_data["inputs"]
    val_inputs = [_map_raw_to_input(r) for r in inputs["PREPOP_VAL"]]
    val_derived = derive_capitalization(val_inputs, inputs["exitDate"], inputs["basis"])
    val_bps = generate_breakpoints(val_derived)

    # Test waterfall at scenario value
    wf_res = allocate_waterfall(val_derived, val_bps, 229900000.0)
    assert wf_res.total_proceeds == pytest.approx(229900000.0, abs=1.0)

    # At 229.9m, all senior liquidation preference is funded
    total_pref = sum(r.total_liquidation_preference for r in val_derived)
    assert total_pref == pytest.approx(195580334.84, abs=1.0)


def test_client_holdings_evaluation(golden_data):
    inputs = golden_data["inputs"]
    val_inputs = [_map_raw_to_input(r) for r in inputs["PREPOP_VAL"]]
    val_derived = derive_capitalization(val_inputs, inputs["exitDate"], inputs["basis"])
    val_bps = generate_breakpoints(val_derived)
    adj_eq = golden_data["valuation"]["adjustedEquity"]

    opm_val = allocate_opm(
        rows=val_derived,
        breakpoints=val_bps,
        equity_value=adj_eq,
        valuation_date=inputs["valDate"],
        exit_date=inputs["exitDate"],
        rf_effective=inputs["rfVal"],
        volatility=inputs["volVal"],
        dividend_yield=inputs["qVal"],
        day_count_basis=inputs["basis"]
    )

    demo_holdings = [
        HoldingInput(fund="S2G Fund I", security="Series F1", units=51, cost=65305),
        HoldingInput(fund="S2G Fund I", security="Series H", units=4000, cost=7000000),
        HoldingInput(fund="S2G Fund I", security="Common Stock", units=300, cost=500000),
        HoldingInput(fund="S2G Fund II", security="Series H", units=2500, cost=4000000),
        HoldingInput(fund="S2G Fund II", security="Common Stock", units=300, cost=450000),
        HoldingInput(fund="S2G Fund III", security="Series H", units=594, cost=1088229),
        HoldingInput(fund="S2G Fund III", security="Common Stock", units=137, cost=195660)
    ]

    summary = evaluate_client_holdings(demo_holdings, val_derived, opm_val.per_share_values)

    # Series F1 in Fund I: 51 * 1152.9549 = 58,800.70; MOIC = 0.90x
    f1_item = next(it for it in summary.items if it.fund == "S2G Fund I" and it.security == "Series F1")
    assert f1_item.concluded_fair_value == pytest.approx(58800.70, abs=1.0)
    assert f1_item.moic == pytest.approx(0.90, abs=0.01)

    # Series H in Fund I: 4000 * 2391.2852 = 9,565,140.76; MOIC = 1.37x
    h_item = next(it for it in summary.items if it.fund == "S2G Fund I" and it.security == "Series H")
    assert h_item.concluded_fair_value == pytest.approx(9565140.76, abs=5.0)
    assert h_item.moic == pytest.approx(1.366, abs=0.01)

    assert len(summary.fund_subtotals) == 3
    assert summary.total_cost == pytest.approx(13299194.0, abs=1.0)
