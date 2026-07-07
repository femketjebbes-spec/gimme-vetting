package com.gimmevettingsolution.intake.service;

import com.gimmevettingsolution.intake.dto.ExcelInvoiceRow;
import com.gimmevettingsolution.intake.dto.RowFailure;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate result of mandatory field validation across a list of rows.
 */
public class ValidationResult {

    private int totalRowsProcessed;
    private int rowsPassed;
    private int rowsFailed;
    private List<ExcelInvoiceRow> passingRows;
    private List<RowFailure> failingRows;

    public ValidationResult() {
        this.passingRows = new ArrayList<>();
        this.failingRows = new ArrayList<>();
    }

    public int getTotalRowsProcessed() {
        return totalRowsProcessed;
    }

    public void setTotalRowsProcessed(int totalRowsProcessed) {
        this.totalRowsProcessed = totalRowsProcessed;
    }

    public int getRowsPassed() {
        return rowsPassed;
    }

    public void setRowsPassed(int rowsPassed) {
        this.rowsPassed = rowsPassed;
    }

    public int getRowsFailed() {
        return rowsFailed;
    }

    public void setRowsFailed(int rowsFailed) {
        this.rowsFailed = rowsFailed;
    }

    public List<ExcelInvoiceRow> getPassingRows() {
        return passingRows;
    }

    public void setPassingRows(List<ExcelInvoiceRow> passingRows) {
        this.passingRows = passingRows;
    }

    public List<RowFailure> getFailingRows() {
        return failingRows;
    }

    public void setFailingRows(List<RowFailure> failingRows) {
        this.failingRows = failingRows;
    }
}
