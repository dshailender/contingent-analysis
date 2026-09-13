package com.example.contingentanalysis.domain.model;

import jakarta.validation.constraints.NotBlank;

public class HoldingInput {
    @NotBlank(message = "Fund name is required")
    private String fund;

    @NotBlank(message = "Security name is required")
    private String security;

    private double units;
    private double cost = 0.0;

    public HoldingInput() {
    }

    public HoldingInput(String fund, String security, double units, double cost) {
        this.fund = fund;
        this.security = security;
        this.units = units;
        this.cost = cost;
    }

    public String getFund() { return fund; }
    public void setFund(String fund) { this.fund = fund; }

    public String getSecurity() { return security; }
    public void setSecurity(String security) { this.security = security; }

    public double getUnits() { return units; }
    public void setUnits(double units) { this.units = units; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
}

