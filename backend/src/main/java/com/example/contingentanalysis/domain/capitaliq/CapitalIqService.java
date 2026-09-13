package com.example.contingentanalysis.domain.capitaliq;

import com.example.contingentanalysis.domain.model.ComparableCompanyVol;
import com.example.contingentanalysis.domain.model.VolatilityAnalysisResult;
import com.example.contingentanalysis.domain.model.VolatilityStats;
import com.example.contingentanalysis.domain.volatility.VolatilityService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class CapitalIqService {

    private final VolatilityService volatilityService;

    public CapitalIqService(VolatilityService volatilityService) {
        this.volatilityService = volatilityService;
    }

    public byte[] createCapitalIqBridgeWorkbook(List<String> tickers,
                                                String calibrationDate,
                                                String valuationDate,
                                                String currency,
                                                String frequency,
                                                int lookbackYears) {
        if (tickers == null) tickers = Collections.emptyList();
        if (currency == null) currency = "USD";
        if (frequency == null) frequency = "Weekly";
        if (lookbackYears <= 0) lookbackYears = 2;

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet wsIntro = wb.createSheet("Instructions");

            byte[] darkBytes = new byte[]{(byte) 0x17, (byte) 0x24, (byte) 0x2B};
            byte[] tealBytes = new byte[]{(byte) 0x0B, (byte) 0x6B, (byte) 0x68};

            XSSFColor darkColor = new XSSFColor(darkBytes, null);
            XSSFColor tealColor = new XSSFColor(tealBytes, null);

            XSSFCellStyle darkStyle = wb.createCellStyle();
            darkStyle.setFillForegroundColor(darkColor);
            darkStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont whiteBoldFont = wb.createFont();
            whiteBoldFont.setFontName("Calibri");
            whiteBoldFont.setFontHeightInPoints((short) 11);
            whiteBoldFont.setBold(true);
            whiteBoldFont.setColor(IndexedColors.WHITE.getIndex());
            darkStyle.setFont(whiteBoldFont);
            darkStyle.setAlignment(HorizontalAlignment.CENTER);
            darkStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFCellStyle tealStyle = wb.createCellStyle();
            tealStyle.setFillForegroundColor(tealColor);
            tealStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            tealStyle.setFont(whiteBoldFont);

            // Row 0: Title
            Row titleRow = wsIntro.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("CAPITAL IQ VOLATILITY BRIDGE WORKBOOK");
            XSSFFont titleFont = wb.createFont();
            titleFont.setFontName("Calibri");
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setBold(true);
            titleFont.setColor(tealColor);
            XSSFCellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Instructions
            String[] instructions = {
                    "Instructions for Excel Desktop:",
                    "1. Open this file in Microsoft Excel Desktop with the S&P Capital IQ plugin signed in.",
                    "2. Click 'Refresh All' on the Capital IQ ribbon.",
                    "3. Wait for all Capital IQ formulas to evaluate and return numeric data.",
                    "4. Save the refreshed workbook (Ctrl+S).",
                    "5. Upload the saved workbook back into the Contingent Claims Analysis application."
            };
            for (int i = 0; i < instructions.length; i++) {
                Row r = wsIntro.createRow(i + 1);
                r.createCell(0).setCellValue(instructions[i]);
            }

            // Metadata
            String[][] meta = {
                    {"Report Currency", currency},
                    {"Calibration Date", String.valueOf(calibrationDate)},
                    {"Valuation Date", String.valueOf(valuationDate)},
                    {"Frequency", frequency},
                    {"Lookback (Years)", String.valueOf(lookbackYears)}
            };
            for (int i = 0; i < meta.length; i++) {
                Row r = wsIntro.createRow(8 + i);
                r.createCell(0).setCellValue(meta[i][0]);
                r.createCell(1).setCellValue(meta[i][1]);
            }

            String[][] dates = {
                    {"Calibration", calibrationDate},
                    {"Valuation", valuationDate}
            };

            for (String[] pair : dates) {
                String label = pair[0];
                String dt = pair[1];

                Sheet wsSnap = wb.createSheet("Snapshot_" + label);
                String[] headers = {
                        "Include", "CIQ ID", "Company Name", "Market Date",
                        "Share Price (" + currency + ")", "Shares Out (mm)",
                        "Minority Interest (" + currency + " mm)", "Preferred Equity (" + currency + " mm)",
                        "Total Debt (" + currency + " mm)", "Currency"
                };

                Row headerRow = wsSnap.createRow(0);
                for (int c = 0; c < headers.length; c++) {
                    Cell cell = headerRow.createCell(c);
                    cell.setCellValue(headers[c]);
                    cell.setCellStyle(darkStyle);
                }

                String dateStr = dt != null ? dt : "";
                for (int i = 0; i < tickers.size(); i++) {
                    String qTicker = tickers.get(i).trim();
                    Row row = wsSnap.createRow(i + 1);
                    row.createCell(0).setCellValue("x");
                    row.createCell(1).setCellValue(qTicker);
                    row.createCell(2).setCellFormula("IQ_COMPANY_NAME(\"" + qTicker + "\")");
                    row.createCell(3).setCellValue(dateStr);
                    row.createCell(4).setCellFormula("IQ_CLOSEPRICE(\"" + qTicker + "\", \"" + dateStr + "\")");
                    row.createCell(5).setCellFormula("IQ_SHARESOUTSTANDING(\"" + qTicker + "\", \"" + dateStr + "\")");
                    row.createCell(6).setCellFormula("IQ_MINORITY_INTEREST(\"" + qTicker + "\", \"" + dateStr + "\")");
                    row.createCell(7).setCellFormula("IQ_PREF_EQUITY(\"" + qTicker + "\", \"" + dateStr + "\")");
                    row.createCell(8).setCellFormula("IQ_TOTAL_DEBT(\"" + qTicker + "\", \"" + dateStr + "\")");
                    row.createCell(9).setCellValue(currency);
                }

                // Prices sheet
                Sheet wsPrices = wb.createSheet("Prices_" + label);
                Row pHeader = wsPrices.createRow(0);
                Cell dCell = pHeader.createCell(0);
                dCell.setCellValue("Date");
                dCell.setCellStyle(tealStyle);

                for (int c = 0; c < tickers.size(); c++) {
                    Cell cell = pHeader.createCell(c + 1);
                    cell.setCellValue(tickers.get(c));
                    cell.setCellStyle(tealStyle);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Capital IQ bridge workbook: " + e.getMessage(), e);
        }
    }

    public Map<String, VolatilityAnalysisResult> parseCapitalIqWorkbook(byte[] fileBytes) {
        return parseCapitalIqWorkbook(fileBytes, 0.02, 1.0, 287252502.92, 0.0, 0.0, 15L * 1024 * 1024);
    }

    public Map<String, VolatilityAnalysisResult> parseCapitalIqWorkbook(byte[] fileBytes,
                                                                       double rfRate,
                                                                       double term,
                                                                       double subjectEquity,
                                                                       double subjectDebt,
                                                                       double subjectPref,
                                                                       long maxFileSizeBytes) {
        if (fileBytes.length > maxFileSizeBytes) {
            throw new IllegalArgumentException(String.format("Uploaded file exceeds maximum limit of %.0fMB",
                    maxFileSizeBytes / (1024.0 * 1024.0)));
        }

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            Map<String, VolatilityAnalysisResult> results = new LinkedHashMap<>();

            for (String label : List.of("Calibration", "Valuation")) {
                String snapTitle = "Snapshot_" + label;
                String pricesTitle = "Prices_" + label;

                Sheet wsSnap = wb.getSheet(snapTitle);
                if (wsSnap == null) {
                    continue;
                }

                int numRows = wsSnap.getPhysicalNumberOfRows();
                if (numRows < 2) {
                    continue;
                }

                Row headerRow = wsSnap.getRow(0);
                if (headerRow == null) {
                    continue;
                }

                List<String> headers = new ArrayList<>();
                for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                    Cell cell = headerRow.getCell(c);
                    headers.add(cell != null ? cell.toString().trim().toLowerCase(Locale.ROOT) : "");
                }

                int ciqIdx = findCol(headers, List.of("ciq id", "ticker", "symbol"), 1);
                int nameIdx = findCol(headers, List.of("company name", "name"), 2);
                int priceIdx = findCol(headers, List.of("share price", "price"), 4);
                int sharesIdx = findCol(headers, List.of("shares out", "shares"), 5);
                int debtIdx = findCol(headers, List.of("total debt", "debt"), 8);
                int prefIdx = findCol(headers, List.of("preferred equity", "pref"), 7);
                int minIdx = findCol(headers, List.of("minority interest", "minority"), 6);

                // Parse price history for equity volatility
                Map<String, Double> volByTicker = new HashMap<>();
                Sheet wsPrices = wb.getSheet(pricesTitle);
                if (wsPrices != null && wsPrices.getPhysicalNumberOfRows() > 3) {
                    Row pHead = wsPrices.getRow(0);
                    List<String> pHeaders = new ArrayList<>();
                    if (pHead != null) {
                        for (int c = 0; c < pHead.getLastCellNum(); c++) {
                            Cell cell = pHead.getCell(c);
                            pHeaders.add(cell != null ? cell.toString().trim() : "");
                        }
                    }

                    for (int col = 1; col < pHeaders.size(); col++) {
                        String ticker = pHeaders.get(col);
                        if (ticker.isEmpty()) continue;

                        List<Double> prices = new ArrayList<>();
                        for (int r = 1; r <= wsPrices.getLastRowNum(); r++) {
                            Row row = wsPrices.getRow(r);
                            if (row != null) {
                                Cell cell = row.getCell(col);
                                Double v = getNumericCellValue(cell);
                                if (v != null && v > 0.0) {
                                    prices.add(v);
                                }
                            }
                        }

                        if (prices.size() > 5) {
                            List<Double> logReturns = new ArrayList<>();
                            for (int i = 0; i < prices.size() - 1; i++) {
                                logReturns.add(Math.log(prices.get(i + 1) / prices.get(i)));
                            }
                            double std = calculateSampleStd(logReturns);
                            double annVol = std * Math.sqrt(52.0); // Default weekly
                            volByTicker.put(ticker, annVol);
                        }
                    }
                }

                List<ComparableCompanyVol> companies = new ArrayList<>();
                for (int r = 1; r <= wsSnap.getLastRowNum(); r++) {
                    Row row = wsSnap.getRow(r);
                    if (row == null) continue;

                    Cell ciqCell = row.getCell(ciqIdx);
                    String ticker = ciqCell != null ? ciqCell.toString().trim() : "";
                    if (ticker.isEmpty() || ticker.equalsIgnoreCase("none")) continue;

                    Cell nameCell = row.getCell(nameIdx);
                    String name = (nameCell != null && !nameCell.toString().trim().isEmpty())
                            ? nameCell.toString().trim() : ticker;

                    double p = getDouble(row.getCell(priceIdx));
                    double sh = getDouble(row.getCell(sharesIdx));
                    double debt = getDouble(row.getCell(debtIdx));
                    double pref = getDouble(row.getCell(prefIdx));
                    double mi = getDouble(row.getCell(minIdx));

                    double mcap = (p > 0.0 && sh > 0.0) ? (p * sh) : 0.0;
                    double eqVol = volByTicker.getOrDefault(ticker, 0.35);

                    double assetVol = volatilityService.mertonAssetVolatility(eqVol, mcap, debt, rfRate, term);
                    if (!Double.isFinite(assetVol)) {
                        assetVol = eqVol;
                    }

                    double relevVol = volatilityService.releverEquityVolatility(assetVol, subjectEquity, subjectDebt, subjectPref);

                    companies.add(new ComparableCompanyVol(
                            ticker, name, p, mcap, debt, pref, mi, eqVol, assetVol,
                            Double.isFinite(relevVol) ? relevVol : null, "USD", true
                    ));
                }

                List<Double> eqVals = new ArrayList<>();
                List<Double> astVals = new ArrayList<>();
                List<Double> relVals = new ArrayList<>();
                for (ComparableCompanyVol c : companies) {
                    eqVals.add(c.getEquityVol());
                    astVals.add(c.getAssetVol());
                    if (c.getReleveredVol() != null) {
                        relVals.add(c.getReleveredVol());
                    }
                }

                VolatilityStats eqStats = volatilityService.calculateVolatilityStats(eqVals);
                VolatilityStats astStats = volatilityService.calculateVolatilityStats(astVals);
                VolatilityStats relStats = !relVals.isEmpty() ? volatilityService.calculateVolatilityStats(relVals) : null;

                double selectedVol = eqStats != null ? eqStats.getMedian() : 0.35;
                double de = subjectEquity > 0.0 ? (subjectDebt + subjectPref) / subjectEquity : 0.0;

                VolatilityAnalysisResult var = new VolatilityAnalysisResult();
                var.setCompanies(companies);
                var.setEquityStats(eqStats);
                var.setAssetStats(astStats);
                var.setReleveredStats(relStats);
                var.setSelectedBasis("equity");
                var.setSelectedStat("median");
                var.setSelectedVolatility(selectedVol);
                var.setSubjectEquityValue(subjectEquity);
                var.setSubjectDebtAndPref(subjectDebt + subjectPref);
                var.setSubjectDebtToEquity(de);
                var.setSubjectReleveredVol((astStats != null) ? (astStats.getMedian() * (1.0 + de)) : selectedVol);

                results.put(label, var);
            }

            return results;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Excel workbook: " + e.getMessage(), e);
        }
    }

    private int findCol(List<String> headers, List<String> candidates, int fallback) {
        for (String c : candidates) {
            for (int idx = 0; idx < headers.size(); idx++) {
                if (headers.get(idx).contains(c)) {
                    return idx;
                }
            }
        }
        return fallback;
    }

    private Double getNumericCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                return Double.parseDouble(cell.getStringCellValue().replace(",", "").trim());
            } else if (cell.getCellType() == CellType.FORMULA) {
                if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                    return cell.getNumericCellValue();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private double getDouble(Cell cell) {
        Double v = getNumericCellValue(cell);
        return v != null ? v : 0.0;
    }

    private double calculateSampleStd(List<Double> values) {
        if (values.size() < 2) return 0.0;
        double sum = 0.0;
        for (double v : values) sum += v;
        double mean = sum / values.size();
        double varSum = 0.0;
        for (double v : values) {
            varSum += (v - mean) * (v - mean);
        }
        return Math.sqrt(varSum / (values.size() - 1));
    }
}

