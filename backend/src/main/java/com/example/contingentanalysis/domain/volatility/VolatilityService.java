package com.example.contingentanalysis.domain.volatility;

import com.example.contingentanalysis.domain.blackscholes.BlackScholesCalculator;
import com.example.contingentanalysis.domain.model.VolatilityStats;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class VolatilityService {

    /**
     * Computes sample quantile strictly matching the reference JavaScript calculation:
     * p = (n - 1) * q
     * b = floor(p), d = p - b
     * returns a[b] + (a[b+1] ?? a[b]) * d
     */
    public double quantile(List<Double> values, double q) {
        if (values == null || values.isEmpty()) {
            return Double.NaN;
        }
        List<Double> valid = new ArrayList<>();
        for (Double v : values) {
            if (v != null && Double.isFinite(v)) {
                valid.add(v);
            }
        }
        if (valid.isEmpty()) {
            return Double.NaN;
        }
        Collections.sort(valid);
        int n = valid.size();
        if (n == 1) {
            return valid.get(0);
        }

        double p = (n - 1) * q;
        int b = (int) Math.floor(p);
        double d = p - b;
        double nextVal = (b + 1 < n) ? valid.get(b + 1) : valid.get(b);
        return valid.get(b) + nextVal * d;
    }

    public VolatilityStats calculateVolatilityStats(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<Double> valid = new ArrayList<>();
        for (Double v : values) {
            if (v != null && Double.isFinite(v)) {
                valid.add(v);
            }
        }
        if (valid.isEmpty()) {
            return null;
        }
        Collections.sort(valid);

        double min = Collections.min(valid);
        double max = Collections.max(valid);
        double q1 = quantile(valid, 0.25);
        double median = quantile(valid, 0.50);
        double q3 = quantile(valid, 0.75);

        double sum = 0.0;
        for (double v : valid) {
            sum += v;
        }
        double mean = sum / valid.size();

        return new VolatilityStats(min, q1, median, mean, q3, max);
    }

    public double mertonAssetVolatility(double equityVol,
                                        double equityMarketCap,
                                        double totalDebt,
                                        double riskFreeRate,
                                        double term) {
        if (!Double.isFinite(equityVol) || equityVol <= 0.0 || equityMarketCap <= 0.0) {
            return Double.NaN;
        }
        if (totalDebt <= 0.0) {
            return equityVol;
        }

        double t = Math.max(1e-6, term);
        double rf = riskFreeRate;
        double total = equityMarketCap + totalDebt;

        double d1 = (Math.log(total / totalDebt) + (rf + 0.5 * equityVol * equityVol) * t) / (equityVol * Math.sqrt(t));
        double nd1 = BlackScholesCalculator.normalCdf(d1);

        if (nd1 > 1e-9) {
            return equityVol * (equityMarketCap / total) / nd1;
        }
        return Double.NaN;
    }

    public double releverEquityVolatility(double assetVol,
                                         double subjectEquityValue,
                                         double subjectDebt,
                                         double subjectPreferred) {
        if (!Double.isFinite(assetVol) || assetVol <= 0.0 || subjectEquityValue <= 0.0) {
            return Double.NaN;
        }

        double de = (subjectDebt + subjectPreferred) / subjectEquityValue;
        return assetVol * (1.0 + de);
    }
}

