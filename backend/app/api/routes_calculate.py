"""Valuation calculation and validation API endpoints.
"""

from fastapi import APIRouter, HTTPException
from ..engine.models import ValuationRequest, ValuationResponse
from ..engine.pipeline import calculate_valuation
from ..engine.validation import validate_valuation_request
from ..engine.default_scenario import get_default_valuation_request
from ..engine.date_math import BASIS_NAMES

router = APIRouter()


@router.post("/calculate", response_model=ValuationResponse)
def calculate_model(request: ValuationRequest):
    """Executes the full valuation analysis for the supplied request parameters."""
    try:
        return calculate_valuation(request)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Valuation calculation failed: {str(e)}")


@router.post("/validate")
def validate_model(request: ValuationRequest):
    """Validates capitalization tables and parameters without running full OPM backsolve."""
    issues = validate_valuation_request(request)
    return {
        "valid": len(issues) == 0,
        "issues": issues
    }


@router.get("/scenario/default", response_model=ValuationRequest)
def get_default_scenario():
    """Retrieves the default pre-populated TADO valuation scenario."""
    return get_default_valuation_request()


@router.get("/basis-conventions")
def get_basis_conventions():
    """Retrieves supported day-count basis conventions."""
    return [
        {"id": basis_id, "name": name}
        for basis_id, name in BASIS_NAMES.items()
    ]

