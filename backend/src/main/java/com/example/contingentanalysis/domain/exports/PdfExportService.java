package com.example.contingentanalysis.domain.exports;

import com.example.contingentanalysis.domain.model.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.WaitUntilState;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class PdfExportService {

    private static final Logger log = LoggerFactory.getLogger(PdfExportService.class);
    private final MeterRegistry meterRegistry;

    public PdfExportService() {
        this(null);
    }

    @Autowired
    public PdfExportService(@Autowired(required = false) MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    private String sanitizeLogoBase64(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim();
        if (trimmed.contains("<") || trimmed.contains(">") || trimmed.toLowerCase(Locale.ROOT).contains("javascript:")) {
            log.warn("Rejected potentially unsafe logo base64 payload");
            return null;
        }
        if (trimmed.startsWith("data:image/") && trimmed.contains(";base64,")) {
            return trimmed;
        }
        if (trimmed.matches("^[A-Za-z0-9+/=]+$")) {
            return "data:image/png;base64," + trimmed;
        }
        return null;
    }

    private String mFmt(Double v, String sym) {
        if (v == null) return "—";
        return String.format(Locale.US, "%s%,.2f", sym, v);
    }

    private String pFmt(Double v) {
        if (v == null) return "—";
        return String.format(Locale.US, "%.2f%%", v * 100.0);
    }

    private String nFmt(Double v) {
        if (v == null) return "—";
        return String.format(Locale.US, "%,.0f", v);
    }

    public String renderPdfHtml(ValuationResponse res) {
        String currSym = "EUR".equalsIgnoreCase(res.getReportCurrency()) ? "€" :
                ("GBP".equalsIgnoreCase(res.getReportCurrency()) ? "£" : "$");
        String secSym = "USD".equalsIgnoreCase(res.getSecondaryCurrency()) ? "$" :
                ("EUR".equalsIgnoreCase(res.getSecondaryCurrency()) ? "€" : "£");

        String safeLogo = sanitizeLogoBase64(res.getFirmLogoBase64());
        String logoHtml;
        if (safeLogo != null) {
            logoHtml = String.format("<div style=\"margin-bottom: 20px;\"><img src=\"%s\" style=\"max-height: 48px; max-width: 220px; object-fit: contain; filter: brightness(0) invert(1);\" alt=\"Firm Logo\"></div>", safeLogo);
        } else {
            logoHtml = "<div style=\"margin-bottom: 16px; font-size: 11pt; font-weight: 700; letter-spacing: 0.15em; color: #A9D6CF; text-transform: uppercase;\">CONTINGENT CLAIMS VALUATION ENGINE</div>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("""
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<style>
  @page {
    size: 297mm 210mm landscape;
    margin: 8mm 10mm;
  }
  * { box-sizing: border-box; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    color: #111827;
    background: #FFFFFF;
    margin: 0;
    padding: 0;
    font-size: 8.5pt;
    line-height: 1.35;
    -webkit-font-smoothing: antialiased;
  }

  table.data-table, .tabular, .kpi-val, td, th {
    font-variant-numeric: tabular-nums lining-nums;
  }

  .watermark {
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
  }
  .watermark-text {
    font-size: 64pt;
    font-weight: 900;
    letter-spacing: 0.15em;
    color: #17242B;
    text-transform: uppercase;
    user-select: none;
    white-space: nowrap;
  }
  .watermark-subtext {
    font-size: 18pt;
    font-weight: 700;
    letter-spacing: 0.1em;
    color: #08615E;
    margin-top: 8px;
    text-transform: uppercase;
  }

  .page {
    page-break-before: always;
    break-before: page;
    position: relative;
    min-height: 100%;
    padding-top: 4px;
  }
  .page:first-of-type {
    page-break-before: auto;
    break-before: auto;
  }

  .cover-page {
    display: flex;
    flex-direction: column;
    justify-content: center;
    height: 185mm;
    padding: 16mm 22mm;
    background: linear-gradient(135deg, #131b24 0%, #1e3a40 55%, #08615e 100%);
    color: #FFFFFF;
    border-radius: 10px;
  }
  .cover-title {
    font-size: 28pt;
    font-weight: 800;
    letter-spacing: -0.02em;
    margin-bottom: 8px;
    color: #FFFFFF;
  }
  .cover-subtitle {
    font-size: 13.5pt;
    color: #E6F4F2;
    margin-bottom: 36px;
    max-width: 850px;
    line-height: 1.4;
  }
  .cover-meta-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
    background: rgba(255, 255, 255, 0.08);
    padding: 20px 24px;
    border-radius: 8px;
    border: 1px solid rgba(255, 255, 255, 0.18);
  }
  .cover-meta-item label {
    display: block;
    font-size: 8pt;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: #A9D6CF;
    margin-bottom: 3px;
    font-weight: 600;
  }
  .cover-meta-item value {
    font-size: 12pt;
    font-weight: 700;
    color: #FFFFFF;
  }

  .header-bar {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    border-bottom: 2px solid #08615E;
    padding-bottom: 5px;
    margin-bottom: 10px;
  }
  .exhibit-num {
    font-size: 9pt;
    font-weight: 800;
    color: #08615E;
    text-transform: uppercase;
    letter-spacing: 0.06em;
  }
  .exhibit-title {
    font-size: 13.5pt;
    font-weight: 800;
    color: #111827;
    margin: 1px 0 0;
  }
  .header-meta {
    text-align: right;
    font-size: 8pt;
    color: #4B5563;
    line-height: 1.35;
  }

  table.data-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 8pt;
    margin-top: 6px;
    margin-bottom: 8px;
  }
  table.data-table th {
    background: #1e3a40;
    color: #FFFFFF;
    font-weight: 700;
    padding: 5px 6px;
    text-align: right;
    border: 1px solid #14282c;
    font-size: 8pt;
    line-height: 1.25;
  }
  table.data-table th:first-child {
    text-align: left;
  }
  table.data-table td {
    padding: 3.5px 6px;
    border-bottom: 1px solid #D1D5DB;
    border-left: 1px solid #E5E7EB;
    border-right: 1px solid #E5E7EB;
    text-align: right;
    font-size: 8pt;
    white-space: nowrap;
  }
  table.data-table td:first-child {
    text-align: left;
    font-weight: 500;
  }
  table.data-table tr:nth-child(even) {
    background: #F9FAFB;
  }
  table.data-table tr.total-row td {
    font-weight: 800;
    background: #EDF6F4;
    border-top: 2px solid #08615E;
    border-bottom: 2px solid #08615E;
    color: #0F172A;
  }

  .section-subtitle {
    font-size: 9pt;
    font-weight: 700;
    color: #1e3a40;
    margin: 10px 0 3px;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  .kpi-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;
    margin-bottom: 12px;
  }
  .kpi-box {
    background: #F4F8F7;
    border: 1px solid #DCE7E4;
    border-radius: 6px;
    padding: 6px 10px;
  }
  .kpi-label {
    font-size: 7.5pt;
    color: #4B5563;
    text-transform: uppercase;
    font-weight: 700;
  }
  .kpi-val {
    font-size: 12.5pt;
    font-weight: 800;
    color: #08615E;
    margin-top: 2px;
  }
  .note-box {
    margin-top: 8px;
    padding: 5px 8px;
    background: #F8FAFA;
    border-left: 3px solid #08615E;
    font-size: 7.5pt;
    color: #4B5563;
    font-style: italic;
    line-height: 1.35;
  }
  .badge {
    display: inline-block;
    padding: 2px 5px;
    border-radius: 3px;
    font-size: 7pt;
    font-weight: 700;
    background: #E8F2F8;
    color: #0F5F91;
  }
</style>
</head>
<body>

<!-- Global Watermark -->
<div class="watermark">
  <div class="watermark-text">HIGHLY CONFIDENTIAL</div>
  <div class="watermark-subtext">""");
        sb.append(res.getReportStatus()).append(" · ").append(res.getCompanyName()).append("""
</div>
</div>

<!-- PAGE 1: COVER SHEET -->
<div class="page">
  <div class="cover-page">
    """);
        sb.append(logoHtml).append("\n");
        sb.append("    <div class=\"cover-title\">CONTINGENT CLAIMS VALUATION REPORT</div>\n");
        sb.append(String.format("    <div class=\"cover-subtitle\">Option Pricing Method (OPM) Backsolve &amp; Allocation Analysis prepared for %s</div>\n", res.getClientName()));
        sb.append("    <div class=\"cover-meta-grid\">\n");
        sb.append(String.format("      <div class=\"cover-meta-item\"><label>Subject Company</label><value>%s</value></div>\n", res.getCompanyName()));
        sb.append(String.format("      <div class=\"cover-meta-item\"><label>Report Purpose</label><value>%s</value></div>\n", res.getReportPurpose()));
        sb.append(String.format("      <div class=\"cover-meta-item\"><label>Report Status</label><value>%s</value></div>\n", res.getReportStatus()));
        sb.append(String.format("      <div class=\"cover-meta-item\"><label>Calibration Date</label><value>%s</value></div>\n", res.getCalibrationDate()));
        sb.append(String.format("      <div class=\"cover-meta-item\"><label>Valuation Date</label><value>%s</value></div>\n", res.getValuationDate()));
        sb.append(String.format("      <div class=\"cover-meta-item\"><label>Concluded Equity Value</label><value>%s</value></div>\n", mFmt(res.getConcludedEquityValue(), currSym)));
        sb.append("""
    </div>
  </div>
</div>

<!-- PAGE 2: INDEX & EXECUTIVE SUMMARY -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Executive Summary</div>
      <div class="exhibit-title">Index of Exhibits &amp; Key Valuation Parameters</div>
    </div>
    <div class="header-meta">
""");
        sb.append(String.format("      %s | %s<br>Valuation Date: %s\n", res.getCompanyName(), res.getClientName(), res.getValuationDate()));
        sb.append("""
    </div>
  </div>

  <div class="kpi-row">
    <div class="kpi-box">
      <div class="kpi-label">Backsolved Equity (Calibration)</div>
""");
        sb.append(String.format("      <div class=\"kpi-val\">%s</div>\n", mFmt(res.getCalibrationSolvedEquity(), currSym)));
        sb.append("""
    </div>
    <div class="kpi-box">
      <div class="kpi-label">Concluded Equity (Valuation)</div>
""");
        sb.append(String.format("      <div class=\"kpi-val\">%s</div>\n", mFmt(res.getConcludedEquityValue(), currSym)));
        sb.append("""
    </div>
    <div class="kpi-box">
      <div class="kpi-label">Valuation Volatility</div>
""");
        sb.append(String.format("      <div class=\"kpi-val\">%.2f%%</div>\n", res.getValuationOpm() != null ? res.getValuationOpm().getVolatility() : 0.0));
        sb.append("""
    </div>
    <div class="kpi-box">
      <div class="kpi-label">Time to Liquidity</div>
""");
        sb.append(String.format("      <div class=\"kpi-val\">%.2f yrs</div>\n", res.getTermValuation()));
        sb.append("""
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
""");
        sb.append(String.format("      <tr><td><b>Exhibit 1.0</b></td><td>Client Holdings Summary &amp; Concluded Portfolio Value</td><td>Valuation Date (%s)</td></tr>\n", res.getValuationDate()));
        sb.append(String.format("      <tr><td><b>Exhibit 2.0</b></td><td>Capitalization Structure &amp; Security Terms</td><td>Calibration Date (%s)</td></tr>\n", res.getCalibrationDate()));
        sb.append(String.format("      <tr><td><b>Exhibit 3.0</b></td><td>Breakpoint Schedule &amp; Claims Allocation Matrix</td><td>Calibration Date (%s)</td></tr>\n", res.getCalibrationDate()));
        sb.append(String.format("      <tr><td><b>Exhibit 4.0</b></td><td>OPM Backsolve Mechanics &amp; Tranche Option Pricing</td><td>Calibration Date (%s)</td></tr>\n", res.getCalibrationDate()));
        sb.append(String.format("      <tr><td><b>Exhibit 5.0</b></td><td>Risk-Free Rate Curve &amp; Interpolation Analysis</td><td>Calibration Date (%s)</td></tr>\n", res.getCalibrationDate()));
        sb.append(String.format("      <tr><td><b>Exhibit 6.0</b></td><td>Capitalization Structure &amp; Security Terms</td><td>Valuation Date (%s)</td></tr>\n", res.getValuationDate()));
        sb.append(String.format("      <tr><td><b>Exhibit 7.0</b></td><td>Breakpoint Schedule &amp; Claims Allocation Matrix</td><td>Valuation Date (%s)</td></tr>\n", res.getValuationDate()));
        sb.append(String.format("      <tr><td><b>Exhibit 8.0</b></td><td>OPM Value Allocation Matrix &amp; Concluded Fair Values</td><td>Valuation Date (%s)</td></tr>\n", res.getValuationDate()));
        sb.append(String.format("      <tr><td><b>Exhibit 9.0</b></td><td>Risk-Free Rate Curve &amp; Interpolation Analysis</td><td>Valuation Date (%s)</td></tr>\n", res.getValuationDate()));
        sb.append("      <tr><td><b>Exhibit 10.0</b></td><td>Comparative Liquidation Waterfall Schedule</td><td>Side-by-Side (Calibration vs. Valuation)</td></tr>\n");
        sb.append("""
    </tbody>
  </table>
  <div class="note-box">
    This valuation book presents the mathematical mechanics, security terms, breakpoint thresholds, Black-Scholes call option tranches, and liquidation proceeds allocations underlying the contingent claims analysis.
  </div>
</div>

<!-- PAGE 3: EXHIBIT 1.0 - CLIENT HOLDINGS SUMMARY -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 1.0</div>
      <div class="exhibit-title">Client Holdings and Value Conclusion — Valuation Date</div>
    </div>
    <div class="header-meta">
""");
        sb.append(String.format("      %s | %s<br>As of %s\n", res.getCompanyName(), res.getClientName(), res.getValuationDate()));
        sb.append("""
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
""");
        if (res.isShowSecondaryCurrency()) {
            sb.append(String.format("        <th>Value (%s)</th>\n", res.getSecondaryCurrency()));
        }
        sb.append("""
        <th>Class Ownership</th>
        <th>FD Ownership</th>
        <th>MOIC</th>
      </tr>
    </thead>
    <tbody>
""");
        double totUnits = 0.0;
        if (res.getHoldings() != null && res.getHoldings().getItems() != null) {
            for (HoldingResultItem it : res.getHoldings().getItems()) {
                totUnits += it.getUnits();
                String moicTxt = it.getMoic() != null ? String.format(Locale.US, "%.2fx", it.getMoic()) : "—";
                String secValTd = res.isShowSecondaryCurrency()
                        ? String.format("<td>%s</td>", mFmt(it.getConcludedFairValue() * res.getSecondaryFxRate(), secSym)) : "";

                sb.append(String.format("""
      <tr>
        <td>%s</td>
        <td>%s</td>
        <td>%s</td>
        <td>%s</td>
        <td>%s</td>
        <td><b>%s</b></td>
        %s
        <td>%s</td>
        <td>%s</td>
        <td><b>%s</b></td>
      </tr>
""", it.getFund(), it.getSecurity(), nFmt(it.getUnits()), mFmt(it.getCost(), currSym),
                        mFmt(it.getFairValuePerShare(), currSym), mFmt(it.getConcludedFairValue(), currSym),
                        secValTd, pFmt(it.getClassOwnershipPct()), pFmt(it.getFullyDilutedOwnershipPct()), moicTxt));
            }
        }
        String cMoic = (res.getHoldings() != null && res.getHoldings().getConsolidatedMoic() != null)
                ? String.format(Locale.US, "%.2fx", res.getHoldings().getConsolidatedMoic()) : "—";
        String secTotTd = (res.isShowSecondaryCurrency() && res.getHoldings() != null)
                ? String.format("<td>%s</td>", mFmt(res.getHoldings().getTotalValue() * res.getSecondaryFxRate(), secSym)) : "";

        double totalCost = res.getHoldings() != null ? res.getHoldings().getTotalCost() : 0.0;
        double totalVal = res.getHoldings() != null ? res.getHoldings().getTotalValue() : 0.0;

        sb.append(String.format("""
      <tr class="total-row">
        <td colspan="2">TOTAL CONSOLIDATED PORTFOLIO</td>
        <td>%s</td>
        <td>%s</td>
        <td>—</td>
        <td>%s</td>
        %s
        <td>—</td>
        <td>—</td>
        <td>%s</td>
      </tr>
    </tbody>
  </table>
  <div class="note-box">
    MOIC equals concluded fair value divided by investment cost where cost is positive. Class ownership equals units held divided by class shares outstanding.
  </div>
</div>
""", nFmt(totUnits), mFmt(totalCost, currSym), mFmt(totalVal, currSym), secTotTd, cMoic));

        // EXHIBIT 2.0 - Calibration Cap Table
        sb.append(String.format("""
<!-- PAGE 4: EXHIBIT 2.0 - CAPITALIZATION TABLE (CALIBRATION DATE) -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 2.0</div>
      <div class="exhibit-title">Capitalization Table and Structure — Calibration Date</div>
    </div>
    <div class="header-meta">
      %s | %s<br>Effective Date: %s
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
""", res.getCompanyName(), res.getClientName(), res.getCalibrationDate()));

        double totCalSh = 0.0, totCalPref = 0.0, totCalFd = 0.0;
        if (res.getCalibrationDerivedSecurities() != null) {
            for (DerivedSecurity s : res.getCalibrationDerivedSecurities()) {
                totCalSh += s.getShares();
                totCalPref += s.getTotalLiquidationPreference();
                totCalFd += s.getFullyDilutedShares();
                sb.append(String.format("""
      <tr>
        <td>%s</td>
        <td>%s</td>
        <td>%d</td>
        <td>%s</td>
        <td>%s</td>
        <td>%.2fx</td>
        <td>%s</td>
        <td>%s</td>
        <td>%s</td>
        <td>%.4f</td>
        <td>%s</td>
      </tr>
""", s.getSecurity(), s.getSecuritySubtype(), s.getSeniority(), nFmt(s.getShares()),
                        mFmt(s.getOriginalIssuePrice(), currSym), s.getLiquidationMultiplier(),
                        mFmt(s.getLiquidationPreferencePerShare(), currSym), mFmt(s.getTotalLiquidationPreference(), currSym),
                        s.getParticipation(), s.getConversionRatio(), nFmt(s.getFullyDilutedShares())));
            }
        }
        sb.append(String.format("""
      <tr class="total-row">
        <td colspan="3">TOTAL CAPITAL STRUCTURE</td>
        <td>%s</td>
        <td>—</td>
        <td>—</td>
        <td>—</td>
        <td>%s</td>
        <td>—</td>
        <td>—</td>
        <td>%s</td>
      </tr>
    </tbody>
  </table>
  <div class="note-box">
    Capitalization structure established as of transaction calibration date (%s).
  </div>
</div>
""", nFmt(totCalSh), mFmt(totCalPref, currSym), nFmt(totCalFd), res.getCalibrationDate()));

        // EXHIBIT 3.0 - Calibration Breakpoints
        sb.append(String.format("""
<!-- PAGE 5: EXHIBIT 3.0 - BREAKPOINTS (CALIBRATION DATE) -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 3.0</div>
      <div class="exhibit-title">Breakpoint Schedule &amp; Claims Matrix — Calibration Date</div>
    </div>
    <div class="header-meta">
      %s | %s<br>As of %s
    </div>
  </div>

  <div class="section-subtitle">Breakpoint Equity Thresholds</div>
  <table class="data-table">
    <thead>
      <tr>
        <th style="width: 8%%;">Tier</th>
        <th style="width: 18%%;">Start Equity</th>
        <th style="width: 18%%;">End Equity</th>
        <th style="width: 16%%;">Tranche Width</th>
        <th style="width: 40%%;">Trigger Event / Participating Securities</th>
      </tr>
    </thead>
    <tbody>
""", res.getCompanyName(), res.getClientName(), res.getCalibrationDate()));

        if (res.getCalibrationBreakpoints() != null) {
            for (BreakpointTier bp : res.getCalibrationBreakpoints()) {
                String endStr = bp.isThereafter() ? "Thereafter" : mFmt(bp.getEndEquity(), currSym);
                String widthStr = !bp.isThereafter() ? mFmt(bp.getWidth(), currSym) : "—";
                sb.append(String.format("""
      <tr>
        <td><span class="badge">Tier %d</span></td>
        <td>%s</td>
        <td>%s</td>
        <td>%s</td>
        <td style="text-align: left;">%s</td>
      </tr>
""", bp.getTier(), mFmt(bp.getStartEquity(), currSym), endStr, widthStr, bp.getEventDescription()));
            }
        }
        sb.append("""
    </tbody>
  </table>
  <div class="note-box">
    Breakpoints delineate incremental cumulative enterprise equity value levels.
  </div>
</div>
""");

        // EXHIBIT 4.0 - Calibration OPM Backsolve
        sb.append(String.format("""
<!-- PAGE 6: EXHIBIT 4.0 - OPM BACKSOLVE MECHANICS (CALIBRATION DATE) -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 4.0</div>
      <div class="exhibit-title">OPM Backsolve Mechanics &amp; Value Allocation — Calibration Date</div>
    </div>
    <div class="header-meta">
      Backsolved Equity: %s
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
""", mFmt(res.getCalibrationSolvedEquity(), currSym)));

        if (res.getCalibrationOpm() != null && res.getCalibrationOpm().getTranches() != null) {
            for (OpmTranche tr : res.getCalibrationOpm().getTranches()) {
                String uStr = tr.getStrikeHigh() > 1e12 ? "∞" : mFmt(tr.getStrikeHigh(), currSym);
                String cStr = tr.getStrikeHigh() > 1e12 ? "0.00" : mFmt(tr.getCallHigh(), currSym);
                sb.append(String.format("""
      <tr>
        <td><span class="badge">Tranche %d</span></td>
        <td>%s</td>
        <td>%s</td>
        <td>%s</td>
        <td>%s</td>
        <td><b>%s</b></td>
      </tr>
""", tr.getTier(), mFmt(tr.getStrikeLow(), currSym), uStr, mFmt(tr.getCallLow(), currSym), cStr, mFmt(tr.getIncrementalCall(), currSym)));
            }
        }
        sb.append(String.format("""
      <tr class="total-row">
        <td colspan="5">TOTAL OPTION VALUE (BACKSOLVED EQUITY)</td>
        <td>%s</td>
      </tr>
    </tbody>
  </table>
  <div class="note-box">
    Backsolve solved enterprise equity value of %s.
  </div>
</div>
""", mFmt(res.getCalibrationSolvedEquity(), currSym), mFmt(res.getCalibrationSolvedEquity(), currSym)));

        // EXHIBIT 5.0 - Calibration Risk-Free
        sb.append(String.format("""
<!-- PAGE 7: EXHIBIT 5.0 - RISK-FREE RATE ANALYSIS (CALIBRATION DATE) -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 5.0</div>
      <div class="exhibit-title">Risk-Free Rate Curve &amp; Interpolation Analysis — Calibration Date</div>
    </div>
    <div class="header-meta">
      Measurement Date: %s
    </div>
  </div>

  <table class="data-table">
    <thead><tr><th>Parameter</th><th>Value</th></tr></thead>
    <tbody>
      <tr><td>Source</td><td>Valuer Specified Manual Input</td></tr>
      <tr><td>Measurement Date</td><td>%s</td></tr>
      <tr><td>Expected Exit Date</td><td>%s</td></tr>
      <tr><td>Expected Term to Exit</td><td>%.4f years</td></tr>
      <tr><td>Annual Effective Risk-Free Rate</td><td><b>%.4f%%</b></td></tr>
      <tr><td>Continuous Risk-Free Rate Used in OPM</td><td><b>%.4f%%</b></td></tr>
    </tbody>
  </table>
  <div class="note-box">
    Annual effective risk-free rate converted to continuous rate for Black-Scholes formula: rc = ln(1 + r_eff).
  </div>
</div>
""", res.getCalibrationDate(), res.getCalibrationDate(), res.getExitDate(),
                res.getTermCalibration(), res.getRfCalibrationEffective(), res.getRfCalibrationContinuous()));

        // EXHIBIT 6.0 - Valuation Cap Table
        sb.append(String.format("""
<!-- PAGE 8: EXHIBIT 6.0 - CAPITALIZATION TABLE (VALUATION DATE) -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 6.0</div>
      <div class="exhibit-title">Capitalization Table and Structure — Valuation Date</div>
    </div>
    <div class="header-meta">
      Valuation Date: %s
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
""", res.getValuationDate()));

        double totValSh = 0.0, totValPref = 0.0, totValFd = 0.0;
        if (res.getValuationDerivedSecurities() != null) {
            for (DerivedSecurity s : res.getValuationDerivedSecurities()) {
                totValSh += s.getShares();
                totValPref += s.getTotalLiquidationPreference();
                totValFd += s.getFullyDilutedShares();
                sb.append(String.format("""
      <tr>
        <td>%s</td>
        <td>%s</td>
        <td>%d</td>
        <td>%s</td>
        <td>%s</td>
        <td>%.2fx</td>
        <td>%s</td>
        <td>%s</td>
        <td>%s</td>
        <td>%.4f</td>
        <td>%s</td>
      </tr>
""", s.getSecurity(), s.getSecuritySubtype(), s.getSeniority(), nFmt(s.getShares()),
                        mFmt(s.getOriginalIssuePrice(), currSym), s.getLiquidationMultiplier(),
                        mFmt(s.getLiquidationPreferencePerShare(), currSym), mFmt(s.getTotalLiquidationPreference(), currSym),
                        s.getParticipation(), s.getConversionRatio(), nFmt(s.getFullyDilutedShares())));
            }
        }
        sb.append(String.format("""
      <tr class="total-row">
        <td colspan="3">TOTAL CAPITAL STRUCTURE</td>
        <td>%s</td>
        <td>—</td>
        <td>—</td>
        <td>—</td>
        <td>%s</td>
        <td>—</td>
        <td>—</td>
        <td>%s</td>
      </tr>
    </tbody>
  </table>
  <div class="note-box">
    Capitalization structure as of valuation measurement date (%s).
  </div>
</div>
""", nFmt(totValSh), mFmt(totValPref, currSym), nFmt(totValFd), res.getValuationDate()));

        // EXHIBIT 7.0 - Valuation Breakpoints
        sb.append(String.format("""
<!-- PAGE 9: EXHIBIT 7.0 - BREAKPOINTS & CLAIMS MATRIX (VALUATION DATE) -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 7.0</div>
      <div class="exhibit-title">Breakpoint Schedule &amp; Claims Matrix — Valuation Date</div>
    </div>
    <div class="header-meta">
      As of %s
    </div>
  </div>

  <div class="section-subtitle">Breakpoint Equity Thresholds</div>
  <table class="data-table">
    <thead>
      <tr>
        <th style="width: 8%%;">Tier</th>
        <th style="width: 18%%;">Start Equity</th>
        <th style="width: 18%%;">End Equity</th>
        <th style="width: 16%%;">Tranche Width</th>
        <th style="width: 40%%;">Trigger Event / Participating Securities</th>
      </tr>
    </thead>
    <tbody>
""", res.getValuationDate()));

        if (res.getValuationBreakpoints() != null) {
            for (BreakpointTier bp : res.getValuationBreakpoints()) {
                String endStr = bp.isThereafter() ? "Thereafter" : mFmt(bp.getEndEquity(), currSym);
                String widthStr = !bp.isThereafter() ? mFmt(bp.getWidth(), currSym) : "—";
                sb.append(String.format("""
      <tr>
        <td><span class="badge">Tier %d</span></td>
        <td>%s</td>
        <td>%s</td>
        <td>%s</td>
        <td style="text-align: left;">%s</td>
      </tr>
""", bp.getTier(), mFmt(bp.getStartEquity(), currSym), endStr, widthStr, bp.getEventDescription()));
            }
        }
        sb.append("""
    </tbody>
  </table>
  <div class="note-box">
    Claims allocation matrix sets forth each share class's participation rate.
  </div>
</div>
""");

        // EXHIBIT 8.0 - Valuation OPM Allocation
        sb.append(String.format("""
<!-- PAGE 10: EXHIBIT 8.0 - OPM ALLOCATION MATRIX (VALUATION DATE) -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 8.0</div>
      <div class="exhibit-title">Option Pricing Method (OPM) Value Allocation — Valuation Date</div>
    </div>
    <div class="header-meta">
      Concluded Equity: %s<br>Effective Date: %s
    </div>
  </div>

  <table class="data-table">
    <thead>
      <tr>
        <th>Share Class</th>
        <th>Subtype</th>
        <th>Shares Outstanding</th>
        <th>Concluded Value</th>
        <th>Value / Share</th>
        <th>%% of Total Equity</th>
        <th>Fully Diluted Shares</th>
      </tr>
    </thead>
    <tbody>
""", mFmt(res.getConcludedEquityValue(), currSym), res.getValuationDate()));

        if (res.getValuationDerivedSecurities() != null && res.getValuationOpm() != null) {
            for (DerivedSecurity sec : res.getValuationDerivedSecurities()) {
                double totV = res.getValuationOpm().getAllocatedValues().getOrDefault(sec.getSecurity(), 0.0);
                double psV = res.getValuationOpm().getPerShareValues().getOrDefault(sec.getSecurity(), 0.0);
                double pctV = res.getValuationOpm().getPercentAllocations().getOrDefault(sec.getSecurity(), 0.0);
                sb.append(String.format("""
      <tr>
        <td>%s</td>
        <td>%s</td>
        <td>%s</td>
        <td>%s</td>
        <td><b>%s</b></td>
        <td>%s</td>
        <td>%s</td>
      </tr>
""", sec.getSecurity(), sec.getSecuritySubtype(), nFmt(sec.getShares()), mFmt(totV, currSym),
                        mFmt(psV, currSym), pFmt(pctV), nFmt(sec.getFullyDilutedShares())));
            }
        }
        sb.append(String.format("""
      <tr class="total-row">
        <td colspan="2">TOTAL CONCLUDED EQUITY</td>
        <td>%s</td>
        <td>%s</td>
        <td>—</td>
        <td>100.0%%</td>
        <td>%s</td>
      </tr>
    </tbody>
  </table>
  <div class="note-box">
    Allocated via incremental Black-Scholes call option tranches.
  </div>
</div>
""", nFmt(totValSh), mFmt(res.getValuationOpm() != null ? res.getValuationOpm().getTotalAllocated() : 0.0, currSym), nFmt(totValFd)));

        // EXHIBIT 9.0 - Valuation Risk-Free
        sb.append(String.format("""
<!-- PAGE 11: EXHIBIT 9.0 - RISK-FREE RATE ANALYSIS (VALUATION DATE) -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 9.0</div>
      <div class="exhibit-title">Risk-Free Rate Curve &amp; Interpolation Analysis — Valuation Date</div>
    </div>
    <div class="header-meta">
      Measurement Date: %s
    </div>
  </div>

  <table class="data-table">
    <thead><tr><th>Parameter</th><th>Value</th></tr></thead>
    <tbody>
      <tr><td>Source</td><td>Valuer Specified Manual Input</td></tr>
      <tr><td>Measurement Date</td><td>%s</td></tr>
      <tr><td>Expected Exit Date</td><td>%s</td></tr>
      <tr><td>Expected Term to Exit</td><td>%.4f years</td></tr>
      <tr><td>Annual Effective Risk-Free Rate</td><td><b>%.4f%%</b></td></tr>
      <tr><td>Continuous Risk-Free Rate Used in OPM</td><td><b>%.4f%%</b></td></tr>
    </tbody>
  </table>
  <div class="note-box">
    Annual effective risk-free rate converted to continuous rate for Black-Scholes formula: rc = ln(1 + r_eff).
  </div>
</div>
""", res.getValuationDate(), res.getValuationDate(), res.getExitDate(),
                res.getTermValuation(), res.getRfValuationEffective(), res.getRfValuationContinuous()));

        // EXHIBIT 10.0 - Comparative Waterfall
        sb.append(String.format("""
<!-- PAGE 12: EXHIBIT 10.0 - COMPARATIVE WATERFALL ANALYSIS -->
<div class="page">
  <div class="header-bar">
    <div>
      <div class="exhibit-num">Exhibit 10.0</div>
      <div class="exhibit-title">Comparative Liquidation Waterfall Schedule</div>
    </div>
    <div class="header-meta">
      Applied Waterfall Equity: %s
    </div>
  </div>

  <table class="data-table">
    <thead>
      <tr>
        <th rowspan="2" style="text-align: left; vertical-align: middle;">Security Class</th>
        <th colspan="3" style="text-align: center; background: #1F363C;">Valuation Date Distribution</th>
        <th colspan="3" style="text-align: center; background: #28444A;">Calibration Date Structure</th>
        <th colspan="2" style="text-align: center; background: #135754;">Variance / Change</th>
      </tr>
      <tr>
        <th>Val Shares</th>
        <th>Distribution</th>
        <th>Per Share</th>
        <th>Cal Shares</th>
        <th>Distribution</th>
        <th>Per Share</th>
        <th>Δ Proceeds</th>
        <th>%% Change</th>
      </tr>
    </thead>
    <tbody>
""", mFmt(res.getWaterfall() != null ? res.getWaterfall().getAppliedEquity() : 0.0, currSym)));

        if (res.getComparativeWaterfall() != null) {
            for (ComparativeWaterfallItem row : res.getComparativeWaterfall()) {
                String pctChgStr = "—";
                if (row.getCalDistribution() != null && row.getCalDistribution() > 0 && row.getChangeInDistribution() != null) {
                    double pctChg = (row.getChangeInDistribution() / row.getCalDistribution()) * 100.0;
                    pctChgStr = String.format(Locale.US, "%+.1f%%", pctChg);
                } else if (row.getChangeInDistribution() != null && row.getChangeInDistribution() != 0) {
                    pctChgStr = "New";
                }
                String chgStr = row.getChangeInDistribution() != null ? mFmt(row.getChangeInDistribution(), currSym) : "—";
                sb.append(String.format("""
      <tr>
        <td>%s</td>
        <td>%s</td>
        <td><b>%s</b></td>
        <td>%s</td>
        <td>%s</td>
        <td>%s</td>
        <td>%s</td>
        <td>%s</td>
        <td><b>%s</b></td>
      </tr>
""", row.getSecurity(), nFmt(row.getValShares()), mFmt(row.getValDistribution(), currSym),
                        mFmt(row.getValPerShare(), currSym), nFmt(row.getCalShares()), mFmt(row.getCalDistribution(), currSym),
                        mFmt(row.getCalPerShare(), currSym), chgStr, pctChgStr));
            }
        }
        sb.append(String.format("""
    </tbody>
  </table>
  <div class="note-box">
    Comparative waterfall models instantaneous contractual priority distributions under selected equity proceeds of %s.
  </div>
</div>

</body>
</html>
""", mFmt(res.getWaterfall() != null ? res.getWaterfall().getAppliedEquity() : 0.0, currSym)));

        return sb.toString();
    }

    public byte[] generateValuationPdf(ValuationResponse response) {
        long start = System.currentTimeMillis();
        log.info("Generating watermarked PDF report for company='{}'",
                response != null ? response.getCompanyName() : "unknown");

        String htmlContent = renderPdfHtml(response);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            try (BrowserContext context = browser.newContext()) {
                context.route("**/*", route -> {
                    String url = route.request().url();
                    if (url.startsWith("data:") || url.startsWith("about:") || url.startsWith("blob:")) {
                        route.resume();
                    } else {
                        route.abort();
                    }
                });

                Page page = context.newPage();
                page.setContent(htmlContent, new Page.SetContentOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

                byte[] pdfBytes = page.pdf(new Page.PdfOptions()
                        .setFormat("A4")
                        .setLandscape(true)
                        .setPrintBackground(true)
                        .setMargin(new Margin().setTop("8mm").setBottom("8mm").setLeft("10mm").setRight("10mm"))
                        .setDisplayHeaderFooter(true)
                        .setHeaderTemplate("""
                        <div style="font-size: 7.5pt; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; width: 100%; padding: 0 10mm; display: flex; justify-content: space-between; color: #64748b; font-weight: 600;">
                          <span style="letter-spacing: 0.08em; text-transform: uppercase;">HIGHLY CONFIDENTIAL — VALUATION EXHIBIT REPORT</span>
                          <span>CONTINGENT CLAIMS ANALYSIS (OPM)</span>
                        </div>
                        """)
                        .setFooterTemplate("""
                        <div style="font-size: 7.5pt; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; width: 100%; padding: 0 10mm; display: flex; justify-content: space-between; color: #64748b;">
                          <span>AICPA / ASC 718 / ASC 820 VALUATION WORKSPACE</span>
                          <span>Page <span class="pageNumber"></span> of <span class="totalPages"></span></span>
                        </div>
                        """));

                long duration = System.currentTimeMillis() - start;
                log.info("Generated PDF report for company='{}' in {} ms (size: {} bytes)",
                        response != null ? response.getCompanyName() : "unknown", duration, pdfBytes.length);
                if (meterRegistry != null) {
                    meterRegistry.timer("export.pdf.timer").record(duration, TimeUnit.MILLISECONDS);
                    meterRegistry.counter("export.pdf.count").increment();
                }
                return pdfBytes;
            } finally {
                browser.close();
            }
        } catch (Exception e) {
            if (meterRegistry != null) {
                meterRegistry.counter("export.pdf.failures").increment();
            }
            log.error("Failed to generate PDF report: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }
}
