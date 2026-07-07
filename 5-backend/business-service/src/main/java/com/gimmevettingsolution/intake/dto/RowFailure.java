package com.gimmevettingsolution.intake.dto;

import java.util.List;

/**
 * Represents a row that failed mandatory field validation.
 * Contains the 0-based row index and the list of canonical field names that were missing or empty.
 */
public class RowFailure {

    private int rowIndex;
    private List<String> missingFields;

    public RowFailure() {
    }

    public RowFailure(int rowIndex, List<String> missingFields) {
        this.rowIndex = rowIndex;
        this.missingFields = missingFields;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }
}
