package com.example.contingentanalysis.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class HoldingInput {
    @NotBlank(message = "Fund name is required")
    @Size(max = 255, message = "Fund name exceeds 255 characters")
    private String fund;

    @NotBlank(message = "Security name is required")
    @Size(max = 255, message = "Security name exceeds 255 characters")
    private String security;

    @PositiveOrZero(message = "Units must be non-negative")
    private double units;

    @PositiveOrZero(message = "Cost must be non-negative")
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

