package com.gimmevettingsolution.poc;

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
}
