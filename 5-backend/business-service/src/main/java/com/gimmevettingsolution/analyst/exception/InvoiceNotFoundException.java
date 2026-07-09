package com.gimmevettingsolution.analyst.exception;

/**
 * Exception thrown when a requested invoice is not found in the database.
 */
public class InvoiceNotFoundException extends RuntimeException {

    private final Long invoiceId;

    public InvoiceNotFoundException(Long invoiceId) {
        super("Invoice with id " + invoiceId + " not found");
        this.invoiceId = invoiceId;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }
}
