package com.example.contingentanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ApiExceptionHandlerTest {

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
    void testMalformedJsonReturns400() throws Exception {
        String malformedJson = "{ company_name: unquoted, broken: json ";

        MvcResult result = mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.path").value("/api/calculate"))
                .andExpect(jsonPath("$.request_id").exists())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(responseNode.get("detail").asText()).contains("Malformed JSON");
    }

    @Test
    void testValidationErrorReturns400WithErrorsMap() throws Exception {
        // ValuationRequest with empty company name, blank client name, invalid date format, invalid display units
        String invalidPayload = """
                {
                    "company_name": "",
                    "client_name": "",
                    "calibration_date": "not-a-date",
                    "display_units": "invalid_units",
                    "waterfall_equity_source": "invalid_source"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String detail = responseNode.get("detail").asText();
        assertThat(detail).contains("companyName");
        assertThat(detail).contains("clientName");
        assertThat(detail).contains("calibrationDate");
        assertThat(detail).contains("displayUnits");
        assertThat(responseNode.get("errors").size()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void testCapitalIqWorkbookValidationError() throws Exception {
        // Tickers list is empty
        String invalidPayload = """
                {
                    "tickers": [],
                    "calibration_date": "invalid",
                    "valuation_date": "2026-06-30"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/capital-iq/workbook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(responseNode.get("detail").asText()).contains("tickers");
    }

    @Test
    void testDateMismatchReturns400WithDetail() throws Exception {
        // Exit date before valuation date triggers IllegalArgumentException in pipeline
        String invalidDatesPayload = """
                {
                    "company_name": "Test Co",
                    "client_name": "Test Client",
                    "calibration_date": "2026-06-30",
                    "valuation_date": "2026-06-30",
                    "exit_date": "2025-01-01"
                }
                """;

        mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidDatesPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void testNotFoundReturns404WithDetail() throws Exception {
        mockMvc.perform(get("/api/unknown-endpoint-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.path").value("/api/unknown-endpoint-path"));
    }
}

