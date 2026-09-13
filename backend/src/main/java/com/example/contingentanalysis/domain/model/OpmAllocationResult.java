package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpmAllocationResult {
    @JsonProperty("equity_value")
    private double equityValue;

    private double term;

    @JsonProperty("risk_free_rate_effective")
    private double riskFreeRateEffective;

    @JsonProperty("risk_free_rate_continuous")
    private double riskFreeRateContinuous;

    private double volatility;

    @JsonProperty("dividend_yield")
    private double dividendYield;

    private List<OpmTranche> tranches = new ArrayList<>();

    @JsonProperty("allocated_values")
    private Map<String, Double> allocatedValues = new LinkedHashMap<>();

    @JsonProperty("per_share_values")
    private Map<String, Double> perShareValues = new LinkedHashMap<>();

    @JsonProperty("percent_allocations")
    private Map<String, Double> percentAllocations = new LinkedHashMap<>();

    @JsonProperty("total_allocated")
    private double totalAllocated = 0.0;

    public OpmAllocationResult() {
    }

    public double getEquityValue() { return equityValue; }
    public void setEquityValue(double equityValue) { this.equityValue = equityValue; }

    public double getTerm() { return term; }
    public void setTerm(double term) { this.term = term; }

    public double getRiskFreeRateEffective() { return riskFreeRateEffective; }
    public void setRiskFreeRateEffective(double riskFreeRateEffective) { this.riskFreeRateEffective = riskFreeRateEffective; }

    public double getRiskFreeRateContinuous() { return riskFreeRateContinuous; }
    public void setRiskFreeRateContinuous(double riskFreeRateContinuous) { this.riskFreeRateContinuous = riskFreeRateContinuous; }

    public double getVolatility() { return volatility; }
    public void setVolatility(double volatility) { this.volatility = volatility; }

    public double getDividendYield() { return dividendYield; }
    public void setDividendYield(double dividendYield) { this.dividendYield = dividendYield; }

    public List<OpmTranche> getTranches() { return tranches; }
    public void setTranches(List<OpmTranche> tranches) { this.tranches = tranches != null ? tranches : new ArrayList<>(); }

    public Map<String, Double> getAllocatedValues() { return allocatedValues; }
    public void setAllocatedValues(Map<String, Double> allocatedValues) { this.allocatedValues = allocatedValues != null ? allocatedValues : new LinkedHashMap<>(); }

    public Map<String, Double> getPerShareValues() { return perShareValues; }
    public void setPerShareValues(Map<String, Double> perShareValues) { this.perShareValues = perShareValues != null ? perShareValues : new LinkedHashMap<>(); }

    public Map<String, Double> getPercentAllocations() { return percentAllocations; }
    public void setPercentAllocations(Map<String, Double> percentAllocations) { this.percentAllocations = percentAllocations != null ? percentAllocations : new LinkedHashMap<>(); }

    public double getTotalAllocated() { return totalAllocated; }
    public void setTotalAllocated(double totalAllocated) { this.totalAllocated = totalAllocated; }
}

