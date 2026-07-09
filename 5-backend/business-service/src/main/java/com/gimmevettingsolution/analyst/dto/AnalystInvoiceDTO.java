package com.gimmevettingsolution.analyst.dto;

/**
 * DTO representing an invoice for the analyst dashboard.
 * Contains all 10 fields required by the WI-CA-001 API contract.
 */
public class AnalystInvoiceDTO {

    private Long id;
    private String invoiceNumber;
    private String debtorName;
    private String address;
    private String bankAccountNumber;
    private String phoneNumber;
    private String status;
    private String poCStatus;
    private String rejectionType;
    private Integer resubmissionCount;

    public AnalystInvoiceDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getDebtorName() {
        return debtorName;
    }

    public void setDebtorName(String debtorName) {
        this.debtorName = debtorName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPoCStatus() {
        return poCStatus;
    }

    public void setPoCStatus(String poCStatus) {
        this.poCStatus = poCStatus;
    }

    public String getRejectionType() {
        return rejectionType;
    }

    public void setRejectionType(String rejectionType) {
        this.rejectionType = rejectionType;
    }

    public Integer getResubmissionCount() {
        return resubmissionCount;
    }

    public void setResubmissionCount(Integer resubmissionCount) {
        this.resubmissionCount = resubmissionCount;
    }
}
