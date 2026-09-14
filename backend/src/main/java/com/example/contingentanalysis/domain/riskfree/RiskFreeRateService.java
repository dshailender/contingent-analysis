package com.example.contingentanalysis.domain.riskfree;

import com.example.contingentanalysis.domain.blackscholes.BlackScholesCalculator;
import com.example.contingentanalysis.domain.date.DateMath;
import com.example.contingentanalysis.domain.model.RiskFreeRateAnalysis;
import com.example.contingentanalysis.domain.model.YieldCurvePoint;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RiskFreeRateService {

    public record CurveTenor(String name, double years) {}

    public static final List<CurveTenor> US_TREASURY_TENORS = List.of(
            new CurveTenor("1 Month", 1.0 / 12.0),
            new CurveTenor("2 Month", 2.0 / 12.0),
            new CurveTenor("3 Month", 0.25),
            new CurveTenor("4 Month", 1.0 / 3.0),
            new CurveTenor("6 Month", 0.5),
            new CurveTenor("1 Year", 1.0),
            new CurveTenor("2 Year", 2.0),
            new CurveTenor("3 Year", 3.0),
            new CurveTenor("5 Year", 5.0),
            new CurveTenor("7 Year", 7.0),
            new CurveTenor("10 Year", 10.0),
            new CurveTenor("20 Year", 20.0),
            new CurveTenor("30 Year", 30.0)
    );

    public static final List<CurveTenor> ECB_TENORS = List.of(
            new CurveTenor("3 Month", 0.25),
            new CurveTenor("6 Month", 0.5),
            new CurveTenor("9 Month", 0.75),
            new CurveTenor("1 Year", 1.0),
            new CurveTenor("2 Year", 2.0),
            new CurveTenor("3 Year", 3.0),
            new CurveTenor("4 Year", 4.0),
            new CurveTenor("5 Year", 5.0),
            new CurveTenor("7 Year", 7.0),
            new CurveTenor("10 Year", 10.0),
            new CurveTenor("15 Year", 15.0),
            new CurveTenor("20 Year", 20.0),
            new CurveTenor("30 Year", 30.0)
    );

    public record InterpolationResult(double ratePercent, String metadata) {}

    public InterpolationResult interpolateYieldCurve(List<double[]> points, double targetTenor) {
        if (points == null || points.isEmpty()) {
            return new InterpolationResult(0.0, "Empty or invalid curve");
        }

        List<double[]> valid = new ArrayList<>();
        for (double[] p : points) {
            if (p != null && p.length >= 2 && p[0] > 0.0 && Double.isFinite(p[1])) {
                valid.add(p);
            }
        }
        if (valid.isEmpty()) {
            return new InterpolationResult(0.0, "Empty or invalid curve");
        }
        valid.sort(Comparator.comparingDouble(p -> p[0]));

        if (targetTenor <= valid.get(0)[0]) {
            double[] first = valid.get(0);
            return new InterpolationResult(first[1], String.format(Locale.US, "Clamped to shortest tenor (%.2fy: %.2f%%)", first[0], first[1]));
        }

        if (targetTenor >= valid.get(valid.size() - 1)[0]) {
            double[] last = valid.get(valid.size() - 1);
            return new InterpolationResult(last[1], String.format(Locale.US, "Clamped to longest tenor (%.2fy: %.2f%%)", last[0], last[1]));
        }

        for (int i = 0; i < valid.size() - 1; i++) {
            double t1 = valid.get(i)[0];
            double r1 = valid.get(i)[1];
            double t2 = valid.get(i + 1)[0];
            double r2 = valid.get(i + 1)[1];

            if (t1 <= targetTenor && targetTenor <= t2) {
                double frac = (targetTenor - t1) / (t2 - t1);
                double interpRate = r1 + frac * (r2 - r1);
                String meta = String.format(Locale.US, "Interpolated between %.2fy (%.2f%%) and %.2fy (%.2f%%)", t1, r1, t2, r2);
                return new InterpolationResult(interpRate, meta);
            }
        }

        return new InterpolationResult(valid.get(valid.size() - 1)[1], "Fallback");
    }

    public RiskFreeRateAnalysis buildRiskFreeAnalysis(String asOfDate,
                                                      String exitDate,
                                                      double annualEffectiveRatePct,
                                                      String sourceName,
                                                      List<YieldCurvePoint> curvePoints,
                                                      int dayCountBasis) {
        double term = Math.max(0.0, DateMath.yearFraction(asOfDate, exitDate, dayCountBasis));
        double rEff = annualEffectiveRatePct / 100.0;
        double rCont = rEff > -1.0 ? BlackScholesCalculator.toContinuousRate(rEff) : 0.0;

        List<YieldCurvePoint> pointsModel = curvePoints != null ? curvePoints : new ArrayList<>();

        String meta = String.format(Locale.US, "Term to liquidity: %.2f years. Continuous rate = ln(1 + %.2f%%) = %.4f%%.",
                term, annualEffectiveRatePct, rCont * 100.0);

        return new RiskFreeRateAnalysis(
                sourceName != null ? sourceName : "Manual / User Specified",
                asOfDate,
                term,
                annualEffectiveRatePct,
                rCont * 100.0,
                pointsModel,
                meta
        );
    }

    public RiskFreeRateAnalysis calculateInterpolatedAnalysis(String asOfDate,
                                                             String exitDate,
                                                             int dayCountBasis,
                                                             String sourceName,
                                                             List<List<Double>> rawPoints) {
        double term = DateMath.yearFraction(asOfDate, exitDate, dayCountBasis);
        if (term <= 0.0) {
            throw new IllegalArgumentException("Exit date must be after as-of date.");
        }

        List<double[]> pointDoubles = new ArrayList<>();
        List<YieldCurvePoint> curvePoints = new ArrayList<>();

        if (rawPoints != null) {
            for (List<Double> p : rawPoints) {
                if (p != null && p.size() >= 2) {
                    double tenor = p.get(0);
                    double rate = p.get(1);
                    pointDoubles.add(new double[]{tenor, rate});
                    curvePoints.add(new YieldCurvePoint(String.format(Locale.US, "%.2fy", tenor), tenor, rate));
                }
            }
        }

        InterpolationResult interp = interpolateYieldCurve(pointDoubles, term);
        return buildRiskFreeAnalysis(
                asOfDate,
                exitDate,
                interp.ratePercent(),
                sourceName,
                curvePoints,
                dayCountBasis
        );
    }
}

