package com.example.contingentanalysis.domain.claims;

import com.example.contingentanalysis.domain.model.BreakpointTier;
import com.example.contingentanalysis.domain.model.ClaimTierAllocation;
import com.example.contingentanalysis.domain.model.DerivedSecurity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ClaimsService {

    public List<ClaimTierAllocation> allocateClaimsByTier(List<DerivedSecurity> rows, List<BreakpointTier> breakpoints) {
        List<String> names = new ArrayList<>();
        for (DerivedSecurity r : rows) {
            names.add(r.getSecurity());
        }

        List<ClaimTierAllocation> tierAllocations = new ArrayList<>();

        for (BreakpointTier bp : breakpoints) {
            double fromEq = bp.getStartEquity();
            double toEq = bp.getEndEquity();
            double width = Math.max(0.0, toEq - fromEq);

            String activeRaw = bp.getClaimantsDescription().replace("100% to ", "");
            Set<String> activeNames = new HashSet<>();
            for (String x : activeRaw.split(",")) {
                if (!x.trim().isEmpty()) {
                    activeNames.add(x.trim());
                }
            }

            boolean isLp = bp.getEventDescription() != null && bp.getEventDescription().contains("Liquidation preference funded");

            Map<String, Double> basis = new LinkedHashMap<>();
            for (DerivedSecurity r : rows) {
                if (activeNames.contains(r.getSecurity())) {
                    basis.put(r.getSecurity(), isLp ? r.getTotalLiquidationPreference() : r.getFullyDilutedShares());
                }
            }

            double denominator = 0.0;
            for (double v : basis.values()) {
                denominator += v;
            }

            Map<String, Double> sharingPcts = new LinkedHashMap<>();
            Map<String, Double> dollarClaims = new LinkedHashMap<>();

            for (String n : names) {
                double val = basis.getOrDefault(n, 0.0);
                double p = denominator > 0.0 ? (val / denominator) : 0.0;
                sharingPcts.put(n, p);
                dollarClaims.put(n, width * p);
            }

            tierAllocations.add(new ClaimTierAllocation(
                    bp.getTier(),
                    fromEq,
                    toEq,
                    width,
                    bp.isThereafter(),
                    sharingPcts,
                    dollarClaims
            ));
        }

        return tierAllocations;
    }
}

