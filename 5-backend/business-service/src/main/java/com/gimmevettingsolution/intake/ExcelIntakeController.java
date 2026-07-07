package com.gimmevettingsolution.intake;

import com.gimmevettingsolution.intake.dto.*;
import com.gimmevettingsolution.intake.service.ExcelParsingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Controller handling Excel file upload and batch processing.
 * <p>
 * NOTE: Authentication is absent for the PoC phase. This endpoint is
 * unauthenticated and should be protected in a future work item.
 */
@RestController
@RequestMapping("/api/v1")
public class ExcelIntakeController {

    private final ExcelParsingService excelParsingService;
    private final Path uploadDir;

    public ExcelIntakeController(ExcelParsingService excelParsingService) throws IOException {
        this.excelParsingService = excelParsingService;
        this.uploadDir = Files.createTempDirectory("excel-upload-" + UUID.randomUUID());
    }

    /**
     * Upload and process an Excel file for invoice intake.
     *
     * @param file the uploaded file (must be .xlsx or .csv)
     * @return processing result or error response
     */
    @PostMapping("/intake/excel")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            // Validate filename for path traversal
            String originalFilename = file.getOriginalFilename();
            if (!excelParsingService.isSafeFilename(originalFilename)) {
                InvalidFileFormatResponse response = new InvalidFileFormatResponse(
                        "INVALID_FILE_FORMAT",
                        "Path traversal detected in filename"
                );
                return ResponseEntity.badRequest().body(response);
            }

            // Validate MIME type server-side
            String mimeType = file.getContentType();
            if (!excelParsingService.isSupportedMimeType(mimeType)) {
                InvalidFileFormatResponse response = new InvalidFileFormatResponse(
                        "INVALID_FILE_FORMAT",
                        "Unsupported MIME type: " + mimeType + ". Expected application/vnd.openxmlformats-officedocument.spreadsheetml.sheet or text/csv."
                );
                return ResponseEntity.badRequest().body(response);
            }

            // Determine file type
            boolean isCsv = mimeType != null && mimeType.startsWith("text/csv");

            // Use bytes for both operations since streams are consumed
            byte[] fileBytes = file.getBytes();

            // Detect unrecognized column names before full parsing
            List<String> unrecognizedColumns = excelParsingService.detectUnrecognizedColumns(new ByteArrayInputStream(fileBytes), isCsv);
            if (!unrecognizedColumns.isEmpty()) {
                ColumnNameMismatchResponse response = new ColumnNameMismatchResponse(
                        "COLUMN_NAME_MISMATCH",
                        unrecognizedColumns
                );
                return ResponseEntity.badRequest().body(response);
            }

            // Parse the file
            ExcelInvoiceRow[] parsedRows = excelParsingService.parse(new ByteArrayInputStream(fileBytes), isCsv);

            // Determine passing and failing rows
            // For WI-002, all rows pass (business rule checks are out of scope)
            // rowsFailed = rows with missing required fields
            int totalRows = parsedRows.length;
            int rowsFailed = 0;
            int rowsPassed = 0;

            for (ExcelInvoiceRow row : parsedRows) {
                boolean hasAllFields = true;
                if (row.getInvoiceNumber() == null || row.getInvoiceNumber().isEmpty()) {
                    hasAllFields = false;
                }
                if (row.getDebtorName() == null || row.getDebtorName().isEmpty()) {
                    hasAllFields = false;
                }
                if (row.getAddress() == null || row.getAddress().isEmpty()) {
                    hasAllFields = false;
                }
                if (row.getPhoneNumber() == null || row.getPhoneNumber().isEmpty()) {
                    hasAllFields = false;
                }
                if (row.getBankAccountNumber() == null || row.getBankAccountNumber().isEmpty()) {
                    hasAllFields = false;
                }
                if (hasAllFields) {
                    rowsPassed++;
                } else {
                    rowsFailed++;
                }
            }

            // Generate return Excel for failing rows
            ExcelInvoiceRow[] failingRows = new ExcelInvoiceRow[rowsFailed];
            int failIdx = 0;
            for (ExcelInvoiceRow row : parsedRows) {
                boolean hasAllFields = true;
                if (row.getInvoiceNumber() == null || row.getInvoiceNumber().isEmpty()) hasAllFields = false;
                if (row.getDebtorName() == null || row.getDebtorName().isEmpty()) hasAllFields = false;
                if (row.getAddress() == null || row.getAddress().isEmpty()) hasAllFields = false;
                if (row.getPhoneNumber() == null || row.getPhoneNumber().isEmpty()) hasAllFields = false;
                if (row.getBankAccountNumber() == null || row.getBankAccountNumber().isEmpty()) hasAllFields = false;
                if (!hasAllFields) {
                    failingRows[failIdx++] = row;
                }
            }

            String downloadLink = null;
            if (rowsFailed > 0) {
                String returnFilename = "return-" + UUID.randomUUID() + ".xlsx";
                Path returnPath = uploadDir.resolve(returnFilename);
                excelParsingService.generateReturnExcel(failingRows, uploadDir);
                // Return a relative path for download
                downloadLink = "/api/v1/intake/excel/download/" + returnFilename;
            }

            ExcelUploadResponse response = new ExcelUploadResponse();
            response.setProcessingStatus("COMPLETED");
            response.setTotalRowsProcessed(totalRows);
            response.setRowsPassed(rowsPassed);
            response.setRowsFailed(rowsFailed);
            response.setReturnExcelDownloadLink(downloadLink != null ? downloadLink : "");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            InvalidFileFormatResponse response = new InvalidFileFormatResponse(
                    "INVALID_FILE_FORMAT",
                    e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            InvalidFileFormatResponse response = new InvalidFileFormatResponse(
                    "INVALID_FILE_FORMAT",
                    "Failed to process uploaded file: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            InternalErrorResponse response = new InternalErrorResponse(
                    "INTERNAL_ERROR",
                    "Unexpected error during Excel processing"
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Download the return Excel file.
     *
     * @param filename the return Excel filename
     * @return the file bytes
     */
    @GetMapping("/intake/excel/download/{filename}")
    public ResponseEntity<byte[]> downloadReturnExcel(@PathVariable String filename) {
        try {
            // Sanitize filename
            String safeName = excelParsingService.sanitizeFilename(filename);
            Path filePath = uploadDir.resolve(safeName);

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] content = Files.readAllBytes(filePath);

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + safeName + "\"")
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
