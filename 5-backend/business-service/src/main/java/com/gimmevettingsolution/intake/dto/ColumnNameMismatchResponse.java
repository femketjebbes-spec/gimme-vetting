package com.gimmevettingsolution.intake.dto;

import java.util.List;

/**
 * Error response for column name mismatch.
 */
public class ColumnNameMismatchResponse {

    private String status;
    private List<String> unrecognizedColumns;

    public ColumnNameMismatchResponse() {
    }

    public ColumnNameMismatchResponse(String status, List<String> unrecognizedColumns) {
        this.status = status;
        this.unrecognizedColumns = unrecognizedColumns;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getUnrecognizedColumns() {
        return unrecognizedColumns;
    }

    public void setUnrecognizedColumns(List<String> unrecognizedColumns) {
        this.unrecognizedColumns = unrecognizedColumns;
    }
}
