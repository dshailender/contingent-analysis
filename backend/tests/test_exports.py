"""Unit tests for Excel and watermarked PDF export generators.
"""

import io
import json
import pytest
from pathlib import Path
import openpyxl
import pypdf

from app.engine.models import ValuationRequest, ValuationResponse
from app.engine.capitalization import derive_capitalization
from app.engine.breakpoints import generate_breakpoints
from app.engine.claims import allocate_claims_by_tier
from app.engine.opm import allocate_opm, backsolve_equity
from app.engine.waterfall import allocate_waterfall
from app.engine.holdings import evaluate_client_holdings
from app.engine.exports_excel import generate_valuation_workbook
from app.engine.exports_pdf import generate_valuation_pdf
from app.engine.date_math import year_fraction, BASIS_NAMES
from app.engine.black_scholes import to_continuous_rate


@pytest.fixture
def sample_valuation_response():
    fixture_path = Path(__file__).resolve().parent.parent.parent / "golden_reference" / "golden_reference_run.json"
    with open(fixture_path, "r", encoding="utf-8") as f:
        golden = json.load(f)

    inputs = golden["inputs"]
    from test_golden_reconciliation import _map_raw_to_input
    cal_inputs = [_map_raw_to_input(r) for r in inputs["PREPOP_CAL"]]
    val_inputs = [_map_raw_to_input(r) for r in inputs["PREPOP_VAL"]]

    cal_derived = derive_capitalization(cal_inputs, inputs["exitDate"], inputs["basis"])
    val_derived = derive_capitalization(val_inputs, inputs["exitDate"], inputs["basis"])

    cal_bps = generate_breakpoints(cal_derived)
    val_bps = generate_breakpoints(val_derived)

    cal_claims = allocate_claims_by_tier(cal_derived, cal_bps)
    val_claims = allocate_claims_by_tier(val_derived, val_bps)

    solved_eq = golden["calibration"]["solvedEquity"]
    adj_eq = golden["valuation"]["adjustedEquity"]

    cal_opm = allocate_opm(cal_derived, cal_bps, solved_eq, inputs["calDate"], inputs["exitDate"], inputs["rfCal"], inputs["volCal"], inputs["qCal"], inputs["basis"])
    val_opm = allocate_opm(val_derived, val_bps, adj_eq, inputs["valDate"], inputs["exitDate"], inputs["rfVal"], inputs["volVal"], inputs["qVal"], inputs["basis"])

    wf = allocate_waterfall(val_derived, val_bps, adj_eq)

    from app.engine.models import HoldingInput
    demo_holdings = [
        HoldingInput(fund="S2G Fund I", security="Series F1", units=51, cost=65305),
        HoldingInput(fund="S2G Fund I", security="Series H", units=4000, cost=7000000),
        HoldingInput(fund="S2G Fund I", security="Common Stock", units=300, cost=500000),
        HoldingInput(fund="S2G Fund II", security="Series H", units=2500, cost=4000000),
        HoldingInput(fund="S2G Fund II", security="Common Stock", units=300, cost=450000),
        HoldingInput(fund="S2G Fund III", security="Series H", units=594, cost=1088229),
        HoldingInput(fund="S2G Fund III", security="Common Stock", units=137, cost=195660)
    ]
    holdings_summary = evaluate_client_holdings(demo_holdings, val_derived, val_opm.per_share_values)

    t_cal = year_fraction(inputs["calDate"], inputs["exitDate"], inputs["basis"])
    t_val = year_fraction(inputs["valDate"], inputs["exitDate"], inputs["basis"])

    return ValuationResponse(
        company_name="TADO",
        client_name="S2G Investments",
        report_status="DRAFT - For Discussion Purposes Only",
        report_purpose="Valuation Analysis",
        report_currency="EUR",
        display_units="actual",
        display_scale=1.0,
        calibration_date=inputs["calDate"],
        valuation_date=inputs["valDate"],
        exit_date=inputs["exitDate"],
        day_count_basis=inputs["basis"],
        day_count_name=BASIS_NAMES[inputs["basis"]],
        term_calibration=t_cal,
        term_valuation=t_val,
        rf_calibration_effective=inputs["rfCal"] * 100.0,
        rf_calibration_continuous=to_continuous_rate(inputs["rfCal"]) * 100.0,
        rf_valuation_effective=inputs["rfVal"] * 100.0,
        rf_valuation_continuous=to_continuous_rate(inputs["rfVal"]) * 100.0,
        calibration_derived_securities=cal_derived,
        calibration_breakpoints=cal_bps,
        calibration_claims=cal_claims,
        calibration_solved_equity=solved_eq,
        calibration_opm=cal_opm,
        valuation_derived_securities=val_derived,
        valuation_breakpoints=val_bps,
        valuation_claims=val_claims,
        concluded_equity_value=adj_eq,
        valuation_opm=val_opm,
        waterfall=wf,
        holdings=holdings_summary
    )


def test_excel_export_generation(sample_valuation_response):
    excel_buf = generate_valuation_workbook(sample_valuation_response)
    assert excel_buf is not None
    assert excel_buf.getvalue()

    wb = openpyxl.load_workbook(excel_buf, data_only=True)
    expected_sheets = [
        "Key Assumptions",
        "Calibration Cap Table",
        "Valuation Cap Table",
        "Breakpoint Schedule",
        "Valuation OPM Allocation",
        "Client Holdings Summary",
        "Waterfall Analysis"
    ]
    for s in expected_sheets:
        assert s in wb.sheetnames


@pytest.mark.asyncio
async def test_pdf_export_watermark(sample_valuation_response):
    pdf_bytes = await generate_valuation_pdf(sample_valuation_response)
    assert pdf_bytes is not None
    assert len(pdf_bytes) > 5000

    # Read generated PDF bytes and verify structure
    import pypdf
    reader = pypdf.PdfReader(io.BytesIO(pdf_bytes))
    assert len(reader.pages) >= 10, f"Expected at least 10 pages, got {len(reader.pages)}"

    # Verify watermark presence and all exhibits
    text_content = ""
    for page in reader.pages:
        text_content += page.extract_text() or ""

    assert "HIGHLY CONFIDENTIAL" in text_content
    assert "TADO" in text_content
    assert "S2G Investments" in text_content

    # Check all exhibits are present
    for ex_num in ["Exhibit 1.0", "Exhibit 2.0", "Exhibit 3.0", "Exhibit 4.0", "Exhibit 5.0", 
                   "Exhibit 6.0", "Exhibit 7.0", "Exhibit 8.0", "Exhibit 9.0", "Exhibit 10.0"]:
        assert ex_num in text_content, f"Missing {ex_num} in generated PDF export"

