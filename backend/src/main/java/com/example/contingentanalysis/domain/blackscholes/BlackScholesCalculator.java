package com.example.contingentanalysis.domain.blackscholes;

import org.apache.commons.math3.special.Erf;

public final class BlackScholesCalculator {

    private static final double SQRT_2 = Math.sqrt(2.0);

    private BlackScholesCalculator() {
    }

    public static double normalCdf(double x) {
        if (Double.isNaN(x)) {
            return Double.NaN;
        }
        if (Double.isInfinite(x)) {
            return x > 0.0 ? 1.0 : 0.0;
        }
        return 0.5 * Erf.erfc(-x / SQRT_2);
    }

    public static double toContinuousRate(double annualEffectiveRate) {
        if (annualEffectiveRate <= -1.0) {
            throw new IllegalArgumentException("Annual effective rate cannot be <= -100%");
        }
        return Math.log(1.0 + annualEffectiveRate);
    }

    public static double fromContinuousRate(double continuousRate) {
        return Math.exp(continuousRate) - 1.0;
    }

    public static double blackScholesCall(double s, double k, double r, double vol, double t, double q) {
        if (s <= 0.0 || vol <= 0.0 || t <= 0.0) {
            double intrinsic = s * Math.exp(-q * t) - k * Math.exp(-r * t);
            return Math.max(0.0, intrinsic);
        }

        if (k <= 0.0) {
            return s * Math.exp(-q * t);
        }

        double vSqrt = vol * Math.sqrt(t);
        double d1 = (Math.log(s / k) + (r - q + 0.5 * vol * vol) * t) / vSqrt;
        double d2 = d1 - vSqrt;

        double callPrice = s * Math.exp(-q * t) * normalCdf(d1) - k * Math.exp(-r * t) * normalCdf(d2);
        return Math.max(0.0, callPrice);
    }
}

