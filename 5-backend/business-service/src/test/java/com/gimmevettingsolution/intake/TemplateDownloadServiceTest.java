package com.gimmevettingsolution.intake;

import com.gimmevettingsolution.intake.service.ExcelParsingService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the template generation service method.
 * Validates that generateTemplateXlsx() produces a valid XLSX file
 * with correct headers and structure per WI-007 contract.
 */
class TemplateDownloadServiceTest {

    private ExcelParsingService service;

    @BeforeEach
    void setUp() {
        service = new ExcelParsingService();
    }

    // --- Test 1: generateTemplateXlsx returns non-null byte array ---

    @Test
    void generateTemplateXlsx_returnsNonEmptyByteArray() throws IOException {
        byte[] result = service.generateTemplateXlsx();

        assertNotNull(result, "Template bytes must not be null");
        assertTrue(result.length > 0, "Template bytes must not be empty");
    }

    // --- Test 2: Returned bytes are valid XLSX (PK ZIP header) ---

    @Test
    void generateTemplateXlsx_validXlsxFormat() throws IOException {
        byte[] result = service.generateTemplateXlsx();

        // XLSX is a ZIP format, starts with PK (0x50 0x4B)
        assertEquals(0x50, result[0], "XLSX must start with PK header byte 0x50");
        assertEquals(0x4B, result[1], "XLSX must start with PK header byte 0x4B");
    }

    // --- Test 3: Template has exactly one sheet ---

    @Test
    void generateTemplateXlsx_hasExactlyOneSheet() throws IOException {
        byte[] result = service.generateTemplateXlsx();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertEquals(1, workbook.getNumberOfSheets(), "Template must have exactly one sheet");
        }
    }

    // --- Test 4: Template has exactly 5 columns in header row ---

    @Test
    void generateTemplateXlsx_headerRowHasFiveColumns() throws IOException {
        byte[] result = service.generateTemplateXlsx();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow, "Header row (row 0) must exist");
            assertEquals(5, headerRow.getLastCellNum() - headerRow.getFirstCellNum(),
                    "Header row must have exactly 5 columns");
        }
    }

    // --- Test 5: Header values match ALLOWED_COLUMN_NAMES constants exactly ---

    @Test
    void generateTemplateXlsx_headersMatchAllowedColumnNames() throws IOException {
        byte[] result = service.generateTemplateXlsx();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            for (int i = 0; i < 5; i++) {
                Cell cell = headerRow.getCell(i);
                assertNotNull(cell, "Column " + i + " header must not be null");
                String value = cell.getStringCellValue().trim();
                assertTrue(ExcelParsingService.ALLOWED_COLUMN_NAMES.contains(value),
                        "Header at index " + i + " (" + value + ") must be in ALLOWED_COLUMN_NAMES");
            }
        }
    }

    // --- Test 6: Template has at least one empty data row ---

    @Test
    void generateTemplateXlsx_hasEmptyDataRow() throws IOException {
        byte[] result = service.generateTemplateXlsx();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row dataRow = sheet.getRow(1);
            assertNotNull(dataRow, "Row 1 (first data row) must exist as visual guide");
        }
    }

    // --- Test 7: Data row is empty (no example data) ---

    @Test
    void generateTemplateXlsx_dataRowIsEmpty() throws IOException {
        byte[] result = service.generateTemplateXlsx();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row dataRow = sheet.getRow(1);
            assertNotNull(dataRow, "Data row must exist");

            for (int i = 0; i < 5; i++) {
                Cell cell = dataRow.getCell(i);
                if (cell != null) {
                    String value = cell.getStringCellValue().trim();
                    assertTrue(value.isEmpty(),
                            "Data row cell " + i + " must be empty (no example data), got: [" + value + "]");
                }
            }
        }
    }

    // --- Test 8: File size under 100KB ---

    @Test
    void generateTemplateXlsx_fileSizeUnder100KB() throws IOException {
        byte[] result = service.generateTemplateXlsx();

        int maxSize = 100 * 1024; // 100KB
        assertTrue(result.length < maxSize,
                "Template file size (" + result.length + " bytes) must be under 100KB");
    }

    // --- Test 9: Header order matches expected column sequence ---

    @Test
    void generateTemplateXlsx_headersInCorrectOrder() throws IOException {
        byte[] result = service.generateTemplateXlsx();

        String[] expectedOrder = {
                "invoice number",
                "debtor name",
                "address",
                "phone number",
                "bank account number"
        };

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            for (int i = 0; i < expectedOrder.length; i++) {
                Cell cell = headerRow.getCell(i);
                assertNotNull(cell);
                assertEquals(expectedOrder[i], cell.getStringCellValue().trim(),
                        "Header at index " + i + " must be '" + expectedOrder[i] + "'");
            }
        }
    }

    // --- Test 10: No extra rows beyond header and one data row ---

    @Test
    void generateTemplateXlsx_noExtraDataRows() throws IOException {
        byte[] result = service.generateTemplateXlsx();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            assertEquals(1, lastRowNum,
                    "Template should have exactly 2 rows (header at 0, one empty data row at 1), got lastRowNum=" + lastRowNum);
        }
    }
}
