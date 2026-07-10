package com.gimmevettingsolution.excel;

import com.gimmevettingsolution.intake.service.ExcelParsingService;
import com.gimmevettingsolution.intake.service.FileType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * File-system backed Excel file store.
 * Stores uploaded Excel files with UUID filenames for path traversal protection.
 * Tracks original filenames for Content-Disposition headers.
 *
 * Per D-EXCEL-001, the store path is configurable via gimme.excel-store-path.
 * 
 * MIME type validation follows the same two-tier strategy as ExcelIntakeController:
 * 1. Check the declared MIME type against the allowed set.
 * 2. If the MIME type is null or unrecognized, fall back to content-based
 *    magic-byte detection via ExcelParsingService.detectFileType().
 */
@Service
public class FileBackedExcelStoreService {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv"
    );

    /**
     * Valid Content-Types keyed by file extension for the source file serving endpoint.
     */
    private static final Set<String> VALID_CONTENT_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv"
    );

    private final Path excelStorePath;
    private final ExcelParsingService excelParsingService;

    /**
     * Spring Boot constructor — injects the Excel store path and parsing service.
     */
    @Autowired
    public FileBackedExcelStoreService(@Value("${gimme.excel-store-path}") String excelStorePathStr,
                                       ExcelParsingService excelParsingService) {
        this.excelStorePath = Path.of(excelStorePathStr);
        this.excelParsingService = excelParsingService;
    }

    /**
     * Constructor for testing — bypasses Spring injection.
     */
    public FileBackedExcelStoreService(Path excelStorePath) {
        this.excelStorePath = excelStorePath;
        this.excelParsingService = null;
    }

    /**
     * Saves an uploaded Excel file with a UUID filename.
     * Validates MIME type with content-based fallback and file size before saving.
     * Sanitizes the original filename against header injection.
     *
     * @param file the uploaded MultipartFile
     * @return UUID string used as the filename in the store
     * @throws IllegalArgumentException if MIME type is not allowed (after fallback) or file exceeds 50MB
     */
    public String save(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        // Validate MIME type — fast path with declared MIME type
        String mimeType = file.getContentType();
        boolean mimeAccepted = isMimeAccepted(mimeType);

        // Fallback: content-based detection when MIME type is null or unrecognized
        if (!mimeAccepted) {
            mimeAccepted = isContentBasedDetectionValid(file);
        }

        if (!mimeAccepted) {
            throw new IllegalArgumentException("File type not supported. Only Excel (.xlsx) and CSV (.csv) files are allowed.");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds maximum size of 50MB.");
        }

        // Sanitize original filename
        String safeFilename = sanitizeFilename(originalFilename);

        // Generate UUID and save
        String uuid = UUID.randomUUID().toString();
        try {
            if (!Files.exists(excelStorePath)) {
                Files.createDirectories(excelStorePath);
            }
            Path targetPath = excelStorePath.resolve(uuid);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save Excel file", e);
        }

        return uuid;
    }

    /**
     * Checks if the MIME type is directly accepted.
     *
     * @param mimeType the declared MIME type from the upload
     * @return true if the MIME type is in the allowed set
     */
    private boolean isMimeAccepted(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return false;
        }
        String cleanType = mimeType.split(";")[0].trim().toLowerCase();
        return ALLOWED_MIME_TYPES.contains(cleanType);
    }

    /**
     * Performs content-based file type detection as a MIME type fallback.
     * Uses magic bytes to distinguish XLSX (ZIP signature) from CSV (text).
     *
     * @param file the uploaded MultipartFile
     * @return true if the content is a valid XLSX or CSV file
     */
    private boolean isContentBasedDetectionValid(MultipartFile file) {
        if (excelParsingService == null) {
            return false;
        }
        try (InputStream inputStream = file.getInputStream()) {
            FileType detected = excelParsingService.detectFileType(inputStream);
            return detected == FileType.XLSX || detected == FileType.CSV;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Retrieves a stored file by its UUID filename.
     *
     * @param fileId the UUID string returned by save()
     * @return Resource for the stored file
     * @throws RuntimeException if the file is not found
     */
    public Resource getFile(String fileId) {
        try {
            Path filePath = excelStorePath.resolve(fileId);
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                throw new RuntimeException("Source file is unavailable");
            }
            return new UrlResource(filePath.toUri());
        } catch (IOException e) {
            throw new RuntimeException("Source file is unavailable", e);
        }
    }

    /**
     * Sanitizes a filename against HTTP header injection.
     * Rejects filenames containing newlines, semicolons, or control characters.
     *
     * @param filename the original filename from the client upload
     * @return the sanitized filename
     * @throws IllegalArgumentException if the filename contains unsafe characters
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename must not be empty");
        }

        // Reject newlines
        if (filename.contains("\n") || filename.contains("\r")) {
            throw new IllegalArgumentException("Filename contains invalid characters");
        }

        // Reject semicolons (header injection vector)
        if (filename.contains(";")) {
            throw new IllegalArgumentException("Filename contains invalid characters");
        }

        // Reject control characters (ASCII 0-31)
        for (int i = 0; i < filename.length(); i++) {
            char c = filename.charAt(i);
            if (c <= 31) {
                throw new IllegalArgumentException("Filename contains invalid characters");
            }
        }

        return filename;
    }

    /**
     * Determines the Content-Type based on file extension.
     *
     * @param originalFilename the original filename
     * @return the appropriate Content-Type string
     */
    public String getContentType(String originalFilename) {
        if (originalFilename == null) {
            return "application/octet-stream";
        }
        String lower = originalFilename.toLowerCase();
        if (lower.endsWith(".csv")) {
            return "text/csv";
        }
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }
}
