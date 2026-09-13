"""Capital structure derivation, dividend accruals, liquidation preference claims,
and fully diluted share conversion calculations.
"""

from typing import List, Optional, Union
from .models import SecurityInput, DerivedSecurity
from .date_math import year_fraction, DateLike


COMPOUNDING_PERIODS = {
    "Annual": 1,
    "Semi-Annual": 2,
    "Quarterly": 4,
    "Daily": 365
}


def parse_participation_cap(v: Optional[Union[str, float]]) -> float:
    """Parse participation cap representation to numeric multiplier.

    Returns 0.0 if 'No Cap', 'NA', None, or empty (indicating uncapped).
    """
    if v is None:
        return 0.0
    s = str(v).strip()
    if not s or s.upper() in ("NA", "NO CAP", "NONE", "UNCAPPED", "0"):
        return 0.0
    import re
    m = re.search(r"([0-9.]+)", s)
    return float(m.group(1)) if m else 0.0


def derive_capitalization(
    securities: List[SecurityInput],
    exit_date: DateLike,
    day_count_basis: int = 1
) -> List[DerivedSecurity]:
    """Derives accrued dividends, liquidation preference claims, conversion ratios,
    and fully diluted shares for every security row.
    """
    derived: List[DerivedSecurity] = []

    for sec in securities:
        sh = float(sec.shares or 0.0)
        issue = float(sec.original_issue_price or 0.0) if sec.original_issue_price is not None else 0.0
        mult = float(sec.liquidation_multiplier or 1.0) if sec.liquidation_multiplier is not None else 1.0
        conv = sec.conversion_price
        ex = float(sec.exercise_price or 0.0) if sec.exercise_price is not None else 0.0

        is_pref = (sec.security_subtype == "Preferred Stock")
        has_issue = (sec.original_issue_price is not None and issue > 0.0)

        # Base liquidation preference per share
        per_pref = (issue * mult) if (is_pref and has_issue) else 0.0

        # Dividend accrual calculation
        rate = (float(sec.dividend_rate) / 100.0) if (sec.dividend_rate is not None) else 0.0
        paid = float(sec.dividends_paid_to_date or 0.0)
        term = year_fraction(sec.issue_date, exit_date, day_count_basis) if sec.issue_date else 0.0
        cc = sec.compounding_convention or "Annual"

        per_div = 0.0
        if is_pref and has_issue and rate > 0.0 and term > 0.0:
            if cc == "Simple Interest":
                per_div = max(0.0, issue * rate * term - paid)
            else:
                p = COMPOUNDING_PERIODS.get(cc, 1)
                per_div = max(0.0, issue * ((1.0 + rate / p) ** (term * p)) - issue - paid)

        # Total preference claim for the series
        total_pref = (per_pref + per_div) * sh if (is_pref and has_issue) else 0.0

        # Conversion ratio and fully diluted shares
        if is_pref:
            if conv is not None and float(conv) > 0.0 and issue > 0.0:
                conv_ratio = issue / float(conv)
            else:
                conv_ratio = 0.0
            fd_shares = sh * conv_ratio
        else:
            conv_ratio = 1.0
            fd_shares = sh

        exercise_proceeds = (sh * ex) if (sec.exercise_price is not None and ex > 0.0) else 0.0
        seniority = int(sec.seniority) if sec.seniority is not None else 999
        cap_mult = parse_participation_cap(sec.max_participation_cap)

        derived.append(DerivedSecurity(
            security=sec.security,
            security_subtype=sec.security_subtype,
            shares=sh,
            original_issue_price=issue,
            conversion_price=float(conv) if conv is not None else None,
            conversion_ratio=conv_ratio,
            liquidation_multiplier=mult,
            seniority=seniority,
            participation=str(sec.participation or "NA"),
            cap_mult=cap_mult if cap_mult > 0.0 else None,
            per_share_dividend=per_div,
            total_accrued_dividends=per_div * sh,
            liquidation_preference_per_share=per_pref + per_div,
            total_liquidation_preference=total_pref,
            fully_diluted_shares=fd_shares,
            exercise_price=ex,
            total_exercise_proceeds=exercise_proceeds
        ))

    return derived

