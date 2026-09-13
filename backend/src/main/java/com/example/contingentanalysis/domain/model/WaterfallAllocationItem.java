package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WaterfallAllocationItem {
    private String security;
    private double shares;
    private double proceeds;

    @JsonProperty("proceeds_per_share")
    private double proceedsPerShare;

    @JsonProperty("percent_recovery")
    private double percentRecovery;

    @JsonProperty("percent_of_total")
    private double percentOfTotal;

    public WaterfallAllocationItem() {
    }

    public WaterfallAllocationItem(String security, double shares, double proceeds, double proceedsPerShare,
                                   double percentRecovery, double percentOfTotal) {
        this.security = security;
        this.shares = shares;
        this.proceeds = proceeds;
        this.proceedsPerShare = proceedsPerShare;
        this.percentRecovery = percentRecovery;
        this.percentOfTotal = percentOfTotal;
    }

    public String getSecurity() { return security; }
    public void setSecurity(String security) { this.security = security; }

    public double getShares() { return shares; }
    public void setShares(double shares) { this.shares = shares; }

    public double getProceeds() { return proceeds; }
    public void setProceeds(double proceeds) { this.proceeds = proceeds; }

    public double getProceedsPerShare() { return proceedsPerShare; }
    public void setProceedsPerShare(double proceedsPerShare) { this.proceedsPerShare = proceedsPerShare; }

    public double getPercentRecovery() { return percentRecovery; }
    public void setPercentRecovery(double percentRecovery) { this.percentRecovery = percentRecovery; }

    public double getPercentOfTotal() { return percentOfTotal; }
    public void setPercentOfTotal(double percentOfTotal) { this.percentOfTotal = percentOfTotal; }
}

