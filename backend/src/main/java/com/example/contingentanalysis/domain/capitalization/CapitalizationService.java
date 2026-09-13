package com.example.contingentanalysis.domain.capitalization;

import com.example.contingentanalysis.domain.date.DateMath;
import com.example.contingentanalysis.domain.model.DerivedSecurity;
import com.example.contingentanalysis.domain.model.SecurityInput;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CapitalizationService {

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("([0-9.]+)");

    public double parseParticipationCap(String v) {
        if (v == null) {
            return 0.0;
        }
        String s = v.trim();
        if (s.isEmpty() || s.equalsIgnoreCase("NA") || s.equalsIgnoreCase("NO CAP")
                || s.equalsIgnoreCase("NONE") || s.equalsIgnoreCase("UNCAPPED")
                || s.equals("0")) {
            return 0.0;
        }
        Matcher m = NUMERIC_PATTERN.matcher(s);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private int getCompoundingPeriods(String cc) {
        if (cc == null) return 1;
        return switch (cc.trim()) {
            case "Semi-Annual" -> 2;
            case "Quarterly" -> 4;
            case "Daily" -> 365;
            default -> 1; // "Annual" or fallback
        };
    }

    public List<DerivedSecurity> deriveCapitalization(List<SecurityInput> securities, String exitDate, int dayCountBasis) {
        List<DerivedSecurity> derived = new ArrayList<>();
        if (securities == null) {
            return derived;
        }

        for (SecurityInput sec : securities) {
            double sh = sec.getShares();
            double issue = sec.getOriginalIssuePrice() != null ? sec.getOriginalIssuePrice() : 0.0;
            double mult = sec.getLiquidationMultiplier() != null ? sec.getLiquidationMultiplier() : 1.0;
            Double conv = sec.getConversionPrice();
            double ex = sec.getExercisePrice() != null ? sec.getExercisePrice() : 0.0;

            boolean isPref = "Preferred Stock".equalsIgnoreCase(sec.getSecuritySubtype());
            boolean hasIssue = sec.getOriginalIssuePrice() != null && issue > 0.0;

            // Base liquidation preference per share
            double perPref = (isPref && hasIssue) ? (issue * mult) : 0.0;

            // Dividend accrual calculation
            double rate = sec.getDividendRate() != null ? (sec.getDividendRate() / 100.0) : 0.0;
            double paid = sec.getDividendsPaidToDate() != null ? sec.getDividendsPaidToDate() : 0.0;
            double term = (sec.getIssueDate() != null && !sec.getIssueDate().trim().isEmpty())
                    ? DateMath.yearFraction(sec.getIssueDate(), exitDate, dayCountBasis) : 0.0;
            String cc = sec.getCompoundingConvention() != null ? sec.getCompoundingConvention() : "Annual";

            double perDiv = 0.0;
            if (isPref && hasIssue && rate > 0.0 && term > 0.0) {
                if ("Simple Interest".equalsIgnoreCase(cc)) {
                    perDiv = Math.max(0.0, issue * rate * term - paid);
                } else {
                    int p = getCompoundingPeriods(cc);
                    perDiv = Math.max(0.0, issue * Math.pow(1.0 + rate / p, term * p) - issue - paid);
                }
            }

            // Total preference claim for the series
            double totalPref = (isPref && hasIssue) ? (perPref + perDiv) * sh : 0.0;

            // Conversion ratio and fully diluted shares
            double convRatio;
            double fdShares;
            if (isPref) {
                if (conv != null && conv > 0.0 && issue > 0.0) {
                    convRatio = issue / conv;
                } else {
                    convRatio = 0.0;
                }
                fdShares = sh * convRatio;
            } else {
                convRatio = 1.0;
                fdShares = sh;
            }

            double exerciseProceeds = (sec.getExercisePrice() != null && ex > 0.0) ? (sh * ex) : 0.0;
            int seniority = sec.getSeniority() != null ? sec.getSeniority() : 999;
            double capMultVal = parseParticipationCap(sec.getMaxParticipationCap());

            DerivedSecurity ds = new DerivedSecurity();
            ds.setSecurity(sec.getSecurity());
            ds.setSecuritySubtype(sec.getSecuritySubtype());
            ds.setShares(sh);
            ds.setOriginalIssuePrice(issue);
            ds.setConversionPrice(conv);
            ds.setConversionRatio(convRatio);
            ds.setLiquidationMultiplier(mult);
            ds.setSeniority(seniority);
            ds.setParticipation(sec.getParticipation() != null ? sec.getParticipation() : "NA");
            ds.setCapMult(capMultVal > 0.0 ? capMultVal : null);
            ds.setPerShareDividend(perDiv);
            ds.setTotalAccruedDividends(perDiv * sh);
            ds.setLiquidationPreferencePerShare(perPref + perDiv);
            ds.setTotalLiquidationPreference(totalPref);
            ds.setFullyDilutedShares(fdShares);
            ds.setExercisePrice(ex);
            ds.setTotalExerciseProceeds(exerciseProceeds);

            derived.add(ds);
        }

        return derived;
    }
}

