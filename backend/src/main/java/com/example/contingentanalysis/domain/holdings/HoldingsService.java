package com.example.contingentanalysis.domain.holdings;

import com.example.contingentanalysis.domain.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HoldingsService {

    public HoldingsSummary evaluateClientHoldings(List<HoldingInput> holdings,
                                                  List<DerivedSecurity> valuationSecurities,
                                                  Map<String, Double> perShareValues) {
        List<HoldingResultItem> items = new ArrayList<>();
        Map<String, DerivedSecurity> secMap = new HashMap<>();
        double totalFdShares = 0.0;

        for (DerivedSecurity r : valuationSecurities) {
            secMap.put(r.getSecurity(), r);
            totalFdShares += r.getFullyDilutedShares();
        }

        if (holdings != null) {
            for (HoldingInput h : holdings) {
                DerivedSecurity secInfo = secMap.get(h.getSecurity());
                double psVal = perShareValues != null ? perShareValues.getOrDefault(h.getSecurity(), 0.0) : 0.0;
                double totVal = h.getUnits() * psVal;

                double classOwn = (secInfo != null && secInfo.getShares() > 0.0)
                        ? (h.getUnits() / secInfo.getShares()) : 0.0;

                double convRatio = secInfo != null ? secInfo.getConversionRatio() : 1.0;
                double asConverted = h.getUnits() * convRatio;
                double fdOwn = totalFdShares > 0.0 ? (asConverted / totalFdShares) : 0.0;

                Double moic = (h.getCost() > 0.0) ? (totVal / h.getCost()) : null;

                items.add(new HoldingResultItem(
                        h.getFund(),
                        h.getSecurity(),
                        h.getUnits(),
                        h.getCost(),
                        psVal,
                        totVal,
                        classOwn,
                        fdOwn,
                        moic
                ));
            }
        }

        // Compute fund-level subtotals preserving input order of funds
        Set<String> fundsOrder = new LinkedHashSet<>();
        if (holdings != null) {
            for (HoldingInput h : holdings) {
                fundsOrder.add(h.getFund());
            }
        }

        List<FundSubtotal> subtotals = new ArrayList<>();
        for (String fName : fundsOrder) {
            double fUnits = 0.0;
            double fCost = 0.0;
            double fVal = 0.0;

            for (HoldingResultItem it : items) {
                if (it.getFund().equals(fName)) {
                    fUnits += it.getUnits();
                    fCost += it.getCost();
                    fVal += it.getConcludedFairValue();
                }
            }

            Double fMoic = fCost > 0.0 ? (fVal / fCost) : null;
            subtotals.add(new FundSubtotal(fName, fUnits, fCost, fVal, fMoic));
        }

        double grandCost = 0.0;
        double grandVal = 0.0;
        for (HoldingResultItem it : items) {
            grandCost += it.getCost();
            grandVal += it.getConcludedFairValue();
        }

        Double grandMoic = grandCost > 0.0 ? (grandVal / grandCost) : null;

        return new HoldingsSummary(items, subtotals, grandCost, grandVal, grandMoic);
    }
}

