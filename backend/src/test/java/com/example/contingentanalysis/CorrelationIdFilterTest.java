package com.example.contingentanalysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class CorrelationIdFilterTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private com.example.contingentanalysis.config.CorrelationIdFilter correlationIdFilter;

    private MockMvc mockMvc;

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F-]{36}$");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .addFilters(correlationIdFilter)
                .build();
    }

    @Test
    void testIncomingRequestIdPreserved() throws Exception {
        String clientReqId = "req-custom-abc-123";

        MvcResult result = mockMvc.perform(get("/api/health")
                        .header("X-Request-Id", clientReqId))
                .andExpect(status().isOk())
                .andReturn();

        String responseReqId = result.getResponse().getHeader("X-Request-Id");
        assertThat(responseReqId).isEqualTo(clientReqId);
    }

    @Test
    void testMissingRequestIdGeneratesUuid() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andReturn();

        String responseReqId = result.getResponse().getHeader("X-Request-Id");
        assertThat(responseReqId).isNotNull();
        assertThat(UUID_PATTERN.matcher(responseReqId).matches()).isTrue();
    }

    @Test
    void testFallbackToCorrelationIdHeader() throws Exception {
        String correlationId = "corr-987654";

        MvcResult result = mockMvc.perform(get("/api/health")
                        .header("X-Correlation-Id", correlationId))
                .andExpect(status().isOk())
                .andReturn();

        String responseReqId = result.getResponse().getHeader("X-Request-Id");
        assertThat(responseReqId).isEqualTo(correlationId);
    }

    @Test
    void testInvalidRequestIdSanitizedToUuid() throws Exception {
        // Headers with unsafe characters (HTML, spaces, control chars) must be replaced
        String maliciousReqId = "req<script>alert(1)</script>";

        MvcResult result = mockMvc.perform(get("/api/health")
                        .header("X-Request-Id", maliciousReqId))
                .andExpect(status().isOk())
                .andReturn();

        String responseReqId = result.getResponse().getHeader("X-Request-Id");
        assertThat(responseReqId).isNotEqualTo(maliciousReqId);
        assertThat(UUID_PATTERN.matcher(responseReqId).matches()).isTrue();
    }
}

