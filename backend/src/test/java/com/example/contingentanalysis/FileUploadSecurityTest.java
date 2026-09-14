package com.example.contingentanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class FileUploadSecurityTest {

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
    void testEmptyFileRejected() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]
        );

        MvcResult result = mockMvc.perform(multipart("/api/capital-iq/upload").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(responseNode.get("detail").asText()).contains("empty");
    }

    @Test
    void testInvalidExtensionRejected() throws Exception {
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                "evil payload".getBytes()
        );

        MvcResult result = mockMvc.perform(multipart("/api/capital-iq/upload").file(maliciousFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(responseNode.get("detail").asText()).contains("supported");
    }

    @Test
    void testPathTraversalFilenameRejected() throws Exception {
        MockMultipartFile traversalFile = new MockMultipartFile(
                "file",
                "../../etc/passwd.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "content".getBytes()
        );

        MvcResult result = mockMvc.perform(multipart("/api/capital-iq/upload").file(traversalFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(responseNode.get("detail").asText()).contains("filename");
    }

    @Test
    void testCorruptedExcelFileHandledSafely() throws Exception {
        MockMultipartFile corruptedFile = new MockMultipartFile(
                "file",
                "corrupted.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "this is definitely not a zip or xlsx file content".getBytes()
        );

        MvcResult result = mockMvc.perform(multipart("/api/capital-iq/upload").file(corruptedFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(responseNode.get("detail").asText()).contains("Invalid or malformed Excel workbook");
    }
}

