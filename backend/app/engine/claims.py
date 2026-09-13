"""Claims allocation across breakpoint tiers.

Calculates sharing ratios and dollar claims per security for each tier.
"""

from typing import List, Dict
from .models import DerivedSecurity, BreakpointTier, ClaimTierAllocation


def allocate_claims_by_tier(
    rows: List[DerivedSecurity],
    breakpoints: List[BreakpointTier]
) -> List[ClaimTierAllocation]:
    """Computes claimant entitlement percentages and monetary claim amounts for each breakpoint tier."""
    names = [r.security for r in rows]
    tier_allocations: List[ClaimTierAllocation] = []

    for bp in breakpoints:
        from_eq = bp.start_equity
        to_eq = bp.end_equity
        width = max(0.0, to_eq - from_eq)

        # Parse claimant names from the breakpoint
        active_raw = bp.claimants_description.replace("100% to ", "").split(",")
        active_names = [x.strip() for x in active_raw if x.strip()]

        is_lp = "Liquidation preference funded" in bp.event_description

        # Determine basis (liquidation preference claim for LP tiers, fully diluted shares for equity tiers)
        basis: Dict[str, float] = {}
        for r in rows:
            if r.security in active_names:
                basis[r.security] = r.total_liquidation_preference if is_lp else r.fully_diluted_shares

        denominator = sum(basis.values())

        sharing_pcts: Dict[str, float] = {}
        dollar_claims: Dict[str, float] = {}

        for n in names:
            val = basis.get(n, 0.0)
            p = (val / denominator) if denominator > 0.0 else 0.0
            sharing_pcts[n] = p
            dollar_claims[n] = width * p

        tier_allocations.append(ClaimTierAllocation(
            tier=bp.tier,
            from_equity=from_eq,
            to_equity=to_eq,
            width=width,
            is_thereafter=bp.is_thereafter,
            sharing_percentages=sharing_pcts,
            dollar_claims=dollar_claims
        ))

    return tier_allocations

