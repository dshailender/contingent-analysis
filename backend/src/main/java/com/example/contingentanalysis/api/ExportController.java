package com.example.contingentanalysis.api;

import com.example.contingentanalysis.domain.exports.ExcelExportService;
import com.example.contingentanalysis.domain.exports.PdfExportService;
import com.example.contingentanalysis.domain.model.ValuationRequest;
import com.example.contingentanalysis.domain.model.ValuationResponse;
import com.example.contingentanalysis.domain.pipeline.ValuationPipelineService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExportController {

    private final ValuationPipelineService pipelineService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final ObjectMapper objectMapper;

    public ExportController(ValuationPipelineService pipelineService,
                            ExcelExportService excelExportService,
                            PdfExportService pdfExportService,
                            ObjectMapper objectMapper) {
        this.pipelineService = pipelineService;
        this.excelExportService = excelExportService;
        this.pdfExportService = pdfExportService;
        this.objectMapper = objectMapper;
    }

    private ValuationResponse ensureResponse(JsonNode node) {
        if (node.has("concluded_equity_value") || node.has("calibration_solved_equity")) {
            return objectMapper.convertValue(node, ValuationResponse.class);
        } else {
            ValuationRequest req = objectMapper.convertValue(node, ValuationRequest.class);
            return pipelineService.calculateValuation(req);
        }
    }

    @PostMapping("/exports/excel")
    public ResponseEntity<byte[]> exportExcelReport(@RequestBody JsonNode payload) {
        ValuationResponse res = ensureResponse(payload);
        byte[] excelBytes = excelExportService.generateValuationWorkbook(res);
        String filename = (res.getCompanyName() != null ? res.getCompanyName().replace(' ', '_') : "Company") + "_Valuation_Report.xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(excelBytes);
    }

    @PostMapping("/exports/pdf")
    public ResponseEntity<byte[]> exportPdfReport(@RequestBody JsonNode payload) {
        ValuationResponse res = ensureResponse(payload);
        byte[] pdfBytes = pdfExportService.generateValuationPdf(res);
        String filename = (res.getCompanyName() != null ? res.getCompanyName().replace(' ', '_') : "Company") + "_Valuation_Report.pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }
}

