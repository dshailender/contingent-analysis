"""Server-side PDF generation engine using Playwright with mandatory
semi-transparent diagonal 'HIGHLY CONFIDENTIAL' watermark on every page.
Generates complete executive valuation exhibits (Cover, Index, Exhibits 1.0 to 11.0).
"""

import io
import asyncio
from typing import Optional, List, Dict
from playwright.async_api import async_playwright

from .models import (
    ValuationResponse,
    DerivedSecurity,
    BreakpointTier,
    ClaimTierAllocation,
    ComparativeWaterfallItem,
)


def render_pdf_html(res: ValuationResponse) -> str:
    """Renders comprehensive HTML for landscape executive valuation exhibit report."""
    curr_sym = "€" if res.report_currency == "EUR" else ("$" if res.report_currency == "USD" else "£")
    sec_sym = "$" if res.secondary_currency == "USD" else ("€" if res.secondary_currency == "EUR" else "£")

    def m_fmt(v: Optional[float], sym: str = curr_sym) -> str:
        if v is None:
            return "—"
        return f"{sym}{v:,.2f}"

    def p_fmt(v: Optional[float]) -> str:
        if v is None:
            return "—"
        return f"{v * 100.0:.2f}%"

    def n_fmt(v: Optional[float]) -> str:
        if v is None:
            return "—"
        return f"{v:,.0f}"

    def date_fmt(d: str) -> str:
        return d or "—"

    # Logo HTML
    logo_html = ""
    if res.firm_logo_base64:
        logo_html = f'<div style="margin-bottom: 20px;"><img src="{res.firm_logo_base64}" style="max-height: 48px; max-width: 220px; object-fit: contain; filter: brightness(0) invert(1);" alt="Firm Logo"></div>'
    else:
        logo_html = '<div style="margin-bottom: 16px; font-size: 11pt; font-weight: 700; letter-spacing: 0.15em; color: #A9D6CF; text-transform: uppercase;">CONTINGENT CLAIMS VALUATION ENGINE</div>'

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<style>
  @page {{
    size: 297mm 210mm landscape;
    margin: 8mm 10mm;
  }}
  * {{ box-sizing: border-box; }}
  body {{
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    color: #111827;
    background: #FFFFFF;
    margin: 0;
    padding: 0;
    font-size: 8.5pt;
    line-height: 1.35;
    -webkit-font-smoothing: antialiased;
  }}

  /* Tabular numbers for financial precision */
  table.data-table, .tabular, .kpi-val, td, th {{
    font-variant-numeric: tabular-nums lining-nums;
  }}

  /* Fixed repeating watermark on every single page */
  .watermark {{
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 9999;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    opacity: 0.045;
    transform: rotate(-30deg);
  }}
  .watermark-text {{
    font-size: 64pt;
    font-weight: 900;
    letter-spacing: 0.15em;
    color: #17242B;
    text-transform: uppercase;
    user-select: none;
    white-space: nowrap;
  }}
  .watermark-subtext {{
    font-size: 18pt;
    font-weight: 700;
    letter-spacing: 0.1em;
    color: #08615E;
    margin-top: 8px;
    text-transform: uppercase;
  }}

  /* Page break utilities */
  .page {{
    page-break-before: always;
    break-before: page;
    position: relative;
    min-height: 100%;
    padding-top: 4px;
  }}
  .page:first-of-type {{
    page-break-before: auto;
    break-before: auto;
  }}

  /* Cover styling */
  .cover-page {{
    display: flex;
    flex-direction: column;
    justify-content: center;
    height: 185mm;
    padding: 16mm 22mm;
    background: linear-gradient(135deg, #131b24 0%, #1e3a40 55%, #08615e 100%);
    color: #FFFFFF;
    border-radius: 10px;
  }}
  .cover-title {{
    font-size: 28pt;
    font-weight: 800;
    letter-spacing: -0.02em;
    margin-bottom: 8px;
    color: #FFFFFF;
  }}
  .cover-subtitle {{
    font-size: 13.5pt;
    color: #E6F4F2;
    margin-bottom: 36px;
    max-width: 850px;
    line-height: 1.4;
  }}
  .cover-meta-grid {{
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
    background: rgba(255, 255, 255, 0.08);
    padding: 20px 24px;
    border-radius: 8px;
    border: 1px solid rgba(255, 255, 255, 0.18);
  }}
  .cover-meta-item label {{
    display: block;
    font-size: 8pt;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: #A9D6CF;
    margin-bottom: 3px;
    font-weight: 600;
  }}
  .cover-meta-item value {{
    font-size: 12pt;
    font-weight: 700;
    color: #FFFFFF;
  }}

  /* Header Bar */
  .header-bar {{
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    border-bottom: 2px solid #08615E;
    padding-bottom: 5px;
    margin-bottom: 10px;
  }}
  .exhibit-num {{
    font-size: 9pt;
    font-weight: 800;
    color: #08615E;
    text-transform: uppercase;
    letter-spacing: 0.06em;
  }}
  .exhibit-title {{
    font-size: 13.5pt;
    font-weight: 800;
    color: #111827;
    margin: 1px 0 0;
  }}
  .header-meta {{
    text-align: right;
    font-size: 8pt;
    color: #4B5563;
    line-height: 1.35;
  }}

  /* Table styling */
  table.data-table {{
    width: 100%;
    border-collapse: collapse;
    font-size: 8pt;
    margin-top: 6px;
    margin-bottom: 8px;
  }}
  table.data-table th {{
    background: #1e3a40;
    color: #FFFFFF;
    font-weight: 700;
    padding: 5px 6px;
    text-align: right;
    border: 1px solid #14282c;
    font-size: 8pt;
    line-height: 1.25;
  }}
  table.data-table th:first-child {{
    text-align: left;
  }}
  table.data-table td {{
    padding: 3.5px 6px;
    border-bottom: 1px solid #D1D5DB;
    border-left: 1px solid #E5E7EB;
    border-right: 1px solid #E5E7EB;
    text-align: right;
    font-size: 8pt;
    white-space: nowrap;
  }}
  table.data-table td:first-child {{
    text-align: left;
    font-weight: 500;
  }}
  table.data-table tr:nth-child(even) {{
    background: #F9FAFB;
  }}
  table.data-table tr.total-row td {{
    font-weight: 800;
    background: #EDF6F4;
    border-top: 2px solid #08615E;
    border-bottom: 2px solid #08615E;
    color: #0F172A;
  }}

  .section-subtitle {{
    font-size: 9pt;
    font-weight: 700;
    color: #1e3a40;
    margin: 10px 0 3px;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }}

  .kpi-row {{
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;
    margin-bottom: 12px;
  }}
  .kpi-box {{
    background: #F4F8F7;
    border: 1px solid #DCE7E4;
    border-radius: 6px;
    padding: 6px 10px;
  }}
  .kpi-label {{
    font-size: 7.5pt;
    color: #4B5563;
    text-transform: uppercase;
    font-weight: 700;
  }}
  .kpi-val {{
    font-size: 12.5pt;
    font-weight: 800;
    color: #08615E;
    margin-top: 2px;
  }}
  .note-box {{
    margin-top: 8px;
    padding: 5px 8px;
    background: #F8FAFA;
    border-left: 3px solid #08615E;
    font-size: 7.5pt;
    color: #4B5563;
    font-style: italic;
    line-height: 1.35;
  }}
  .badge {{
    display: inline-block;
    padding: 2px 5px;
    border-radius: 3px;
    font-size: 7pt;
    font-weight: 700;
    background: #E8F2F8;
    color: #0F5F91;
  }}
</style>
</head>
<body>

<!-- Global Watermark -->
<div class="watermark">
  <div class="watermark-text">HIGHLY CONFIDENTIAL</div>
  <div class="watermark-subtext">{res.report_status} · {res.company_name}</div>
</div>

<!-- ========================================================================= -->
<!-- PAGE 1: COVER SHEET -->
<!-- ========================================================================= -->
<div class="page">
  <div class="cover-page">
    {logo_html}
    <div class="cover-title">CONTINGENT CLAIMS VALUATION REPORT</div>
    <div class="cover-subtitle">Option Pricing Method (OPM) Backsolve &amp; Allocation Analysis prepared for {res.client_name}</div>
    <div class="cover-meta-grid">
      <div class="cover-meta-item">
        <label>Subject Company</label>
        <value>{res.company_name}</value>
      </div>
      <div class="cover-meta-item">
        <label>Report Purpose</label>
        <value>{res.report_purpose}</value>
      </div>
      <div class="cover-meta-item">
        <label>Report Status</label>
        <value>{res.report_status}</value>
      </div>
      <div class="cover-meta-item">
        <label>Calibration Date</label>
        <value>{res.calibration_date}</value>
      </div>
      <div class="cover-meta-item">
        <label>Valuation Date</label>
        <value>{res.valuation_date}</value>
      </div>
      <div class="cover-meta-item">
        <label>Concluded Equity Value</label>
        <value>{m_fmt(res.concluded_equity_value)}</value>
      </div>
    </div>
  </div>
</div>

<!-- ========================================================================= -->
<!-- PAGE 2: INDEX & EXECUTIVE SUMMARY -->
<!-- ========================================================================= -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Executive Summary</div>
      <div class="exhibit-title">Index of Exhibits &amp; Key Valuation Parameters</div>
    </div>
    <div class="header-meta">
      {res.company_name} | {res.client_name}<br>Valuation Date: {res.valuation_date}
    </div>
  </div>

  <div class="kpi-row">
    <div class="kpi-box">
      <div class="kpi-label">Backsolved Equity (Calibration)</div>
      <div class="kpi-val">{m_fmt(res.calibration_solved_equity)}</div>
    </div>
    <div class="kpi-box">
      <div class="kpi-label">Concluded Equity (Valuation)</div>
      <div class="kpi-val">{m_fmt(res.concluded_equity_value)}</div>
    </div>
    <div class="kpi-box">
      <div class="kpi-label">Valuation Volatility</div>
      <div class="kpi-val">{p_fmt(res.valuation_opm.volatility / 100.0)}</div>
    </div>
    <div class="kpi-box">
      <div class="kpi-label">Time to Liquidity</div>
      <div class="kpi-val">{res.term_valuation:.2f} yrs</div>
    </div>
  </div>

  <table class="data-table">
    <thead>
      <tr>
        <th style="width: 12%;">Exhibit</th>
        <th style="text-align: left; width: 62%;">Description</th>
        <th style="width: 26%;">Scope / Effective Date</th>
      </tr>
    </thead>
    <tbody>
      <tr><td><b>Exhibit 1.0</b></td><td>Client Holdings Summary &amp; Concluded Portfolio Value</td><td>Valuation Date ({res.valuation_date})</td></tr>
      <tr><td><b>Exhibit 2.0</b></td><td>Capitalization Structure &amp; Security Terms</td><td>Calibration Date ({res.calibration_date})</td></tr>
      <tr><td><b>Exhibit 3.0</b></td><td>Breakpoint Schedule &amp; Claims Allocation Matrix</td><td>Calibration Date ({res.calibration_date})</td></tr>
      <tr><td><b>Exhibit 4.0</b></td><td>OPM Backsolve Mechanics &amp; Tranche Option Pricing</td><td>Calibration Date ({res.calibration_date})</td></tr>
      <tr><td><b>Exhibit 5.0</b></td><td>Risk-Free Rate Curve &amp; Interpolation Analysis</td><td>Calibration Date ({res.calibration_date})</td></tr>
      <tr><td><b>Exhibit 6.0</b></td><td>Capitalization Structure &amp; Security Terms</td><td>Valuation Date ({res.valuation_date})</td></tr>
      <tr><td><b>Exhibit 7.0</b></td><td>Breakpoint Schedule &amp; Claims Allocation Matrix</td><td>Valuation Date ({res.valuation_date})</td></tr>
      <tr><td><b>Exhibit 8.0</b></td><td>OPM Value Allocation Matrix &amp; Concluded Fair Values</td><td>Valuation Date ({res.valuation_date})</td></tr>
      <tr><td><b>Exhibit 9.0</b></td><td>Risk-Free Rate Curve &amp; Interpolation Analysis</td><td>Valuation Date ({res.valuation_date})</td></tr>
      <tr><td><b>Exhibit 10.0</b></td><td>Comparative Liquidation Waterfall Schedule</td><td>Side-by-Side (Calibration vs. Valuation)</td></tr>
    </tbody>
  </table>
  <div class="note-box">
    This valuation book presents the mathematical mechanics, security terms, breakpoint thresholds, Black-Scholes call option tranches, and liquidation proceeds allocations underlying the contingent claims analysis.
  </div>
</div>

<!-- ========================================================================= -->
<!-- PAGE 3: EXHIBIT 1.0 - CLIENT HOLDINGS SUMMARY -->
<!-- ========================================================================= -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 1.0</div>
      <div class="exhibit-title">Client Holdings and Value Conclusion — Valuation Date</div>
    </div>
    <div class="header-meta">
      {res.company_name} | {res.client_name}<br>As of {res.valuation_date}
    </div>
  </div>

  <table class="data-table">
    <thead>
      <tr>
        <th>Fund / Vehicle</th>
        <th>Security Class</th>
        <th>Units Held</th>
        <th>Investment Cost</th>
        <th>Fair Value / Share</th>
        <th>Concluded Fair Value</th>
        {'<th>Value (' + res.secondary_currency + ')</th>' if res.show_secondary_currency else ''}
        <th>Class Ownership</th>
        <th>FD Ownership</th>
        <th>MOIC</th>
      </tr>
    </thead>
    <tbody>
"""
    for it in res.holdings.items:
        moic_txt = f"{it.moic:.2f}x" if it.moic is not None else "—"
        sec_val_td = ""
        if res.show_secondary_currency:
            s_val = it.concluded_fair_value * res.secondary_fx_rate
            sec_val_td = f"<td>{m_fmt(s_val, sec_sym)}</td>"
        html += f"""
      <tr>
        <td>{it.fund}</td>
        <td>{it.security}</td>
        <td>{n_fmt(it.units)}</td>
        <td>{m_fmt(it.cost)}</td>
        <td>{m_fmt(it.fair_value_per_share)}</td>
        <td><b>{m_fmt(it.concluded_fair_value)}</b></td>
        {sec_val_td}
        <td>{p_fmt(it.class_ownership_pct)}</td>
        <td>{p_fmt(it.fully_diluted_ownership_pct)}</td>
        <td><b>{moic_txt}</b></td>
      </tr>
"""
    c_moic = f"{res.holdings.consolidated_moic:.2f}x" if res.holdings.consolidated_moic else "—"
    sec_tot_td = ""
    if res.show_secondary_currency:
        s_tot = res.holdings.total_value * res.secondary_fx_rate
        sec_tot_td = f"<td>{m_fmt(s_tot, sec_sym)}</td>"
    html += f"""
      <tr class="total-row">
        <td colspan="2">TOTAL CONSOLIDATED PORTFOLIO</td>
        <td>{n_fmt(sum(it.units for it in res.holdings.items))}</td>
        <td>{m_fmt(res.holdings.total_cost)}</td>
        <td>—</td>
        <td>{m_fmt(res.holdings.total_value)}</td>
        {sec_tot_td}
        <td>—</td>
        <td>—</td>
        <td>{c_moic}</td>
      </tr>
    </tbody>
  </table>
  <div class="note-box">
    MOIC equals concluded fair value divided by investment cost where cost is positive. Class ownership equals units held divided by class shares outstanding.
    {' Secondary currency conversion applied at FX rate of ' + str(res.secondary_fx_rate) + ' ' + res.secondary_currency + ' per ' + res.report_currency + '.' if res.show_secondary_currency else ''}
  </div>
</div>

<!-- ========================================================================= -->
<!-- PAGE 4: EXHIBIT 2.0 - CAPITALIZATION TABLE (CALIBRATION DATE) -->
<!-- ========================================================================= -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 2.0</div>
      <div class="exhibit-title">Capitalization Table and Structure — Calibration Date</div>
    </div>
    <div class="header-meta">
      {res.company_name} | {res.client_name}<br>Effective Date: {res.calibration_date}
    </div>
  </div>

  <table class="data-table">
    <thead>
      <tr>
        <th>Security Class</th>
        <th>Subtype</th>
        <th>Seniority</th>
        <th>Shares Outstanding</th>
        <th>Issue Price</th>
        <th>Liq Multiplier</th>
        <th>Pref / Share</th>
        <th>Total Preference</th>
        <th>Participation</th>
        <th>Conversion Ratio</th>
        <th>FD Shares</th>
      </tr>
    </thead>
    <tbody>
"""
    for sec in res.calibration_derived_securities:
        html += f"""
      <tr>
        <td>{sec.security}</td>
        <td>{sec.security_subtype}</td>
        <td>{sec.seniority}</td>
        <td>{n_fmt(sec.shares)}</td>
        <td>{m_fmt(sec.original_issue_price)}</td>
        <td>{sec.liquidation_multiplier:.2f}x</td>
        <td>{m_fmt(sec.liquidation_preference_per_share)}</td>
        <td>{m_fmt(sec.total_liquidation_preference)}</td>
        <td>{sec.participation}</td>
        <td>{sec.conversion_ratio:.4f}</td>
        <td>{n_fmt(sec.fully_diluted_shares)}</td>
      </tr>
"""
    tot_cal_shares = sum(s.shares for s in res.calibration_derived_securities)
    tot_cal_pref = sum(s.total_liquidation_preference for s in res.calibration_derived_securities)
    tot_cal_fd = sum(s.fully_diluted_shares for s in res.calibration_derived_securities)
    html += f"""
      <tr class="total-row">
        <td colspan="3">TOTAL CAPITAL STRUCTURE</td>
        <td>{n_fmt(tot_cal_shares)}</td>
        <td>—</td>
        <td>—</td>
        <td>—</td>
        <td>{m_fmt(tot_cal_pref)}</td>
        <td>—</td>
        <td>—</td>
        <td>{n_fmt(tot_cal_fd)}</td>
      </tr>
    </tbody>
  </table>
  <div class="note-box">
    Capitalization structure established as of transaction calibration date ({res.calibration_date}). Fully diluted shares reflect as-converted common shares and exercisable option positions.
  </div>
</div>

<!-- ========================================================================= -->
<!-- PAGE 5: EXHIBIT 3.0 - BREAKPOINTS & CLAIMS MATRIX (CALIBRATION DATE) -->
<!-- ========================================================================= -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 3.0</div>
      <div class="exhibit-title">Breakpoint Schedule &amp; Claims Matrix — Calibration Date</div>
    </div>
    <div class="header-meta">
      {res.company_name} | {res.client_name}<br>As of {res.calibration_date}
    </div>
  </div>

  <div class="section-subtitle">Breakpoint Equity Thresholds</div>
  <table class="data-table">
    <thead>
      <tr>
        <th style="width: 8%;">Tier</th>
        <th style="width: 18%;">Start Equity</th>
        <th style="width: 18%;">End Equity</th>
        <th style="width: 16%;">Tranche Width</th>
        <th style="width: 40%;">Trigger Event / Participating Securities</th>
      </tr>
    </thead>
    <tbody>
"""
    for bp in res.calibration_breakpoints:
        end_str = "Thereafter" if bp.is_thereafter else m_fmt(bp.end_equity)
        html += f"""
      <tr>
        <td><span class="badge">Tier {bp.tier}</span></td>
        <td>{m_fmt(bp.start_equity)}</td>
        <td>{end_str}</td>
        <td>{m_fmt(bp.width) if not bp.is_thereafter else '—'}</td>
        <td style="text-align: left;">{bp.event_description or bp.claimants_description}</td>
      </tr>
"""
    html += """
    </tbody>
  </table>

  <div class="section-subtitle">Claims Allocation Percentage Matrix by Breakpoint Tier</div>
  <table class="data-table">
    <thead>
      <tr>
        <th style="width: 8%;">Tier</th>
        <th style="width: 16%;">Range</th>
"""
    for sec in res.calibration_derived_securities:
        html += f"<th>{sec.security}</th>"
    html += """
      </tr>
    </thead>
    <tbody>
"""
    for cl in res.calibration_claims:
        rng = f"{m_fmt(cl.from_equity)} – {'Thereafter' if cl.is_thereafter else m_fmt(cl.to_equity)}"
        html += f"""
      <tr>
        <td><span class="badge">Tier {cl.tier}</span></td>
        <td style="text-align: left;">{rng}</td>
"""
        for sec in res.calibration_derived_securities:
            pct = cl.sharing_percentages.get(sec.security, 0.0)
            val_td = f"{pct * 100.0:.1f}%" if pct > 0 else "—"
            html += f"<td>{val_td}</td>"
        html += "</tr>"

    html += f"""
    </tbody>
  </table>
  <div class="note-box">
    Breakpoints delineate incremental cumulative enterprise equity value levels where the economic sharing rights among preferred and common classes transition according to contractual terms.
  </div>
</div>

<!-- ========================================================================= -->
<!-- PAGE 6: EXHIBIT 4.0 - OPM BACKSOLVE MECHANICS (CALIBRATION DATE) -->
<!-- ========================================================================= -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 4.0</div>
      <div class="exhibit-title">OPM Backsolve Mechanics &amp; Value Allocation — Calibration Date</div>
    </div>
    <div class="header-meta">
      Transaction Price: {m_fmt(res.calibration_opm.per_share_values.get(res.calibration_derived_securities[0].security, 0.0))}<br>
      Backsolved Equity: {m_fmt(res.calibration_solved_equity)}
    </div>
  </div>

  <div class="section-subtitle">Black-Scholes Call Option Tranche Pricing</div>
  <table class="data-table">
    <thead>
      <tr>
        <th>Tranche</th>
        <th>Lower Strike (Xk-1)</th>
        <th>Upper Strike (Xk)</th>
        <th>Call Price C(Xk-1)</th>
        <th>Call Price C(Xk)</th>
        <th>Incremental Value (ΔCk)</th>
      </tr>
    </thead>
    <tbody>
"""
    for tr in res.calibration_opm.tranches:
        u_str = "∞" if tr.strike_high > 1e12 else m_fmt(tr.strike_high)
        c_str = "0.00" if tr.strike_high > 1e12 else m_fmt(tr.call_high)
        html += f"""
      <tr>
        <td><span class="badge">Tranche {tr.tier}</span></td>
        <td>{m_fmt(tr.strike_low)}</td>
        <td>{u_str}</td>
        <td>{m_fmt(tr.call_low)}</td>
        <td>{c_str}</td>
        <td><b>{m_fmt(tr.incremental_call)}</b></td>
      </tr>
"""
    html += f"""
      <tr class="total-row">
        <td colspan="5">TOTAL OPTION VALUE (BACKSOLVED EQUITY)</td>
        <td>{m_fmt(res.calibration_solved_equity)}</td>
      </tr>
    </tbody>
  </table>

  <div class="section-subtitle">Security-by-Security Allocated Value &amp; Per-Share Metrics</div>
  <table class="data-table">
    <thead>
      <tr>
        <th>Security Class</th>
        <th>Shares Outstanding</th>
        <th>Total Allocated Value</th>
        <th>Value / Share</th>
        <th>% of Total Equity</th>
        <th>Fully Diluted Shares</th>
      </tr>
    </thead>
    <tbody>
"""
    for sec in res.calibration_derived_securities:
        tot_v = res.calibration_opm.allocated_values.get(sec.security, 0.0)
        ps_v = res.calibration_opm.per_share_values.get(sec.security, 0.0)
        pct_v = res.calibration_opm.percent_allocations.get(sec.security, 0.0)
        html += f"""
      <tr>
        <td>{sec.security}</td>
        <td>{n_fmt(sec.shares)}</td>
        <td>{m_fmt(tot_v)}</td>
        <td><b>{m_fmt(ps_v)}</b></td>
        <td>{p_fmt(pct_v)}</td>
        <td>{n_fmt(sec.fully_diluted_shares)}</td>
      </tr>
"""
    html += f"""
      <tr class="total-row">
        <td>TOTAL CONCLUDED BACKSOLVE</td>
        <td>{n_fmt(tot_cal_shares)}</td>
        <td>{m_fmt(res.calibration_opm.total_allocated)}</td>
        <td>—</td>
        <td>100.0%</td>
        <td>{n_fmt(tot_cal_fd)}</td>
      </tr>
    </tbody>
  </table>
  <div class="note-box">
    Backsolve solved enterprise equity value of {m_fmt(res.calibration_solved_equity)} such that the per-share value of the calibration security matches the observed transaction pricing at rf = {res.rf_calibration_effective:.2f}% and volatility = {res.calibration_opm.volatility:.1f}%.
  </div>
</div>

<!-- ========================================================================= -->
<!-- PAGE 7: EXHIBIT 5.0 - RISK-FREE RATE ANALYSIS (CALIBRATION DATE) -->
<!-- ========================================================================= -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 5.0</div>
      <div class="exhibit-title">Risk-Free Rate Curve &amp; Interpolation Analysis — Calibration Date</div>
    </div>
    <div class="header-meta">
      {res.company_name} | {res.client_name}<br>Measurement Date: {res.calibration_date}
    </div>
  </div>
"""
    cal_rf = res.calibration_rf_analysis
    if cal_rf and cal_rf.curve_points:
        html += """
  <div class="section-subtitle">Published Sovereign Yield Curve Observations</div>
  <table class="data-table">
    <thead>
      <tr>
        <th>Tenor / Benchmark Maturity</th>
        <th>Published Yield to Maturity</th>
        <th>Term (Years)</th>
      </tr>
    </thead>
    <tbody>
"""
        for pt in cal_rf.curve_points:
            html += f"""
      <tr>
        <td>{pt.tenor_name}</td>
        <td>{pt.rate_percent:.2f}%</td>
        <td>{pt.tenor_years:.2f}</td>
      </tr>
"""
        html += f"""
    </tbody>
  </table>

  <div class="section-subtitle">Interpolation to Expected Term to Exit</div>
  <table class="data-table">
    <thead>
      <tr>
        <th>Interpolation Parameter</th>
        <th>Yield Rate</th>
        <th>Term (Years)</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td>Expected Term to Liquidity Exit</td>
        <td>—</td>
        <td><b>{cal_rf.term_years:.4f}</b></td>
      </tr>
      <tr>
        <td>Concluded Annual Effective Risk-Free Rate</td>
        <td><b>{cal_rf.interpolated_annual_effective_rate * 100.0:.4f}%</b></td>
        <td>{cal_rf.term_years:.4f}</td>
      </tr>
      <tr>
        <td>Continuous Risk-Free Rate (Used in OPM)</td>
        <td><b>{cal_rf.continuous_rate * 100.0:.4f}%</b></td>
        <td>{cal_rf.term_years:.4f}</td>
      </tr>
    </tbody>
  </table>
  <div class="note-box">
    {cal_rf.interpolation_metadata or 'Yield rate linearly interpolated between nearest benchmark points. Continuous rate rc = ln(1 + r_eff).'}
  </div>
"""
    else:
        html += f"""
  <table class="data-table">
    <thead><tr><th>Parameter</th><th>Value</th></tr></thead>
    <tbody>
      <tr><td>Source</td><td>Valuer Specified Manual Input</td></tr>
      <tr><td>Measurement Date</td><td>{res.calibration_date}</td></tr>
      <tr><td>Expected Exit Date</td><td>{res.exit_date}</td></tr>
      <tr><td>Expected Term to Exit</td><td>{res.term_calibration:.4f} years</td></tr>
      <tr><td>Annual Effective Risk-Free Rate</td><td><b>{res.rf_calibration_effective:.4f}%</b></td></tr>
      <tr><td>Continuous Risk-Free Rate Used in OPM</td><td><b>{res.rf_calibration_continuous:.4f}%</b></td></tr>
    </tbody>
  </table>
  <div class="note-box">
    Annual effective risk-free rate converted to continuous rate for Black-Scholes formula: rc = ln(1 + r_eff).
  </div>
"""
    html += "</div>"

    # =========================================================================
    # PAGE 8: EXHIBIT 6.0 - CAPITALIZATION TABLE (VALUATION DATE)
    # =========================================================================
    html += f"""
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 6.0</div>
      <div class="exhibit-title">Capitalization Table and Structure — Valuation Date</div>
    </div>
    <div class="header-meta">
      {res.company_name} | {res.client_name}<br>Valuation Date: {res.valuation_date}
    </div>
  </div>

  <table class="data-table">
    <thead>
      <tr>
        <th>Security Class</th>
        <th>Subtype</th>
        <th>Seniority</th>
        <th>Shares Outstanding</th>
        <th>Issue Price</th>
        <th>Liq Multiplier</th>
        <th>Pref / Share</th>
        <th>Total Preference</th>
        <th>Participation</th>
        <th>Conversion Ratio</th>
        <th>FD Shares</th>
      </tr>
    </thead>
    <tbody>
"""
    for sec in res.valuation_derived_securities:
        html += f"""
      <tr>
        <td>{sec.security}</td>
        <td>{sec.security_subtype}</td>
        <td>{sec.seniority}</td>
        <td>{n_fmt(sec.shares)}</td>
        <td>{m_fmt(sec.original_issue_price)}</td>
        <td>{sec.liquidation_multiplier:.2f}x</td>
        <td>{m_fmt(sec.liquidation_preference_per_share)}</td>
        <td>{m_fmt(sec.total_liquidation_preference)}</td>
        <td>{sec.participation}</td>
        <td>{sec.conversion_ratio:.4f}</td>
        <td>{n_fmt(sec.fully_diluted_shares)}</td>
      </tr>
"""
    tot_val_shares = sum(s.shares for s in res.valuation_derived_securities)
    tot_val_pref = sum(s.total_liquidation_preference for s in res.valuation_derived_securities)
    tot_val_fd = sum(s.fully_diluted_shares for s in res.valuation_derived_securities)
    html += f"""
      <tr class="total-row">
        <td colspan="3">TOTAL CAPITAL STRUCTURE</td>
        <td>{n_fmt(tot_val_shares)}</td>
        <td>—</td>
        <td>—</td>
        <td>—</td>
        <td>{m_fmt(tot_val_pref)}</td>
        <td>—</td>
        <td>—</td>
        <td>{n_fmt(tot_val_fd)}</td>
      </tr>
    </tbody>
  </table>
  <div class="note-box">
    Capitalization structure as of valuation measurement date ({res.valuation_date}). Accommodates secondary share issuances, anti-dilution adjustments, and vesting modifications.
  </div>
</div>

<!-- ========================================================================= -->
<!-- PAGE 9: EXHIBIT 7.0 - BREAKPOINTS & CLAIMS MATRIX (VALUATION DATE) -->
<!-- ========================================================================= -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 7.0</div>
      <div class="exhibit-title">Breakpoint Schedule &amp; Claims Matrix — Valuation Date</div>
    </div>
    <div class="header-meta">
      {res.company_name} | {res.client_name}<br>As of {res.valuation_date}
    </div>
  </div>

  <div class="section-subtitle">Breakpoint Equity Thresholds</div>
  <table class="data-table">
    <thead>
      <tr>
        <th style="width: 8%;">Tier</th>
        <th style="width: 18%;">Start Equity</th>
        <th style="width: 18%;">End Equity</th>
        <th style="width: 16%;">Tranche Width</th>
        <th style="width: 40%;">Trigger Event / Participating Securities</th>
      </tr>
    </thead>
    <tbody>
"""
    for bp in res.valuation_breakpoints:
        end_str = "Thereafter" if bp.is_thereafter else m_fmt(bp.end_equity)
        html += f"""
      <tr>
        <td><span class="badge">Tier {bp.tier}</span></td>
        <td>{m_fmt(bp.start_equity)}</td>
        <td>{end_str}</td>
        <td>{m_fmt(bp.width) if not bp.is_thereafter else '—'}</td>
        <td style="text-align: left;">{bp.event_description or bp.claimants_description}</td>
      </tr>
"""
    html += """
    </tbody>
  </table>

  <div class="section-subtitle">Claims Allocation Percentage Matrix by Breakpoint Tier</div>
  <table class="data-table">
    <thead>
      <tr>
        <th style="width: 8%;">Tier</th>
        <th style="width: 16%;">Range</th>
"""
    for sec in res.valuation_derived_securities:
        html += f"<th>{sec.security}</th>"
    html += """
      </tr>
    </thead>
    <tbody>
"""
    for cl in res.valuation_claims:
        rng = f"{m_fmt(cl.from_equity)} – {'Thereafter' if cl.is_thereafter else m_fmt(cl.to_equity)}"
        html += f"""
      <tr>
        <td><span class="badge">Tier {cl.tier}</span></td>
        <td style="text-align: left;">{rng}</td>
"""
        for sec in res.valuation_derived_securities:
            pct = cl.sharing_percentages.get(sec.security, 0.0)
            val_td = f"{pct * 100.0:.1f}%" if pct > 0 else "—"
            html += f"<td>{val_td}</td>"
        html += "</tr>"

    html += f"""
    </tbody>
  </table>
  <div class="note-box">
    Claims allocation matrix sets forth each share class's marginal participation rate across each breakpoint tier as governed by liquidation preference seniority and conversion parity.
  </div>
</div>

<!-- ========================================================================= -->
<!-- PAGE 10: EXHIBIT 8.0 - OPM ALLOCATION MATRIX (VALUATION DATE) -->
<!-- ========================================================================= -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 8.0</div>
      <div class="exhibit-title">Option Pricing Method (OPM) Value Allocation — Valuation Date</div>
    </div>
    <div class="header-meta">
      Concluded Equity: {m_fmt(res.concluded_equity_value)}<br>Effective Date: {res.valuation_date}
    </div>
  </div>

  <div class="section-subtitle">Call Option Tranche Pricing at Concluded Equity</div>
  <table class="data-table">
    <thead>
      <tr>
        <th>Tranche</th>
        <th>Lower Strike (Xk-1)</th>
        <th>Upper Strike (Xk)</th>
        <th>Call Price C(Xk-1)</th>
        <th>Call Price C(Xk)</th>
        <th>Incremental Value (ΔCk)</th>
      </tr>
    </thead>
    <tbody>
"""
    for tr in res.valuation_opm.tranches:
        u_str = "∞" if tr.strike_high > 1e12 else m_fmt(tr.strike_high)
        c_str = "0.00" if tr.strike_high > 1e12 else m_fmt(tr.call_high)
        html += f"""
      <tr>
        <td><span class="badge">Tranche {tr.tier}</span></td>
        <td>{m_fmt(tr.strike_low)}</td>
        <td>{u_str}</td>
        <td>{m_fmt(tr.call_low)}</td>
        <td>{c_str}</td>
        <td><b>{m_fmt(tr.incremental_call)}</b></td>
      </tr>
"""
    html += f"""
      <tr class="total-row">
        <td colspan="5">TOTAL CONCLUDED EQUITY ALLOCATED</td>
        <td>{m_fmt(res.valuation_opm.total_allocated)}</td>
      </tr>
    </tbody>
  </table>

  <div class="section-subtitle">Concluded Fair Values per Share Class</div>
  <table class="data-table">
    <thead>
      <tr>
        <th>Share Class</th>
        <th>Subtype</th>
        <th>Shares Outstanding</th>
        <th>Concluded Value</th>
        {'<th>Value (' + res.secondary_currency + ')</th>' if res.show_secondary_currency else ''}
        <th>Value / Share</th>
        {'<th>Per Share (' + res.secondary_currency + ')</th>' if res.show_secondary_currency else ''}
        <th>% of Total Equity</th>
        <th>Fully Diluted Shares</th>
      </tr>
    </thead>
    <tbody>
"""
    for sec in res.valuation_derived_securities:
        tot_v = res.valuation_opm.allocated_values.get(sec.security, 0.0)
        ps_v = res.valuation_opm.per_share_values.get(sec.security, 0.0)
        pct_v = res.valuation_opm.percent_allocations.get(sec.security, 0.0)
        sec_col1 = ""
        sec_col2 = ""
        if res.show_secondary_currency:
            s_val = tot_v * res.secondary_fx_rate
            s_ps = ps_v * res.secondary_fx_rate
            sec_col1 = f"<td>{m_fmt(s_val, sec_sym)}</td>"
            sec_col2 = f"<td><b>{m_fmt(s_ps, sec_sym)}</b></td>"
        html += f"""
      <tr>
        <td>{sec.security}</td>
        <td>{sec.security_subtype}</td>
        <td>{n_fmt(sec.shares)}</td>
        <td>{m_fmt(tot_v)}</td>
        {sec_col1}
        <td><b>{m_fmt(ps_v)}</b></td>
        {sec_col2}
        <td>{p_fmt(pct_v)}</td>
        <td>{n_fmt(sec.fully_diluted_shares)}</td>
      </tr>
"""
    sec_tot1 = f"<td>{m_fmt(res.valuation_opm.total_allocated * res.secondary_fx_rate, sec_sym)}</td>" if res.show_secondary_currency else ""
    sec_tot2 = "<td>—</td>" if res.show_secondary_currency else ""
    html += f"""
      <tr class="total-row">
        <td colspan="2">TOTAL CONCLUDED EQUITY</td>
        <td>{n_fmt(tot_val_shares)}</td>
        <td>{m_fmt(res.valuation_opm.total_allocated)}</td>
        {sec_tot1}
        <td>—</td>
        {sec_tot2}
        <td>100.0%</td>
        <td>{n_fmt(tot_val_fd)}</td>
      </tr>
    </tbody>
  </table>
  <div class="note-box">
    Allocated via incremental Black-Scholes call option tranches across {len(res.valuation_breakpoints)} breakpoint intervals at risk-free rate rc = {res.valuation_opm.risk_free_rate_continuous * 100.0:.4f}% and volatility = {res.valuation_opm.volatility:.1f}%.
  </div>
</div>

<!-- ========================================================================= -->
<!-- PAGE 11: EXHIBIT 9.0 - RISK-FREE RATE ANALYSIS (VALUATION DATE) -->
<!-- ========================================================================= -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 9.0</div>
      <div class="exhibit-title">Risk-Free Rate Curve &amp; Interpolation Analysis — Valuation Date</div>
    </div>
    <div class="header-meta">
      {res.company_name} | {res.client_name}<br>Measurement Date: {res.valuation_date}
    </div>
  </div>
"""
    val_rf = res.valuation_rf_analysis
    if val_rf and val_rf.curve_points:
        html += """
  <div class="section-subtitle">Published Sovereign Yield Curve Observations</div>
  <table class="data-table">
    <thead>
      <tr>
        <th>Tenor / Benchmark Maturity</th>
        <th>Published Yield to Maturity</th>
        <th>Term (Years)</th>
      </tr>
    </thead>
    <tbody>
"""
        for pt in val_rf.curve_points:
            html += f"""
      <tr>
        <td>{pt.tenor_name}</td>
        <td>{pt.rate_percent:.2f}%</td>
        <td>{pt.tenor_years:.2f}</td>
      </tr>
"""
        html += f"""
    </tbody>
  </table>

  <div class="section-subtitle">Interpolation to Expected Term to Exit</div>
  <table class="data-table">
    <thead>
      <tr>
        <th>Interpolation Parameter</th>
        <th>Yield Rate</th>
        <th>Term (Years)</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td>Expected Term to Liquidity Exit</td>
        <td>—</td>
        <td><b>{val_rf.term_years:.4f}</b></td>
      </tr>
      <tr>
        <td>Concluded Annual Effective Risk-Free Rate</td>
        <td><b>{val_rf.interpolated_annual_effective_rate * 100.0:.4f}%</b></td>
        <td>{val_rf.term_years:.4f}</td>
      </tr>
      <tr>
        <td>Continuous Risk-Free Rate (Used in OPM)</td>
        <td><b>{val_rf.continuous_rate * 100.0:.4f}%</b></td>
        <td>{val_rf.term_years:.4f}</td>
      </tr>
    </tbody>
  </table>
  <div class="note-box">
    {val_rf.interpolation_metadata or 'Linear interpolation between closest maturities. Continuous rate rc = ln(1 + r_eff).'}
  </div>
"""
    else:
        html += f"""
  <table class="data-table">
    <thead><tr><th>Parameter</th><th>Value</th></tr></thead>
    <tbody>
      <tr><td>Source</td><td>Valuer Specified Manual Input</td></tr>
      <tr><td>Measurement Date</td><td>{res.valuation_date}</td></tr>
      <tr><td>Expected Exit Date</td><td>{res.exit_date}</td></tr>
      <tr><td>Expected Term to Exit</td><td>{res.term_valuation:.4f} years</td></tr>
      <tr><td>Annual Effective Risk-Free Rate</td><td><b>{res.rf_valuation_effective:.4f}%</b></td></tr>
      <tr><td>Continuous Risk-Free Rate Used in OPM</td><td><b>{res.rf_valuation_continuous:.4f}%</b></td></tr>
    </tbody>
  </table>
  <div class="note-box">
    Annual effective risk-free rate converted to continuous rate for Black-Scholes formula: rc = ln(1 + r_eff).
  </div>
"""
    html += "</div>"

    # =========================================================================
    # PAGE 12: EXHIBIT 10.0 - COMPARATIVE WATERFALL ANALYSIS
    # =========================================================================
    html += f"""
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 10.0</div>
      <div class="exhibit-title">Comparative Liquidation Waterfall Schedule</div>
    </div>
    <div class="header-meta">
      Applied Waterfall Equity: {m_fmt(res.waterfall.applied_equity)}<br>
      Calibration vs. Valuation Parity
    </div>
  </div>

  <table class="data-table">
    <thead>
      <tr>
        <th rowspan="2" style="text-align: left; vertical-align: middle;">Security Class</th>
        <th colspan="3" style="text-align: center; background: #1F363C; border-bottom: 1px solid #3B5B63;">Valuation Date Distribution</th>
        <th colspan="3" style="text-align: center; background: #28444A; border-bottom: 1px solid #3B5B63;">Calibration Date Structure</th>
        <th colspan="2" style="text-align: center; background: #135754; border-bottom: 1px solid #3B5B63;">Variance / Change</th>
      </tr>
      <tr>
        <th>Val Shares</th>
        <th>Distribution</th>
        <th>Per Share</th>
        <th>Cal Shares</th>
        <th>Distribution</th>
        <th>Per Share</th>
        <th>Δ Proceeds</th>
        <th>% Change</th>
      </tr>
    </thead>
    <tbody>
"""
    comp_items = res.comparative_waterfall
    for row in comp_items:
        pct_chg_str = "—"
        if row.cal_distribution and row.cal_distribution > 0 and row.change_in_distribution is not None:
            pct_chg = (row.change_in_distribution / row.cal_distribution) * 100.0
            pct_chg_str = f"{pct_chg:+.1f}%"
        elif row.change_in_distribution is not None and row.change_in_distribution != 0:
            pct_chg_str = "New"

        chg_str = m_fmt(row.change_in_distribution) if row.change_in_distribution is not None else "—"
        html += f"""
      <tr>
        <td>{row.security}</td>
        <td>{n_fmt(row.val_shares)}</td>
        <td><b>{m_fmt(row.val_distribution)}</b></td>
        <td>{m_fmt(row.val_per_share)}</td>
        <td>{n_fmt(row.cal_shares)}</td>
        <td>{m_fmt(row.cal_distribution)}</td>
        <td>{m_fmt(row.cal_per_share)}</td>
        <td>{chg_str}</td>
        <td><b>{pct_chg_str}</b></td>
      </tr>
"""
    tot_val_dist = sum(r.val_distribution or 0.0 for r in comp_items)
    tot_cal_dist = sum(r.cal_distribution or 0.0 for r in comp_items)
    tot_chg = tot_val_dist - tot_cal_dist
    html += f"""
      <tr class="total-row">
        <td>TOTAL WATERFALL ALLOCATION</td>
        <td>{n_fmt(tot_val_shares)}</td>
        <td>{m_fmt(tot_val_dist)}</td>
        <td>—</td>
        <td>{n_fmt(tot_cal_shares)}</td>
        <td>{m_fmt(tot_cal_dist)}</td>
        <td>—</td>
        <td>{m_fmt(tot_chg)}</td>
        <td>—</td>
      </tr>
    </tbody>
  </table>
  <div class="note-box">
    Comparative waterfall models instantaneous contractual priority distributions under selected equity proceeds of {m_fmt(res.waterfall.applied_equity)}. Distinguishes contractual liquidation rights from optionality captured in OPM.
  </div>
</div>

</body>
</html>
"""
    return html


async def generate_valuation_pdf(response: ValuationResponse) -> bytes:
    """Generates server-side PDF with visible diagonal HIGHLY CONFIDENTIAL watermark
    on every page using Playwright Chromium headless.
    """
    html_content = render_pdf_html(response)

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page()

        await page.set_content(html_content, wait_until="networkidle")

        pdf_bytes = await page.pdf(
            format="A4",
            landscape=True,
            print_background=True,
            margin={"top": "8mm", "bottom": "8mm", "left": "10mm", "right": "10mm"},
            display_header_footer=True,
            header_template="""
            <div style="font-size: 7.5pt; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; width: 100%; padding: 0 10mm; display: flex; justify-content: space-between; color: #64748b; font-weight: 600;">
              <span style="letter-spacing: 0.08em; text-transform: uppercase;">HIGHLY CONFIDENTIAL — VALUATION EXHIBIT REPORT</span>
              <span>CONTINGENT CLAIMS ANALYSIS (OPM)</span>
            </div>
            """,
            footer_template="""
            <div style="font-size: 7.5pt; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; width: 100%; padding: 0 10mm; display: flex; justify-content: space-between; color: #64748b;">
              <span>AICPA / ASC 718 / ASC 820 VALUATION WORKSPACE</span>
              <span>Page <span class="pageNumber"></span> of <span class="totalPages"></span></span>
            </div>
            """
        )

        await browser.close()
        return pdf_bytes
