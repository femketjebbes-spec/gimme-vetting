package com.gimmevettingsolution.intake;

import com.gimmevettingsolution.intake.dto.ExcelUploadResponse;
import com.gimmevettingsolution.intake.dto.InvalidFileFormatResponse;
import com.gimmevettingsolution.intake.dto.ColumnNameMismatchResponse;
import com.gimmevettingsolution.intake.service.ExcelParsingService;
import com.gimmevettingsolution.intake.service.MandatoryFieldValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Integration tests for ExcelIntakeController covering:
 * - Full upload flow with valid XLSX
 * - Full upload flow with valid CSV
 * - Invalid MIME type (400)
 * - Column name mismatch (400)
 * - Path traversal filename (400)
 * - Return Excel download
 */
class ExcelIntakeControllerTest {

    private MockMvc mockMvc;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        ExcelParsingService parsingService = new ExcelParsingService();
        MandatoryFieldValidationService validationService = new MandatoryFieldValidationService();
        ExcelIntakeController controller = new ExcelIntakeController(parsingService, validationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // --- Valid Upload Tests ---

    @Test
    void uploadExcel_validXlsx_returns200WithCompletion() throws Exception {
        byte[] xlsxBytes = createTestXlsx();

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes);

        mockMvc.perform(multipart("/api/v1/intake/excel").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.totalRowsProcessed").isNumber())
                .andExpect(jsonPath("$.rowsPassed").isNumber())
                .andExpect(jsonPath("$.rowsFailed").isNumber())
                .andExpect(jsonPath("$.returnExcelDownloadLink").isString());
    }

    @Test
    void uploadExcel_validCsv_returns200WithCompletion() throws Exception {
        String csv = "invoice number,debtor name,address,phone number,bank account number\n"
                + "INV-001,Test Corp,Main St 1,+31612345678,NL12TEST0123456789\n"
                + "INV-002,Corp B,Street 2,+31687654321,NL99TEST9876543210\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv",
                "text/csv",
                csv.getBytes());

        mockMvc.perform(multipart("/api/v1/intake/excel").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.totalRowsProcessed").value(2));
    }

    @Test
    void uploadExcel_allFieldsPresent_rowsPassedEqualsTotal() throws Exception {
        byte[] xlsxBytes = createFullXlsx(3);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes);

        mockMvc.perform(multipart("/api/v1/intake/excel").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRowsProcessed").value(3))
                .andExpect(jsonPath("$.rowsPassed").value(3))
                .andExpect(jsonPath("$.rowsFailed").value(0));
    }

    // --- Invalid File Format Tests ---

    @Test
    void uploadExcel_invalidMimeType_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.docx",
                "application/msword",
                "some content".getBytes());

        mockMvc.perform(multipart("/api/v1/intake/excel").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_FILE_FORMAT"));
    }

    @Test
    void uploadExcel_nullMimeType_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx",
                null,
                new byte[0]);

        mockMvc.perform(multipart("/api/v1/intake/excel").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_FILE_FORMAT"));
    }

    @Test
    void uploadExcel_emptyFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]);

        mockMvc.perform(multipart("/api/v1/intake/excel").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_FILE_FORMAT"));
    }

    // --- Column Name Mismatch Tests ---

    @Test
    void uploadExcel_unrecognizedColumns_returns400() throws Exception {
        byte[] xlsxBytes = createXlsxWithInvalidHeaders();

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes);

        mockMvc.perform(multipart("/api/v1/intake/excel").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("COLUMN_NAME_MISMATCH"))
                .andExpect(jsonPath("$.unrecognizedColumns").isArray())
                .andExpect(jsonPath("$.unrecognizedColumns[0]").value("client name"));
    }

    // --- Path Traversal Tests ---

    @Test
    void uploadExcel_pathTraversalFilename_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../etc/passwd",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]);

        mockMvc.perform(multipart("/api/v1/intake/excel").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_FILE_FORMAT"));
    }

    @Test
    void uploadExcel_absolutePathFilename_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "/etc/shadow",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]);

        mockMvc.perform(multipart("/api/v1/intake/excel").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_FILE_FORMAT"));
    }

    // --- Return Excel Download Tests ---

    @Test
    void downloadReturnExcel_existingFile_returns200() throws Exception {
        // Upload a file with failing rows first
        byte[] xlsxBytes = createPartialXlsx(2);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes);

        String uploadResult = mockMvc.perform(multipart("/api/v1/intake/excel").file(file))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Verify download link is returned
        assertTrue(uploadResult.contains("/api/v1/intake/excel/download/"));
    }

    @Test
    void downloadReturnExcel_nonExistentFile_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/intake/excel/download/nonexistent.xlsx"))
                .andExpect(status().isNotFound());
    }

    // --- Subtask 5, Test 10: Full upload -> download integration ---

    @Test
    void uploadExcel_thenDownload_returnFileExistsWithContent() throws Exception {
        // Upload a file with failing rows
        byte[] xlsxBytes = createPartialXlsx(2);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes);

        // Step 1: Upload and get the download link
        String uploadContent = mockMvc.perform(multipart("/api/v1/intake/excel").file(file))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Extract download link from JSON response
        // Expected format: /api/v1/intake/excel/download/return-<uuid>.xlsx
        String downloadLink = extractDownloadLink(uploadContent);
        assertNotNull(downloadLink);
        assertTrue(downloadLink.startsWith("/api/v1/intake/excel/download/"));

        // Extract filename from the link
        String filename = downloadLink.substring(downloadLink.lastIndexOf("/") + 1);

        // Step 2: Actually download the file
        byte[] downloadedBytes = mockMvc.perform(get(downloadLink))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        // Step 3: Verify file has content (not empty)
        assertTrue(downloadedBytes.length > 0, "Downloaded file should not be empty");

        // For XLSX, verify it starts with PK header (ZIP format)
        // First two bytes should be 0x50 0x4B (PK)
        assertEquals((byte) 0x50, downloadedBytes[0], "XLSX file should start with PK header");
        assertEquals((byte) 0x4B, downloadedBytes[1], "XLSX file should start with PK header");
    }

    // --- Template Download Tests ---

    @Test
    void getTemplate_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/intake/excel/template"))
                .andExpect(status().isOk());
    }

    @Test
    void getTemplate_returnsXlsxContentType() throws Exception {
        mockMvc.perform(get("/api/v1/intake/excel/template"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        org.springframework.http.MediaType.valueOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
    }

    @Test
    void getTemplate_returnsCorrectContentDisposition() throws Exception {
        mockMvc.perform(get("/api/v1/intake/excel/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"invoice-intake-template.xlsx\""));
    }

    @Test
    void getTemplate_returnsNonEmptyBytes() throws Exception {
        byte[] bytes = mockMvc.perform(get("/api/v1/intake/excel/template"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertNotNull(bytes);
        assertTrue(bytes.length > 0, "Template bytes must not be empty");
    }

    @Test
    void getTemplate_returnsValidXlsxFormat() throws Exception {
        byte[] bytes = mockMvc.perform(get("/api/v1/intake/excel/template"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertEquals(0x50, bytes[0], "XLSX must start with PK header");
        assertEquals(0x4B, bytes[1], "XLSX must start with PK header");
    }

    @Test
    void getTemplate_fileSizeUnder100KB() throws Exception {
        byte[] bytes = mockMvc.perform(get("/api/v1/intake/excel/template"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue(bytes.length < 100 * 1024,
                "Template file size (" + bytes.length + " bytes) must be under 100KB");
    }

    // --- Helper Methods ---

    private byte[] createTestXlsx() throws IOException {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");

            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("invoice number");
            header.createCell(1).setCellValue("debtor name");
            header.createCell(2).setCellValue("address");
            header.createCell(3).setCellValue("phone number");
            header.createCell(4).setCellValue("bank account number");

            org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("INV-001");
            dataRow.createCell(1).setCellValue("Test Corp");
            dataRow.createCell(2).setCellValue("Main St 1");
            dataRow.createCell(3).setCellValue("+31612345678");
            dataRow.createCell(4).setCellValue("NL12TEST0123456789");

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createFullXlsx(int rowCount) throws IOException {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");

            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("invoice number");
            header.createCell(1).setCellValue("debtor name");
            header.createCell(2).setCellValue("address");
            header.createCell(3).setCellValue("phone number");
            header.createCell(4).setCellValue("bank account number");

            for (int i = 0; i < rowCount; i++) {
                org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(i + 1);
                dataRow.createCell(0).setCellValue("INV-" + (i + 1));
                dataRow.createCell(1).setCellValue("Corp " + (i + 1));
                dataRow.createCell(2).setCellValue("Street " + (i + 1));
                dataRow.createCell(3).setCellValue("+316" + (10000000 + i));
                dataRow.createCell(4).setCellValue("NL" + (i + 1) + "TEST" + (i + 1));
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createPartialXlsx(int rowCount) throws IOException {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");

            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("invoice number");
            header.createCell(1).setCellValue("debtor name");

            for (int i = 0; i < rowCount; i++) {
                org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(i + 1);
                dataRow.createCell(0).setCellValue("INV-" + (i + 1));
                dataRow.createCell(1).setCellValue("Corp " + (i + 1));
                // Missing address, phone, bank - these should be failing rows
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createXlsxWithInvalidHeaders() throws IOException {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");

            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("invoice number");
            header.createCell(1).setCellValue("client name");
            header.createCell(2).setCellValue("address");

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private String extractDownloadLink(String jsonResponse) {
        // JSON format: "returnExcelDownloadLink":"/api/v1/intake/excel/download/return-<uuid>.xlsx"
        int start = jsonResponse.indexOf("\"returnExcelDownloadLink\":\"");
        if (start < 0) {
            return null;
        }
        start = start + "\"returnExcelDownloadLink\":\"".length();
        int end = jsonResponse.indexOf("\"", start);
        return jsonResponse.substring(start, end);
    }
}
