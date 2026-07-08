package com.gimmevettingsolution.poc;

import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for PoC (Proof of Correspondence) existence verification.
 * Checks whether a PoC file matching a given invoice number exists in the PoC store.
 */
public interface PoCStoreService {

    /**
     * Checks whether at least one PoC file matches the given invoice number
     * using case-insensitive full-string filename matching.
     *
     * @param invoiceNumber the invoice number to match
     * @return true if at least one matching PoC filename exists, false otherwise
     */
    boolean hasMatchingPoC(String invoiceNumber);

    /**
     * Stores an uploaded PoC file in the PoC store.
     * The filename is validated for path traversal. Duplicate filenames overwrite existing files.
     *
     * @param file the uploaded PoC file
     * @throws SecurityException if the filename contains path traversal characters
     */
    void store(MultipartFile file);
}
