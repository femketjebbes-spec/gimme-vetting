package com.gimmevettingsolution.intake.service;

import com.gimmevettingsolution.intake.dto.ExcelInvoiceRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for parsing Excel files (XLSX and CSV) and mapping columns to domain fields.
 * <p>
 * NOTE: Authentication is absent for the PoC phase. This endpoint is unauthenticated
 * and should be protected in a future work item.
 * <p>
 * Supported column names (case-insensitive):
 * "invoice number", "debtor name", "address", "phone number", "bank account number"
 */
@Service
public class ExcelParsingService {

    public static final Set<String> ALLOWED_COLUMN_NAMES = Set.of(
            "invoice number",
            "debtor name",
            "address",
            "phone number",
            "bank account number"
    );

    /**
     * Ordered column names for template generation.
     * Mirrors ALLOWED_COLUMN_NAMES values but preserves insertion order.
     */
    private static final List<String> TEMPLATE_COLUMN_HEADERS = List.of(
            "invoice number",
            "debtor name",
            "address",
            "phone number",
            "bank account number"
    );

    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[A-Za-z0-9\\-_. ]+$");
    private static final String[] SUPPORTED_MIME_TYPES = {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv"
    };

    /**
     * Validate MIME type server-side.
     *
     * @param mimeType the MIME type to validate
     * @return true if the MIME type is supported
     */
    public boolean isSupportedMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return false;
        }
        String cleanType = mimeType.split(";")[0].trim().toLowerCase();
        for (String supported : SUPPORTED_MIME_TYPES) {
            if (supported.equals(cleanType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Magic byte constant for ZIP local file header (first 4 bytes of .xlsx files).
     */
    private static final byte[] ZIP_HEADER = new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04};

    /**
     * Detect file type by inspecting content (magic bytes).
     * <p>
     * Used as a fallback when MIME type is null, empty, or unrecognized.
     *
     * @param inputStream the file input stream (will be reset to position 0 after reading)
     * @return FileType.XLSX if ZIP signature detected, FileType.CSV if valid text, FileType.UNKNOWN otherwise
     */
    public FileType detectFileType(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return FileType.UNKNOWN;
        }

        byte[] header = new byte[4];
        int bytesRead = inputStream.read(header);

        if (bytesRead < 4) {
            // Empty or too small to determine type
            return FileType.UNKNOWN;
        }

        // Check for ZIP signature (XLSX files start with PK\x03\x04)
        if (matchesZipHeader(header)) {
            return FileType.XLSX;
        }

        // Check if content is valid text (CSV)
        if (isTextContent(header)) {
            return FileType.CSV;
        }

        return FileType.UNKNOWN;
    }

    /**
     * Check if the first 4 bytes match the ZIP local file header signature.
     */
    private boolean matchesZipHeader(byte[] header) {
        for (int i = 0; i < ZIP_HEADER.length; i++) {
            if (header[i] != ZIP_HEADER[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the content appears to be valid text (CSV detection).
     * Returns true if all non-null bytes are printable ASCII or valid UTF-8 start bytes.
     */
    private boolean isTextContent(byte[] header) {
        for (byte b : header) {
            // Skip common whitespace (newline, carriage return, tab)
            if (b == '\n' || b == '\r' || b == '\t') {
                continue;
            }
            // Printable ASCII range (0x20 to 0x7E)
            if (b >= 0x20 && b <= 0x7E) {
                continue;
            }
            // UTF-8 multi-byte continuation bytes (0x80 to 0xBF)
            if (b >= 0x80 && b <= 0xBF) {
                continue;
            }
            // UTF-8 leading bytes for multi-byte sequences (0xC0 to 0xFD)
            if (b >= 0xC0 && b <= 0xFD) {
                continue;
            }
            // Not a recognized text byte
            return false;
        }
        return true;
    }

    /**
     * Validate filename against path traversal patterns.
     *
     * @param filename the original filename
     * @return true if the filename is safe
     */
    public boolean isSafeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        // Check raw filename for path traversal patterns first
        if (filename.contains("..") || filename.contains("/")) {
            return false;
        }
        String cleanName = Paths.get(filename).getFileName().toString();
        return SAFE_FILENAME_PATTERN.matcher(cleanName).matches();
    }

    /**
     * Sanitize filename to prevent path traversal.
     *
     * @param filename the original filename
     * @return the sanitized filename
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "upload.xlsx";
        }
        String cleanName = Paths.get(filename).getFileName().toString();
        if (!SAFE_FILENAME_PATTERN.matcher(cleanName).matches()) {
            cleanName = cleanName.replaceAll("[^A-Za-z0-9\\-_.]", "_");
        }
        return cleanName;
    }

    /**
     * Parse an Excel file and return parsed invoice rows.
     * Detects header row and validates column names.
     *
     * @param inputStream the file input stream
     * @param isCsv       true if the file is CSV, false if XLSX
     * @return array of parsed ExcelInvoiceRow objects
     * @throws IOException if the file cannot be read
     */
    public ExcelInvoiceRow[] parse(InputStream inputStream, boolean isCsv) throws IOException {
        List<ExcelInvoiceRow> rows = new ArrayList<>();

        if (isCsv) {
            rows = parseCsv(inputStream);
        } else {
            rows = parseXlsx(inputStream);
        }

        return rows.toArray(new ExcelInvoiceRow[0]);
    }

    /**
     * Detect unrecognized column names from file headers.
     *
     * @param inputStream the file input stream
     * @param isCsv       true if the file is CSV
     * @return list of unrecognized column names
     * @throws IOException if the file cannot be read
     */
    public List<String> detectUnrecognizedColumns(InputStream inputStream, boolean isCsv) throws IOException {
        if (isCsv) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String[] headers = readCsvLine(reader);
            reader.close();
            if (headers == null) {
                return Collections.emptyList();
            }
            return validateColumnNames(headers);
        } else {
            try (Workbook workbook = WorkbookFactory.create(inputStream)) {
                Sheet sheet = workbook.getSheetAt(0);
                if (!sheet.iterator().hasNext()) {
                    return Collections.emptyList();
                }
                Row headerRow = sheet.getRow(0);
                String[] headers = readHeaderRow(headerRow);
                return validateColumnNames(headers);
            }
        }
    }

    /**
     * Parse a CSV file.
     */
    private List<ExcelInvoiceRow> parseCsv(InputStream inputStream) throws IOException {
        List<ExcelInvoiceRow> rows = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String[] headers = readCsvLine(reader);
        if (headers == null) {
            return rows;
        }

        boolean hasHeader = detectHeader(headers);
        int startRow = hasHeader ? 0 : 0;
        int rowIndex = 0;
        String line;

        // If no header was detected, the first line (headers[]) is actually data
        if (!hasHeader) {
            String[] cells = headers;
            ExcelInvoiceRow row = mapRow(cells, rowIndex, false, null);
            if (row != null) {
                rows.add(row);
            }
            rowIndex++;
        }

        while ((line = reader.readLine()) != null) {
            if (isBlankLine(line)) {
                continue;
            }
            String[] cells = splitCsvLine(line);
            ExcelInvoiceRow row = mapRow(cells, rowIndex, hasHeader, headers);
            if (row != null) {
                rows.add(row);
            }
            rowIndex++;
        }
        reader.close();
        return rows;
    }

    /**
     * Parse an XLSX file.
     */
    private List<ExcelInvoiceRow> parseXlsx(InputStream inputStream) throws IOException {
        List<ExcelInvoiceRow> rows = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (!sheet.iterator().hasNext()) {
                return rows;
            }

            Row headerRow = sheet.getRow(0);
            String[] headers = readHeaderRow(headerRow);
            boolean hasHeader = detectHeader(headers);

            int startRow = hasHeader ? 1 : 0;
            int rowIndex = 0;
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row dataRow = sheet.getRow(i);
                if (isBlankRow(dataRow)) {
                    continue;
                }
                String[] cells = readDataRow(dataRow);
                ExcelInvoiceRow row = mapRow(cells, rowIndex, hasHeader, headers);
                if (row != null) {
                    rows.add(row);
                }
                rowIndex++;
            }
        }

        return rows;
    }

    /**
     * Read header values from a workbook row.
     */
    private String[] readHeaderRow(Row row) {
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Cell cell = row != null ? row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL) : null;
            if (cell != null) {
                headers.add(getCellStringValue(cell));
            } else {
                headers.add("");
            }
        }
        return headers.toArray(new String[0]);
    }

    /**
     * Read data cells from a row.
     */
    private String[] readDataRow(Row row) {
        List<String> values = new ArrayList<>();
        int lastCell = Math.max(row.getLastCellNum(), 5);
        for (int i = 0; i < lastCell; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null) {
                values.add(getCellStringValue(cell));
            } else {
                values.add("");
            }
        }
        return values.toArray(new String[0]);
    }

    /**
     * Get string value from a cell.
     */
    private String getCellStringValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return NumberToTextConverter.toText(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return NumberToTextConverter.toText(cell.getNumericCellValue());
                }
            case BLANK:
            default:
                return "";
        }
    }

    /**
     * Check if a row is blank.
     */
    private boolean isBlankRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i <= row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && getCellStringValue(cell).trim().length() > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Detect if the first row is a header row.
     */
    private boolean detectHeader(String[] headers) {
        Set<String> normalizedHeaders = new HashSet<>();
        for (String h : headers) {
            String trimmed = h.trim().toLowerCase();
            if (!trimmed.isEmpty()) {
                normalizedHeaders.add(trimmed);
            }
        }

        for (String allowed : ALLOWED_COLUMN_NAMES) {
            if (normalizedHeaders.contains(allowed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate column names against the allowlist.
     *
     * @param headers the header names from the file
     * @return list of unrecognized column names, empty if all are valid
     */
    public List<String> validateColumnNames(String[] headers) {
        List<String> unrecognized = new ArrayList<>();
        for (String header : headers) {
            String trimmed = header.trim().toLowerCase();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!ALLOWED_COLUMN_NAMES.contains(trimmed)) {
                unrecognized.add(header.trim());
            }
        }
        return unrecognized;
    }

    /**
     * Map a row of cell values to an ExcelInvoiceRow.
     */
    private ExcelInvoiceRow mapRow(String[] cells, int rowIndex, boolean hasHeader, String[] headers) {
        ExcelInvoiceRow row = new ExcelInvoiceRow();
        row.setRowIndex(rowIndex);

        List<String> errors = new ArrayList<>();

        if (hasHeader) {
            Map<String, Integer> columnMap = buildHeaderColumnMap(headers);
            String invoiceNumber = getCellValueByColumn(cells, columnMap, "invoice number", errors);
            String debtorName = getCellValueByColumn(cells, columnMap, "debtor name", errors);
            String address = getCellValueByColumn(cells, columnMap, "address", errors);
            String phoneNumber = getCellValueByColumn(cells, columnMap, "phone number", errors);
            String bankAccountNumber = getCellValueByColumn(cells, columnMap, "bank account number", errors);

            row.setInvoiceNumber(invoiceNumber);
            row.setDebtorName(debtorName);
            row.setAddress(address);
            row.setPhoneNumber(phoneNumber);
            row.setBankAccountNumber(bankAccountNumber);
        } else {
            row.setInvoiceNumber(getCellAtPosition(cells, 0));
            row.setDebtorName(getCellAtPosition(cells, 1));
            row.setAddress(getCellAtPosition(cells, 2));
            row.setPhoneNumber(getCellAtPosition(cells, 3));
            row.setBankAccountNumber(getCellAtPosition(cells, 4));
        }

        row.setParseErrors(errors.isEmpty() ? null : errors);
        return row;
    }

    /**
     * Build a mapping from normalized header name to column index.
     */
    private Map<String, Integer> buildHeaderColumnMap(String[] headers) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String trimmed = headers[i].trim().toLowerCase();
            if (!trimmed.isEmpty() && ALLOWED_COLUMN_NAMES.contains(trimmed)) {
                map.put(trimmed, i);
            }
        }
        return map;
    }

    /**
     * Get cell value by column name from headers mapping.
     */
    private String getCellValueByColumn(String[] cells, Map<String, Integer> columnMap,
                                         String columnName, List<String> errors) {
        Integer colIndex = columnMap.get(columnName);
        if (colIndex == null) {
            errors.add("MISSING_FIELD:" + columnName);
            return "";
        }
        if (colIndex >= cells.length || cells[colIndex].trim().isEmpty()) {
            errors.add("MISSING_FIELD:" + columnName);
            return "";
        }
        return cells[colIndex].trim();
    }

    /**
     * Get cell value at a specific position (for position-based mapping).
     */
    private String getCellAtPosition(String[] cells, int position) {
        if (position >= cells.length) {
            return "";
        }
        return cells[position].trim();
    }

    /**
     * Parse CSV line handling quoted fields.
     */
    private String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString().trim());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString().trim());
        return fields.toArray(new String[0]);
    }

    /**
     * Read a CSV line from BufferedReader (no quoting support, simple split).
     */
    private String[] readCsvLine(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null || line.isEmpty()) {
            return null;
        }
        return splitCsvLine(line);
    }

    /**
     * Check if a line is blank.
     */
    private boolean isBlankLine(String line) {
        return line.trim().isEmpty();
    }

    /**
     * Generate an Excel template file for invoice intake.
     * <p>
     * The template contains exactly five column headers matching
     * {@link #ALLOWED_COLUMN_NAMES} in row 0, and one empty data row
     * at index 1 as a visual guide. No example data, no validation
     * rules, no formatting beyond standard headers.
     *
     * @return the template as a byte array in XLSX format
     * @throws IOException if the workbook cannot be written
     */
    public byte[] generateTemplateXlsx() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Template");

            // Create header row in fixed order using TEMPLATE_COLUMN_HEADERS
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_COLUMN_HEADERS.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(TEMPLATE_COLUMN_HEADERS.get(i));
            }

            // Create one empty data row as visual guide
            sheet.createRow(1);

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Generate a return Excel file containing only the rows that failed validation.
     *
     * @param failingRows the rows that failed validation
     * @param outputDir   the directory to write the file to
     * @return path to the generated file, or null if no failing rows
     */
    /**
     * Generate a return Excel file containing only rows that failed validation.
     * Backward-compatible 2-param method delegates to 4-param overload with defaults.
     *
     * @param failingRows the rows that failed validation
     * @param outputDir   the directory to write the file to
     * @return path to the generated file, or null if no failing rows
     */
    public Path generateReturnExcel(ExcelInvoiceRow[] failingRows, Path outputDir) throws IOException {
        String defaultFilename = "return-excel.xlsx";
        return generateReturnExcel(failingRows, outputDir, defaultFilename, false);
    }

    /**
     * Generate a return Excel file containing only rows that failed validation.
     * Supports both XLSX and CSV output formats.
     *
     * @param failingRows the rows that failed validation
     * @param outputDir   the directory to write the file to
     * @param filename    the output filename (must include extension)
     * @param isCsv       true to generate CSV, false for XLSX
     * @return path to the generated file, or null if no failing rows
     */
    public Path generateReturnExcel(ExcelInvoiceRow[] failingRows, Path outputDir, String filename, boolean isCsv) throws IOException {
        if (failingRows == null || failingRows.length == 0) {
            return null;
        }

        Path outputFile = outputDir.resolve(filename);
        String[] headers = {"invoice number", "debtor name", "address", "phone number", "bank account number", "Issue"};

        if (isCsv) {
            return generateReturnCsv(failingRows, outputFile, headers);
        } else {
            return generateReturnXlsx(failingRows, outputFile, headers);
        }
    }

    /**
     * Generate return Excel in XLSX format using Apache POI.
     */
    private Path generateReturnXlsx(ExcelInvoiceRow[] failingRows, Path outputFile, String[] headers) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Failed Rows");

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Create data rows
            int rowNum = 1;
            for (ExcelInvoiceRow row : failingRows) {
                Row dataRow = sheet.createRow(rowNum);

                dataRow.createCell(0).setCellValue(row.getInvoiceNumber() != null ? row.getInvoiceNumber() : "");
                dataRow.createCell(1).setCellValue(row.getDebtorName() != null ? row.getDebtorName() : "");
                dataRow.createCell(2).setCellValue(row.getAddress() != null ? row.getAddress() : "");
                dataRow.createCell(3).setCellValue(row.getPhoneNumber() != null ? row.getPhoneNumber() : "");
                dataRow.createCell(4).setCellValue(row.getBankAccountNumber() != null ? row.getBankAccountNumber() : "");
                dataRow.createCell(5).setCellValue(buildIssue(row));

                rowNum++;
            }

            try (FileOutputStream outputStream = new FileOutputStream(outputFile.toFile())) {
                workbook.write(outputStream);
            }
        }

        return outputFile;
    }

    /**
     * Generate return Excel in CSV format with RFC 4180 escaping.
     */
    private Path generateReturnCsv(ExcelInvoiceRow[] failingRows, Path outputFile, String[] headers) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.toFile()))) {
            writer.write(String.join(",", headers));
            writer.newLine();
            for (ExcelInvoiceRow row : failingRows) {
                writer.write(formatCsvRow(row));
                writer.newLine();
            }
        }

        return outputFile;
    }

    /**
     * Format a single row as a CSV string with RFC 4180 escaping.
     */
    private String formatCsvRow(ExcelInvoiceRow row) {
        String[] values = {
                csvEscape(row.getInvoiceNumber()),
                csvEscape(row.getDebtorName()),
                csvEscape(row.getAddress()),
                csvEscape(row.getPhoneNumber()),
                csvEscape(row.getBankAccountNumber()),
                csvEscape(buildIssue(row))
        };
        return String.join(",", values);
    }

    /**
     * RFC 4180 CSV escaping.
     * Values containing commas, double quotes, or newlines are enclosed in double quotes.
     * Double quotes within the value are escaped by doubling them.
     */
    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Build the issue description for a failing row.
     *
     * @param row the failing row
     * @return issue description string
     */
    private String buildIssue(ExcelInvoiceRow row) {
        List<String> issues = new ArrayList<>();

        boolean hasMissingFields = false;
        List<String> missingFields = new ArrayList<>();
        if ((row.getInvoiceNumber() == null || row.getInvoiceNumber().isEmpty())) {
            missingFields.add("invoiceNumber");
            hasMissingFields = true;
        }
        if ((row.getDebtorName() == null || row.getDebtorName().isEmpty())) {
            missingFields.add("debtorName");
            hasMissingFields = true;
        }
        if ((row.getAddress() == null || row.getAddress().isEmpty())) {
            missingFields.add("address");
            hasMissingFields = true;
        }
        if ((row.getPhoneNumber() == null || row.getPhoneNumber().isEmpty())) {
            missingFields.add("phoneNumber");
            hasMissingFields = true;
        }
        if ((row.getBankAccountNumber() == null || row.getBankAccountNumber().isEmpty())) {
            missingFields.add("bankAccountNumber");
            hasMissingFields = true;
        }

        if (hasMissingFields) {
            issues.add("MISSING_FIELDS: " + String.join(", ", missingFields));
        }

        if (issues.isEmpty()) {
            issues.add("MISSING_POC");
        }

        return String.join("; ", issues);
    }

    /**
     * Build the issue description with an explicit issue string.
     * Used for PoC failure integration (e.g., "MISSING_POC").
     *
     * @param row           the failing row (unused but kept for API consistency)
     * @param explicitIssue the explicit issue string
     * @return the explicit issue string
     */
    String buildIssue(ExcelInvoiceRow row, String explicitIssue) {
        return explicitIssue;
    }
}
