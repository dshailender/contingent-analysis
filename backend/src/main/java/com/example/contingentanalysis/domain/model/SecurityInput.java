package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class SecurityInput {
    @NotBlank(message = "Security name is required")
    @Size(max = 255, message = "Security name exceeds 255 characters")
    private String security;

    @Size(max = 100, message = "Security subtype exceeds 100 characters")
    @JsonProperty("security_subtype")
    private String securitySubtype = "Preferred Stock";

    @PositiveOrZero(message = "Shares must be non-negative")
    private double shares = 0.0;

    @PositiveOrZero(message = "Exercise price must be non-negative")
    @JsonProperty("exercise_price")
    private Double exercisePrice;

    @PositiveOrZero(message = "Original issue price must be non-negative")
    @JsonProperty("original_issue_price")
    private Double originalIssuePrice;

    @PositiveOrZero(message = "Conversion price must be non-negative")
    @JsonProperty("conversion_price")
    private Double conversionPrice;

    @PositiveOrZero(message = "Liquidation multiplier must be non-negative")
    @JsonProperty("liquidation_multiplier")
    private Double liquidationMultiplier = 1.0;

    @Size(max = 50, message = "Participation exceeds 50 characters")
    private String participation = "NA";

    @Size(max = 50, message = "Max participation cap exceeds 50 characters")
    @JsonProperty("max_participation_cap")
    private String maxParticipationCap = "NA";

    private Integer seniority;

    @JsonProperty("issue_date")
    private String issueDate;

    @PositiveOrZero(message = "Dividend rate must be non-negative")
    @JsonProperty("dividend_rate")
    private Double dividendRate;

    @Size(max = 50, message = "Compounding convention exceeds 50 characters")
    @JsonProperty("compounding_convention")
    private String compoundingConvention = "Annual";

    @PositiveOrZero(message = "Dividends paid to date must be non-negative")
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

