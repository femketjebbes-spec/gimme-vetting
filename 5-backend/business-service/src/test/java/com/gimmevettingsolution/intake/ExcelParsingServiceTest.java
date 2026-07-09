package com.gimmevettingsolution.intake;

import com.gimmevettingsolution.intake.dto.ExcelInvoiceRow;
import com.gimmevettingsolution.intake.service.ExcelParsingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExcelParsingService covering:
 * - XLSX with header row
 * - XLSX without header row (position mapping)
 * - CSV parsing
 * - Malformed file
 * - Empty rows
 * - Partial rows
 * - Column name validation
 * - MIME type validation
 * - Filename validation
 */
class ExcelParsingServiceTest {

    private ExcelParsingService service;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        service = new ExcelParsingService();
        tempDir = Files.createTempDirectory("test-excel");
    }

    // --- MIME Type Validation Tests ---

    @Test
    void isSupportedMimeType_xlsx_returnsTrue() {
        assertTrue(service.isSupportedMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void isSupportedMimeType_csv_returnsTrue() {
        assertTrue(service.isSupportedMimeType("text/csv"));
    }

    @Test
    void isSupportedMimeType_word_returnsFalse() {
        assertFalse(service.isSupportedMimeType("application/msword"));
    }

    @Test
    void isSupportedMimeType_null_returnsFalse() {
        assertFalse(service.isSupportedMimeType(null));
    }

    @Test
    void isSupportedMimeType_empty_returnsFalse() {
        assertFalse(service.isSupportedMimeType(""));
    }

    @Test
    void isSupportedMimeType_withCharset_returnsTrue() {
        assertTrue(service.isSupportedMimeType("text/csv; charset=utf-8"));
    }

    // --- FileType Detection Tests ---

    @Test
    void detectFileType_xlsxSignature_returnsXLSX() throws IOException {
        // ZIP local file header signature: PK\x03\x04
        byte[] xlsxSignature = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00};
        assertEquals(com.gimmevettingsolution.intake.service.FileType.XLSX,
                service.detectFileType(new ByteArrayInputStream(xlsxSignature)));
    }

    @Test
    void detectFileType_fullXlsxFile_returnsXLSX() throws IOException {
        // Create a real XLSX file using Apache POI
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("invoice number");
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            workbook.write(baos);
            byte[] xlsxBytes = baos.toByteArray();

            assertEquals(com.gimmevettingsolution.intake.service.FileType.XLSX,
                    service.detectFileType(new ByteArrayInputStream(xlsxBytes)));
        }
    }

    @Test
    void detectFileType_csvText_returnsCSV() throws IOException {
        String csvContent = "invoice number,debtor name,address\nINV-001,Test Corp,Main St\n";
        assertEquals(com.gimmevettingsolution.intake.service.FileType.CSV,
                service.detectFileType(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void detectFileType_asciiText_returnsCSV() throws IOException {
        String asciiContent = "hello world\nfoo bar\n";
        assertEquals(com.gimmevettingsolution.intake.service.FileType.CSV,
                service.detectFileType(new ByteArrayInputStream(asciiContent.getBytes(StandardCharsets.US_ASCII))));
    }

    @Test
    void detectFileType_binaryContent_returnsUnknown() throws IOException {
        // First 4 bytes are non-text binary bytes (0x00-0x1F range, control chars)
        byte[] binaryContent = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
        assertEquals(com.gimmevettingsolution.intake.service.FileType.UNKNOWN,
                service.detectFileType(new ByteArrayInputStream(binaryContent)));
    }

    @Test
    void detectFileType_jpgContent_returnsUnknown() throws IOException {
        // JPEG magic bytes: FF D8 FF E0
        byte[] jpgContent = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x01};
        assertEquals(com.gimmevettingsolution.intake.service.FileType.UNKNOWN,
                service.detectFileType(new ByteArrayInputStream(jpgContent)));
    }

    @Test
    void detectFileType_emptyStream_returnsUnknown() throws IOException {
        byte[] empty = new byte[0];
        assertEquals(com.gimmevettingsolution.intake.service.FileType.UNKNOWN,
                service.detectFileType(new ByteArrayInputStream(empty)));
    }

    @Test
    void detectFileType_nullStream_returnsUnknown() throws IOException {
        assertEquals(com.gimmevettingsolution.intake.service.FileType.UNKNOWN,
                service.detectFileType(null));
    }

    @Test
    void detectFileType_singleByte_returnsUnknown() throws IOException {
        byte[] single = new byte[]{0x41};
        assertEquals(com.gimmevettingsolution.intake.service.FileType.UNKNOWN,
                service.detectFileType(new ByteArrayInputStream(single)));
    }

    // --- Filename Validation Tests ---

    @Test
    void isSafeFilename_normal_returnsTrue() {
        assertTrue(service.isSafeFilename("invoices.xlsx"));
    }

    @Test
    void isSafeFilename_withDots_returnsTrue() {
        assertTrue(service.isSafeFilename("data.2026.xlsx"));
    }

    @Test
    void isSafeFilename_pathTraversal_returnsFalse() {
        assertFalse(service.isSafeFilename("../../etc/passwd"));
    }

    @Test
    void isSafeFilename_absolutePath_returnsFalse() {
        assertFalse(service.isSafeFilename("/etc/passwd"));
    }

    @Test
    void isSafeFilename_null_returnsFalse() {
        assertFalse(service.isSafeFilename(null));
    }

    @Test
    void isSafeFilename_empty_returnsFalse() {
        assertFalse(service.isSafeFilename(""));
    }

    @Test
    void sanitizeFilename_specialChars_replacesWithUnderscore() {
        String result = service.sanitizeFilename("inv@#oices.xlsx");
        assertEquals("inv__oices.xlsx", result);
    }

    @Test
    void sanitizeFilename_normal_returnsSame() {
        String result = service.sanitizeFilename("invoices.xlsx");
        assertEquals("invoices.xlsx", result);
    }

    @Test
    void sanitizeFilename_null_returnsDefault() {
        String result = service.sanitizeFilename(null);
        assertEquals("upload.xlsx", result);
    }

    // --- Column Name Validation Tests ---

    @Test
    void validateColumnNames_allValid_returnsEmpty() {
        String[] headers = {"invoice number", "debtor name", "address"};
        List<String> result = service.validateColumnNames(headers);
        assertTrue(result.isEmpty());
    }

    @Test
    void validateColumnNames_caseInsensitive_allValid_returnsEmpty() {
        String[] headers = {"Invoice Number", "DEBTOR NAME", "Address"};
        List<String> result = service.validateColumnNames(headers);
        assertTrue(result.isEmpty());
    }

    @Test
    void validateColumnNames_unrecognizedColumn_returnsList() {
        String[] headers = {"invoice number", "client name", "address"};
        List<String> result = service.validateColumnNames(headers);
        assertEquals(1, result.size());
        assertEquals("client name", result.get(0));
    }

    @Test
    void validateColumnNames_multipleUnrecognized_returnsAll() {
        String[] headers = {"invoice id", "client name", "amount"};
        List<String> result = service.validateColumnNames(headers);
        assertEquals(3, result.size());
    }

    @Test
    void validateColumnNames_emptyStrings_skipped() {
        String[] headers = {"invoice number", "", "  "};
        List<String> result = service.validateColumnNames(headers);
        assertTrue(result.isEmpty());
    }

    // --- XLSX Parsing Tests ---

    @Test
    void parseXlsx_withHeader_allFieldsParsed() throws IOException {
        // Create a minimal valid XLSX file in memory
        // A valid XLSX is a ZIP file with specific structure
        // We'll use Apache POI to create one
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");

            // Header row
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("invoice number");
            header.createCell(1).setCellValue("debtor name");
            header.createCell(2).setCellValue("address");
            header.createCell(3).setCellValue("phone number");
            header.createCell(4).setCellValue("bank account number");

            // Data row
            org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("INV-001");
            dataRow.createCell(1).setCellValue("Test Corp");
            dataRow.createCell(2).setCellValue("Main St 1");
            dataRow.createCell(3).setCellValue("+31612345678");
            dataRow.createCell(4).setCellValue("NL12TEST0123456789");

            // Write toByteArray
            byte[] xlsxBytes = getWorkbookBytes(workbook);

            ExcelInvoiceRow[] rows = service.parse(new ByteArrayInputStream(xlsxBytes), false);
            assertEquals(1, rows.length);
            assertEquals("INV-001", rows[0].getInvoiceNumber());
            assertEquals("Test Corp", rows[0].getDebtorName());
            assertEquals("Main St 1", rows[0].getAddress());
            assertEquals("+31612345678", rows[0].getPhoneNumber());
            assertEquals("NL12TEST0123456789", rows[0].getBankAccountNumber());
        }
    }

    @Test
    void parseXlsx_withoutHeader_usesPositionMapping() throws IOException {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");

            // No header - just data
            org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(0);
            dataRow.createCell(0).setCellValue("INV-002");
            dataRow.createCell(1).setCellValue("Corp B");
            dataRow.createCell(2).setCellValue("Street 2");
            dataRow.createCell(3).setCellValue("+31687654321");
            dataRow.createCell(4).setCellValue("NL99TEST9876543210");

            byte[] xlsxBytes = getWorkbookBytes(workbook);

            ExcelInvoiceRow[] rows = service.parse(new ByteArrayInputStream(xlsxBytes), false);
            assertEquals(1, rows.length);
            assertEquals("INV-002", rows[0].getInvoiceNumber());
            assertEquals("Corp B", rows[0].getDebtorName());
            assertEquals("Street 2", rows[0].getAddress());
            assertEquals("+31687654321", rows[0].getPhoneNumber());
            assertEquals("NL99TEST9876543210", rows[0].getBankAccountNumber());
        }
    }

    @Test
    void parseXlsx_withPartialRow_nullsForMissingFields() throws IOException {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");

            org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(0);
            dataRow.createCell(0).setCellValue("INV-003");
            dataRow.createCell(1).setCellValue("Partial Corp");
            // Missing columns 2, 3, 4

            byte[] xlsxBytes = getWorkbookBytes(workbook);

            ExcelInvoiceRow[] rows = service.parse(new ByteArrayInputStream(xlsxBytes), false);
            assertEquals(1, rows.length);
            assertEquals("INV-003", rows[0].getInvoiceNumber());
            assertEquals("Partial Corp", rows[0].getDebtorName());
            assertEquals("", rows[0].getAddress());
            assertEquals("", rows[0].getPhoneNumber());
            assertEquals("", rows[0].getBankAccountNumber());
        }
    }

    @Test
    void parseXlsx_withExtraColumns_ignoresExtra() throws IOException {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");

            org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(0);
            dataRow.createCell(0).setCellValue("INV-004");
            dataRow.createCell(1).setCellValue("Corp D");
            dataRow.createCell(2).setCellValue("Street 4");
            dataRow.createCell(3).setCellValue("+31600000000");
            dataRow.createCell(4).setCellValue("NL00TEST0000000000");
            dataRow.createCell(5).setCellValue("EXTRA");
            dataRow.createCell(6).setCellValue("MORE");

            byte[] xlsxBytes = getWorkbookBytes(workbook);

            ExcelInvoiceRow[] rows = service.parse(new ByteArrayInputStream(xlsxBytes), false);
            assertEquals(1, rows.length);
            assertEquals("INV-004", rows[0].getInvoiceNumber());
            assertNull(rows[0].getParseErrors()); // extra columns should be ignored, no errors
        }
    }

    @Test
    void parseXlsx_withEmptyRows_skipsEmpty() throws IOException {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");

            // Header row
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("invoice number");
            header.createCell(1).setCellValue("debtor name");

            // Empty row
            sheet.createRow(1);

            // Data row
            org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(2);
            dataRow.createCell(0).setCellValue("INV-005");
            dataRow.createCell(1).setCellValue("Corp E");

            byte[] xlsxBytes = getWorkbookBytes(workbook);

            ExcelInvoiceRow[] rows = service.parse(new ByteArrayInputStream(xlsxBytes), false);
            assertEquals(1, rows.length);
            assertEquals("INV-005", rows[0].getInvoiceNumber());
        }
    }

    // --- CSV Parsing Tests ---

    @Test
    void parseCsv_withHeader_allFieldsParsed() throws IOException {
        String csv = "invoice number,debtor name,address,phone number,bank account number\n"
                + "INV-006,Test CSV,Main St 6,+31611111111,NL11TEST1111111111\n";

        ExcelInvoiceRow[] rows = service.parse(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), true);
        assertEquals(1, rows.length);
        assertEquals("INV-006", rows[0].getInvoiceNumber());
        assertEquals("Test CSV", rows[0].getDebtorName());
        assertEquals("Main St 6", rows[0].getAddress());
        assertEquals("+31611111111", rows[0].getPhoneNumber());
        assertEquals("NL11TEST1111111111", rows[0].getBankAccountNumber());
    }

    @Test
    void parseCsv_withoutHeader_usesPositionMapping() throws IOException {
        String csv = "INV-007,Corp G,Street 7,+31622222222,NL22TEST2222222222\n";

        ExcelInvoiceRow[] rows = service.parse(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), true);
        assertEquals(1, rows.length);
        assertEquals("INV-007", rows[0].getInvoiceNumber());
        assertEquals("Corp G", rows[0].getDebtorName());
        assertEquals("Street 7", rows[0].getAddress());
        assertEquals("+31622222222", rows[0].getPhoneNumber());
        assertEquals("NL22TEST2222222222", rows[0].getBankAccountNumber());
    }

    @Test
    void parseCsv_withPartialRow_nullsForMissing() throws IOException {
        String csv = "INV-008,Partial Corp\n";

        ExcelInvoiceRow[] rows = service.parse(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), true);
        assertEquals(1, rows.length);
        assertEquals("INV-008", rows[0].getInvoiceNumber());
        assertEquals("Partial Corp", rows[0].getDebtorName());
        assertEquals("", rows[0].getAddress());
        assertEquals("", rows[0].getPhoneNumber());
        assertEquals("", rows[0].getBankAccountNumber());
    }

    @Test
    void parseCsv_withEmptyLines_skipsEmpty() throws IOException {
        String csv = "INV-009,Corp I\n"
                + "\n"
                + "INV-010,Corp J\n";

        ExcelInvoiceRow[] rows = service.parse(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), true);
        assertEquals(2, rows.length);
        assertEquals("INV-009", rows[0].getInvoiceNumber());
        assertEquals("INV-010", rows[1].getInvoiceNumber());
    }

    @Test
    void parseCsv_emptyFile_returnsEmpty() throws IOException {
        ExcelInvoiceRow[] rows = service.parse(
                new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)), true);
        assertEquals(0, rows.length);
    }

    // --- Return Excel Generation Tests ---

    @Test
    void generateReturnExcel_withFailingRows_createsFile() throws IOException {
        ExcelInvoiceRow[] failingRows = new ExcelInvoiceRow[]{
                createFailingRow(0, "INV-FAIL", "", "Corp Fail", "Street", "+31600000000", "NL00FAIL"),
                createFailingRow(1, "", "Missing Corp", "", "", "", "")
        };

        Path result = service.generateReturnExcel(failingRows, tempDir);
        assertNotNull(result);
        assertTrue(Files.exists(result));
    }

    @Test
    void generateReturnExcel_noFailingRows_returnsNull() throws IOException {
        ExcelInvoiceRow[] failingRows = new ExcelInvoiceRow[0];

        Path result = service.generateReturnExcel(failingRows, tempDir);
        assertNull(result);
    }

    @Test
    void generateReturnExcel_nullFailingRows_returnsNull() throws IOException {
        Path result = service.generateReturnExcel(null, tempDir);
        assertNull(result);
    }

    // --- Header Detection Tests ---

    @Test
    void detectUnrecognizedColumns_xlsx_withValidHeaders_returnsEmpty() throws IOException {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("invoice number");
            header.createCell(1).setCellValue("debtor name");

            byte[] xlsxBytes = getWorkbookBytes(workbook);
            List<String> result = service.detectUnrecognizedColumns(new ByteArrayInputStream(xlsxBytes), false);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void detectUnrecognizedColumns_xlsx_withInvalidHeaders_returnsList() throws IOException {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("invoice number");
            header.createCell(1).setCellValue("client name");

            byte[] xlsxBytes = getWorkbookBytes(workbook);
            List<String> result = service.detectUnrecognizedColumns(new ByteArrayInputStream(xlsxBytes), false);
            assertEquals(1, result.size());
            assertEquals("client name", result.get(0));
        }
    }

    @Test
    void detectUnrecognizedColumns_csv_withValidHeaders_returnsEmpty() throws IOException {
        String csv = "invoice number,debtor name,address\n";
        List<String> result = service.detectUnrecognizedColumns(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), true);
        assertTrue(result.isEmpty());
    }

    @Test
    void detectUnrecognizedColumns_csv_withInvalidHeaders_returnsList() throws IOException {
        String csv = "invoice number,budget amount\n";
        List<String> result = service.detectUnrecognizedColumns(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), true);
        assertEquals(1, result.size());
        assertEquals("budget amount", result.get(0));
    }

    // --- Row Index Tests ---

    @Test
    void parseXlsx_rowIndex_isZeroBased() throws IOException {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");

            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("invoice number");
            header.createCell(1).setCellValue("debtor name");

            for (int i = 0; i < 3; i++) {
                org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(i + 1);
                dataRow.createCell(0).setCellValue("INV-" + i);
                dataRow.createCell(1).setCellValue("Corp " + i);
            }

            byte[] xlsxBytes = getWorkbookBytes(workbook);
            ExcelInvoiceRow[] rows = service.parse(new ByteArrayInputStream(xlsxBytes), false);
            assertEquals(3, rows.length);
            assertEquals(0, rows[0].getRowIndex());
            assertEquals(1, rows[1].getRowIndex());
            assertEquals(2, rows[2].getRowIndex());
        }
    }

    // --- Helpers ---

    private ExcelInvoiceRow createFailingRow(int rowIdx, String invoice, String debtor, String address,
                                              String phone, String bank) {
        ExcelInvoiceRow row = new ExcelInvoiceRow();
        row.setRowIndex(rowIdx);
        row.setInvoiceNumber(invoice);
        row.setDebtorName(debtor);
        row.setAddress(address);
        row.setPhoneNumber(phone);
        row.setBankAccountNumber(bank);
        return row;
    }

    private ExcelInvoiceRow createFailingRow(int rowIdx, String invoice, String debtor, String address,
                                              String phone, String bank, String issue) {
        ExcelInvoiceRow row = createFailingRow(rowIdx, invoice, debtor, address, phone, bank);
        row.setParseErrors(List.of(issue));
        return row;
    }

    private byte[] getWorkbookBytes(org.apache.poi.xssf.usermodel.XSSFWorkbook workbook) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }
}
