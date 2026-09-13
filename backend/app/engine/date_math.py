"""Date arithmetic and year-fraction calculation supporting Excel YEARFRAC conventions.

Conventions supported:
- Basis 0: US 30/360 (NASD)
- Basis 1: Actual/Actual (Excel convention: actual days / average days in spanned years)
- Basis 2: Actual/360
- Basis 3: Actual/365
- Basis 4: European 30/360
"""

from datetime import date, datetime
from typing import Union

DateLike = Union[str, date, datetime]

BASIS_NAMES = {
    0: "US 30/360",
    1: "Actual/Actual",
    2: "Actual/360",
    3: "Actual/365",
    4: "European 30/360"
}


def parse_date(d: DateLike) -> date:
    """Parse date-like object to datetime.date in UTC/naive format."""
    if isinstance(d, datetime):
        return d.date()
    if isinstance(d, date):
        return d
    if isinstance(d, str):
        # Support YYYY-MM-DD or ISO strings
        s = d.strip()
        if "T" in s:
            s = s.split("T")[0]
        return datetime.strptime(s, "%Y-%m-%d").date()
    raise ValueError(f"Cannot parse date: {d}")


def is_leap_year(y: int) -> bool:
    """Check if year is a leap year according to Gregorian rules."""
    return (y % 4 == 0) and (y % 100 != 0 or y % 400 == 0)


def days_between(d1: date, d2: date) -> int:
    """Actual calendar days between two dates."""
    return (d2 - d1).days


def year_fraction(d1_raw: DateLike, d2_raw: DateLike, basis: int = 1) -> float:
    """Compute year fraction between two dates adhering to the specified day-count basis.

    Preserves exact Excel and reference JS implementation semantics.
    """
    if not d1_raw or not d2_raw:
        return 0.0

    d1 = parse_date(d1_raw)
    d2 = parse_date(d2_raw)

    sign = 1.0
    if d2 < d1:
        d1, d2 = d2, d1
        sign = -1.0

    days = days_between(d1, d2)
    if days == 0:
        return 0.0

    basis = int(basis)
    if basis == 2:
        # Actual/360
        return sign * (days / 360.0)

    if basis == 3:
        # Actual/365
        return sign * (days / 365.0)

    if basis in (0, 4):
        # 30/360 conventions
        y1, m1, dd1 = d1.year, d1.month, d1.day
        y2, m2, dd2 = d2.year, d2.month, d2.day

        if basis == 0:
            # US 30/360
            if dd1 == 31:
                dd1 = 30
            if dd2 == 31 and dd1 >= 30:
                dd2 = 30
        else:
            # European 30/360
            dd1 = min(dd1, 30)
            dd2 = min(dd2, 30)

        num_days = (y2 - y1) * 360 + (m2 - m1) * 30 + (dd2 - dd1)
        return sign * (num_days / 360.0)

    # Basis 1: Actual/Actual (Excel YEARFRAC: actual days / average days in calendar years spanned)
    y1, y2 = d1.year, d2.year
    years_count = y2 - y1 + 1
    total_year_days = sum(366 if is_leap_year(y) else 365 for y in range(y1, y2 + 1))
    avg_year_days = total_year_days / float(years_count)
    return sign * (days / avg_year_days)

