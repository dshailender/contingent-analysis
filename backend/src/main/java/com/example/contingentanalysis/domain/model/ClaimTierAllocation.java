package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClaimTierAllocation {
    private int tier;

    @JsonProperty("from_equity")
    private double fromEquity;

    @JsonProperty("to_equity")
    private double toEquity;

    private double width;

    @JsonProperty("is_thereafter")
    private boolean isThereafter = false;

    @JsonProperty("sharing_percentages")
    private Map<String, Double> sharingPercentages = new LinkedHashMap<>();

    @JsonProperty("dollar_claims")
    private Map<String, Double> dollarClaims = new LinkedHashMap<>();

    public ClaimTierAllocation() {
    }

    public ClaimTierAllocation(int tier, double fromEquity, double toEquity, double width, boolean isThereafter,
                               Map<String, Double> sharingPercentages, Map<String, Double> dollarClaims) {
        this.tier = tier;
        this.fromEquity = fromEquity;
        this.toEquity = toEquity;
        this.width = width;
        this.isThereafter = isThereafter;
        this.sharingPercentages = sharingPercentages != null ? sharingPercentages : new LinkedHashMap<>();
        this.dollarClaims = dollarClaims != null ? dollarClaims : new LinkedHashMap<>();
    }

    public int getTier() { return tier; }
    public void setTier(int tier) { this.tier = tier; }

    public double getFromEquity() { return fromEquity; }
    public void setFromEquity(double fromEquity) { this.fromEquity = fromEquity; }

    public double getToEquity() { return toEquity; }
    public void setToEquity(double toEquity) { this.toEquity = toEquity; }

    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }

    public boolean isThereafter() { return isThereafter; }
    public void setThereafter(boolean thereafter) { isThereafter = thereafter; }

    public Map<String, Double> getSharingPercentages() { return sharingPercentages; }
    public void setSharingPercentages(Map<String, Double> sharingPercentages) { this.sharingPercentages = sharingPercentages; }

    public Map<String, Double> getDollarClaims() { return dollarClaims; }
    public void setDollarClaims(Map<String, Double> dollarClaims) { this.dollarClaims = dollarClaims; }
}

