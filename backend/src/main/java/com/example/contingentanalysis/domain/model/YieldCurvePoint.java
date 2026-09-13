package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class YieldCurvePoint {
    @JsonProperty("tenor_name")
    private String tenorName;

    @JsonProperty("tenor_years")
    private double tenorYears;

    @JsonProperty("rate_percent")
    private double ratePercent;

    public YieldCurvePoint() {
    }

    public YieldCurvePoint(String tenorName, double tenorYears, double ratePercent) {
        this.tenorName = tenorName;
        this.tenorYears = tenorYears;
        this.ratePercent = ratePercent;
    }

    public String getTenorName() { return tenorName; }
    public void setTenorName(String tenorName) { this.tenorName = tenorName; }

    public double getTenorYears() { return tenorYears; }
    public void setTenorYears(double tenorYears) { this.tenorYears = tenorYears; }

    public double getRatePercent() { return ratePercent; }
    public void setRatePercent(double ratePercent) { this.ratePercent = ratePercent; }
}

