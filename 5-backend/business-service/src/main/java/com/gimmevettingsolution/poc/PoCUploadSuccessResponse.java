package com.gimmevettingsolution.poc;

/**
 * Response DTO for successful PoC upload.
 */
public class PoCUploadSuccessResponse {

    private String status;
    private String invoiceNumber;

    public PoCUploadSuccessResponse() {
    }

    public PoCUploadSuccessResponse(String status, String invoiceNumber) {
        this.status = status;
        this.invoiceNumber = invoiceNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }
}
