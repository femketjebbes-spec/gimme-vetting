package com.gimmevettingsolution.intake.dto;

/**
 * Error response for internal processing failures.
 */
public class InternalErrorResponse {

    private String status;
    private String errorDetail;

    public InternalErrorResponse() {
    }

    public InternalErrorResponse(String status, String errorDetail) {
        this.status = status;
        this.errorDetail = errorDetail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }
}
