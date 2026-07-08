package com.gimmevettingsolution.intake;

import com.gimmevettingsolution.intake.dto.*;
import com.gimmevettingsolution.intake.service.ExcelParsingService;
import com.gimmevettingsolution.intake.service.MandatoryFieldValidationService;
import com.gimmevettingsolution.intake.service.ValidationResult;
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
    private final MandatoryFieldValidationService mandatoryFieldValidationService;
    private final Path uploadDir;

    public ExcelIntakeController(ExcelParsingService excelParsingService,
                                 MandatoryFieldValidationService mandatoryFieldValidationService) throws IOException {
        this.excelParsingService = excelParsingService;
        this.mandatoryFieldValidationService = mandatoryFieldValidationService;
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

            // Validate mandatory fields using dedicated service
            List<ExcelInvoiceRow> rowList = java.util.Arrays.asList(parsedRows);
            ValidationResult validationResult = mandatoryFieldValidationService.validate(rowList);

            // Generate return Excel for failing rows
            String downloadLink = null;
            if (validationResult.getRowsFailed() > 0) {
                // Build failing rows array for return Excel generation
                ExcelInvoiceRow[] failingRowsForExcel = new ExcelInvoiceRow[validationResult.getRowsFailed()];
                int failIdx = 0;
                for (ExcelInvoiceRow originalRow : rowList) {
                    boolean isFailing = validationResult.getFailingRows().stream()
                            .anyMatch(rf -> rf.getRowIndex() == (originalRow != null ? originalRow.getRowIndex() : -1));
                    if (isFailing) {
                        failingRowsForExcel[failIdx++] = originalRow;
                    }
                }
                String returnFilename = "return-" + UUID.randomUUID() + (isCsv ? ".csv" : ".xlsx");
                Path returnPath = excelParsingService.generateReturnExcel(failingRowsForExcel, uploadDir, returnFilename, isCsv);
                if (returnPath != null) {
                    downloadLink = "/api/v1/intake/excel/download/" + returnFilename;
                }
            }

            ExcelUploadResponse response = new ExcelUploadResponse();
            response.setProcessingStatus("COMPLETED");
            response.setTotalRowsProcessed(validationResult.getTotalRowsProcessed());
            response.setRowsPassed(validationResult.getRowsPassed());
            response.setRowsFailed(validationResult.getRowsFailed());
            response.setReturnExcelDownloadLink(downloadLink != null ? downloadLink : "");
            response.setFailingRows(mandatoryFieldValidationService.toFailingRows(validationResult.getFailingRows()));

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

    /**
     * Download the Excel template file for invoice intake.
     *
     * @return the template XLSX file as binary response
     */
    @GetMapping("/intake/excel/template")
    public ResponseEntity<byte[]> getTemplate() {
        try {
            byte[] templateBytes = excelParsingService.generateTemplateXlsx();

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.valueOf(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header("Content-Disposition", "attachment; filename=\"invoice-intake-template.xlsx\"")
                    .body(templateBytes);
        } catch (IOException e) {
            InternalErrorResponse response = new InternalErrorResponse(
                    "INTERNAL_ERROR",
                    "Template generation failed"
            );
            return ResponseEntity.status(500).body(null);
        }
    }
}
