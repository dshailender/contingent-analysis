package com.example.contingentanalysis.api;

import com.example.contingentanalysis.domain.date.DateMath;
import com.example.contingentanalysis.domain.model.RiskFreeRateAnalysis;
import com.example.contingentanalysis.domain.model.YieldCurvePoint;
import com.example.contingentanalysis.domain.riskfree.RiskFreeRateService;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class RiskFreeRatesController {

    private final RiskFreeRateService riskFreeRateService;

    public RiskFreeRatesController(RiskFreeRateService riskFreeRateService) {
        this.riskFreeRateService = riskFreeRateService;
    }

    public static class InterpolateRequest {
        @JsonProperty("as_of_date")
        private String asOfDate;

        @JsonProperty("exit_date")
        private String exitDate;

        @JsonProperty("day_count_basis")
        private int dayCountBasis = 1;

        @JsonProperty("source_name")
        private String sourceName = "Interpolated Curve";

        private List<List<Double>> points;

        public String getAsOfDate() { return asOfDate; }
        public void setAsOfDate(String asOfDate) { this.asOfDate = asOfDate; }

        public String getExitDate() { return exitDate; }
        public void setExitDate(String exitDate) { this.exitDate = exitDate; }

        public int getDayCountBasis() { return dayCountBasis; }
        public void setDayCountBasis(int dayCountBasis) { this.dayCountBasis = dayCountBasis; }

        public String getSourceName() { return sourceName; }
        public void setSourceName(String sourceName) { this.sourceName = sourceName; }

        public List<List<Double>> getPoints() { return points; }
        public void setPoints(List<List<Double>> points) { this.points = points; }
    }

    @GetMapping("/risk-free-rates/curves")
    public Map<String, Object> getSupportedCurves() {
        List<Map<String, Object>> usTreasury = RiskFreeRateService.US_TREASURY_TENORS.stream()
                .map(t -> Map.<String, Object>of("name", t.name(), "years", t.years()))
                .toList();

        List<Map<String, Object>> ecb = RiskFreeRateService.ECB_TENORS.stream()
                .map(t -> Map.<String, Object>of("name", t.name(), "years", t.years()))
                .toList();

        return Map.of(
                "us_treasury", usTreasury,
                "ecb", ecb
        );
    }

    @PostMapping("/risk-free-rates/interpolate")
    public RiskFreeRateAnalysis interpolateRate(@RequestBody InterpolateRequest req) {
        double term = DateMath.yearFraction(req.getAsOfDate(), req.getExitDate(), req.getDayCountBasis());
        if (term <= 0.0) {
            throw new IllegalArgumentException("Exit date must be after as-of date.");
        }

        List<double[]> pointDoubles = new ArrayList<>();
        List<YieldCurvePoint> curvePoints = new ArrayList<>();

        if (req.getPoints() != null) {
            for (List<Double> p : req.getPoints()) {
                if (p != null && p.size() >= 2) {
                    double tenor = p.get(0);
                    double rate = p.get(1);
                    pointDoubles.add(new double[]{tenor, rate});
                    curvePoints.add(new YieldCurvePoint(String.format(Locale.US, "%.2fy", tenor), tenor, rate));
                }
            }
        }

        RiskFreeRateService.InterpolationResult interp = riskFreeRateService.interpolateYieldCurve(pointDoubles, term);

        return riskFreeRateService.buildRiskFreeAnalysis(
                req.getAsOfDate(),
                req.getExitDate(),
                interp.ratePercent(),
                req.getSourceName(),
                curvePoints,
                req.getDayCountBasis()
        );
    }
}

