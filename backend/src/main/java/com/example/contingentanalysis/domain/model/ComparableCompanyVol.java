package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ComparableCompanyVol {
    @JsonProperty("ciq_id")
    private String ciqId;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("share_price")
    private double sharePrice;

    @JsonProperty("market_cap")
    private double marketCap;

    @JsonProperty("total_debt")
    private double totalDebt;

    @JsonProperty("preferred_equity")
    private double preferredEquity;

    @JsonProperty("minority_interest")
    private double minorityInterest;

    @JsonProperty("equity_vol")
    private double equityVol;

    @JsonProperty("asset_vol")
    private double assetVol;

    @JsonProperty("relevered_vol")
    private Double releveredVol;

    private String currency = "USD";
    private boolean include = true;

    public ComparableCompanyVol() {
    }

    public ComparableCompanyVol(String ciqId, String companyName, double sharePrice, double marketCap,
                                double totalDebt, double preferredEquity, double minorityInterest,
                                double equityVol, double assetVol, Double releveredVol,
                                String currency, boolean include) {
        this.ciqId = ciqId;
        this.companyName = companyName;
        this.sharePrice = sharePrice;
        this.marketCap = marketCap;
        this.totalDebt = totalDebt;
        this.preferredEquity = preferredEquity;
        this.minorityInterest = minorityInterest;
        this.equityVol = equityVol;
        this.assetVol = assetVol;
        this.releveredVol = releveredVol;
        this.currency = currency != null ? currency : "USD";
        this.include = include;
    }

    public String getCiqId() { return ciqId; }
    public void setCiqId(String ciqId) { this.ciqId = ciqId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public double getSharePrice() { return sharePrice; }
    public void setSharePrice(double sharePrice) { this.sharePrice = sharePrice; }

    public double getMarketCap() { return marketCap; }
    public void setMarketCap(double marketCap) { this.marketCap = marketCap; }

    public double getTotalDebt() { return totalDebt; }
    public void setTotalDebt(double totalDebt) { this.totalDebt = totalDebt; }

    public double getPreferredEquity() { return preferredEquity; }
    public void setPreferredEquity(double preferredEquity) { this.preferredEquity = preferredEquity; }

    public double getMinorityInterest() { return minorityInterest; }
    public void setMinorityInterest(double minorityInterest) { this.minorityInterest = minorityInterest; }

    public double getEquityVol() { return equityVol; }
    public void setEquityVol(double equityVol) { this.equityVol = equityVol; }

    public double getAssetVol() { return assetVol; }
    public void setAssetVol(double assetVol) { this.assetVol = assetVol; }

    public Double getReleveredVol() { return releveredVol; }
    public void setReleveredVol(Double releveredVol) { this.releveredVol = releveredVol; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isInclude() { return include; }
    public void setInclude(boolean include) { this.include = include; }
}

