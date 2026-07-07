package com.gimmevettingsolution.intake.dto;

/**
 * Response DTO for 400 Bad Request when Type A rejection (no PoC found).
 */
public class RejectedTypeAResponse {

    private String status;
    private String rejectionReason;
    private boolean resubmitAllowed;

    public RejectedTypeAResponse() {
    }

    public RejectedTypeAResponse(String status, String rejectionReason, boolean resubmitAllowed) {
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
