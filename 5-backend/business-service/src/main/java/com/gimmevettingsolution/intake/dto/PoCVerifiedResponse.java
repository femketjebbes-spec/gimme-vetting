package com.gimmevettingsolution.intake.dto;

/**
 * Response DTO for 202 Accepted when PoC is verified.
 */
public class PoCVerifiedResponse {

    private String status;
    private String nextStep;
    private String invoiceId;

    public PoCVerifiedResponse() {
    }

    public PoCVerifiedResponse(String status, String nextStep, String invoiceId) {
        this.status = status;
        this.nextStep = nextStep;
        this.invoiceId = invoiceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNextStep() {
        return nextStep;
    }

    public void setNextStep(String nextStep) {
        this.nextStep = nextStep;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }
}
