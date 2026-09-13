package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class ValuationRequest {
    @JsonProperty("company_name")
    private String companyName = "TADO";

    @JsonProperty("client_name")
    private String clientName = "S2G Investments";

    @JsonProperty("report_status")
    private String reportStatus = "DRAFT - For Discussion Purposes Only";

    @JsonProperty("report_purpose")
    private String reportPurpose = "Valuation Analysis";

    @JsonProperty("report_purpose_manual")
    private String reportPurposeManual = "";

    @JsonProperty("calibration_date")
    private String calibrationDate = "2025-02-26";

    @JsonProperty("valuation_date")
    private String valuationDate = "2026-06-30";

    @JsonProperty("exit_date")
    private String exitDate = "2027-06-30";

    @JsonProperty("day_count_basis")
    private int dayCountBasis = 1;

    @JsonProperty("report_currency")
    private String reportCurrency = "EUR";

    @JsonProperty("display_units")
    private String displayUnits = "actual";

    @JsonProperty("calibration_securities")
    private List<SecurityInput> calibrationSecurities = new ArrayList<>();

    @JsonProperty("valuation_securities")
    private List<SecurityInput> valuationSecurities = new ArrayList<>();

    @JsonProperty("calibration_security_name")
    private String calibrationSecurityName = "Series I";

    @JsonProperty("transaction_price")
    private double transactionPrice = 2021.90;

    @JsonProperty("rf_calibration")
    private double rfCalibration = 2.1;

    @JsonProperty("vol_calibration")
    private double volCalibration = 35.0;

    @JsonProperty("dividend_yield_calibration")
    private double dividendYieldCalibration = 0.0;

    @JsonProperty("rf_valuation")
    private double rfValuation = 2.4;

    @JsonProperty("vol_valuation")
    private double volValuation = 35.0;

    @JsonProperty("dividend_yield_valuation")
    private double dividendYieldValuation = 0.0;

    @JsonProperty("market_adjustment")
    private double marketAdjustment = 0.0;

    @JsonProperty("company_adjustment")
    private double companyAdjustment = 0.0;

    @JsonProperty("waterfall_equity_source")
    private String waterfallEquitySource = "concluded";

    @JsonProperty("manual_waterfall_equity")
    private double manualWaterfallEquity = 229900000.0;

    private List<HoldingInput> holdings = new ArrayList<>();

    @JsonProperty("firm_logo_base64")
    private String firmLogoBase64;

    @JsonProperty("show_secondary_currency")
    private boolean showSecondaryCurrency = false;

    @JsonProperty("secondary_currency")
    private String secondaryCurrency = "USD";

    @JsonProperty("secondary_fx_rate")
    private double secondaryFxRate = 1.0;

    public ValuationRequest() {
    }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getReportStatus() { return reportStatus; }
    public void setReportStatus(String reportStatus) { this.reportStatus = reportStatus; }

    public String getReportPurpose() { return reportPurpose; }
    public void setReportPurpose(String reportPurpose) { this.reportPurpose = reportPurpose; }

    public String getReportPurposeManual() { return reportPurposeManual; }
    public void setReportPurposeManual(String reportPurposeManual) { this.reportPurposeManual = reportPurposeManual; }

    public String getCalibrationDate() { return calibrationDate; }
    public void setCalibrationDate(String calibrationDate) { this.calibrationDate = calibrationDate; }

    public String getValuationDate() { return valuationDate; }
    public void setValuationDate(String valuationDate) { this.valuationDate = valuationDate; }

    public String getExitDate() { return exitDate; }
    public void setExitDate(String exitDate) { this.exitDate = exitDate; }

    public int getDayCountBasis() { return dayCountBasis; }
    public void setDayCountBasis(int dayCountBasis) { this.dayCountBasis = dayCountBasis; }

    public String getReportCurrency() { return reportCurrency; }
    public void setReportCurrency(String reportCurrency) { this.reportCurrency = reportCurrency; }

    public String getDisplayUnits() { return displayUnits; }
    public void setDisplayUnits(String displayUnits) { this.displayUnits = displayUnits; }

    public List<SecurityInput> getCalibrationSecurities() { return calibrationSecurities; }
    public void setCalibrationSecurities(List<SecurityInput> calibrationSecurities) { this.calibrationSecurities = calibrationSecurities != null ? calibrationSecurities : new ArrayList<>(); }

    public List<SecurityInput> getValuationSecurities() { return valuationSecurities; }
    public void setValuationSecurities(List<SecurityInput> valuationSecurities) { this.valuationSecurities = valuationSecurities != null ? valuationSecurities : new ArrayList<>(); }

    public String getCalibrationSecurityName() { return calibrationSecurityName; }
    public void setCalibrationSecurityName(String calibrationSecurityName) { this.calibrationSecurityName = calibrationSecurityName; }

    public double getTransactionPrice() { return transactionPrice; }
    public void setTransactionPrice(double transactionPrice) { this.transactionPrice = transactionPrice; }

    public double getRfCalibration() { return rfCalibration; }
    public void setRfCalibration(double rfCalibration) { this.rfCalibration = rfCalibration; }

    public double getVolCalibration() { return volCalibration; }
    public void setVolCalibration(double volCalibration) { this.volCalibration = volCalibration; }

    public double getDividendYieldCalibration() { return dividendYieldCalibration; }
    public void setDividendYieldCalibration(double dividendYieldCalibration) { this.dividendYieldCalibration = dividendYieldCalibration; }

    public double getRfValuation() { return rfValuation; }
    public void setRfValuation(double rfValuation) { this.rfValuation = rfValuation; }

    public double getVolValuation() { return volValuation; }
    public void setVolValuation(double volValuation) { this.volValuation = volValuation; }

    public double getDividendYieldValuation() { return dividendYieldValuation; }
    public void setDividendYieldValuation(double dividendYieldValuation) { this.dividendYieldValuation = dividendYieldValuation; }

    public double getMarketAdjustment() { return marketAdjustment; }
    public void setMarketAdjustment(double marketAdjustment) { this.marketAdjustment = marketAdjustment; }

    public double getCompanyAdjustment() { return companyAdjustment; }
    public void setCompanyAdjustment(double companyAdjustment) { this.companyAdjustment = companyAdjustment; }

    public String getWaterfallEquitySource() { return waterfallEquitySource; }
    public void setWaterfallEquitySource(String waterfallEquitySource) { this.waterfallEquitySource = waterfallEquitySource; }

    public double getManualWaterfallEquity() { return manualWaterfallEquity; }
    public void setManualWaterfallEquity(double manualWaterfallEquity) { this.manualWaterfallEquity = manualWaterfallEquity; }

    public List<HoldingInput> getHoldings() { return holdings; }
    public void setHoldings(List<HoldingInput> holdings) { this.holdings = holdings != null ? holdings : new ArrayList<>(); }

    public String getFirmLogoBase64() { return firmLogoBase64; }
    public void setFirmLogoBase64(String firmLogoBase64) { this.firmLogoBase64 = firmLogoBase64; }

    public boolean isShowSecondaryCurrency() { return showSecondaryCurrency; }
    public void setShowSecondaryCurrency(boolean showSecondaryCurrency) { this.showSecondaryCurrency = showSecondaryCurrency; }

    public String getSecondaryCurrency() { return secondaryCurrency; }
    public void setSecondaryCurrency(String secondaryCurrency) { this.secondaryCurrency = secondaryCurrency; }

    public double getSecondaryFxRate() { return secondaryFxRate; }
    public void setSecondaryFxRate(double secondaryFxRate) { this.secondaryFxRate = secondaryFxRate; }
}

