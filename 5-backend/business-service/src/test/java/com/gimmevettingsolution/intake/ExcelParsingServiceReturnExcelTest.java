package com.gimmevettingsolution.intake;

import com.gimmevettingsolution.intake.dto.ExcelInvoiceRow;
import com.gimmevettingsolution.intake.service.ExcelParsingService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExcelParsingService return Excel generation.
 * Covers Subtask 5 of WI-004 delegation plan.
 */
class ExcelParsingServiceReturnExcelTest {

    private ExcelParsingService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new ExcelParsingService();
    }

    // --- Subtask 5, Test 1: Correct number of data rows ---

    @Test
    void generateReturnExcel_generatesCorrectNumberOfDataRows() throws Exception {
        ExcelInvoiceRow[] failingRows = new ExcelInvoiceRow[]{
                createPartialRow("INV-001", "Corp A", null, null, null),
                createPartialRow("INV-002", "Corp B", null, null, null),
                createPartialRow("INV-003", "Corp C", null, null, null)
        };

        Path result = service.generateReturnExcel(failingRows, tempDir);

        assertNotNull(result);
        assertTrue(Files.exists(result));

        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(result.toFile()))) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getPhysicalNumberOfRows();
            // 1 header row + 3 data rows
            assertEquals(4, rowCount);
        }
    }

    // --- Subtask 5, Test 2: Includes all original column data ---

    @Test
    void generateReturnExcel_includesAllOriginalColumnData() throws Exception {
        ExcelInvoiceRow[] failingRows = new ExcelInvoiceRow[]{
                createPartialRow("INV-100", "Test Corp", "Main St 1", "+31612345678", null)
        };

        Path result = service.generateReturnExcel(failingRows, tempDir);

        assertNotNull(result);
        assertTrue(Files.exists(result));

        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(result.toFile()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row dataRow = sheet.getRow(1);

            assertEquals("INV-100", dataRow.getCell(0).getStringCellValue());
            assertEquals("Test Corp", dataRow.getCell(1).getStringCellValue());
            assertEquals("Main St 1", dataRow.getCell(2).getStringCellValue());
            assertEquals("+31612345678", dataRow.getCell(3).getStringCellValue());
            // bank account is null -> empty cell
            assertEquals("", dataRow.getCell(4).getStringCellValue());
            // Issue column should contain MISSING_FIELDS with bankAccountNumber
            String issue = dataRow.getCell(5).getStringCellValue();
            assertTrue(issue.startsWith("MISSING_FIELDS: "));
            assertTrue(issue.contains("bankAccountNumber"));
        }
    }

    // --- Subtask 5, Test 3: Correct Issue format ---

    @Test
    void generateReturnExcel_includesCorrectIssueFormat() throws Exception {
        ExcelInvoiceRow[] failingRows = new ExcelInvoiceRow[]{
                createPartialRow(null, null, "Main St", "+31612345678", "NL12TEST")
        };

        Path result = service.generateReturnExcel(failingRows, tempDir);

        assertNotNull(result);
        assertTrue(Files.exists(result));

        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(result.toFile()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row dataRow = sheet.getRow(1);
            String issue = dataRow.getCell(5).getStringCellValue();

            // Format: "MISSING_FIELDS: invoiceNumber, debtorName" (space after colon)
            assertTrue(issue.startsWith("MISSING_FIELDS: "), "Issue should start with 'MISSING_FIELDS: ' (with space)");
            assertTrue(issue.contains("invoiceNumber"), "Issue should contain invoiceNumber");
            assertTrue(issue.contains("debtorName"), "Issue should contain debtorName");
            // Verify no format without space — check that "MISSING_FIELDS:" followed by letter is absent
            assertFalse(issue.contains("MISSING_FIELDS:invoiceNumber"), "Issue should not have format without space after colon");
        }
    }

    // --- Subtask 5, Test 4: No failing rows returns null (4-param) ---

    @Test
    void generateReturnExcel_noFailingRows_4param_returnsNull() throws Exception {
        ExcelInvoiceRow[] emptyRows = new ExcelInvoiceRow[]{};

        Path result = service.generateReturnExcel(emptyRows, tempDir, "return.xlsx", false);

        assertNull(result);
    }

    @Test
    void generateReturnExcel_nullFailingRows_4param_returnsNull() throws Exception {
        Path result = service.generateReturnExcel(null, tempDir, "return.xlsx", false);

        assertNull(result);
    }

    // --- Subtask 5, Test 5: Preserves null values as empty cells ---

    @Test
    void generateReturnExcel_preservesNullValuesAsEmptyCells() throws Exception {
        ExcelInvoiceRow[] failingRows = new ExcelInvoiceRow[]{
                createPartialRow(null, null, null, null, null)
        };

        Path result = service.generateReturnExcel(failingRows, tempDir);

        assertNotNull(result);
        assertTrue(Files.exists(result));

        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(result.toFile()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row dataRow = sheet.getRow(1);

            for (int i = 0; i < 5; i++) {
                Cell cell = dataRow.getCell(i);
                assertEquals(CellType.STRING, cell.getCellType(), "Cell " + i + " should be a string type");
                assertEquals("", cell.getStringCellValue(), "Cell " + i + " should be empty string for null value");
            }
        }
    }

    // --- Subtask 5, Test 6: XLSX format matches upload ---

    @Test
    void generateReturnExcel_xlsxFormat_generatesXlsxFile() throws Exception {
        ExcelInvoiceRow[] failingRows = new ExcelInvoiceRow[]{
                createPartialRow("INV-1", "Corp", null, null, null)
        };

        Path result = service.generateReturnExcel(failingRows, tempDir, "return.xlsx", false);

        assertNotNull(result);
        assertTrue(result.getFileName().toString().endsWith(".xlsx"));
        assertTrue(Files.exists(result));

        // Verify it can be read as XLSX
        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(result.toFile()))) {
            assertNotNull(workbook.getSheetAt(0));
        }
    }

    // --- Subtask 5, Test 7: CSV format generates valid CSV ---

    @Test
    void generateReturnExcel_csvFormat_generatesValidCsv() throws Exception {
        ExcelInvoiceRow[] failingRows = new ExcelInvoiceRow[]{
                createPartialRow("INV-1", "Corp A", "Main St", "+31612345678", "NL12TEST")
        };

        Path result = service.generateReturnExcel(failingRows, tempDir, "return.csv", true);

        assertNotNull(result);
        assertTrue(result.getFileName().toString().endsWith(".csv"));
        assertTrue(Files.exists(result));

        String content = Files.readString(result, StandardCharsets.UTF_8);
        String[] lines = content.trim().split("\n");

        // Header + 1 data row
        assertEquals(2, lines.length);
        // Header should contain Issue column
        assertTrue(lines[0].contains("Issue"));
        // Data row should contain at least one issue (MISSING_FIELDS or MISSING_POC)
        assertTrue(lines[1].contains("MISSING_FIELDS:") || lines[1].contains("MISSING_POC"));
    }

    // --- Subtask 5, Test 8: CSV escaping for values containing commas ---

    @Test
    void generateReturnCsv_escaping_handlesCommasInValues() throws Exception {
        ExcelInvoiceRow[] failingRows = new ExcelInvoiceRow[]{
                createPartialRow("INV, with comma", "Corp, Ltd", "Street, 1", "+31612345678", "NL12TEST")
        };

        Path result = service.generateReturnExcel(failingRows, tempDir, "return.csv", true);

        assertNotNull(result);
        assertTrue(Files.exists(result));

        String content = Files.readString(result, StandardCharsets.UTF_8);
        String[] lines = content.trim().split("\n");

        assertEquals(2, lines.length);
        // First data row should have quoted values for fields containing commas
        String dataLine = lines[1];
        assertTrue(dataLine.contains("\"INV, with comma\""), "Invoice number with comma should be quoted");
        assertTrue(dataLine.contains("\"Corp, Ltd\""), "Debtor name with comma should be quoted");
    }

    @Test
    void generateReturnCsv_escaping_handlesDoubleQuotesInValues() throws Exception {
        ExcelInvoiceRow[] failingRows = new ExcelInvoiceRow[]{
                createPartialRow("INV \"quoted\"", "Corp \"Co\"", "Main St", "+31612345678", "NL12TEST")
        };

        Path result = service.generateReturnExcel(failingRows, tempDir, "return.csv", true);

        assertNotNull(result);
        assertTrue(Files.exists(result));

        String content = Files.readString(result, StandardCharsets.UTF_8);
        String[] lines = content.trim().split("\n");

        // Values with double quotes should be quoted and the internal quotes doubled
        String dataLine = lines[1];
        assertTrue(dataLine.contains("\"INV \"\"quoted\"\"\""), "Double quotes should be escaped by doubling");
    }

    // --- Subtask 5, Test 9: Backward compatibility (2-param overload) ---

    @Test
    void generateReturnExcel_2paramOverload_backwardCompatible() throws Exception {
        ExcelInvoiceRow[] failingRows = new ExcelInvoiceRow[]{
                createPartialRow("INV-1", "Corp", null, null, null)
        };

        // Call the 2-param method (backward compatibility)
        Path result = service.generateReturnExcel(failingRows, tempDir);

        assertNotNull(result);
        assertTrue(result.getFileName().toString().endsWith(".xlsx"));
        assertTrue(Files.exists(result));

        // Verify file is a valid XLSX
        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(result.toFile()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(2, sheet.getPhysicalNumberOfRows()); // header + 1 data
        }
    }

    @Test
    void generateReturnExcel_2paramOverload_emptyArray_returnsNull() throws Exception {
        Path result = service.generateReturnExcel(new ExcelInvoiceRow[]{}, tempDir);
        assertNull(result);
    }

    // --- Helper ---

    private ExcelInvoiceRow createPartialRow(String invoiceNumber, String debtorName,
                                              String address, String phoneNumber,
                                              String bankAccountNumber) {
        ExcelInvoiceRow row = new ExcelInvoiceRow();
        row.setInvoiceNumber(invoiceNumber);
        row.setDebtorName(debtorName);
        row.setAddress(address);
        row.setPhoneNumber(phoneNumber);
        row.setBankAccountNumber(bankAccountNumber);
        row.setRowIndex(0);
        return row;
    }
}
