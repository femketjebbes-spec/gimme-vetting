package com.gimmevettingsolution.intake;

import com.gimmevettingsolution.intake.dto.ExcelInvoiceRow;
import com.gimmevettingsolution.intake.dto.FailingRow;
import com.gimmevettingsolution.intake.dto.RowFailure;
import com.gimmevettingsolution.intake.service.MandatoryFieldValidationService;
import com.gimmevettingsolution.intake.service.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MandatoryFieldValidationService covering:
 * - All fields present (row passes)
 * - Missing individual fields
 * - Multiple fields missing
 * - Whitespace-only values treated as empty
 * - Null values treated as empty
 * - Empty input
 * - Mixed pass/fail
 * - All rows fail
 */
class MandatoryFieldValidationServiceTest {

    private MandatoryFieldValidationService service;

    @BeforeEach
    void setUp() {
        service = new MandatoryFieldValidationService();
    }

    // --- Test 1: All fields present — row passes ---

    @Test
    void validate_allFieldsPresent_rowPasses() {
        ExcelInvoiceRow row = createRow(0, "INV-001", "Test Corp", "Main St 1", "+31612345678", "NL12TEST0123456789");
        List<ExcelInvoiceRow> rows = List.of(row);

        ValidationResult result = service.validate(rows);

        assertEquals(1, result.getTotalRowsProcessed());
        assertEquals(1, result.getRowsPassed());
        assertEquals(0, result.getRowsFailed());
        assertEquals(1, result.getPassingRows().size());
        assertTrue(result.getFailingRows().isEmpty());
    }

    // --- Test 2: Missing debtorName only — correct missingFields ---

    @Test
    void validate_missingDebtorName_onlyDebtorNameInMissingFields() {
        ExcelInvoiceRow row = createRow(0, "INV-001", null, "Main St 1", "+31612345678", "NL12TEST0123456789");
        List<ExcelInvoiceRow> rows = List.of(row);

        ValidationResult result = service.validate(rows);

        assertEquals(1, result.getTotalRowsProcessed());
        assertEquals(0, result.getRowsPassed());
        assertEquals(1, result.getRowsFailed());
        assertEquals(1, result.getFailingRows().size());

        RowFailure failure = result.getFailingRows().get(0);
        assertEquals(0, failure.getRowIndex());
        assertEquals(1, failure.getMissingFields().size());
        assertEquals("debtorName", failure.getMissingFields().get(0));
    }

    // --- Test 3: Multiple fields missing — all listed ---

    @Test
    void validate_multipleFieldsMissing_allListedInMissingFields() {
        ExcelInvoiceRow row = createRow(0, "INV-001", null, null, "+31612345678", "NL12TEST0123456789");
        List<ExcelInvoiceRow> rows = List.of(row);

        ValidationResult result = service.validate(rows);

        assertEquals(1, result.getTotalRowsProcessed());
        assertEquals(0, result.getRowsPassed());
        assertEquals(1, result.getRowsFailed());

        RowFailure failure = result.getFailingRows().get(0);
        assertEquals(2, failure.getMissingFields().size());
        assertTrue(failure.getMissingFields().contains("debtorName"));
        assertTrue(failure.getMissingFields().contains("address"));
    }

    // --- Test 4: Missing invoiceNumber — row fails ---

    @Test
    void validate_missingInvoiceNumber_rowFails() {
        ExcelInvoiceRow row = createRow(0, null, "Test Corp", "Main St 1", "+31612345678", "NL12TEST0123456789");
        List<ExcelInvoiceRow> rows = List.of(row);

        ValidationResult result = service.validate(rows);

        assertEquals(0, result.getRowsPassed());
        assertEquals(1, result.getRowsFailed());

        RowFailure failure = result.getFailingRows().get(0);
        assertTrue(failure.getMissingFields().contains("invoiceNumber"));
    }

    // --- Test 5: Whitespace-only debtorName — treated as empty ---

    @Test
    void validate_whitespaceOnlyDebtorName_treatedAsEmpty() {
        ExcelInvoiceRow row = createRow(0, "INV-001", "   ", "Main St 1", "+31612345678", "NL12TEST0123456789");
        List<ExcelInvoiceRow> rows = List.of(row);

        ValidationResult result = service.validate(rows);

        assertEquals(0, result.getRowsPassed());
        assertEquals(1, result.getRowsFailed());

        RowFailure failure = result.getFailingRows().get(0);
        assertTrue(failure.getMissingFields().contains("debtorName"));
    }

    // --- Test 6: Null address — treated as empty ---

    @Test
    void validate_nullAddress_treatedAsEmpty() {
        ExcelInvoiceRow row = createRow(0, "INV-001", "Test Corp", null, "+31612345678", "NL12TEST0123456789");
        List<ExcelInvoiceRow> rows = List.of(row);

        ValidationResult result = service.validate(rows);

        assertEquals(0, result.getRowsPassed());
        assertEquals(1, result.getRowsFailed());

        RowFailure failure = result.getFailingRows().get(0);
        assertTrue(failure.getMissingFields().contains("address"));
    }

    // --- Test 7: All rows pass — failingRows empty ---

    @Test
    void validate_allRowsPass_failingRowsEmpty() {
        ExcelInvoiceRow row1 = createRow(0, "INV-001", "Corp A", "Street 1", "+31611111111", "NL01TEST0101010101");
        ExcelInvoiceRow row2 = createRow(1, "INV-002", "Corp B", "Street 2", "+31622222222", "NL02TEST0202020202");
        ExcelInvoiceRow row3 = createRow(2, "INV-003", "Corp C", "Street 3", "+31633333333", "NL03TEST0303030303");
        List<ExcelInvoiceRow> rows = List.of(row1, row2, row3);

        ValidationResult result = service.validate(rows);

        assertEquals(3, result.getTotalRowsProcessed());
        assertEquals(3, result.getRowsPassed());
        assertEquals(0, result.getRowsFailed());
        assertTrue(result.getFailingRows().isEmpty());
    }

    // --- Test 8: All rows fail — all in failingRows ---

    @Test
    void validate_allRowsFail_allInFailingRows() {
        ExcelInvoiceRow row1 = createRow(0, null, null, null, null, null);
        ExcelInvoiceRow row2 = createRow(1, "INV-002", null, null, null, null);
        List<ExcelInvoiceRow> rows = List.of(row1, row2);

        ValidationResult result = service.validate(rows);

        assertEquals(2, result.getTotalRowsProcessed());
        assertEquals(0, result.getRowsPassed());
        assertEquals(2, result.getRowsFailed());
        assertEquals(2, result.getFailingRows().size());

        // Verify each failing row has rowIndex set correctly
        assertEquals(0, result.getFailingRows().get(0).getRowIndex());
        assertEquals(1, result.getFailingRows().get(1).getRowIndex());
    }

    // --- Test 9: Mixed pass/fail — correct split ---

    @Test
    void validate_mixedPassFail_correctSplit() {
        ExcelInvoiceRow passRow = createRow(0, "INV-001", "Corp A", "Street 1", "+31611111111", "NL01TEST0101010101");
        ExcelInvoiceRow failRow = createRow(1, "INV-002", null, "Street 2", "+31622222222", "NL02TEST0202020202");
        ExcelInvoiceRow anotherPassRow = createRow(2, "INV-003", "Corp C", "Street 3", "+31633333333", "NL03TEST0303030303");
        List<ExcelInvoiceRow> rows = List.of(passRow, failRow, anotherPassRow);

        ValidationResult result = service.validate(rows);

        assertEquals(3, result.getTotalRowsProcessed());
        assertEquals(2, result.getRowsPassed());
        assertEquals(1, result.getRowsFailed());
        assertEquals(2, result.getPassingRows().size());
        assertEquals(1, result.getFailingRows().size());

        RowFailure failure = result.getFailingRows().get(0);
        assertEquals(1, failure.getRowIndex());
        assertEquals(1, failure.getMissingFields().size());
        assertEquals("debtorName", failure.getMissingFields().get(0));
    }

    // --- Test 10: Empty field with only spaces — treated as empty ---

    @Test
    void validate_emptyFieldWithOnlySpaces_treatedAsEmpty() {
        ExcelInvoiceRow row = createRow(0, "  ", "Test Corp", "Main St 1", "+31612345678", "NL12TEST0123456789");
        List<ExcelInvoiceRow> rows = List.of(row);

        ValidationResult result = service.validate(rows);

        assertEquals(0, result.getRowsPassed());
        assertEquals(1, result.getRowsFailed());

        RowFailure failure = result.getFailingRows().get(0);
        assertTrue(failure.getMissingFields().contains("invoiceNumber"));
    }

    // --- Additional: Null list — returns empty result ---

    @Test
    void validate_nullList_returnsEmptyResult() {
        ValidationResult result = service.validate(null);

        assertEquals(0, result.getTotalRowsProcessed());
        assertEquals(0, result.getRowsPassed());
        assertEquals(0, result.getRowsFailed());
        assertTrue(result.getPassingRows().isEmpty());
        assertTrue(result.getFailingRows().isEmpty());
    }

    // --- Additional: toFailingRows maps correctly ---

    @Test
    void toFailingRows_correctlyMapsRowFailuresToFailingRows() {
        List<RowFailure> rowFailures = new ArrayList<>();
        rowFailures.add(new RowFailure(0, List.of("debtorName", "address")));
        rowFailures.add(new RowFailure(3, List.of("bankAccountNumber")));

        List<FailingRow> failingRows = service.toFailingRows(rowFailures);

        assertEquals(2, failingRows.size());
        assertEquals(Integer.valueOf(0), failingRows.get(0).getRowIndex());
        assertEquals(2, failingRows.get(0).getMissingFields().size());
        assertEquals(Integer.valueOf(3), failingRows.get(1).getRowIndex());
        assertEquals(1, failingRows.get(1).getMissingFields().size());
        assertEquals("bankAccountNumber", failingRows.get(1).getMissingFields().get(0));
    }

    // --- Additional: Canonical field names in missingFields ---

    @Test
    void validate_missingFieldsContainsOnlyCanonicalNames() {
        ExcelInvoiceRow row = createRow(0, null, null, null, null, null);
        List<ExcelInvoiceRow> rows = List.of(row);

        ValidationResult result = service.validate(rows);

        RowFailure failure = result.getFailingRows().get(0);
        assertEquals(5, failure.getMissingFields().size());
        assertTrue(failure.getMissingFields().contains("invoiceNumber"));
        assertTrue(failure.getMissingFields().contains("debtorName"));
        assertTrue(failure.getMissingFields().contains("address"));
        assertTrue(failure.getMissingFields().contains("phoneNumber"));
        assertTrue(failure.getMissingFields().contains("bankAccountNumber"));
    }

    // --- Additional: Whitespace-only all fields ---

    @Test
    void validate_whitespaceOnlyAllFields_allFail() {
        ExcelInvoiceRow row = createRow(0, "   ", "  ", "\t", "  ", "\n");
        List<ExcelInvoiceRow> rows = List.of(row);

        ValidationResult result = service.validate(rows);

        assertEquals(1, result.getTotalRowsProcessed());
        assertEquals(0, result.getRowsPassed());
        assertEquals(1, result.getRowsFailed());
        assertEquals(5, result.getFailingRows().get(0).getMissingFields().size());
    }

    // --- Helper ---

    private ExcelInvoiceRow createRow(int rowIndex, String invoiceNumber, String debtorName,
                                      String address, String phoneNumber, String bankAccountNumber) {
        ExcelInvoiceRow row = new ExcelInvoiceRow();
        row.setRowIndex(rowIndex);
        row.setInvoiceNumber(invoiceNumber);
        row.setDebtorName(debtorName);
        row.setAddress(address);
        row.setPhoneNumber(phoneNumber);
        row.setBankAccountNumber(bankAccountNumber);
        return row;
    }
}
