package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class HoldingsSummary {
    private List<HoldingResultItem> items = new ArrayList<>();

    @JsonProperty("fund_subtotals")
    private List<FundSubtotal> fundSubtotals = new ArrayList<>();

    @JsonProperty("total_cost")
    private double totalCost = 0.0;

    @JsonProperty("total_value")
    private double totalValue = 0.0;

    @JsonProperty("consolidated_moic")
    private Double consolidatedMoic;

    public HoldingsSummary() {
    }

    public HoldingsSummary(List<HoldingResultItem> items, List<FundSubtotal> fundSubtotals, double totalCost,
                           double totalValue, Double consolidatedMoic) {
        this.items = items != null ? items : new ArrayList<>();
        this.fundSubtotals = fundSubtotals != null ? fundSubtotals : new ArrayList<>();
        this.totalCost = totalCost;
        this.totalValue = totalValue;
        this.consolidatedMoic = consolidatedMoic;
    }

    public List<HoldingResultItem> getItems() { return items; }
    public void setItems(List<HoldingResultItem> items) { this.items = items != null ? items : new ArrayList<>(); }

    public List<FundSubtotal> getFundSubtotals() { return fundSubtotals; }
    public void setFundSubtotals(List<FundSubtotal> fundSubtotals) { this.fundSubtotals = fundSubtotals != null ? fundSubtotals : new ArrayList<>(); }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public double getTotalValue() { return totalValue; }
    public void setTotalValue(double totalValue) { this.totalValue = totalValue; }

    public Double getConsolidatedMoic() { return consolidatedMoic; }
    public void setConsolidatedMoic(Double consolidatedMoic) { this.consolidatedMoic = consolidatedMoic; }
}

