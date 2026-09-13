package com.example.contingentanalysis.domain.model;

public class VolatilityStats {
    private double min;
    private double q1;
    private double median;
    private double mean;
    private double q3;
    private double max;

    public VolatilityStats() {
    }

    public VolatilityStats(double min, double q1, double median, double mean, double q3, double max) {
        this.min = min;
        this.q1 = q1;
        this.median = median;
        this.mean = mean;
        this.q3 = q3;
        this.max = max;
    }

    public double getMin() { return min; }
    public void setMin(double min) { this.min = min; }

    public double getQ1() { return q1; }
    public void setQ1(double q1) { this.q1 = q1; }

    public double getMedian() { return median; }
    public void setMedian(double median) { this.median = median; }

    public double getMean() { return mean; }
    public void setMean(double mean) { this.mean = mean; }

    public double getQ3() { return q3; }
    public void setQ3(double q3) { this.q3 = q3; }

    public double getMax() { return max; }
    public void setMax(double max) { this.max = max; }
}

