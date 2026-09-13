package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class WaterfallResult {
    @JsonProperty("applied_equity")
    private double appliedEquity;

    private List<WaterfallAllocationItem> distribution = new ArrayList<>();

    @JsonProperty("total_proceeds")
    private double totalProceeds = 0.0;

    public WaterfallResult() {
    }

    public WaterfallResult(double appliedEquity, List<WaterfallAllocationItem> distribution, double totalProceeds) {
        this.appliedEquity = appliedEquity;
        this.distribution = distribution != null ? distribution : new ArrayList<>();
        this.totalProceeds = totalProceeds;
    }

    public double getAppliedEquity() { return appliedEquity; }
    public void setAppliedEquity(double appliedEquity) { this.appliedEquity = appliedEquity; }

    public List<WaterfallAllocationItem> getDistribution() { return distribution; }
    public void setDistribution(List<WaterfallAllocationItem> distribution) { this.distribution = distribution != null ? distribution : new ArrayList<>(); }

    public double getTotalProceeds() { return totalProceeds; }
    public void setTotalProceeds(double totalProceeds) { this.totalProceeds = totalProceeds; }
}

