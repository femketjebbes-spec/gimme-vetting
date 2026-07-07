package com.gimmevettingsolution.poc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * File-system backed implementation of PoCStoreService.
 * Reads PoC files from a configurable directory path.
 *
 * Per D-003, the PoC store path is injected at runtime via application.yml
 * and must never appear in error messages or logs.
 */
@Service
public class FileBackedPoCStoreService implements PoCStoreService {

    private static final String SAFE_PATTERN = "^[A-Za-z0-9\\-_.]+$";

    private final Path pocStorePath;

    /**
     * Spring Boot constructor — injects the PoC store path from application.yml.
     */
    public FileBackedPoCStoreService(@Value("${gimme.poc-store-path}") String pocStorePathStr) {
        this.pocStorePath = Path.of(pocStorePathStr);
    }

    /**
     * Constructor for testing — bypasses Spring injection.
     */
    FileBackedPoCStoreService(Path pocStorePath) {
        this.pocStorePath = pocStorePath;
    }

    @Override
    public boolean hasMatchingPoC(String invoiceNumber) {
        if (invoiceNumber == null || !invoiceNumber.matches(SAFE_PATTERN)) {
            return false;
        }

        if (!Files.exists(pocStorePath) || !Files.isDirectory(pocStorePath)) {
            return false;
        }

        try {
            String normalizedInvoiceNumber = invoiceNumber.toLowerCase();
            return Files.list(pocStorePath)
                    .filter(Files::isRegularFile)
                    .anyMatch(p -> {
                        String fileName = p.getFileName().toString();
                        String lowerFileName = fileName.toLowerCase();
                        if (lowerFileName.endsWith(".pdf")) {
                            lowerFileName = lowerFileName.substring(0, lowerFileName.length() - 4);
                        }
                        return lowerFileName.equals(normalizedInvoiceNumber);
                    });
        } catch (IOException e) {
            return false;
        }
    }
}
