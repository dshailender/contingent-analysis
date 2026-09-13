package com.example.contingentanalysis.domain.opm;

import com.example.contingentanalysis.domain.blackscholes.BlackScholesCalculator;
import com.example.contingentanalysis.domain.claims.ClaimsService;
import com.example.contingentanalysis.domain.date.DateMath;
import com.example.contingentanalysis.domain.model.*;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OpmService {

    private final ClaimsService claimsService;

    public OpmService(ClaimsService claimsService) {
        this.claimsService = claimsService;
    }

    public OpmAllocationResult allocateOpm(List<DerivedSecurity> rows,
                                           List<BreakpointTier> breakpoints,
                                           double equityValue,
                                           String valuationDate,
                                           String exitDate,
                                           double rfEffective,
                                           double volatility,
                                           double dividendYield,
                                           int dayCountBasis) {
        double t = Math.max(0.0, DateMath.yearFraction(valuationDate, exitDate, dayCountBasis));
        double rfContinuous = rfEffective > -1.0 ? BlackScholesCalculator.toContinuousRate(rfEffective) : 0.0;

        // Strikes are 0 followed by all breakpoint upper bounds
        List<Double> strikes = new ArrayList<>();
        strikes.add(0.0);
        for (BreakpointTier bp : breakpoints) {
            strikes.add(bp.getEndEquity());
        }

        List<Double> calls = new ArrayList<>();
        for (double k : strikes) {
            calls.add(BlackScholesCalculator.blackScholesCall(equityValue, k, rfContinuous, volatility, t, dividendYield));
        }

        // Incremental call tranche values
        List<Double> incs = new ArrayList<>();
        for (int i = 0; i < calls.size() - 1; i++) {
            incs.add(Math.max(0.0, calls.get(i) - calls.get(i + 1)));
        }
        // Final call value above the last breakpoint (Thereafter slice)
        double lastCall = !calls.isEmpty() ? calls.get(calls.size() - 1) : 0.0;
        incs.add(lastCall);

        // Build tranches metadata
        List<OpmTranche> tranches = new ArrayList<>();
        for (int i = 0; i < breakpoints.size(); i++) {
            BreakpointTier bp = breakpoints.get(i);
            tranches.add(new OpmTranche(
                    bp.getTier(),
                    bp.getStartEquity(),
                    bp.getEndEquity(),
                    calls.get(i),
                    calls.get(i + 1),
                    incs.get(i)
            ));
        }

        // Calculate claim allocations per tier
        List<ClaimTierAllocation> tierClaims = claimsService.allocateClaimsByTier(rows, breakpoints);

        Map<String, Double> allocated = new LinkedHashMap<>();
        for (DerivedSecurity r : rows) {
            allocated.put(r.getSecurity(), 0.0);
        }

        for (int i = 0; i < tierClaims.size(); i++) {
            ClaimTierAllocation tr = tierClaims.get(i);
            double tot = 0.0;
            for (double v : tr.getDollarClaims().values()) {
                tot += v;
            }
            if (tot > 0.0) {
                double inc = incs.get(i);
                for (Map.Entry<String, Double> entry : tr.getDollarClaims().entrySet()) {
                    String sec = entry.getKey();
                    double val = entry.getValue();
                    allocated.put(sec, allocated.get(sec) + inc * (val / tot));
                }
            }
        }

        // Allocate Thereafter slice pro-rata by fully diluted shares
        double totalFd = 0.0;
        for (DerivedSecurity r : rows) {
            totalFd += r.getFullyDilutedShares();
        }
        if (totalFd > 0.0) {
            for (DerivedSecurity r : rows) {
                String sec = r.getSecurity();
                allocated.put(sec, allocated.get(sec) + lastCall * (r.getFullyDilutedShares() / totalFd));
            }
        }

        // Per share values and percentages
        Map<String, Double> perShare = new LinkedHashMap<>();
        Map<String, Double> pctAlloc = new LinkedHashMap<>();
        double totalAlloc = 0.0;
        for (double v : allocated.values()) {
            totalAlloc += v;
        }

        for (DerivedSecurity r : rows) {
            String sec = r.getSecurity();
            double totVal = allocated.get(sec);
            double sh = r.getShares();
            perShare.put(sec, sh > 0.0 ? (totVal / sh) : 0.0);
            pctAlloc.put(sec, totalAlloc > 0.0 ? (totVal / totalAlloc) : 0.0);
        }

        OpmAllocationResult res = new OpmAllocationResult();
        res.setEquityValue(equityValue);
        res.setTerm(t);
        res.setRiskFreeRateEffective(rfEffective);
        res.setRiskFreeRateContinuous(rfContinuous);
        res.setVolatility(volatility);
        res.setDividendYield(dividendYield);
        res.setTranches(tranches);
        res.setAllocatedValues(allocated);
        res.setPerShareValues(perShare);
        res.setPercentAllocations(pctAlloc);
        res.setTotalAllocated(totalAlloc);

        return res;
    }

    public double backsolveEquity(List<DerivedSecurity> rows,
                                  List<BreakpointTier> breakpoints,
                                  String calibrationDate,
                                  String exitDate,
                                  double rfEffective,
                                  double volatility,
                                  double dividendYield,
                                  String calibrationSecurityName,
                                  double targetPrice,
                                  int dayCountBasis) {
        DerivedSecurity calRow = null;
        for (DerivedSecurity r : rows) {
            if (r.getSecurity().equals(calibrationSecurityName)) {
                calRow = r;
                break;
            }
        }

        if (calRow == null || calRow.getShares() <= 0.0 || targetPrice <= 0.0) {
            throw new IllegalArgumentException(String.format("Invalid calibration security '%s' or target price %f",
                    calibrationSecurityName, targetPrice));
        }

        final double sh = calRow.getShares();

        UnivariateFunction objective = s -> {
            if (s <= 0.0) {
                return -targetPrice;
            }
            OpmAllocationResult allocRes = allocateOpm(
                    rows,
                    breakpoints,
                    s,
                    calibrationDate,
                    exitDate,
                    rfEffective,
                    volatility,
                    dividendYield,
                    dayCountBasis
            );
            double secVal = allocRes.getAllocatedValues().getOrDefault(calibrationSecurityName, 0.0);
            return (secVal / sh) - targetPrice;
        };

        // Bracket search
        double lo = 1e-6;
        double hi = Math.max(1e6, targetPrice * sh * 5.0);

        boolean bracketed = false;
        for (int i = 0; i < 100; i++) {
            if (objective.value(hi) >= 0.0) {
                bracketed = true;
                break;
            }
            hi *= 2.0;
        }

        if (!bracketed) {
            throw new RuntimeException("Failed to bracket root for OPM backsolve (upper bound expansion exhausted)");
        }

        // Solve using Brent's method with fallback to bisection
        try {
            BrentSolver solver = new BrentSolver(1e-8);
            return solver.solve(200, objective, lo, hi);
        } catch (Exception ignored) {
            // Robust bisection fallback matching Python implementation
            for (int i = 0; i < 160; i++) {
                double mid = lo + (hi - lo) / 2.0;
                double fm = objective.value(mid);
                if (Math.abs(fm) <= 1e-12) {
                    return mid;
                }
                if (fm > 0.0) {
                    hi = mid;
                } else {
                    lo = mid;
                }
                if ((hi - lo) <= Math.max(1e-7, Math.abs(mid) * 1e-14)) {
                    break;
                }
            }
            return lo + (hi - lo) / 2.0;
        }
    }
}

