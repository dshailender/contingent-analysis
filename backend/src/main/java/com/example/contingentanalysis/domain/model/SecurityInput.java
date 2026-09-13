package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class SecurityInput {
    @NotBlank(message = "Security name is required")
    private String security;

    @JsonProperty("security_subtype")
    private String securitySubtype = "Preferred Stock";

    private double shares = 0.0;

    @JsonProperty("exercise_price")
    private Double exercisePrice;

    @JsonProperty("original_issue_price")
    private Double originalIssuePrice;

    @JsonProperty("conversion_price")
    private Double conversionPrice;

    @JsonProperty("liquidation_multiplier")
    private Double liquidationMultiplier = 1.0;

    private String participation = "NA";

    @JsonProperty("max_participation_cap")
    private String maxParticipationCap = "NA";

    private Integer seniority;

    @JsonProperty("issue_date")
    private String issueDate;

    @JsonProperty("dividend_rate")
    private Double dividendRate;

    @JsonProperty("compounding_convention")
    private String compoundingConvention = "Annual";

    @JsonProperty("dividends_paid_to_date")
    private Double dividendsPaidToDate = 0.0;

    public SecurityInput() {
    }

    public SecurityInput(String security, String securitySubtype, double shares, Double exercisePrice,
                         Double originalIssuePrice, Double conversionPrice, Double liquidationMultiplier,
                         String participation, String maxParticipationCap, Integer seniority,
                         String issueDate, Double dividendRate, String compoundingConvention,
                         Double dividendsPaidToDate) {
        this.security = security;
        this.securitySubtype = securitySubtype != null ? securitySubtype : "Preferred Stock";
        this.shares = shares;
        this.exercisePrice = exercisePrice;
        this.originalIssuePrice = originalIssuePrice;
        this.conversionPrice = conversionPrice;
        this.liquidationMultiplier = liquidationMultiplier != null ? liquidationMultiplier : 1.0;
        this.participation = participation != null ? participation : "NA";
        this.maxParticipationCap = maxParticipationCap != null ? maxParticipationCap : "NA";
        this.seniority = seniority;
        this.issueDate = issueDate;
        this.dividendRate = dividendRate;
        this.compoundingConvention = compoundingConvention != null ? compoundingConvention : "Annual";
        this.dividendsPaidToDate = dividendsPaidToDate != null ? dividendsPaidToDate : 0.0;
    }

    public String getSecurity() { return security; }
    public void setSecurity(String security) { this.security = security; }

    public String getSecuritySubtype() { return securitySubtype; }
    public void setSecuritySubtype(String securitySubtype) { this.securitySubtype = securitySubtype; }

    public double getShares() { return shares; }
    public void setShares(double shares) { this.shares = shares; }

    public Double getExercisePrice() { return exercisePrice; }
    public void setExercisePrice(Double exercisePrice) { this.exercisePrice = exercisePrice; }

    public Double getOriginalIssuePrice() { return originalIssuePrice; }
    public void setOriginalIssuePrice(Double originalIssuePrice) { this.originalIssuePrice = originalIssuePrice; }

    public Double getConversionPrice() { return conversionPrice; }
    public void setConversionPrice(Double conversionPrice) { this.conversionPrice = conversionPrice; }

    public Double getLiquidationMultiplier() { return liquidationMultiplier; }
    public void setLiquidationMultiplier(Double liquidationMultiplier) { this.liquidationMultiplier = liquidationMultiplier; }

    public String getParticipation() { return participation; }
    public void setParticipation(String participation) { this.participation = participation; }

    public String getMaxParticipationCap() { return maxParticipationCap; }
    public void setMaxParticipationCap(String maxParticipationCap) { this.maxParticipationCap = maxParticipationCap; }

    public Integer getSeniority() { return seniority; }
    public void setSeniority(Integer seniority) { this.seniority = seniority; }

    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }

    public Double getDividendRate() { return dividendRate; }
    public void setDividendRate(Double dividendRate) { this.dividendRate = dividendRate; }

    public String getCompoundingConvention() { return compoundingConvention; }
    public void setCompoundingConvention(String compoundingConvention) { this.compoundingConvention = compoundingConvention; }

    public Double getDividendsPaidToDate() { return dividendsPaidToDate; }
    public void setDividendsPaidToDate(Double dividendsPaidToDate) { this.dividendsPaidToDate = dividendsPaidToDate; }
}

