package com.gimmevettingsolution.intake.service;

import com.gimmevettingsolution.intake.dto.ExcelInvoiceRow;
import com.gimmevettingsolution.intake.dto.FailingRow;
import com.gimmevettingsolution.intake.dto.RowFailure;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs mandatory field validation on a list of parsed Excel invoice rows.
 * <p>
 * Five fields are mandatory per row: invoiceNumber, debtorName, address,
 * phoneNumber, bankAccountNumber. A field is considered empty when its
 * value is null, empty string, or contains only whitespace characters
 * (decisions D-022 and D-023).
 * <p>
 * The service produces per-row failure detail containing the canonical
 * field names only (decision D-010, security constraint S-012).
 */
@Service
public class MandatoryFieldValidationService {

    private static final String[] MANDATORY_FIELD_NAMES = {
            "invoiceNumber",
            "debtorName",
            "address",
            "phoneNumber",
            "bankAccountNumber"
    };

    /**
     * Validate all rows for mandatory field presence.
     *
     * @param rows the list of parsed Excel invoice rows
     * @return a ValidationResult containing aggregate counts and per-row detail
     */
    public ValidationResult validate(List<ExcelInvoiceRow> rows) {
        ValidationResult result = new ValidationResult();

        if (rows == null) {
            return result;
        }

        for (ExcelInvoiceRow row : rows) {
            int rowIndex = row != null && row.getRowIndex() != null
                    ? row.getRowIndex()
                    : result.getTotalRowsProcessed();

            List<String> missingFields = new ArrayList<>();

            missingFields.add(checkField(row, "invoiceNumber", row::getInvoiceNumber));
            missingFields.add(checkField(row, "debtorName", row::getDebtorName));
            missingFields.add(checkField(row, "address", row::getAddress));
            missingFields.add(checkField(row, "phoneNumber", row::getPhoneNumber));
            missingFields.add(checkField(row, "bankAccountNumber", row::getBankAccountNumber));

            // Remove null entries from missingFields (fields that were present and non-empty)
            missingFields.removeIf(field -> field == null);

            result.setTotalRowsProcessed(result.getTotalRowsProcessed() + 1);

            if (missingFields.isEmpty()) {
                result.setRowsPassed(result.getRowsPassed() + 1);
                result.getPassingRows().add(row);
            } else {
                result.setRowsFailed(result.getRowsFailed() + 1);
                result.getFailingRows().add(new RowFailure(rowIndex, missingFields));
            }
        }

        return result;
    }

    /**
     * Check a single field value. Returns the canonical field name if the
     * field is null, empty, or whitespace-only. Returns null if the field
     * has a valid value.
     */
    private String checkField(ExcelInvoiceRow row, String fieldName,
                              java.util.function.Supplier<String> valueGetter) {
        if (row == null) {
            return fieldName;
        }
        String value = valueGetter.get();
        if (value == null || value.trim().isEmpty()) {
            return fieldName;
        }
        return null;
    }

    /**
     * Map internal RowFailure list to API-level FailingRow list.
     * Uses Integer (nullable) for rowIndex to support JSON serialization.
     *
     * @param rowFailures the internal RowFailure objects from ValidationResult
     * @return list of FailingRow DTOs for API response
     */
    public List<FailingRow> toFailingRows(List<RowFailure> rowFailures) {
        List<FailingRow> failingRows = new ArrayList<>();
        if (rowFailures == null) {
            return failingRows;
        }
        for (RowFailure rf : rowFailures) {
            failingRows.add(new FailingRow(rf.getRowIndex(), rf.getMissingFields()));
        }
        return failingRows;
    }
}
