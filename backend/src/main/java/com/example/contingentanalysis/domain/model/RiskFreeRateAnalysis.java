package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class RiskFreeRateAnalysis {
    @JsonProperty("source_name")
    private String sourceName;

    @JsonProperty("as_of_date")
    private String asOfDate;

    @JsonProperty("term_years")
    private double termYears;

    @JsonProperty("interpolated_annual_effective_rate")
    private double interpolatedAnnualEffectiveRate;

    @JsonProperty("continuous_rate")
    private double continuousRate;

    @JsonProperty("curve_points")
    private List<YieldCurvePoint> curvePoints = new ArrayList<>();

    @JsonProperty("interpolation_metadata")
    private String interpolationMetadata = "";

    public RiskFreeRateAnalysis() {
    }

    public RiskFreeRateAnalysis(String sourceName, String asOfDate, double termYears,
                                double interpolatedAnnualEffectiveRate, double continuousRate,
                                List<YieldCurvePoint> curvePoints, String interpolationMetadata) {
        this.sourceName = sourceName;
        this.asOfDate = asOfDate;
        this.termYears = termYears;
        this.interpolatedAnnualEffectiveRate = interpolatedAnnualEffectiveRate;
        this.continuousRate = continuousRate;
        this.curvePoints = curvePoints != null ? curvePoints : new ArrayList<>();
        this.interpolationMetadata = interpolationMetadata != null ? interpolationMetadata : "";
    }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getAsOfDate() { return asOfDate; }
    public void setAsOfDate(String asOfDate) { this.asOfDate = asOfDate; }

    public double getTermYears() { return termYears; }
    public void setTermYears(double termYears) { this.termYears = termYears; }

    public double getInterpolatedAnnualEffectiveRate() { return interpolatedAnnualEffectiveRate; }
    public void setInterpolatedAnnualEffectiveRate(double interpolatedAnnualEffectiveRate) { this.interpolatedAnnualEffectiveRate = interpolatedAnnualEffectiveRate; }

    public double getContinuousRate() { return continuousRate; }
    public void setContinuousRate(double continuousRate) { this.continuousRate = continuousRate; }

    public List<YieldCurvePoint> getCurvePoints() { return curvePoints; }
    public void setCurvePoints(List<YieldCurvePoint> curvePoints) { this.curvePoints = curvePoints != null ? curvePoints : new ArrayList<>(); }

    public String getInterpolationMetadata() { return interpolationMetadata; }
    public void setInterpolationMetadata(String interpolationMetadata) { this.interpolationMetadata = interpolationMetadata; }
}

