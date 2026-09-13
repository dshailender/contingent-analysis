package com.example.contingentanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ApiEndpointsTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testApiHealth() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void testApiRootServesFrontend() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("<app-root>");
    }

    @Test
    void testApiDefaultScenarioAndCalculate() throws Exception {
        // 1. Get default scenario
        MvcResult defResult = mockMvc.perform(get("/api/scenario/default"))
                .andExpect(status().isOk())
                .andReturn();

        String defaultJson = defResult.getResponse().getContentAsString();
        JsonNode defNode = objectMapper.readTree(defaultJson);
        assertThat(defNode.get("company_name").asText()).isEqualTo("TADO");

        // 2. Validate
        mockMvc.perform(post("/api/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(defaultJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        // 3. Calculate
        MvcResult calcResult = mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(defaultJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode calcNode = objectMapper.readTree(calcResult.getResponse().getContentAsString());
        assertThat(calcNode.get("company_name").asText()).isEqualTo("TADO");
        assertThat(calcNode.get("calibration_solved_equity").asDouble()).isCloseTo(287252502.92, within(1.0));
        assertThat(calcNode.get("calibration_opm").get("per_share_values").get("Series I").asDouble()).isCloseTo(2021.90, within(0.01));
        assertThat(calcNode.get("valuation_opm").get("per_share_values").get("Series I").asDouble()).isCloseTo(1992.50, within(0.05));
        assertThat(calcNode.has("calibration_waterfall")).isTrue();
        assertThat(calcNode.has("comparative_waterfall")).isTrue();
        assertThat(calcNode.get("comparative_waterfall").size()).isGreaterThan(0);
    }

    @Test
    void testApiCalculateWithDeletedCalibrationSecurity() throws Exception {
        MvcResult defResult = mockMvc.perform(get("/api/scenario/default"))
                .andExpect(status().isOk())
                .andReturn();

        ObjectNode reqNode = (ObjectNode) objectMapper.readTree(defResult.getResponse().getContentAsString());
        ArrayNode calSecs = (ArrayNode) reqNode.get("calibration_securities");
        // Remove first element (Series I)
        calSecs.remove(0);
        // Keep calibration_security_name as 'Series I'
        reqNode.put("calibration_security_name", "Series I");

        MvcResult calcResult = mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqNode.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(calcResult.getResponse().getContentAsString());
        assertThat(data.get("calibration_security_name").asText()).isEqualTo("Series H");
        assertThat(data.get("calibration_solved_equity").asDouble()).isGreaterThan(0);
        assertThat(data.get("calibration_opm").get("per_share_values").has("Series H")).isTrue();
    }

    @Test
    void testApiRiskFreeCurves() throws Exception {
        mockMvc.perform(get("/api/risk-free-rates/curves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.us_treasury").isArray())
                .andExpect(jsonPath("$.ecb").isArray());
    }

    @Test
    void testApiCapitalIqTemplateGeneration() throws Exception {
        String payload = """
                {
                    "tickers": ["IQ247543", "IQ28472"],
                    "calibration_date": "2025-02-26",
                    "valuation_date": "2026-06-30",
                    "currency": "EUR"
                }
                """;

        MvcResult res = mockMvc.perform(post("/api/capital-iq/workbook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        byte[] content = res.getResponse().getContentAsByteArray();
        assertThat(content.length).isGreaterThan(1000);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            List<String> sheets = new ArrayList<>();
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                sheets.add(wb.getSheetName(i));
            }
            assertThat(sheets).contains("Instructions", "Snapshot_Calibration");
        }
    }

    @Test
    void testApiExportExcel() throws Exception {
        MvcResult defResult = mockMvc.perform(get("/api/scenario/default"))
                .andExpect(status().isOk())
                .andReturn();

        String reqJson = defResult.getResponse().getContentAsString();

        MvcResult res = mockMvc.perform(post("/api/exports/excel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isOk())
                .andReturn();

        byte[] content = res.getResponse().getContentAsByteArray();
        assertThat(content.length).isGreaterThan(2000);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            List<String> sheets = new ArrayList<>();
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                sheets.add(wb.getSheetName(i));
            }
            assertThat(sheets).contains("Valuation OPM Allocation");
        }
    }

    @Test
    void testApiExportPdfWithWatermark() throws Exception {
        MvcResult defResult = mockMvc.perform(get("/api/scenario/default"))
                .andExpect(status().isOk())
                .andReturn();

        ObjectNode reqNode = (ObjectNode) objectMapper.readTree(defResult.getResponse().getContentAsString());
        reqNode.put("show_secondary_currency", true);
        reqNode.put("secondary_currency", "USD");
        reqNode.put("secondary_fx_rate", 1.08);

        MvcResult res = mockMvc.perform(post("/api/exports/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqNode.toString()))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdfBytes = res.getResponse().getContentAsByteArray();
        assertThat(pdfBytes.length).isGreaterThan(5000);

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(10);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            assertThat(text).contains("HIGHLY CONFIDENTIAL");
            assertThat(text).contains("TADO");
            assertThat(text).contains("Exhibit 1.0");
            assertThat(text).contains("Exhibit 10.0");
        }
    }
}

