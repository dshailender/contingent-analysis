"""Multi-fund portfolio aggregation, class ownership percentages, fair value, and MOIC.
"""

from typing import List, Dict
from .models import (
    HoldingInput,
    HoldingResultItem,
    FundSubtotal,
    HoldingsSummary,
    DerivedSecurity
)


def evaluate_client_holdings(
    holdings: List[HoldingInput],
    valuation_securities: List[DerivedSecurity],
    per_share_values: Dict[str, float]
) -> HoldingsSummary:
    """Evaluates client holdings across multiple funds and computes MOIC and ownership."""
    items: List[HoldingResultItem] = []

    sec_map = {r.security: r for r in valuation_securities}
    total_fd_shares = sum(r.fully_diluted_shares for r in valuation_securities)

    for h in holdings:
        sec_info = sec_map.get(h.security)
        ps_val = per_share_values.get(h.security, 0.0)
        tot_val = h.units * ps_val

        # Class ownership percentage
        if sec_info and sec_info.shares > 0.0:
            class_own = h.units / sec_info.shares
        else:
            class_own = 0.0

        # Fully diluted ownership percentage
        conv_ratio = sec_info.conversion_ratio if sec_info else 1.0
        as_converted = h.units * conv_ratio
        if total_fd_shares > 0.0:
            fd_own = as_converted / total_fd_shares
        else:
            fd_own = 0.0

        # MOIC = Concluded Value / Investment Cost
        moic = (tot_val / h.cost) if (h.cost > 0.0) else None

        items.append(HoldingResultItem(
            fund=h.fund,
            security=h.security,
            units=h.units,
            cost=h.cost,
            fair_value_per_share=ps_val,
            concluded_fair_value=tot_val,
            class_ownership_pct=class_own,
            fully_diluted_ownership_pct=fd_own,
            moic=moic
        ))

    # Compute fund-level subtotals
    funds_order = list(dict.fromkeys(h.fund for h in holdings))
    subtotals: List[FundSubtotal] = []

    for f_name in funds_order:
        f_items = [it for it in items if it.fund == f_name]
        f_units = sum(it.units for it in f_items)
        f_cost = sum(it.cost for it in f_items)
        f_val = sum(it.concluded_fair_value for it in f_items)
        f_moic = (f_val / f_cost) if f_cost > 0.0 else None

        subtotals.append(FundSubtotal(
            fund=f_name,
            total_units=f_units,
            total_cost=f_cost,
            total_value=f_val,
            moic=f_moic
        ))

    grand_cost = sum(it.cost for it in items)
    grand_val = sum(it.concluded_fair_value for it in items)
    grand_moic = (grand_val / grand_cost) if grand_cost > 0.0 else None

    return HoldingsSummary(
        items=items,
        fund_subtotals=subtotals,
        total_cost=grand_cost,
        total_value=grand_val,
        consolidated_moic=grand_moic
    )

