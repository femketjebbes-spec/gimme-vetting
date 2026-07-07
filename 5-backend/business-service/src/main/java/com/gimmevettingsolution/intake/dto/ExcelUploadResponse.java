package com.gimmevettingsolution.intake.dto;

/**
 * Response DTO for successful Excel upload processing.
 */
public class ExcelUploadResponse {

    private String processingStatus;
    private Integer totalRowsProcessed;
    private Integer rowsPassed;
    private Integer rowsFailed;
    private String returnExcelDownloadLink;

    public ExcelUploadResponse() {
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }

    public Integer getTotalRowsProcessed() {
        return totalRowsProcessed;
    }

    public void setTotalRowsProcessed(Integer totalRowsProcessed) {
        this.totalRowsProcessed = totalRowsProcessed;
    }

    public Integer getRowsPassed() {
        return rowsPassed;
    }

    public void setRowsPassed(Integer rowsPassed) {
        this.rowsPassed = rowsPassed;
    }

    public Integer getRowsFailed() {
        return rowsFailed;
    }

    public void setRowsFailed(Integer rowsFailed) {
        this.rowsFailed = rowsFailed;
    }

    public String getReturnExcelDownloadLink() {
        return returnExcelDownloadLink;
    }

    public void setReturnExcelDownloadLink(String returnExcelDownloadLink) {
        this.returnExcelDownloadLink = returnExcelDownloadLink;
    }
}
