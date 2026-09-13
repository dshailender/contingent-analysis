"""Sequential scenario waterfall allocation at a specified enterprise equity value.
"""

from typing import List, Dict
from .models import DerivedSecurity, BreakpointTier, WaterfallResult, WaterfallAllocationItem, ComparativeWaterfallItem
from .claims import allocate_claims_by_tier


def allocate_waterfall(
    rows: List[DerivedSecurity],
    breakpoints: List[BreakpointTier],
    equity_value: float
) -> WaterfallResult:
    """Distributes an arbitrary scenario equity value through breakpoint tiers.

    Proceeds are allocated sequentially through senior preference tiers,
    participation ranges, and option exercises, with residual equity distributed
    pro-rata by fully diluted shares.
    """
    equity = max(0.0, float(equity_value))
    tier_claims = allocate_claims_by_tier(rows, breakpoints)

    proceeds_map: Dict[str, float] = {r.security: 0.0 for r in rows}

    # Allocate across finite breakpoint intervals
    for tr in tier_claims:
        from_eq = tr.from_equity
        to_eq = tr.to_equity
        if equity <= from_eq:
            continue

        available_width = max(0.0, min(equity, to_eq) - from_eq)
        for sec, p in tr.sharing_percentages.items():
            proceeds_map[sec] += available_width * p

    # Allocate residual equity above the last breakpoint (Thereafter tier)
    if breakpoints:
        last_bp_eq = breakpoints[-1].end_equity
        if equity > last_bp_eq:
            excess = equity - last_bp_eq
            total_fd = sum(r.fully_diluted_shares for r in rows)
            if total_fd > 0.0:
                for r in rows:
                    proceeds_map[r.security] += excess * (r.fully_diluted_shares / total_fd)

    total_proceeds = sum(proceeds_map.values())

    items: List[WaterfallAllocationItem] = []
    for r in rows:
        sec = r.security
        proc = proceeds_map[sec]
        sh = r.shares
        ps = (proc / sh) if sh > 0.0 else 0.0

        # Percent recovery against liquidation claim (for preferred)
        pref = r.total_liquidation_preference
        if pref > 0.0:
            rec_pct = proc / pref
        else:
            rec_pct = 1.0 if proc > 0.0 else 0.0

        tot_pct = (proc / total_proceeds) if total_proceeds > 0.0 else 0.0

        items.append(WaterfallAllocationItem(
            security=sec,
            shares=sh,
            proceeds=proc,
            proceeds_per_share=ps,
            percent_recovery=rec_pct,
            percent_of_total=tot_pct
        ))

    return WaterfallResult(
        applied_equity=equity,
        distribution=items,
        total_proceeds=total_proceeds
    )


def build_comparative_waterfall(
    cal_rows: List[DerivedSecurity],
    val_rows: List[DerivedSecurity],
    cal_waterfall: WaterfallResult,
    val_waterfall: WaterfallResult
) -> List[ComparativeWaterfallItem]:
    """Generates the side-by-side comparative distribution schedule comparing Calibration
    Date and Valuation Date structures at the identical scenario equity value.
    """
    cal_map = {r.security: r for r in cal_rows}
    val_map = {r.security: r for r in val_rows}
    cal_dist_map = {it.security: it for it in cal_waterfall.distribution}
    val_dist_map = {it.security: it for it in val_waterfall.distribution}

    cal_fd_tot = sum(r.fully_diluted_shares for r in cal_rows)
    val_fd_tot = sum(r.fully_diluted_shares for r in val_rows)

    names: List[str] = []
    seen = set()
    for r in val_rows:
        if r.security not in seen:
            seen.add(r.security)
            names.append(r.security)
    for r in cal_rows:
        if r.security not in seen:
            seen.add(r.security)
            names.append(r.security)

    items: List[ComparativeWaterfallItem] = []
    for n in names:
        rc = cal_map.get(n)
        rv = val_map.get(n)
        dc = cal_dist_map.get(n)
        dv = val_dist_map.get(n)

        vc = dc.proceeds if dc else None
        vv = dv.proceeds if dv else None
        delta = (vv if vv is not None else 0.0) - (vc if vc is not None else 0.0)

        cal_sh = rc.shares if rc else None
        cal_fd = (rc.fully_diluted_shares / cal_fd_tot) if rc and cal_fd_tot > 0 else None
        cal_ps = (vc / rc.shares) if rc and rc.shares > 0 and vc is not None else None

        val_sh = rv.shares if rv else None
        val_fd = (rv.fully_diluted_shares / val_fd_tot) if rv and val_fd_tot > 0 else None
        val_ps = (vv / rv.shares) if rv and rv.shares > 0 and vv is not None else None

        items.append(ComparativeWaterfallItem(
            security=n,
            cal_shares=cal_sh,
            cal_fd_ownership=cal_fd,
            val_shares=val_sh,
            val_fd_ownership=val_fd,
            cal_distribution=vc,
            cal_per_share=cal_ps,
            val_distribution=vv,
            val_per_share=val_ps,
            change_in_distribution=delta
        ))

    return items

