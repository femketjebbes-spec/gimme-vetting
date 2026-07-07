package com.gimmevettingsolution.intake.dto;

/**
 * Response DTO for 400 Bad Request when validation fails.
 */
public class ValidationErrorResponse {

    private String status;
    private String rejectionReason;
    private boolean resubmitAllowed;

    public ValidationErrorResponse() {
    }

    public ValidationErrorResponse(String status, String rejectionReason, boolean resubmitAllowed) {
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.resubmitAllowed = resubmitAllowed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public boolean isResubmitAllowed() {
        return resubmitAllowed;
    }

    public void setResubmitAllowed(boolean resubmitAllowed) {
        this.resubmitAllowed = resubmitAllowed;
    }
}
