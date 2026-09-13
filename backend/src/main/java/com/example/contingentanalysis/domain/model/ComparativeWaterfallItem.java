package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ComparativeWaterfallItem {
    private String security;

    @JsonProperty("cal_shares")
    private Double calShares;

    @JsonProperty("cal_fd_ownership")
    private Double calFdOwnership;

    @JsonProperty("val_shares")
    private Double valShares;

    @JsonProperty("val_fd_ownership")
    private Double valFdOwnership;

    @JsonProperty("cal_distribution")
    private Double calDistribution;

    @JsonProperty("cal_per_share")
    private Double calPerShare;

    @JsonProperty("val_distribution")
    private Double valDistribution;

    @JsonProperty("val_per_share")
    private Double valPerShare;

    @JsonProperty("change_in_distribution")
    private Double changeInDistribution;

    public ComparativeWaterfallItem() {
    }

    public String getSecurity() { return security; }
    public void setSecurity(String security) { this.security = security; }

    public Double getCalShares() { return calShares; }
    public void setCalShares(Double calShares) { this.calShares = calShares; }

    public Double getCalFdOwnership() { return calFdOwnership; }
    public void setCalFdOwnership(Double calFdOwnership) { this.calFdOwnership = calFdOwnership; }

    public Double getValShares() { return valShares; }
    public void setValShares(Double valShares) { this.valShares = valShares; }

    public Double getValFdOwnership() { return valFdOwnership; }
    public void setValFdOwnership(Double valFdOwnership) { this.valFdOwnership = valFdOwnership; }

    public Double getCalDistribution() { return calDistribution; }
    public void setCalDistribution(Double calDistribution) { this.calDistribution = calDistribution; }

    public Double getCalPerShare() { return calPerShare; }
    public void setCalPerShare(Double calPerShare) { this.calPerShare = calPerShare; }

    public Double getValDistribution() { return valDistribution; }
    public void setValDistribution(Double valDistribution) { this.valDistribution = valDistribution; }

    public Double getValPerShare() { return valPerShare; }
    public void setValPerShare(Double valPerShare) { this.valPerShare = valPerShare; }

    public Double getChangeInDistribution() { return changeInDistribution; }
    public void setChangeInDistribution(Double changeInDistribution) { this.changeInDistribution = changeInDistribution; }
}

