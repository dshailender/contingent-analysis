"""Input validation rules and consistency checks for valuation models.
"""

from typing import List
from .models import SecurityInput, ValuationRequest


def validate_securities(securities: List[SecurityInput], context_label: str = "Capitalization Table") -> List[str]:
    """Validates security ledger rows and returns human-readable issues."""
    issues: List[str] = []

    if not securities:
        issues.append(f"{context_label}: At least one security class is required.")
        return issues

    seen_names = set()
    for i, s in enumerate(securities):
        row_num = i + 1
        name = (s.security or "").strip()
        if not name:
            issues.append(f"{context_label} (Row {row_num}): Security name is required.")
            continue

        if name.lower() in seen_names:
            issues.append(f"{context_label} (Row {row_num}): Duplicate security name '{name}'.")
        seen_names.add(name.lower())

        if s.shares <= 0.0:
            issues.append(f"{context_label} (Row {row_num} - {name}): Number of shares must be greater than 0.")

        if s.security_subtype == "Preferred Stock":
            if s.original_issue_price is None or s.original_issue_price <= 0.0:
                issues.append(f"{context_label} (Row {row_num} - {name}): Original Issue Price must be greater than 0.")
            if s.seniority is None or s.seniority <= 0:
                issues.append(f"{context_label} (Row {row_num} - {name}): Seniority must be a positive integer (1 = most senior).")
            if s.conversion_price is not None and s.conversion_price < 0.0:
                issues.append(f"{context_label} (Row {row_num} - {name}): Conversion Price cannot be negative (0 indicates non-converting).")

        elif s.security_subtype in ("Option", "Warrant"):
            if s.exercise_price is not None and s.exercise_price < 0.0:
                issues.append(f"{context_label} (Row {row_num} - {name}): Weighted average exercise price cannot be negative.")

    return issues


def validate_valuation_request(req: ValuationRequest) -> List[str]:
    """Validates an entire valuation calculation request."""
    issues: List[str] = []

    if not req.company_name.strip():
        issues.append("Company Name is required.")
    if not req.client_name.strip():
        issues.append("Client Name is required.")
    if not req.calibration_date.strip():
        issues.append("Calibration Date is required.")
    if not req.valuation_date.strip():
        issues.append("Valuation Date is required.")
    if not req.exit_date.strip():
        issues.append("Global Exit Date is required.")

    if req.calibration_date and req.exit_date and req.calibration_date >= req.exit_date:
        issues.append("Calibration Date must precede Global Exit Date.")
    if req.valuation_date and req.exit_date and req.valuation_date >= req.exit_date:
        issues.append("Valuation Date must precede Global Exit Date.")

    # Validate cap tables
    issues.extend(validate_securities(req.calibration_securities, "Calibration Date Cap Table"))
    issues.extend(validate_securities(req.valuation_securities, "Valuation Date Cap Table"))

    # Validate and auto-resolve calibration security
    cal_names = [s.security.strip() for s in req.calibration_securities if s.security and s.security.strip()]
    if not cal_names:
        issues.append("Calibration Date Cap Table must contain at least one valid security class.")
    elif req.calibration_security_name.strip() not in cal_names:
        # Graceful auto-resolution: pick first available Preferred Stock or first valid security
        preferred = next(
            (s.security.strip() for s in req.calibration_securities 
             if s.security_subtype == "Preferred Stock" and s.security and s.security.strip()),
            None
        )
        req.calibration_security_name = preferred or cal_names[0]
    else:
        req.calibration_security_name = req.calibration_security_name.strip()

    if req.transaction_price <= 0.0:
        issues.append("Observed Transaction Price must be greater than 0.")

    if req.vol_calibration <= 0.0:
        issues.append("Calibration Volatility must be greater than 0%.")
    if req.vol_valuation <= 0.0:
        issues.append("Valuation Volatility must be greater than 0%.")

    return issues

