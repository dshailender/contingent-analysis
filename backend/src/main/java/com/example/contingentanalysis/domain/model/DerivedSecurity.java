package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DerivedSecurity {
    private String security;

    @JsonProperty("security_subtype")
    private String securitySubtype;

    private double shares;

    @JsonProperty("original_issue_price")
    private double originalIssuePrice;

    @JsonProperty("conversion_price")
    private Double conversionPrice;

    @JsonProperty("conversion_ratio")
    private double conversionRatio;

    @JsonProperty("liquidation_multiplier")
    private double liquidationMultiplier;

    private int seniority;

    private String participation;

    @JsonProperty("cap_mult")
    private Double capMult;

    @JsonProperty("per_share_dividend")
    private double perShareDividend = 0.0;

    @JsonProperty("total_accrued_dividends")
    private double totalAccruedDividends = 0.0;

    @JsonProperty("liquidation_preference_per_share")
    private double liquidationPreferencePerShare = 0.0;

    @JsonProperty("total_liquidation_preference")
    private double totalLiquidationPreference = 0.0;

    @JsonProperty("fully_diluted_shares")
    private double fullyDilutedShares = 0.0;

    @JsonProperty("exercise_price")
    private double exercisePrice = 0.0;

    @JsonProperty("total_exercise_proceeds")
    private double totalExerciseProceeds = 0.0;

    public DerivedSecurity() {
    }

    public String getSecurity() { return security; }
    public void setSecurity(String security) { this.security = security; }

    public String getSecuritySubtype() { return securitySubtype; }
    public void setSecuritySubtype(String securitySubtype) { this.securitySubtype = securitySubtype; }

    public double getShares() { return shares; }
    public void setShares(double shares) { this.shares = shares; }

    public double getOriginalIssuePrice() { return originalIssuePrice; }
    public void setOriginalIssuePrice(double originalIssuePrice) { this.originalIssuePrice = originalIssuePrice; }

    public Double getConversionPrice() { return conversionPrice; }
    public void setConversionPrice(Double conversionPrice) { this.conversionPrice = conversionPrice; }

    public double getConversionRatio() { return conversionRatio; }
    public void setConversionRatio(double conversionRatio) { this.conversionRatio = conversionRatio; }

    public double getLiquidationMultiplier() { return liquidationMultiplier; }
    public void setLiquidationMultiplier(double liquidationMultiplier) { this.liquidationMultiplier = liquidationMultiplier; }

    public int getSeniority() { return seniority; }
    public void setSeniority(int seniority) { this.seniority = seniority; }

    public String getParticipation() { return participation; }
    public void setParticipation(String participation) { this.participation = participation; }

    public Double getCapMult() { return capMult; }
    public void setCapMult(Double capMult) { this.capMult = capMult; }

    public double getPerShareDividend() { return perShareDividend; }
    public void setPerShareDividend(double perShareDividend) { this.perShareDividend = perShareDividend; }

    public double getTotalAccruedDividends() { return totalAccruedDividends; }
    public void setTotalAccruedDividends(double totalAccruedDividends) { this.totalAccruedDividends = totalAccruedDividends; }

    public double getLiquidationPreferencePerShare() { return liquidationPreferencePerShare; }
    public void setLiquidationPreferencePerShare(double liquidationPreferencePerShare) { this.liquidationPreferencePerShare = liquidationPreferencePerShare; }

    public double getTotalLiquidationPreference() { return totalLiquidationPreference; }
    public void setTotalLiquidationPreference(double totalLiquidationPreference) { this.totalLiquidationPreference = totalLiquidationPreference; }

    public double getFullyDilutedShares() { return fullyDilutedShares; }
    public void setFullyDilutedShares(double fullyDilutedShares) { this.fullyDilutedShares = fullyDilutedShares; }

    public double getExercisePrice() { return exercisePrice; }
    public void setExercisePrice(double exercisePrice) { this.exercisePrice = exercisePrice; }

    public double getTotalExerciseProceeds() { return totalExerciseProceeds; }
    public void setTotalExerciseProceeds(double totalExerciseProceeds) { this.totalExerciseProceeds = totalExerciseProceeds; }
}

