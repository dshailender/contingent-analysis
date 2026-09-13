package com.example.contingentanalysis.api;

import com.example.contingentanalysis.domain.capitaliq.CapitalIqService;
import com.example.contingentanalysis.domain.model.VolatilityAnalysisResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CapitalIqController {

    private final CapitalIqService capitalIqService;

    public CapitalIqController(CapitalIqService capitalIqService) {
        this.capitalIqService = capitalIqService;
    }

    public static class TemplateRequest {
        private List<String> tickers = List.of("IQ247543", "IQ28472", "IQ385732", "IQ94821", "IQ582910");

        @JsonProperty("calibration_date")
        private String calibrationDate = "2025-02-26";

        @JsonProperty("valuation_date")
        private String valuationDate = "2026-06-30";

        private String currency = "EUR";
        private String frequency = "Weekly";

        @JsonProperty("lookback_years")
        private int lookbackYears = 2;

        public List<String> getTickers() { return tickers; }
        public void setTickers(List<String> tickers) { this.tickers = tickers; }

        public String getCalibrationDate() { return calibrationDate; }
        public void setCalibrationDate(String calibrationDate) { this.calibrationDate = calibrationDate; }

        public String getValuationDate() { return valuationDate; }
        public void setValuationDate(String valuationDate) { this.valuationDate = valuationDate; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getFrequency() { return frequency; }
        public void setFrequency(String frequency) { this.frequency = frequency; }

        public int getLookbackYears() { return lookbackYears; }
        public void setLookbackYears(int lookbackYears) { this.lookbackYears = lookbackYears; }
    }

    @PostMapping("/capital-iq/workbook")
    public ResponseEntity<byte[]> downloadCapitalIqWorkbook(@RequestBody TemplateRequest req) {
        byte[] excelBytes = capitalIqService.createCapitalIqBridgeWorkbook(
                req.getTickers(),
                req.getCalibrationDate(),
                req.getValuationDate(),
                req.getCurrency(),
                req.getFrequency(),
                req.getLookbackYears()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Capital_IQ_Volatility_Bridge.xlsx\"")
                .body(excelBytes);
    }

    @PostMapping("/capital-iq/upload")
    public Map<String, VolatilityAnalysisResult> uploadCapitalIqWorkbook(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xlsm"))) {
            throw new IllegalArgumentException("Only .xlsx or .xlsm files are supported.");
        }

        return capitalIqService.parseCapitalIqWorkbook(file.getBytes());
    }
}

