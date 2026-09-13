"""Cumulative equity breakpoint schedule generator across seniority tiers,
option/warrant exercises, conversion thresholds, and participation caps.
"""

from typing import List, Tuple
from .models import DerivedSecurity, BreakpointTier


def total_equity_at_threshold(rows: List[DerivedSecurity], z: float) -> float:
    """Calculates total enterprise equity value corresponding to common share value z.

    Exact port of reference JS totalValue function.
    """
    s = 0.0
    for r in rows:
        k = r.total_liquidation_preference
        p = r.fully_diluted_shares
        ep = r.exercise_price
        q = r.participation.upper().startswith("Y")
        cm = r.cap_mult or 0.0
        isw = "WARRANT" in r.security.upper()

        if k > 0.0:
            if q:
                cap_lim = r.original_issue_price * r.shares * cm
                s += min(k + z * p, cap_lim) if cm > 0.0 else (k + z * p)
            else:
                s += max(k, z * p)
        elif ep > 0.0:
            s += (z * p + max(0.0, (z - ep) * p)) if isw else max(0.0, (z - ep) * p)
        else:
            s += z * p
    return s


def get_active_claimants(rows: List[DerivedSecurity], z: float) -> List[str]:
    """Identifies securities that participate in the next marginal dollar above share threshold z.

    Exact port of reference JS activeNames function.
    """
    active = []
    for r in rows:
        k = r.total_liquidation_preference
        p = r.fully_diluted_shares
        ep = r.exercise_price
        q = r.participation.upper().startswith("Y")
        cm = r.cap_mult or 0.0
        isw = "WARRANT" in r.security.upper()

        if k == 0.0:
            if ep <= 0.0 or isw:
                active.append(r.security)
            elif z > ep:
                active.append(r.security)
        elif not q:
            den = p if p > 0.0 else 1.0
            if z > (k / den):
                active.append(r.security)
        elif cm <= 0.0:
            active.append(r.security)
        else:
            den = p if p > 0.0 else 1.0
            capx = (r.original_issue_price * r.shares * cm - k) / den
            capconv = (r.original_issue_price * r.shares * cm) / den
            if z < capx or z >= capconv:
                active.append(r.security)

    return active


def generate_breakpoints(rows: List[DerivedSecurity]) -> List[BreakpointTier]:
    """Generates the ordered breakpoint schedule with detailed event triggers."""
    events: List[Tuple[float, str, str]] = []

    # 1. Seniority tiers for liquidation preferences
    seniority_levels = sorted({
        r.seniority for r in rows
        if r.total_liquidation_preference > 0.0 and r.seniority > 0
    })

    for s_level in seniority_levels:
        ev = sum(
            r.total_liquidation_preference
            for r in rows
            if r.total_liquidation_preference > 0.0 and r.seniority <= s_level
        )
        names = [
            r.security for r in rows
            if r.total_liquidation_preference > 0.0 and r.seniority == s_level
        ]
        claimant_str = "100% to " + ", ".join(names)
        desc = f"Tier {s_level} | Liquidation preference funded | Seniority tier {s_level} LP fully funded."
        events.append((ev, claimant_str, desc))

    # 2. Conversion, exercise, and participation cap thresholds
    for r in rows:
        k = r.total_liquidation_preference
        p = r.fully_diluted_shares
        ep = r.exercise_price
        q = r.participation.upper().startswith("Y")
        cm = r.cap_mult or 0.0
        n = r.security

        # Non-participating conversion
        if k > 0.0 and not q and p > 0.0:
            z = k / p
            eq_val = total_equity_at_threshold(rows, z)
            claimants = ", ".join(get_active_claimants(rows, z))
            desc = f"{n} | Conversion | Converts when as-converted value exceeds LP + accrued dividend (threshold ${z:,.2f} per common-equivalent share)."
            events.append((eq_val, claimants, desc))

        # Exercise of Options / Warrants
        if k == 0.0 and ep > 0.0 and p > 0.0:
            z = ep
            eq_val = total_equity_at_threshold(rows, z)
            claimants = ", ".join(get_active_claimants(rows, z))
            desc = f"{n} | Exercise | Exercisable above ${z:,.2f} per share."
            events.append((eq_val, claimants, desc))

        # Participating preferred with cap
        if k > 0.0 and q and cm > 0.0 and p > 0.0:
            z1 = max(0.0, (r.original_issue_price * r.shares * cm - k) / p)
            eq_val1 = total_equity_at_threshold(rows, z1)
            claimants1 = ", ".join(get_active_claimants(rows, z1 - 1e-6))
            desc1 = f"{n} | Participation cap | Stops incremental participation at {cm:.1f}x cap."
            events.append((eq_val1, claimants1, desc1))

            z2 = r.original_issue_price * r.shares * cm / p
            eq_val2 = total_equity_at_threshold(rows, z2)
            claimants2 = ", ".join(get_active_claimants(rows, z2 - 1e-6))
            desc2 = f"{n} | Post-cap conversion | As-converted value exceeds capped participation after ${z2:,.2f} per share."
            events.append((eq_val2, claimants2, desc2))

    # 3. Filter valid events and sort by equity value
    valid_events = [e for e in events if e[0] > 0.0 and float("-inf") < e[0] < float("inf")]
    valid_events.sort(key=lambda x: x[0])

    # 4. Consolidate events at the same equity value (to 2 decimal places)
    grouped: List[List[Any]] = []
    for ev_val, claimants, desc in valid_events:
        if grouped and round(grouped[-1][0] * 100) == round(ev_val * 100):
            existing_claimants = [c.strip() for c in grouped[-1][1].split(", ") if c.strip()]
            new_claimants = [c.strip() for c in claimants.split(", ") if c.strip()]
            combined_claimants = list(dict.fromkeys(existing_claimants + new_claimants))
            grouped[-1][1] = ", ".join(combined_claimants)
            grouped[-1][2] += " | " + desc
        else:
            grouped.append([ev_val, claimants, desc])

    # 5. Build BreakpointTier instances
    tiers: List[BreakpointTier] = []
    prev_eq = 0.0
    for i, (end_eq, claimants_str, desc) in enumerate(grouped):
        tier_num = i + 1
        width = end_eq - prev_eq
        tiers.append(BreakpointTier(
            tier=tier_num,
            start_equity=prev_eq,
            end_equity=end_eq,
            width=width,
            claimants_description=claimants_str,
            event_description=desc,
            is_thereafter=False
        ))
        prev_eq = end_eq

    return tiers
