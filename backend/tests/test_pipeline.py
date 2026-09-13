"""Unit test verifying end-to-end valuation execution pipeline and default scenario loader.
"""

import pytest
from app.engine.default_scenario import get_default_valuation_request
from app.engine.pipeline import calculate_valuation
from app.engine.validation import validate_valuation_request


def test_default_scenario_and_pipeline():
    req = get_default_valuation_request()
    assert req.company_name == "TADO"
    assert len(req.calibration_securities) == 16
    assert len(req.valuation_securities) == 16
    assert len(req.holdings) == 7

    # Validate
    issues = validate_valuation_request(req)
    assert len(issues) == 0, f"Validation issues: {issues}"

    # Calculate
    resp = calculate_valuation(req)
    assert resp.company_name == "TADO"
    assert resp.calibration_solved_equity == pytest.approx(287252502.92, abs=1.0)
    assert resp.concluded_equity_value == pytest.approx(287252502.92, abs=1.0)

    # Series I per-share in calibration OPM
    series_i_cal = resp.calibration_opm.per_share_values["Series I"]
    assert series_i_cal == pytest.approx(2021.90, abs=0.01)

    # Series I and H per-share in valuation OPM
    series_i_val = resp.valuation_opm.per_share_values["Series I"]
    assert series_i_val == pytest.approx(1992.50, abs=0.05)

    series_h_val = resp.valuation_opm.per_share_values["Series H"]
    assert series_h_val == pytest.approx(2391.29, abs=0.05)

    # Holdings summary
    assert resp.holdings.total_cost == pytest.approx(13299194.0, abs=1.0)
    assert resp.holdings.total_value == pytest.approx(17568066.01, abs=5.0)
    assert resp.holdings.consolidated_moic == pytest.approx(1.32, abs=0.01)
