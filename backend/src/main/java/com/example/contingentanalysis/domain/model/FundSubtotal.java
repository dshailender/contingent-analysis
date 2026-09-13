package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FundSubtotal {
    private String fund;

    @JsonProperty("total_units")
    private double totalUnits;

    @JsonProperty("total_cost")
    private double totalCost;

    @JsonProperty("total_value")
    private double totalValue;

    private Double moic;

    public FundSubtotal() {
    }

    public FundSubtotal(String fund, double totalUnits, double totalCost, double totalValue, Double moic) {
        this.fund = fund;
        this.totalUnits = totalUnits;
        this.totalCost = totalCost;
        this.totalValue = totalValue;
        this.moic = moic;
    }

    public String getFund() { return fund; }
    public void setFund(String fund) { this.fund = fund; }

    public double getTotalUnits() { return totalUnits; }
    public void setTotalUnits(double totalUnits) { this.totalUnits = totalUnits; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public double getTotalValue() { return totalValue; }
    public void setTotalValue(double totalValue) { this.totalValue = totalValue; }

    public Double getMoic() { return moic; }
    public void setMoic(Double moic) { this.moic = moic; }
}

