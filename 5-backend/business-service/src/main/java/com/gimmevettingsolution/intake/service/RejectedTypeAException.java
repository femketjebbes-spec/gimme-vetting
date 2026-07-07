package com.gimmevettingsolution.intake.service;

/**
 * Exception thrown when no PoC is found for the given invoice number.
 * Results in 400 REJECTED_TYPE_A response.
 */
public class RejectedTypeAException extends Exception {

    private final String invoiceNumber;

    public RejectedTypeAException(String invoiceNumber) {
        super("No PoC linked to invoice " + invoiceNumber);
        this.invoiceNumber = invoiceNumber;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }
}
