package com.example.contingentanalysis.domain.exports;

import com.example.contingentanalysis.domain.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Service
public class ExcelExportService {

    public byte[] generateValuationWorkbook(ValuationResponse response) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            byte[] navyBytes = new byte[]{(byte) 0x17, (byte) 0x24, (byte) 0x2B};
            byte[] tealBytes = new byte[]{(byte) 0x0B, (byte) 0x6B, (byte) 0x68};
            byte[] totalBytes = new byte[]{(byte) 0xED, (byte) 0xF6, (byte) 0xF4};
            byte[] borderBytes = new byte[]{(byte) 0xDC, (byte) 0xE1, (byte) 0xE7};

            XSSFColor navyColor = new XSSFColor(navyBytes, null);
            XSSFColor tealColor = new XSSFColor(tealBytes, null);
            XSSFColor totalColor = new XSSFColor(totalBytes, null);
            XSSFColor borderColor = new XSSFColor(borderBytes, null);

            XSSFFont whiteBoldFont = wb.createFont();
            whiteBoldFont.setFontName("Calibri");
            whiteBoldFont.setFontHeightInPoints((short) 10);
            whiteBoldFont.setBold(true);
            whiteBoldFont.setColor(IndexedColors.WHITE.getIndex());

            XSSFFont titleFont = wb.createFont();
            titleFont.setFontName("Calibri");
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setBold(true);
            titleFont.setColor(navyColor);

            XSSFFont italicFont = wb.createFont();
            italicFont.setFontName("Calibri");
            italicFont.setFontHeightInPoints((short) 9);
            italicFont.setItalic(true);

            XSSFFont regularFont = wb.createFont();
            regularFont.setFontName("Calibri");
            regularFont.setFontHeightInPoints((short) 10);

            XSSFFont boldFont = wb.createFont();
            boldFont.setFontName("Calibri");
            boldFont.setFontHeightInPoints((short) 10);
            boldFont.setBold(true);

            DataFormat df = wb.createDataFormat();
            short currencyFormat = df.getFormat("$#,##0.00");
            short numberFormat = df.getFormat("#,##0");
            short percentFormat = df.getFormat("0.00%");

            // Helper to style headers
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(navyColor);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setFont(whiteBoldFont);
            headerStyle.setAlignment(HorizontalAlignment.RIGHT);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle headerLeftStyle = wb.createCellStyle();
            headerLeftStyle.setFillForegroundColor(navyColor);
            headerLeftStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerLeftStyle.setFont(whiteBoldFont);
            headerLeftStyle.setAlignment(HorizontalAlignment.LEFT);
            headerLeftStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 1. Key Assumptions Sheet
            Sheet wsAssump = wb.createSheet("Key Assumptions");
            Row r0 = wsAssump.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("CONTINGENT CLAIMS ANALYSIS — VALUATION REPORT");
            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);
            c0.setCellStyle(titleStyle);

            Row r1 = wsAssump.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue(String.format("Company: %s | Client: %s | Status: %s",
                    response.getCompanyName(), response.getClientName(), response.getReportStatus()));
            CellStyle subStyle = wb.createCellStyle();
            subStyle.setFont(italicFont);
            c1.setCellStyle(subStyle);

            String[][] assumptions = {
                    {"Valuation Parameter", "Calibration Date", "Valuation Date"},
                    {"Effective Measurement Date", response.getCalibrationDate(), response.getValuationDate()},
                    {"Global Expected Exit Date", response.getExitDate(), response.getExitDate()},
                    {"Day Count Convention", response.getDayCountName(), response.getDayCountName()},
                    {"Reporting Currency", response.getReportCurrency(), response.getReportCurrency()},
                    {"Display Units", response.getDisplayUnits(), response.getDisplayUnits()},
                    {"Risk-Free Rate (Annual Effective)", String.format(Locale.US, "%.2f%%", response.getRfCalibrationEffective()), String.format(Locale.US, "%.2f%%", response.getRfValuationEffective())},
                    {"Risk-Free Rate (Continuous rc)", String.format(Locale.US, "%.4f%%", response.getRfCalibrationContinuous()), String.format(Locale.US, "%.4f%%", response.getRfValuationContinuous())},
                    {"Expected Volatility (Annualized)", String.format(Locale.US, "%.1f%%", response.getCalibrationOpm() != null ? response.getCalibrationOpm().getVolatility() : 0.0), String.format(Locale.US, "%.1f%%", response.getValuationOpm() != null ? response.getValuationOpm().getVolatility() : 0.0)},
                    {"Term to Liquidity (Years)", String.format(Locale.US, "%.2f yrs", response.getTermCalibration()), String.format(Locale.US, "%.2f yrs", response.getTermValuation())},
                    {"Concluded Equity Value", String.format(Locale.US, "$%,.2f", response.getCalibrationSolvedEquity()), String.format(Locale.US, "$%,.2f", response.getConcludedEquityValue())}
            };

            for (int i = 0; i < assumptions.length; i++) {
                Row row = wsAssump.createRow(3 + i);
                for (int c = 0; c < 3; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(assumptions[i][c]);
                    if (i == 0) {
                        cell.setCellStyle(c == 0 ? headerLeftStyle : headerStyle);
                    } else {
                        CellStyle cellStyle = wb.createCellStyle();
                        cellStyle.setFont(regularFont);
                        cell.setCellStyle(cellStyle);
                    }
                }
            }
            autoFitColumns(wsAssump, 3);

            // 2. Calibration & Valuation Cap Tables
            record CapTableConfig(String title, List<DerivedSecurity> securities) {}
            List<CapTableConfig> capTables = List.of(
                    new CapTableConfig("Calibration Cap Table", response.getCalibrationDerivedSecurities()),
                    new CapTableConfig("Valuation Cap Table", response.getValuationDerivedSecurities())
            );

            for (CapTableConfig cfg : capTables) {
                Sheet ws = wb.createSheet(cfg.title());
                Row row0 = ws.createRow(0);
                Cell cell0 = row0.createCell(0);
                cell0.setCellValue(String.format("Capitalization Table & Instrument Terms (%s)", cfg.title()));
                cell0.setCellStyle(titleStyle);

                String[] headers = {
                        "Share Class / Instrument", "Subtype", "Shares Outstanding", "Original Issue Price",
                        "Conversion Price", "Conversion Ratio", "Seniority", "Participation",
                        "Accrued Div / Share", "Total Preference Claim", "Fully Diluted Shares"
                };
                Row hRow = ws.createRow(2);
                for (int c = 0; c < headers.length; c++) {
                    Cell cell = hRow.createCell(c);
                    cell.setCellValue(headers[c]);
                    cell.setCellStyle(c == 0 ? headerLeftStyle : headerStyle);
                }

                double totShares = 0.0;
                double totPref = 0.0;
                double totFd = 0.0;

                int rowIdx = 3;
                if (cfg.securities() != null) {
                    for (DerivedSecurity sec : cfg.securities()) {
                        Row r = ws.createRow(rowIdx++);
                        r.createCell(0).setCellValue(sec.getSecurity());
                        r.createCell(1).setCellValue(sec.getSecuritySubtype());

                        Cell cShares = r.createCell(2);
                        cShares.setCellValue(sec.getShares());
                        setNumberFormat(wb, cShares, numberFormat, regularFont);
                        totShares += sec.getShares();

                        Cell cOip = r.createCell(3);
                        if (sec.getOriginalIssuePrice() > 0) {
                            cOip.setCellValue(sec.getOriginalIssuePrice());
                            setNumberFormat(wb, cOip, currencyFormat, regularFont);
                        } else {
                            cOip.setCellValue("—");
                        }

                        Cell cCp = r.createCell(4);
                        if (sec.getConversionPrice() != null && sec.getConversionPrice() > 0) {
                            cCp.setCellValue(sec.getConversionPrice());
                            setNumberFormat(wb, cCp, currencyFormat, regularFont);
                        } else {
                            cCp.setCellValue("—");
                        }

                        r.createCell(5).setCellValue(sec.getConversionRatio() > 0 ? String.format(Locale.US, "%.2fx", sec.getConversionRatio()) : "0.00x");
                        r.createCell(6).setCellValue(sec.getSeniority() < 900 ? String.valueOf(sec.getSeniority()) : "—");
                        r.createCell(7).setCellValue(sec.getParticipation());

                        Cell cDiv = r.createCell(8);
                        cDiv.setCellValue(sec.getPerShareDividend());
                        setNumberFormat(wb, cDiv, currencyFormat, regularFont);

                        Cell cPref = r.createCell(9);
                        cPref.setCellValue(sec.getTotalLiquidationPreference());
                        setNumberFormat(wb, cPref, currencyFormat, regularFont);
                        totPref += sec.getTotalLiquidationPreference();

                        Cell cFd = r.createCell(10);
                        cFd.setCellValue(sec.getFullyDilutedShares());
                        setNumberFormat(wb, cFd, numberFormat, regularFont);
                        totFd += sec.getFullyDilutedShares();
                    }
                }

                // Total row
                Row totRow = ws.createRow(rowIdx);
                totRow.createCell(0).setCellValue("TOTAL");
                for (int c = 1; c < headers.length; c++) {
                    totRow.createCell(c);
                }
                Cell totShCell = totRow.getCell(2);
                totShCell.setCellValue(totShares);
                setTotalStyle(wb, totShCell, numberFormat, boldFont, totalColor);

                Cell totPrefCell = totRow.getCell(9);
                totPrefCell.setCellValue(totPref);
                setTotalStyle(wb, totPrefCell, currencyFormat, boldFont, totalColor);

                Cell totFdCell = totRow.getCell(10);
                totFdCell.setCellValue(totFd);
                setTotalStyle(wb, totFdCell, numberFormat, boldFont, totalColor);

                for (int c = 0; c < headers.length; c++) {
                    if (c != 2 && c != 9 && c != 10) {
                        setTotalStyle(wb, totRow.getCell(c), null, boldFont, totalColor);
                    }
                }

                autoFitColumns(ws, headers.length);
            }

            // 3. Breakpoint Schedule
            Sheet wsBp = wb.createSheet("Breakpoint Schedule");
            Row rBp0 = wsBp.createRow(0);
            Cell cBp0 = rBp0.createCell(0);
            cBp0.setCellValue("Valuation Date Breakpoint Schedule");
            cBp0.setCellStyle(titleStyle);

            String[] bpHeaders = {"Tier", "Start Equity ($)", "End Equity ($)", "Width ($)", "Claimants", "Trigger Description"};
            Row bpHeadRow = wsBp.createRow(2);
            for (int c = 0; c < bpHeaders.length; c++) {
                Cell cell = bpHeadRow.createCell(c);
                cell.setCellValue(bpHeaders[c]);
                cell.setCellStyle(c == 0 ? headerLeftStyle : headerStyle);
            }

            int bpRowIdx = 3;
            if (response.getValuationBreakpoints() != null) {
                for (BreakpointTier bp : response.getValuationBreakpoints()) {
                    Row r = wsBp.createRow(bpRowIdx++);
                    r.createCell(0).setCellValue(bp.getTier());

                    Cell cStart = r.createCell(1);
                    cStart.setCellValue(bp.getStartEquity());
                    setNumberFormat(wb, cStart, currencyFormat, regularFont);

                    Cell cEnd = r.createCell(2);
                    cEnd.setCellValue(bp.getEndEquity());
                    setNumberFormat(wb, cEnd, currencyFormat, regularFont);

                    Cell cWidth = r.createCell(3);
                    cWidth.setCellValue(bp.getWidth());
                    setNumberFormat(wb, cWidth, currencyFormat, regularFont);

                    r.createCell(4).setCellValue(bp.getClaimantsDescription());
                    r.createCell(5).setCellValue(bp.getEventDescription());
                }
            }
            autoFitColumns(wsBp, bpHeaders.length);

            // 4. Valuation OPM Allocation
            Sheet wsOpm = wb.createSheet("Valuation OPM Allocation");
            Row rOpm0 = wsOpm.createRow(0);
            Cell cOpm0 = rOpm0.createCell(0);
            cOpm0.setCellValue("Valuation Date Option Pricing Method (OPM) Allocation");
            cOpm0.setCellStyle(titleStyle);

            Row rOpm1 = wsOpm.createRow(1);
            Cell cOpm1 = rOpm1.createCell(0);
            cOpm1.setCellValue(String.format(Locale.US, "Concluded Enterprise Equity Value: $%,.2f", response.getConcludedEquityValue()));
            cOpm1.setCellStyle(subStyle);

            String[] opmHeaders = {"Share Class", "Shares", "Concluded Value ($)", "Value Per Share ($)", "% Allocation", "Fully Diluted %"};
            Row opmHeadRow = wsOpm.createRow(3);
            for (int c = 0; c < opmHeaders.length; c++) {
                Cell cell = opmHeadRow.createCell(c);
                cell.setCellValue(opmHeaders[c]);
                cell.setCellStyle(c == 0 ? headerLeftStyle : headerStyle);
            }

            double totOpmShares = 0.0;
            double totFdVal = 0.0;
            if (response.getValuationDerivedSecurities() != null) {
                for (DerivedSecurity s : response.getValuationDerivedSecurities()) {
                    totFdVal += s.getFullyDilutedShares();
                }
            }

            int opmRowIdx = 4;
            if (response.getValuationDerivedSecurities() != null && response.getValuationOpm() != null) {
                for (DerivedSecurity sec : response.getValuationDerivedSecurities()) {
                    String name = sec.getSecurity();
                    double totVal = response.getValuationOpm().getAllocatedValues().getOrDefault(name, 0.0);
                    double psVal = response.getValuationOpm().getPerShareValues().getOrDefault(name, 0.0);
                    double pctVal = response.getValuationOpm().getPercentAllocations().getOrDefault(name, 0.0);
                    double fdPct = totFdVal > 0.0 ? (sec.getFullyDilutedShares() / totFdVal) : 0.0;
                    totOpmShares += sec.getShares();

                    Row r = wsOpm.createRow(opmRowIdx++);
                    r.createCell(0).setCellValue(name);

                    Cell cSh = r.createCell(1);
                    cSh.setCellValue(sec.getShares());
                    setNumberFormat(wb, cSh, numberFormat, regularFont);

                    Cell cTot = r.createCell(2);
                    cTot.setCellValue(totVal);
                    setNumberFormat(wb, cTot, currencyFormat, regularFont);

                    Cell cPs = r.createCell(3);
                    cPs.setCellValue(psVal);
                    setNumberFormat(wb, cPs, currencyFormat, regularFont);

                    Cell cPct = r.createCell(4);
                    cPct.setCellValue(pctVal);
                    setNumberFormat(wb, cPct, percentFormat, regularFont);

                    Cell cFdPct = r.createCell(5);
                    cFdPct.setCellValue(fdPct);
                    setNumberFormat(wb, cFdPct, percentFormat, regularFont);
                }
            }

            // Total row for OPM
            Row opmTotRow = wsOpm.createRow(opmRowIdx);
            opmTotRow.createCell(0).setCellValue("TOTAL");
            for (int c = 1; c < opmHeaders.length; c++) opmTotRow.createCell(c);

            Cell totShOpm = opmTotRow.getCell(1);
            totShOpm.setCellValue(totOpmShares);
            setTotalStyle(wb, totShOpm, numberFormat, boldFont, totalColor);

            Cell totValOpm = opmTotRow.getCell(2);
            totValOpm.setCellValue(response.getValuationOpm() != null ? response.getValuationOpm().getTotalAllocated() : 0.0);
            setTotalStyle(wb, totValOpm, currencyFormat, boldFont, totalColor);

            opmTotRow.getCell(3).setCellValue("—");
            setTotalStyle(wb, opmTotRow.getCell(3), null, boldFont, totalColor);

            Cell totPctOpm = opmTotRow.getCell(4);
            totPctOpm.setCellValue(1.0);
            setTotalStyle(wb, totPctOpm, percentFormat, boldFont, totalColor);

            Cell totFdPctOpm = opmTotRow.getCell(5);
            totFdPctOpm.setCellValue(1.0);
            setTotalStyle(wb, totFdPctOpm, percentFormat, boldFont, totalColor);

            setTotalStyle(wb, opmTotRow.getCell(0), null, boldFont, totalColor);
            autoFitColumns(wsOpm, opmHeaders.length);

            // 5. Client Holdings Summary
            Sheet wsHold = wb.createSheet("Client Holdings Summary");
            Row rH0 = wsHold.createRow(0);
            Cell cH0 = rH0.createCell(0);
            cH0.setCellValue("Client Holdings & Valuation Summary");
            cH0.setCellStyle(titleStyle);

            String[] hHeaders = {
                    "Fund / Vehicle", "Security", "Units Held", "Investment Cost ($)",
                    "Valuation Fair Value / Share ($)", "Concluded Fair Value ($)",
                    "Class Ownership %", "FD Ownership %", "MOIC"
            };
            Row hHeadRow = wsHold.createRow(2);
            for (int c = 0; c < hHeaders.length; c++) {
                Cell cell = hHeadRow.createCell(c);
                cell.setCellValue(hHeaders[c]);
                cell.setCellStyle(c == 0 ? headerLeftStyle : headerStyle);
            }

            int hRowIdx = 3;
            double totUnits = 0.0;
            if (response.getHoldings() != null && response.getHoldings().getItems() != null) {
                for (HoldingResultItem it : response.getHoldings().getItems()) {
                    Row r = wsHold.createRow(hRowIdx++);
                    r.createCell(0).setCellValue(it.getFund());
                    r.createCell(1).setCellValue(it.getSecurity());

                    Cell cU = r.createCell(2);
                    cU.setCellValue(it.getUnits());
                    setNumberFormat(wb, cU, numberFormat, regularFont);
                    totUnits += it.getUnits();

                    Cell cCost = r.createCell(3);
                    cCost.setCellValue(it.getCost());
                    setNumberFormat(wb, cCost, currencyFormat, regularFont);

                    Cell cPs = r.createCell(4);
                    cPs.setCellValue(it.getFairValuePerShare());
                    setNumberFormat(wb, cPs, currencyFormat, regularFont);

                    Cell cVal = r.createCell(5);
                    cVal.setCellValue(it.getConcludedFairValue());
                    setNumberFormat(wb, cVal, currencyFormat, regularFont);

                    Cell cClass = r.createCell(6);
                    cClass.setCellValue(it.getClassOwnershipPct());
                    setNumberFormat(wb, cClass, percentFormat, regularFont);

                    Cell cFd = r.createCell(7);
                    cFd.setCellValue(it.getFullyDilutedOwnershipPct());
                    setNumberFormat(wb, cFd, percentFormat, regularFont);

                    r.createCell(8).setCellValue(it.getMoic() != null ? String.format(Locale.US, "%.2fx", it.getMoic()) : "—");
                }
            }

            // Total row
            Row hTotRow = wsHold.createRow(hRowIdx);
            hTotRow.createCell(0).setCellValue("TOTAL PORTFOLIO");
            for (int c = 1; c < hHeaders.length; c++) hTotRow.createCell(c);

            Cell totUCell = hTotRow.getCell(2);
            totUCell.setCellValue(totUnits);
            setTotalStyle(wb, totUCell, numberFormat, boldFont, totalColor);

            Cell totCostCell = hTotRow.getCell(3);
            totCostCell.setCellValue(response.getHoldings() != null ? response.getHoldings().getTotalCost() : 0.0);
            setTotalStyle(wb, totCostCell, currencyFormat, boldFont, totalColor);

            hTotRow.getCell(4).setCellValue("—");
            setTotalStyle(wb, hTotRow.getCell(4), null, boldFont, totalColor);

            Cell totValCell = hTotRow.getCell(5);
            totValCell.setCellValue(response.getHoldings() != null ? response.getHoldings().getTotalValue() : 0.0);
            setTotalStyle(wb, totValCell, currencyFormat, boldFont, totalColor);

            hTotRow.getCell(6).setCellValue("—");
            setTotalStyle(wb, hTotRow.getCell(6), null, boldFont, totalColor);
            hTotRow.getCell(7).setCellValue("—");
            setTotalStyle(wb, hTotRow.getCell(7), null, boldFont, totalColor);

            String moicStr = (response.getHoldings() != null && response.getHoldings().getConsolidatedMoic() != null)
                    ? String.format(Locale.US, "%.2fx", response.getHoldings().getConsolidatedMoic()) : "—";
            hTotRow.getCell(8).setCellValue(moicStr);
            setTotalStyle(wb, hTotRow.getCell(8), null, boldFont, totalColor);
            setTotalStyle(wb, hTotRow.getCell(0), null, boldFont, totalColor);
            setTotalStyle(wb, hTotRow.getCell(1), null, boldFont, totalColor);

            autoFitColumns(wsHold, hHeaders.length);

            // 6. Waterfall Analysis
            Sheet wsWf = wb.createSheet("Waterfall Analysis");
            Row rWf0 = wsWf.createRow(0);
            Cell cWf0 = rWf0.createCell(0);
            double appliedEq = response.getWaterfall() != null ? response.getWaterfall().getAppliedEquity() : 0.0;
            cWf0.setCellValue(String.format(Locale.US, "Waterfall Distribution at Selected Equity: $%,.2f", appliedEq));
            cWf0.setCellStyle(titleStyle);

            String[] wfHeaders = {"Share Class", "Shares", "Proceeds ($)", "Proceeds Per Share ($)", "% Recovery", "% of Total Proceeds"};
            Row wfHeadRow = wsWf.createRow(2);
            for (int c = 0; c < wfHeaders.length; c++) {
                Cell cell = wfHeadRow.createCell(c);
                cell.setCellValue(wfHeaders[c]);
                cell.setCellStyle(c == 0 ? headerLeftStyle : headerStyle);
            }

            int wfRowIdx = 3;
            if (response.getWaterfall() != null && response.getWaterfall().getDistribution() != null) {
                for (WaterfallAllocationItem item : response.getWaterfall().getDistribution()) {
                    Row r = wsWf.createRow(wfRowIdx++);
                    r.createCell(0).setCellValue(item.getSecurity());

                    Cell cSh = r.createCell(1);
                    cSh.setCellValue(item.getShares());
                    setNumberFormat(wb, cSh, numberFormat, regularFont);

                    Cell cProc = r.createCell(2);
                    cProc.setCellValue(item.getProceeds());
                    setNumberFormat(wb, cProc, currencyFormat, regularFont);

                    Cell cPs = r.createCell(3);
                    cPs.setCellValue(item.getProceedsPerShare());
                    setNumberFormat(wb, cPs, currencyFormat, regularFont);

                    Cell cRec = r.createCell(4);
                    cRec.setCellValue(item.getPercentRecovery());
                    setNumberFormat(wb, cRec, percentFormat, regularFont);

                    Cell cTot = r.createCell(5);
                    cTot.setCellValue(item.getPercentOfTotal());
                    setNumberFormat(wb, cTot, percentFormat, regularFont);
                }
            }
            autoFitColumns(wsWf, wfHeaders.length);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate valuation workbook: " + e.getMessage(), e);
        }
    }

    private void setNumberFormat(Workbook wb, Cell cell, short format, Font font) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(format);
        style.setFont(font);
        cell.setCellStyle(style);
    }

    private void setTotalStyle(Workbook wb, Cell cell, Short format, Font font, XSSFColor totalColor) {
        XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
        style.setFillForegroundColor(totalColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.DOUBLE);
        if (format != null) {
            style.setDataFormat(format);
        }
        cell.setCellStyle(style);
    }

    private void autoFitColumns(Sheet sheet, int cols) {
        for (int c = 0; c < cols; c++) {
            sheet.autoSizeColumn(c);
            int currWidth = sheet.getColumnWidth(c);
            sheet.setColumnWidth(c, Math.max(currWidth + 1024, 3072));
        }
    }
}

