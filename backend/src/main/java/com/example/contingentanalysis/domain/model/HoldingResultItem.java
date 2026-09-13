package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HoldingResultItem {
    private String fund;
    private String security;
    private double units;
    private double cost;

    @JsonProperty("fair_value_per_share")
    private double fairValuePerShare;

    @JsonProperty("concluded_fair_value")
    private double concludedFairValue;

    @JsonProperty("class_ownership_pct")
    private double classOwnershipPct;

    @JsonProperty("fully_diluted_ownership_pct")
    private double fullyDilutedOwnershipPct;

    private Double moic;

    public HoldingResultItem() {
    }

    public HoldingResultItem(String fund, String security, double units, double cost, double fairValuePerShare,
                             double concludedFairValue, double classOwnershipPct, double fullyDilutedOwnershipPct,
                             Double moic) {
        this.fund = fund;
        this.security = security;
        this.units = units;
        this.cost = cost;
        this.fairValuePerShare = fairValuePerShare;
        this.concludedFairValue = concludedFairValue;
        this.classOwnershipPct = classOwnershipPct;
        this.fullyDilutedOwnershipPct = fullyDilutedOwnershipPct;
        this.moic = moic;
    }

    public String getFund() { return fund; }
    public void setFund(String fund) { this.fund = fund; }

    public String getSecurity() { return security; }
    public void setSecurity(String security) { this.security = security; }

    public double getUnits() { return units; }
    public void setUnits(double units) { this.units = units; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public double getFairValuePerShare() { return fairValuePerShare; }
    public void setFairValuePerShare(double fairValuePerShare) { this.fairValuePerShare = fairValuePerShare; }

    public double getConcludedFairValue() { return concludedFairValue; }
    public void setConcludedFairValue(double concludedFairValue) { this.concludedFairValue = concludedFairValue; }

    public double getClassOwnershipPct() { return classOwnershipPct; }
    public void setClassOwnershipPct(double classOwnershipPct) { this.classOwnershipPct = classOwnershipPct; }

    public double getFullyDilutedOwnershipPct() { return fullyDilutedOwnershipPct; }
    public void setFullyDilutedOwnershipPct(double fullyDilutedOwnershipPct) { this.fullyDilutedOwnershipPct = fullyDilutedOwnershipPct; }

    public Double getMoic() { return moic; }
    public void setMoic(Double moic) { this.moic = moic; }
}

