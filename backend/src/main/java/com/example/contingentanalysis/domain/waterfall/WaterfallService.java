package com.example.contingentanalysis.domain.waterfall;

import com.example.contingentanalysis.domain.claims.ClaimsService;
import com.example.contingentanalysis.domain.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WaterfallService {

    private final ClaimsService claimsService;

    public WaterfallService(ClaimsService claimsService) {
        this.claimsService = claimsService;
    }

    public WaterfallResult allocateWaterfall(List<DerivedSecurity> rows,
                                             List<BreakpointTier> breakpoints,
                                             double equityValue) {
        double equity = Math.max(0.0, equityValue);
        List<ClaimTierAllocation> tierClaims = claimsService.allocateClaimsByTier(rows, breakpoints);

        Map<String, Double> proceedsMap = new LinkedHashMap<>();
        for (DerivedSecurity r : rows) {
            proceedsMap.put(r.getSecurity(), 0.0);
        }

        // Allocate across finite breakpoint intervals
        for (ClaimTierAllocation tr : tierClaims) {
            double fromEq = tr.getFromEquity();
            double toEq = tr.getToEquity();
            if (equity <= fromEq) {
                continue;
            }

            double availableWidth = Math.max(0.0, Math.min(equity, toEq) - fromEq);
            for (Map.Entry<String, Double> entry : tr.getSharingPercentages().entrySet()) {
                String sec = entry.getKey();
                double p = entry.getValue();
                proceedsMap.put(sec, proceedsMap.get(sec) + availableWidth * p);
            }
        }

        // Allocate residual equity above the last breakpoint (Thereafter tier)
        if (!breakpoints.isEmpty()) {
            double lastBpEq = breakpoints.get(breakpoints.size() - 1).getEndEquity();
            if (equity > lastBpEq) {
                double excess = equity - lastBpEq;
                double totalFd = 0.0;
                for (DerivedSecurity r : rows) {
                    totalFd += r.getFullyDilutedShares();
                }
                if (totalFd > 0.0) {
                    for (DerivedSecurity r : rows) {
                        String sec = r.getSecurity();
                        proceedsMap.put(sec, proceedsMap.get(sec) + excess * (r.getFullyDilutedShares() / totalFd));
                    }
                }
            }
        }

        double totalProceeds = 0.0;
        for (double v : proceedsMap.values()) {
            totalProceeds += v;
        }

        List<WaterfallAllocationItem> items = new ArrayList<>();
        for (DerivedSecurity r : rows) {
            String sec = r.getSecurity();
            double proc = proceedsMap.get(sec);
            double sh = r.getShares();
            double ps = sh > 0.0 ? (proc / sh) : 0.0;

            double pref = r.getTotalLiquidationPreference();
            double recPct;
            if (pref > 0.0) {
                recPct = proc / pref;
            } else {
                recPct = proc > 0.0 ? 1.0 : 0.0;
            }

            double totPct = totalProceeds > 0.0 ? (proc / totalProceeds) : 0.0;

            items.add(new WaterfallAllocationItem(
                    sec,
                    sh,
                    proc,
                    ps,
                    recPct,
                    totPct
            ));
        }

        return new WaterfallResult(equity, items, totalProceeds);
    }

    public List<ComparativeWaterfallItem> buildComparativeWaterfall(List<DerivedSecurity> calRows,
                                                                    List<DerivedSecurity> valRows,
                                                                    WaterfallResult calWaterfall,
                                                                    WaterfallResult valWaterfall) {
        Map<String, DerivedSecurity> calMap = new HashMap<>();
        for (DerivedSecurity r : calRows) {
            calMap.put(r.getSecurity(), r);
        }

        Map<String, DerivedSecurity> valMap = new HashMap<>();
        for (DerivedSecurity r : valRows) {
            valMap.put(r.getSecurity(), r);
        }

        Map<String, WaterfallAllocationItem> calDistMap = new HashMap<>();
        if (calWaterfall != null && calWaterfall.getDistribution() != null) {
            for (WaterfallAllocationItem it : calWaterfall.getDistribution()) {
                calDistMap.put(it.getSecurity(), it);
            }
        }

        Map<String, WaterfallAllocationItem> valDistMap = new HashMap<>();
        if (valWaterfall != null && valWaterfall.getDistribution() != null) {
            for (WaterfallAllocationItem it : valWaterfall.getDistribution()) {
                valDistMap.put(it.getSecurity(), it);
            }
        }

        double calFdTot = 0.0;
        for (DerivedSecurity r : calRows) {
            calFdTot += r.getFullyDilutedShares();
        }

        double valFdTot = 0.0;
        for (DerivedSecurity r : valRows) {
            valFdTot += r.getFullyDilutedShares();
        }

        Set<String> names = new LinkedHashSet<>();
        for (DerivedSecurity r : valRows) {
            names.add(r.getSecurity());
        }
        for (DerivedSecurity r : calRows) {
            names.add(r.getSecurity());
        }

        List<ComparativeWaterfallItem> items = new ArrayList<>();
        for (String n : names) {
            DerivedSecurity rc = calMap.get(n);
            DerivedSecurity rv = valMap.get(n);
            WaterfallAllocationItem dc = calDistMap.get(n);
            WaterfallAllocationItem dv = valDistMap.get(n);

            Double vc = dc != null ? dc.getProceeds() : null;
            Double vv = dv != null ? dv.getProceeds() : null;
            double delta = (vv != null ? vv : 0.0) - (vc != null ? vc : 0.0);

            Double calSh = rc != null ? rc.getShares() : null;
            Double calFd = (rc != null && calFdTot > 0.0) ? (rc.getFullyDilutedShares() / calFdTot) : null;
            Double calPs = (rc != null && rc.getShares() > 0.0 && vc != null) ? (vc / rc.getShares()) : null;

            Double valSh = rv != null ? rv.getShares() : null;
            Double valFd = (rv != null && valFdTot > 0.0) ? (rv.getFullyDilutedShares() / valFdTot) : null;
            Double valPs = (rv != null && rv.getShares() > 0.0 && vv != null) ? (vv / rv.getShares()) : null;

            ComparativeWaterfallItem item = new ComparativeWaterfallItem();
            item.setSecurity(n);
            item.setCalShares(calSh);
            item.setCalFdOwnership(calFd);
            item.setValShares(valSh);
            item.setValFdOwnership(valFd);
            item.setCalDistribution(vc);
            item.setCalPerShare(calPs);
            item.setValDistribution(vv);
            item.setValPerShare(valPs);
            item.setChangeInDistribution(delta);

            items.add(item);
        }

        return items;
    }
}

