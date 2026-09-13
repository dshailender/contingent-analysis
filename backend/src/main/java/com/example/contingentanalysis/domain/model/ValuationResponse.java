package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class ValuationResponse {
    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("report_status")
    private String reportStatus;

    @JsonProperty("report_purpose")
    private String reportPurpose;

    @JsonProperty("report_currency")
    private String reportCurrency;

    @JsonProperty("display_units")
    private String displayUnits;

    @JsonProperty("display_scale")
    private double displayScale = 1.0;

    @JsonProperty("firm_logo_base64")
    private String firmLogoBase64;

    @JsonProperty("show_secondary_currency")
    private boolean showSecondaryCurrency = false;

    @JsonProperty("secondary_currency")
    private String secondaryCurrency = "USD";

    @JsonProperty("secondary_fx_rate")
    private double secondaryFxRate = 1.0;

    @JsonProperty("calibration_date")
    private String calibrationDate;

    @JsonProperty("valuation_date")
    private String valuationDate;

    @JsonProperty("exit_date")
    private String exitDate;

    @JsonProperty("day_count_basis")
    private int dayCountBasis;

    @JsonProperty("day_count_name")
    private String dayCountName;

    @JsonProperty("term_calibration")
    private double termCalibration;

    @JsonProperty("term_valuation")
    private double termValuation;

    @JsonProperty("rf_calibration_effective")
    private double rfCalibrationEffective;

    @JsonProperty("rf_calibration_continuous")
    private double rfCalibrationContinuous;

    @JsonProperty("rf_valuation_effective")
    private double rfValuationEffective;

    @JsonProperty("rf_valuation_continuous")
    private double rfValuationContinuous;

    @JsonProperty("calibration_security_name")
    private String calibrationSecurityName;

    @JsonProperty("calibration_derived_securities")
    private List<DerivedSecurity> calibrationDerivedSecurities = new ArrayList<>();

    @JsonProperty("calibration_breakpoints")
    private List<BreakpointTier> calibrationBreakpoints = new ArrayList<>();

    @JsonProperty("calibration_claims")
    private List<ClaimTierAllocation> calibrationClaims = new ArrayList<>();

    @JsonProperty("calibration_solved_equity")
    private double calibrationSolvedEquity;

    @JsonProperty("calibration_opm")
    private OpmAllocationResult calibrationOpm;

    @JsonProperty("calibration_rf_analysis")
    private RiskFreeRateAnalysis calibrationRfAnalysis;

    @JsonProperty("valuation_derived_securities")
    private List<DerivedSecurity> valuationDerivedSecurities = new ArrayList<>();

    @JsonProperty("valuation_breakpoints")
    private List<BreakpointTier> valuationBreakpoints = new ArrayList<>();

    @JsonProperty("valuation_claims")
    private List<ClaimTierAllocation> valuationClaims = new ArrayList<>();

    @JsonProperty("concluded_equity_value")
    private double concludedEquityValue;

    @JsonProperty("valuation_opm")
    private OpmAllocationResult valuationOpm;

    @JsonProperty("valuation_rf_analysis")
    private RiskFreeRateAnalysis valuationRfAnalysis;

    private WaterfallResult waterfall;

    @JsonProperty("calibration_waterfall")
    private WaterfallResult calibrationWaterfall;

    @JsonProperty("comparative_waterfall")
    private List<ComparativeWaterfallItem> comparativeWaterfall = new ArrayList<>();

    private HoldingsSummary holdings;

    @JsonProperty("calibration_volatility")
    private VolatilityAnalysisResult calibrationVolatility;

    @JsonProperty("valuation_volatility")
    private VolatilityAnalysisResult valuationVolatility;

    public ValuationResponse() {
    }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getReportStatus() { return reportStatus; }
    public void setReportStatus(String reportStatus) { this.reportStatus = reportStatus; }

    public String getReportPurpose() { return reportPurpose; }
    public void setReportPurpose(String reportPurpose) { this.reportPurpose = reportPurpose; }

    public String getReportCurrency() { return reportCurrency; }
    public void setReportCurrency(String reportCurrency) { this.reportCurrency = reportCurrency; }

    public String getDisplayUnits() { return displayUnits; }
    public void setDisplayUnits(String displayUnits) { this.displayUnits = displayUnits; }

    public double getDisplayScale() { return displayScale; }
    public void setDisplayScale(double displayScale) { this.displayScale = displayScale; }

    public String getFirmLogoBase64() { return firmLogoBase64; }
    public void setFirmLogoBase64(String firmLogoBase64) { this.firmLogoBase64 = firmLogoBase64; }

    public boolean isShowSecondaryCurrency() { return showSecondaryCurrency; }
    public void setShowSecondaryCurrency(boolean showSecondaryCurrency) { this.showSecondaryCurrency = showSecondaryCurrency; }

    public String getSecondaryCurrency() { return secondaryCurrency; }
    public void setSecondaryCurrency(String secondaryCurrency) { this.secondaryCurrency = secondaryCurrency; }

    public double getSecondaryFxRate() { return secondaryFxRate; }
    public void setSecondaryFxRate(double secondaryFxRate) { this.secondaryFxRate = secondaryFxRate; }

    public String getCalibrationDate() { return calibrationDate; }
    public void setCalibrationDate(String calibrationDate) { this.calibrationDate = calibrationDate; }

    public String getValuationDate() { return valuationDate; }
    public void setValuationDate(String valuationDate) { this.valuationDate = valuationDate; }

    public String getExitDate() { return exitDate; }
    public void setExitDate(String exitDate) { this.exitDate = exitDate; }

    public int getDayCountBasis() { return dayCountBasis; }
    public void setDayCountBasis(int dayCountBasis) { this.dayCountBasis = dayCountBasis; }

    public String getDayCountName() { return dayCountName; }
    public void setDayCountName(String dayCountName) { this.dayCountName = dayCountName; }

    public double getTermCalibration() { return termCalibration; }
    public void setTermCalibration(double termCalibration) { this.termCalibration = termCalibration; }

    public double getTermValuation() { return termValuation; }
    public void setTermValuation(double termValuation) { this.termValuation = termValuation; }

    public double getRfCalibrationEffective() { return rfCalibrationEffective; }
    public void setRfCalibrationEffective(double rfCalibrationEffective) { this.rfCalibrationEffective = rfCalibrationEffective; }

    public double getRfCalibrationContinuous() { return rfCalibrationContinuous; }
    public void setRfCalibrationContinuous(double rfCalibrationContinuous) { this.rfCalibrationContinuous = rfCalibrationContinuous; }

    public double getRfValuationEffective() { return rfValuationEffective; }
    public void setRfValuationEffective(double rfValuationEffective) { this.rfValuationEffective = rfValuationEffective; }

    public double getRfValuationContinuous() { return rfValuationContinuous; }
    public void setRfValuationContinuous(double rfValuationContinuous) { this.rfValuationContinuous = rfValuationContinuous; }

    public String getCalibrationSecurityName() { return calibrationSecurityName; }
    public void setCalibrationSecurityName(String calibrationSecurityName) { this.calibrationSecurityName = calibrationSecurityName; }

    public List<DerivedSecurity> getCalibrationDerivedSecurities() { return calibrationDerivedSecurities; }
    public void setCalibrationDerivedSecurities(List<DerivedSecurity> calibrationDerivedSecurities) { this.calibrationDerivedSecurities = calibrationDerivedSecurities != null ? calibrationDerivedSecurities : new ArrayList<>(); }

    public List<BreakpointTier> getCalibrationBreakpoints() { return calibrationBreakpoints; }
    public void setCalibrationBreakpoints(List<BreakpointTier> calibrationBreakpoints) { this.calibrationBreakpoints = calibrationBreakpoints != null ? calibrationBreakpoints : new ArrayList<>(); }

    public List<ClaimTierAllocation> getCalibrationClaims() { return calibrationClaims; }
    public void setCalibrationClaims(List<ClaimTierAllocation> calibrationClaims) { this.calibrationClaims = calibrationClaims != null ? calibrationClaims : new ArrayList<>(); }

    public double getCalibrationSolvedEquity() { return calibrationSolvedEquity; }
    public void setCalibrationSolvedEquity(double calibrationSolvedEquity) { this.calibrationSolvedEquity = calibrationSolvedEquity; }

    public OpmAllocationResult getCalibrationOpm() { return calibrationOpm; }
    public void setCalibrationOpm(OpmAllocationResult calibrationOpm) { this.calibrationOpm = calibrationOpm; }

    public RiskFreeRateAnalysis getCalibrationRfAnalysis() { return calibrationRfAnalysis; }
    public void setCalibrationRfAnalysis(RiskFreeRateAnalysis calibrationRfAnalysis) { this.calibrationRfAnalysis = calibrationRfAnalysis; }

    public List<DerivedSecurity> getValuationDerivedSecurities() { return valuationDerivedSecurities; }
    public void setValuationDerivedSecurities(List<DerivedSecurity> valuationDerivedSecurities) { this.valuationDerivedSecurities = valuationDerivedSecurities != null ? valuationDerivedSecurities : new ArrayList<>(); }

    public List<BreakpointTier> getValuationBreakpoints() { return valuationBreakpoints; }
    public void setValuationBreakpoints(List<BreakpointTier> valuationBreakpoints) { this.valuationBreakpoints = valuationBreakpoints != null ? valuationBreakpoints : new ArrayList<>(); }

    public List<ClaimTierAllocation> getValuationClaims() { return valuationClaims; }
    public void setValuationClaims(List<ClaimTierAllocation> valuationClaims) { this.valuationClaims = valuationClaims != null ? valuationClaims : new ArrayList<>(); }

    public double getConcludedEquityValue() { return concludedEquityValue; }
    public void setConcludedEquityValue(double concludedEquityValue) { this.concludedEquityValue = concludedEquityValue; }

    public OpmAllocationResult getValuationOpm() { return valuationOpm; }
    public void setValuationOpm(OpmAllocationResult valuationOpm) { this.valuationOpm = valuationOpm; }

    public RiskFreeRateAnalysis getValuationRfAnalysis() { return valuationRfAnalysis; }
    public void setValuationRfAnalysis(RiskFreeRateAnalysis valuationRfAnalysis) { this.valuationRfAnalysis = valuationRfAnalysis; }

    public WaterfallResult getWaterfall() { return waterfall; }
    public void setWaterfall(WaterfallResult waterfall) { this.waterfall = waterfall; }

    public WaterfallResult getCalibrationWaterfall() { return calibrationWaterfall; }
    public void setCalibrationWaterfall(WaterfallResult calibrationWaterfall) { this.calibrationWaterfall = calibrationWaterfall; }

    public List<ComparativeWaterfallItem> getComparativeWaterfall() { return comparativeWaterfall; }
    public void setComparativeWaterfall(List<ComparativeWaterfallItem> comparativeWaterfall) { this.comparativeWaterfall = comparativeWaterfall != null ? comparativeWaterfall : new ArrayList<>(); }

    public HoldingsSummary getHoldings() { return holdings; }
    public void setHoldings(HoldingsSummary holdings) { this.holdings = holdings; }

    public VolatilityAnalysisResult getCalibrationVolatility() { return calibrationVolatility; }
    public void setCalibrationVolatility(VolatilityAnalysisResult calibrationVolatility) { this.calibrationVolatility = calibrationVolatility; }

    public VolatilityAnalysisResult getValuationVolatility() { return valuationVolatility; }
    public void setValuationVolatility(VolatilityAnalysisResult valuationVolatility) { this.valuationVolatility = valuationVolatility; }
}

