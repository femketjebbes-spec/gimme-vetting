package com.gimmevettingsolution.intake.dto;

import java.util.List;

/**
 * API-level DTO for per-row failure detail in ExcelUploadResponse.
 * Contains 0-based rowIndex and the list of canonical field names that are missing.
 */
public class FailingRow {

    private Integer rowIndex;
    private List<String> missingFields;

    public FailingRow() {
    }

    public FailingRow(Integer rowIndex, List<String> missingFields) {
        this.rowIndex = rowIndex;
        this.missingFields = missingFields;
    }

    public Integer getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(Integer rowIndex) {
        this.rowIndex = rowIndex;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }
}
