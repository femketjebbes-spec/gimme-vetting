package com.gimmevettingsolution.poc;

import com.gimmevettingsolution.intake.dto.InvalidFileFormatResponse;
import com.gimmevettingsolution.intake.dto.InternalErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller handling separate PoC (Proof of Correspondence) file upload.
 * <p>
 * NOTE: Authentication is absent for the PoC phase (D-020). This endpoint
 * is unauthenticated and should be protected in a future work item.
 */
@RestController
@RequestMapping("/api/v1")
public class PoCUploadController {

    private final PoCStoreService pocStoreService;

    public PoCUploadController(PoCStoreService pocStoreService) {
        this.pocStoreService = pocStoreService;
    }

    /**
     * Upload a PoC file for a specific invoice.
     * The invoice number is extracted from the filename by stripping the .pdf extension.
     *
     * @param file the uploaded file (must be PDF)
     * @return upload result or error response
     */
    @PostMapping("/poc-upload")
    public ResponseEntity<?> uploadPoc(@RequestParam("file") MultipartFile file) {
        try {
            // Validate MIME type server-side (D-015)
            String mimeType = file.getContentType();
            if (!isPdfMimeType(mimeType)) {
                String actualType = mimeType != null ? mimeType : "unknown";
                InvalidFileFormatResponse response = new InvalidFileFormatResponse(
                        "INVALID_FILE_FORMAT",
                        "Only PDF files are accepted. Uploaded file type: " + actualType
                );
                return ResponseEntity.badRequest().body(response);
            }

            // Store the file (path traversal protection is in FileBackedPoCStoreService.store())
            pocStoreService.store(file);

            // Extract invoice number from filename (strip .pdf extension, case-normalized)
            String originalFilename = file.getOriginalFilename();
            String invoiceNumber = extractInvoiceNumber(originalFilename);

            PoCUploadSuccessResponse response = new PoCUploadSuccessResponse("UPLOADED", invoiceNumber);
            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            // Path traversal detected
            InvalidFileFormatResponse response = new InvalidFileFormatResponse(
                    "INVALID_FILE_FORMAT",
                    "Path traversal detected in filename"
            );
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            // Catch all other exceptions (IOException from store, etc.)
            InternalErrorResponse response = new InternalErrorResponse(
                    "INTERNAL_ERROR",
                    "Unexpected error during PoC upload"
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Checks if the given MIME type indicates a PDF file.
     */
    private boolean isPdfMimeType(String mimeType) {
        return "application/pdf".equals(mimeType);
    }

    /**
     * Extracts the invoice number from the filename by stripping the .pdf extension.
     * Case-insensitive handling: strips .pdf, .Pdf, .PDF, .PDF, etc.
     * If no .pdf extension is present, the full filename is returned.
     */
    private String extractInvoiceNumber(String filename) {
        if (filename == null) {
            return "";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return lower.substring(0, lower.length() - 4);
        }
        return lower;
    }
}
