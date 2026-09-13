package com.example.contingentanalysis.domain.breakpoints;

import com.example.contingentanalysis.domain.model.BreakpointTier;
import com.example.contingentanalysis.domain.model.DerivedSecurity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BreakpointService {

    public double totalEquityAtThreshold(List<DerivedSecurity> rows, double z) {
        double s = 0.0;
        for (DerivedSecurity r : rows) {
            double k = r.getTotalLiquidationPreference();
            double p = r.getFullyDilutedShares();
            double ep = r.getExercisePrice();
            boolean q = r.getParticipation() != null && r.getParticipation().toUpperCase().startsWith("Y");
            double cm = r.getCapMult() != null ? r.getCapMult() : 0.0;
            boolean isw = r.getSecurity() != null && r.getSecurity().toUpperCase().contains("WARRANT");

            if (k > 0.0) {
                if (q) {
                    double capLim = r.getOriginalIssuePrice() * r.getShares() * cm;
                    s += (cm > 0.0) ? Math.min(k + z * p, capLim) : (k + z * p);
                } else {
                    s += Math.max(k, z * p);
                }
            } else if (ep > 0.0) {
                s += isw ? (z * p + Math.max(0.0, (z - ep) * p)) : Math.max(0.0, (z - ep) * p);
            } else {
                s += z * p;
            }
        }
        return s;
    }

    public List<String> getActiveClaimants(List<DerivedSecurity> rows, double z) {
        List<String> active = new ArrayList<>();
        for (DerivedSecurity r : rows) {
            double k = r.getTotalLiquidationPreference();
            double p = r.getFullyDilutedShares();
            double ep = r.getExercisePrice();
            boolean q = r.getParticipation() != null && r.getParticipation().toUpperCase().startsWith("Y");
            double cm = r.getCapMult() != null ? r.getCapMult() : 0.0;
            boolean isw = r.getSecurity() != null && r.getSecurity().toUpperCase().contains("WARRANT");

            if (k == 0.0) {
                if (ep <= 0.0 || isw) {
                    active.add(r.getSecurity());
                } else if (z > ep) {
                    active.add(r.getSecurity());
                }
            } else if (!q) {
                double den = p > 0.0 ? p : 1.0;
                if (z > (k / den)) {
                    active.add(r.getSecurity());
                }
            } else if (cm <= 0.0) {
                active.add(r.getSecurity());
            } else {
                double den = p > 0.0 ? p : 1.0;
                double capx = (r.getOriginalIssuePrice() * r.getShares() * cm - k) / den;
                double capconv = (r.getOriginalIssuePrice() * r.getShares() * cm) / den;
                if (z < capx || z >= capconv) {
                    active.add(r.getSecurity());
                }
            }
        }
        return active;
    }

    private static class BreakpointEvent {
        double equityValue;
        String claimants;
        String description;

        BreakpointEvent(double equityValue, String claimants, String description) {
            this.equityValue = equityValue;
            this.claimants = claimants;
            this.description = description;
        }
    }

    public List<BreakpointTier> generateBreakpoints(List<DerivedSecurity> rows) {
        List<BreakpointEvent> events = new ArrayList<>();

        // 1. Seniority tiers for liquidation preferences
        TreeSet<Integer> seniorityLevels = new TreeSet<>();
        for (DerivedSecurity r : rows) {
            if (r.getTotalLiquidationPreference() > 0.0 && r.getSeniority() > 0) {
                seniorityLevels.add(r.getSeniority());
            }
        }

        for (int sLevel : seniorityLevels) {
            double ev = 0.0;
            List<String> names = new ArrayList<>();
            for (DerivedSecurity r : rows) {
                if (r.getTotalLiquidationPreference() > 0.0) {
                    if (r.getSeniority() <= sLevel) {
                        ev += r.getTotalLiquidationPreference();
                    }
                    if (r.getSeniority() == sLevel) {
                        names.add(r.getSecurity());
                    }
                }
            }
            String claimantStr = "100% to " + String.join(", ", names);
            String desc = String.format("Tier %d | Liquidation preference funded | Seniority tier %d LP fully funded.", sLevel, sLevel);
            events.add(new BreakpointEvent(ev, claimantStr, desc));
        }

        // 2. Conversion, exercise, and participation cap thresholds
        for (DerivedSecurity r : rows) {
            double k = r.getTotalLiquidationPreference();
            double p = r.getFullyDilutedShares();
            double ep = r.getExercisePrice();
            boolean q = r.getParticipation() != null && r.getParticipation().toUpperCase().startsWith("Y");
            double cm = r.getCapMult() != null ? r.getCapMult() : 0.0;
            String n = r.getSecurity();

            // Non-participating conversion
            if (k > 0.0 && !q && p > 0.0) {
                double z = k / p;
                double eqVal = totalEquityAtThreshold(rows, z);
                String claimants = String.join(", ", getActiveClaimants(rows, z));
                String desc = String.format("%s | Conversion | Converts when as-converted value exceeds LP + accrued dividend (threshold $%,.2f per common-equivalent share).", n, z);
                events.add(new BreakpointEvent(eqVal, claimants, desc));
            }

            // Exercise of Options / Warrants
            if (k == 0.0 && ep > 0.0 && p > 0.0) {
                double z = ep;
                double eqVal = totalEquityAtThreshold(rows, z);
                String claimants = String.join(", ", getActiveClaimants(rows, z));
                String desc = String.format("%s | Exercise | Exercisable above $%,.2f per share.", n, z);
                events.add(new BreakpointEvent(eqVal, claimants, desc));
            }

            // Participating preferred with cap
            if (k > 0.0 && q && cm > 0.0 && p > 0.0) {
                double z1 = Math.max(0.0, (r.getOriginalIssuePrice() * r.getShares() * cm - k) / p);
                double eqVal1 = totalEquityAtThreshold(rows, z1);
                String claimants1 = String.join(", ", getActiveClaimants(rows, z1 - 1e-6));
                String desc1 = String.format("%s | Participation cap | Stops incremental participation at %.1fx cap.", n, cm);
                events.add(new BreakpointEvent(eqVal1, claimants1, desc1));

                double z2 = r.getOriginalIssuePrice() * r.getShares() * cm / p;
                double eqVal2 = totalEquityAtThreshold(rows, z2);
                String claimants2 = String.join(", ", getActiveClaimants(rows, z2 - 1e-6));
                String desc2 = String.format("%s | Post-cap conversion | As-converted value exceeds capped participation after $%,.2f per share.", n, z2);
                events.add(new BreakpointEvent(eqVal2, claimants2, desc2));
            }
        }

        // 3. Filter valid events and sort by equity value
        List<BreakpointEvent> validEvents = new ArrayList<>();
        for (BreakpointEvent e : events) {
            if (e.equityValue > 0.0 && !Double.isInfinite(e.equityValue) && !Double.isNaN(e.equityValue)) {
                validEvents.add(e);
            }
        }
        validEvents.sort(Comparator.comparingDouble(e -> e.equityValue));

        // 4. Consolidate events at the same equity value (to 2 decimal places)
        List<BreakpointEvent> grouped = new ArrayList<>();
        for (BreakpointEvent ev : validEvents) {
            if (!grouped.isEmpty() && Math.round(grouped.get(grouped.size() - 1).equityValue * 100.0) == Math.round(ev.equityValue * 100.0)) {
                BreakpointEvent last = grouped.get(grouped.size() - 1);
                Set<String> combinedClaimants = new LinkedHashSet<>();
                for (String c : last.claimants.split(", ")) {
                    if (!c.trim().isEmpty()) combinedClaimants.add(c.trim());
                }
                for (String c : ev.claimants.split(", ")) {
                    if (!c.trim().isEmpty()) combinedClaimants.add(c.trim());
                }
                last.claimants = String.join(", ", combinedClaimants);
                last.description += " | " + ev.description;
            } else {
                grouped.add(new BreakpointEvent(ev.equityValue, ev.claimants, ev.description));
            }
        }

        // 5. Build BreakpointTier instances
        List<BreakpointTier> tiers = new ArrayList<>();
        double prevEq = 0.0;
        for (int i = 0; i < grouped.size(); i++) {
            BreakpointEvent grp = grouped.get(i);
            int tierNum = i + 1;
            double width = grp.equityValue - prevEq;
            tiers.add(new BreakpointTier(
                    tierNum,
                    prevEq,
                    grp.equityValue,
                    width,
                    grp.claimants,
                    grp.description,
                    false
            ));
            prevEq = grp.equityValue;
        }

        return tiers;
    }
}

