package com.example.contingentanalysis.api;

import com.example.contingentanalysis.domain.model.RiskFreeRateAnalysis;
import com.example.contingentanalysis.domain.riskfree.RiskFreeRateService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RiskFreeRatesController {

    private final RiskFreeRateService riskFreeRateService;

    public RiskFreeRatesController(RiskFreeRateService riskFreeRateService) {
        this.riskFreeRateService = riskFreeRateService;
    }

    public static class InterpolateRequest {
        @NotBlank(message = "As-of date is required")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "As-of date must be YYYY-MM-DD")
        @JsonProperty("as_of_date")
        private String asOfDate;

        @NotBlank(message = "Exit date is required")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Exit date must be YYYY-MM-DD")
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
    public RiskFreeRateAnalysis interpolateRate(@Valid @RequestBody InterpolateRequest req) {
        return riskFreeRateService.calculateInterpolatedAnalysis(
                req.getAsOfDate(),
                req.getExitDate(),
                req.getDayCountBasis(),
                req.getSourceName(),
                req.getPoints()
        );
    }
}

