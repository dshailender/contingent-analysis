package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class VolatilityAnalysisResult {
    private List<ComparableCompanyVol> companies = new ArrayList<>();

    @JsonProperty("equity_stats")
    private VolatilityStats equityStats;

    @JsonProperty("asset_stats")
    private VolatilityStats assetStats;

    @JsonProperty("relevered_stats")
    private VolatilityStats releveredStats;

    @JsonProperty("selected_basis")
    private String selectedBasis = "equity";

    @JsonProperty("selected_stat")
    private String selectedStat = "median";

    @JsonProperty("selected_volatility")
    private double selectedVolatility = 0.35;

    @JsonProperty("subject_equity_value")
    private double subjectEquityValue = 0.0;

    @JsonProperty("subject_debt_and_pref")
    private double subjectDebtAndPref = 0.0;

    @JsonProperty("subject_debt_to_equity")
    private double subjectDebtToEquity = 0.0;

    @JsonProperty("subject_relevered_vol")
    private double subjectReleveredVol = 0.35;

    public VolatilityAnalysisResult() {
    }

    public List<ComparableCompanyVol> getCompanies() { return companies; }
    public void setCompanies(List<ComparableCompanyVol> companies) { this.companies = companies != null ? companies : new ArrayList<>(); }

    public VolatilityStats getEquityStats() { return equityStats; }
    public void setEquityStats(VolatilityStats equityStats) { this.equityStats = equityStats; }

    public VolatilityStats getAssetStats() { return assetStats; }
    public void setAssetStats(VolatilityStats assetStats) { this.assetStats = assetStats; }

    public VolatilityStats getReleveredStats() { return releveredStats; }
    public void setReleveredStats(VolatilityStats releveredStats) { this.releveredStats = releveredStats; }

    public String getSelectedBasis() { return selectedBasis; }
    public void setSelectedBasis(String selectedBasis) { this.selectedBasis = selectedBasis; }

    public String getSelectedStat() { return selectedStat; }
    public void setSelectedStat(String selectedStat) { this.selectedStat = selectedStat; }

    public double getSelectedVolatility() { return selectedVolatility; }
    public void setSelectedVolatility(double selectedVolatility) { this.selectedVolatility = selectedVolatility; }

    public double getSubjectEquityValue() { return subjectEquityValue; }
    public void setSubjectEquityValue(double subjectEquityValue) { this.subjectEquityValue = subjectEquityValue; }

    public double getSubjectDebtAndPref() { return subjectDebtAndPref; }
    public void setSubjectDebtAndPref(double subjectDebtAndPref) { this.subjectDebtAndPref = subjectDebtAndPref; }

    public double getSubjectDebtToEquity() { return subjectDebtToEquity; }
    public void setSubjectDebtToEquity(double subjectDebtToEquity) { this.subjectDebtToEquity = subjectDebtToEquity; }

    public double getSubjectReleveredVol() { return subjectReleveredVol; }
    public void setSubjectReleveredVol(double subjectReleveredVol) { this.subjectReleveredVol = subjectReleveredVol; }
}

