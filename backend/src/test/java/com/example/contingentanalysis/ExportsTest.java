package com.example.contingentanalysis;

import com.example.contingentanalysis.domain.breakpoints.BreakpointService;
import com.example.contingentanalysis.domain.capitalization.CapitalizationService;
import com.example.contingentanalysis.domain.claims.ClaimsService;
import com.example.contingentanalysis.domain.defaultscenario.DefaultScenarioService;
import com.example.contingentanalysis.domain.exports.ExcelExportService;
import com.example.contingentanalysis.domain.exports.PdfExportService;
import com.example.contingentanalysis.domain.holdings.HoldingsService;
import com.example.contingentanalysis.domain.model.ValuationRequest;
import com.example.contingentanalysis.domain.model.ValuationResponse;
import com.example.contingentanalysis.domain.opm.OpmService;
import com.example.contingentanalysis.domain.pipeline.ValuationPipelineService;
import com.example.contingentanalysis.domain.riskfree.RiskFreeRateService;
import com.example.contingentanalysis.domain.validation.ValuationValidationService;
import com.example.contingentanalysis.domain.waterfall.WaterfallService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ExportsTest {

    private ValuationResponse sampleValuationResponse;
    private ExcelExportService excelExportService;
    private PdfExportService pdfExportService;

    @BeforeEach
    void setUp() {
        CapitalizationService capitalizationService = new CapitalizationService();
        BreakpointService breakpointService = new BreakpointService();
        ClaimsService claimsService = new ClaimsService();
        OpmService opmService = new OpmService(claimsService);
        WaterfallService waterfallService = new WaterfallService(claimsService);
        HoldingsService holdingsService = new HoldingsService();
        ValuationValidationService validationService = new ValuationValidationService();
        RiskFreeRateService riskFreeRateService = new RiskFreeRateService();
        DefaultScenarioService defaultScenarioService = new DefaultScenarioService();
        ValuationPipelineService pipelineService = new ValuationPipelineService(
                validationService, capitalizationService, breakpointService, claimsService,
                opmService, waterfallService, holdingsService, riskFreeRateService
        );

        ValuationRequest req = defaultScenarioService.getDefaultValuationRequest();
        sampleValuationResponse = pipelineService.calculateValuation(req);

        excelExportService = new ExcelExportService();
        pdfExportService = new PdfExportService();
    }

    @Test
    void testExcelExportGeneration() throws IOException {
        byte[] excelBytes = excelExportService.generateValuationWorkbook(sampleValuationResponse);
        assertThat(excelBytes).isNotNull();
        assertThat(excelBytes.length).isGreaterThan(2000);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            List<String> sheetNames = new ArrayList<>();
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                sheetNames.add(wb.getSheetName(i));
            }

            List<String> expectedSheets = List.of(
                    "Key Assumptions",
                    "Calibration Cap Table",
                    "Valuation Cap Table",
                    "Breakpoint Schedule",
                    "Valuation OPM Allocation",
                    "Client Holdings Summary",
                    "Waterfall Analysis"
            );

            for (String sheet : expectedSheets) {
                assertThat(sheetNames).contains(sheet);
            }
        }
    }

    @Test
    void testPdfExportWatermark() throws IOException {
        byte[] pdfBytes = pdfExportService.generateValuationPdf(sampleValuationResponse);
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(5000);

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(10);

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            assertThat(text).contains("HIGHLY CONFIDENTIAL");
            assertThat(text).contains("TADO");
            assertThat(text).contains("S2G Investments");

            for (String exNum : List.of(
                    "Exhibit 1.0", "Exhibit 2.0", "Exhibit 3.0", "Exhibit 4.0", "Exhibit 5.0",
                    "Exhibit 6.0", "Exhibit 7.0", "Exhibit 8.0", "Exhibit 9.0", "Exhibit 10.0"
            )) {
                assertThat(text).as("Missing " + exNum + " in generated PDF export").contains(exNum);
            }
        }
    }
}

