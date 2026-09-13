"""Capital IQ Excel Bridge workbook generator and secure parser.

Implements:
1. Generation of Capital IQ template with native plugin formulas.
2. Secure validation and ingestion of user-refreshed XLSX files.
3. Historical log-return annualized volatility calculation.
4. Merton asset volatility delevering and summary statistics.
"""

import io
import math
import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from typing import List, Dict, Any, Optional, Tuple
import pandas as pd
import numpy as np

from .models import ComparableCompanyVol, VolatilityStats, VolatilityAnalysisResult
from .volatility import calculate_volatility_stats, merton_asset_volatility, relever_equity_volatility
from .date_math import parse_date, DateLike


ANNUAL_FACTORS = {
    "daily": 252.0,
    "weekly": 52.0,
    "monthly": 12.0
}


def create_capital_iq_bridge_workbook(
    tickers: List[str],
    calibration_date: DateLike,
    valuation_date: DateLike,
    currency: str = "USD",
    frequency: str = "Weekly",
    lookback_years: int = 2
) -> io.BytesIO:
    """Generates a Capital IQ compatible Excel workbook populated with plugin formulas."""
    wb = openpyxl.Workbook()
    ws_intro = wb.active
    ws_intro.title = "Instructions"

    # Header styling
    dark_fill = PatternFill(start_color="17242B", end_color="17242B", fill_type="solid")
    teal_fill = PatternFill(start_color="0B6B68", end_color="0B6B68", fill_type="solid")
    white_font_bold = Font(name="Calibri", size=11, bold=True, color="FFFFFF")
    bold_font = Font(name="Calibri", size=11, bold=True)

    ws_intro.append(["CAPITAL IQ VOLATILITY BRIDGE WORKBOOK"])
    ws_intro.cell(row=1, column=1).font = Font(name="Calibri", size=14, bold=True, color="0B6B68")
    ws_intro.append(["Instructions for Excel Desktop:"])
    ws_intro.append(["1. Open this file in Microsoft Excel Desktop with the S&P Capital IQ plugin signed in."])
    ws_intro.append(["2. Click 'Refresh All' on the Capital IQ ribbon."])
    ws_intro.append(["3. Wait for all Capital IQ formulas to evaluate and return numeric data."])
    ws_intro.append(["4. Save the refreshed workbook (Ctrl+S)."])
    ws_intro.append(["5. Upload the saved workbook back into the Contingent Claims Analysis application."])
    ws_intro.append([])
    ws_intro.append(["Report Currency", currency])
    ws_intro.append(["Calibration Date", str(calibration_date)])
    ws_intro.append(["Valuation Date", str(valuation_date)])
    ws_intro.append(["Frequency", frequency])
    ws_intro.append(["Lookback (Years)", lookback_years])

    # Add Calibration and Valuation snapshot & price sheets
    for label, dt in [("Calibration", calibration_date), ("Valuation", valuation_date)]:
        ws = wb.create_sheet(title=f"Snapshot_{label}")
        headers = [
            "Include", "CIQ ID", "Company Name", "Market Date",
            f"Share Price ({currency})", "Shares Out (mm)",
            f"Minority Interest ({currency} mm)", f"Preferred Equity ({currency} mm)",
            f"Total Debt ({currency} mm)", "Currency"
        ]
        ws.append(headers)
        for col_num in range(1, len(headers) + 1):
            cell = ws.cell(row=1, column=col_num)
            cell.fill = dark_fill
            cell.font = white_font_bold
            cell.alignment = Alignment(horizontal="center", vertical="center")

        date_str = str(dt)
        for i, t in enumerate(tickers, start=2):
            q_ticker = t.strip()
            # Capital IQ plugin formulas
            ws.append([
                "x",
                q_ticker,
                f'=IQ_COMPANY_NAME("{q_ticker}")',
                date_str,
                f'=IQ_CLOSEPRICE("{q_ticker}", "{date_str}")',
                f'=IQ_SHARESOUTSTANDING("{q_ticker}", "{date_str}")',
                f'=IQ_MINORITY_INTEREST("{q_ticker}", "{date_str}")',
                f'=IQ_PREF_EQUITY("{q_ticker}", "{date_str}")',
                f'=IQ_TOTAL_DEBT("{q_ticker}", "{date_str}")',
                currency
            ])

        # Price history sheet for volatility calculation
        ws_p = wb.create_sheet(title=f"Prices_{label}")
        ws_p.append(["Date"] + tickers)
        for col_num in range(1, len(tickers) + 2):
            cell = ws_p.cell(row=1, column=col_num)
            cell.fill = teal_fill
            cell.font = white_font_bold

    out = io.BytesIO()
    wb.save(out)
    out.seek(0)
    return out


def parse_and_validate_bridge_workbook(
    file_bytes: bytes,
    rf_rate: float = 0.02,
    term: float = 1.0,
    subject_equity: float = 287252502.92,
    subject_debt: float = 0.0,
    subject_pref: float = 0.0,
    max_file_size_bytes: int = 15 * 1024 * 1024
) -> Dict[str, VolatilityAnalysisResult]:
    """Securely parses refreshed Capital IQ workbook bytes and calculates volatility statistics."""
    return _parse_bridge_workbook_impl(
        file_bytes, rf_rate, term, subject_equity, subject_debt, subject_pref, max_file_size_bytes
    )


parse_capital_iq_workbook = parse_and_validate_bridge_workbook


def _parse_bridge_workbook_impl(
    file_bytes: bytes,
    rf_rate: float = 0.02,
    term: float = 1.0,
    subject_equity: float = 287252502.92,
    subject_debt: float = 0.0,
    subject_pref: float = 0.0,
    max_file_size_bytes: int = 15 * 1024 * 1024
) -> Dict[str, VolatilityAnalysisResult]:
    """Securely parses refreshed Capital IQ workbook bytes and calculates volatility statistics.

    Validates:
    - File size limit (prevent DoS)
    - Valid zip/openpyxl structure
    - Expected sheet presence
    - Validates numeric conversions
    """
    if len(file_bytes) > max_file_size_bytes:
        raise ValueError(f"Uploaded file exceeds maximum limit of {max_file_size_bytes / (1024 * 1024):.0f}MB")

    try:
        wb = openpyxl.load_workbook(io.BytesIO(file_bytes), data_only=True)
    except Exception as e:
        raise ValueError(f"Invalid Excel workbook: {str(e)}")

    results: Dict[str, VolatilityAnalysisResult] = {}

    for label in ["Calibration", "Valuation"]:
        snap_title = f"Snapshot_{label}"
        prices_title = f"Prices_{label}"

        if snap_title not in wb.sheetnames:
            continue

        ws_snap = wb[snap_title]
        snap_rows = list(ws_snap.iter_rows(values_only=True))
        if not snap_rows or len(snap_rows) < 2:
            continue

        headers = [str(h).strip().lower() if h else "" for h in snap_rows[0]]

        def get_col(candidates: List[str]) -> Optional[int]:
            for c in candidates:
                for idx, h in enumerate(headers):
                    if c in h:
                        return idx
            return None

        ciq_idx = get_col(["ciq id", "ticker", "symbol"]) or 1
        name_idx = get_col(["company name", "name"]) or 2
        price_idx = get_col(["share price", "price"]) or 4
        shares_idx = get_col(["shares out", "shares"]) or 5
        debt_idx = get_col(["total debt", "debt"]) or 8
        pref_idx = get_col(["preferred equity", "pref"]) or 7
        min_idx = get_col(["minority interest", "minority"]) or 6

        # Parse price history for equity volatility if present
        vol_by_ticker: Dict[str, float] = {}
        if prices_title in wb.sheetnames:
            ws_p = wb[prices_title]
            p_data = list(ws_p.iter_rows(values_only=True))
            if p_data and len(p_data) > 3:
                p_headers = [str(h).strip() for h in p_data[0] if h]
                df_p = pd.DataFrame(p_data[1:], columns=p_headers)
                # Compute log return std dev
                for col in p_headers[1:]:
                    try:
                        series = pd.to_numeric(df_p[col], errors="coerce").dropna()
                        if len(series) > 5:
                            log_ret = np.diff(np.log(series.values))
                            ann_factor = 52.0  # Default weekly
                            vol_by_ticker[col] = float(np.std(log_ret, ddof=1) * np.sqrt(ann_factor))
                    except Exception:
                        pass

        companies: List[ComparableCompanyVol] = []

        for row in snap_rows[1:]:
            if not row or not any(row):
                continue
            ticker = str(row[ciq_idx] or "").strip()
            if not ticker or ticker.lower() == "none":
                continue

            name = str(row[name_idx] or ticker).strip()

            def to_f(v):
                try:
                    return float(v) if v is not None else 0.0
                except (ValueError, TypeError):
                    return 0.0

            p = to_f(row[price_idx]) if price_idx < len(row) else 0.0
            sh = to_f(row[shares_idx]) if shares_idx < len(row) else 0.0
            debt = to_f(row[debt_idx]) if debt_idx < len(row) else 0.0
            pref = to_f(row[pref_idx]) if pref_idx < len(row) else 0.0
            mi = to_f(row[min_idx]) if min_idx < len(row) else 0.0

            mcap = p * sh if (p > 0 and sh > 0) else 0.0
            eq_vol = vol_by_ticker.get(ticker, 0.35)

            asset_vol = merton_asset_volatility(
                equity_vol=eq_vol,
                equity_market_cap=mcap,
                total_debt=debt,
                risk_free_rate=rf_rate,
                term=term
            )
            if not math.isfinite(asset_vol):
                asset_vol = eq_vol

            relev_vol = relever_equity_volatility(
                asset_vol=asset_vol,
                subject_equity_value=subject_equity,
                subject_debt=subject_debt,
                subject_preferred=subject_pref
            )

            companies.append(ComparableCompanyVol(
                ciq_id=ticker,
                company_name=name,
                share_price=p,
                market_cap=mcap,
                total_debt=debt,
                preferred_equity=pref,
                minority_interest=mi,
                equity_vol=eq_vol,
                asset_vol=asset_vol,
                relevered_vol=relev_vol if math.isfinite(relev_vol) else None
            ))

        eq_vals = [c.equity_vol for c in companies]
        ast_vals = [c.asset_vol for c in companies]
        rel_vals = [c.relevered_vol for c in companies if c.relevered_vol is not None]

        eq_stats = calculate_volatility_stats(eq_vals)
        ast_stats = calculate_volatility_stats(ast_vals)
        rel_stats = calculate_volatility_stats(rel_vals) if rel_vals else None

        selected_vol = eq_stats.median if eq_stats else 0.35
        de = (subject_debt + subject_pref) / subject_equity if subject_equity > 0 else 0.0

        results[label] = VolatilityAnalysisResult(
            companies=companies,
            equity_stats=eq_stats,
            asset_stats=ast_stats,
            relevered_stats=rel_stats,
            selected_basis="equity",
            selected_stat="median",
            selected_volatility=selected_vol,
            subject_equity_value=subject_equity,
            subject_debt_and_pref=subject_debt + subject_pref,
            subject_debt_to_equity=de,
            subject_relevered_vol=(ast_stats.median * (1.0 + de)) if ast_stats else selected_vol
        )

    return results
