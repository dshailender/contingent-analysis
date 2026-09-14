package com.example.contingentanalysis.api;

import com.example.contingentanalysis.domain.capitaliq.CapitalIqService;
import com.example.contingentanalysis.domain.model.VolatilityAnalysisResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CapitalIqController {

    private final CapitalIqService capitalIqService;

    public CapitalIqController(CapitalIqService capitalIqService) {
        this.capitalIqService = capitalIqService;
    }

    public static class TemplateRequest {
        @NotEmpty(message = "Tickers list cannot be empty")
        @Size(max = 50, message = "Maximum 50 tickers allowed")
        private List<String> tickers = List.of("IQ247543", "IQ28472", "IQ385732", "IQ94821", "IQ582910");

        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Calibration date must be YYYY-MM-DD")
        @JsonProperty("calibration_date")
        private String calibrationDate = "2025-02-26";

        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Valuation date must be YYYY-MM-DD")
        @JsonProperty("valuation_date")
        private String valuationDate = "2026-06-30";

        private String currency = "EUR";
        private String frequency = "Weekly";

        @Min(1)
        @Max(10)
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
    public ResponseEntity<byte[]> downloadCapitalIqWorkbook(@Valid @RequestBody TemplateRequest req) {
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
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new IllegalArgumentException("Uploaded file is empty or not provided.");
        }
        if (file.getSize() > 15L * 1024 * 1024) {
            throw new IllegalArgumentException("Uploaded file exceeds the maximum permitted size of 15MB.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File must have a valid filename.");
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\") || filename.contains("\0")) {
            throw new IllegalArgumentException("Malicious or invalid filename detected.");
        }

        String lower = filename.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".xlsm")) {
            throw new IllegalArgumentException("Only .xlsx or .xlsm files are supported.");
        }

        return capitalIqService.parseCapitalIqWorkbook(file.getBytes());
    }
}

