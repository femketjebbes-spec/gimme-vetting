package com.gimmevettingsolution.analyst;

import com.gimmevettingsolution.analyst.dto.AnalystInvoiceDTO;
import com.gimmevettingsolution.analyst.service.InputValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InputValidationService covering:
 * - page parameter validation (must be >= 0)
 * - size parameter validation (must be 1-200)
 * - sort field validation (allowlisted fields only)
 * - sort direction validation (asc/desc)
 * - status enum validation (QUEUED/REJECTED_TYPE_A/REJECTED_TYPE_B)
 * - search length validation (max 256 characters)
 * - id validation (must be positive)
 * - valid parameters pass all checks
 * - multiple invalid parameters produce multiple errors
 */
@ExtendWith(MockitoExtension.class)
class InputValidationServiceTest {

    private InputValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new InputValidationService();
    }

    // --- Test 1: Valid page parameter passes ---

    @Test
    void validatePage_zeroPasses() {
        List<String> errors = validationService.validatePage("0");
        assertTrue(errors.isEmpty());
    }

    @Test
    void validatePage_positivePasses() {
        List<String> errors = validationService.validatePage("5");
        assertTrue(errors.isEmpty());
    }

    // --- Test 2: Negative page parameter fails ---

    @Test
    void validatePage_negative_returnsError() {
        List<String> errors = validationService.validatePage("-1");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("page"));
    }

    @Test
    void validatePage_nonNumeric_returnsError() {
        List<String> errors = validationService.validatePage("abc");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("page"));
    }

    // --- Test 3: Valid size parameter passes ---

    @Test
    void validateSize_minSize_passes() {
        List<String> errors = validationService.validateSize("1");
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateSize_maxSize_passes() {
        List<String> errors = validationService.validateSize("200");
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateSize_defaultSize_passes() {
        List<String> errors = validationService.validateSize("50");
        assertTrue(errors.isEmpty());
    }

    // --- Test 4: Invalid size parameter fails ---

    @Test
    void validateSize_zero_returnsError() {
        List<String> errors = validationService.validateSize("0");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("size"));
    }

    @Test
    void validateSize_exceeds200_returnsError() {
        List<String> errors = validationService.validateSize("201");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("size"));
    }

    @Test
    void validateSize_negative_returnsError() {
        List<String> errors = validationService.validateSize("-10");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("size"));
    }

    @Test
    void validateSize_nonNumeric_returnsError() {
        List<String> errors = validationService.validateSize("xyz");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("size"));
    }

    // --- Test 5: Valid sort field passes ---

    @Test
    void validateSort_validFieldAndDirection_passes() {
        List<String> errors = validationService.validateSort("id,asc");
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateSort_allAllowlistedFields_passes() {
        String[] allowedFields = {"id", "invoiceNumber", "debtorName", "status", "poCStatus", "rejectionType", "resubmissionCount"};
        for (String field : allowedFields) {
            List<String> errors = validationService.validateSort(field + ",asc");
            assertTrue(errors.isEmpty(), "Field " + field + " should be allowed");
        }
    }

    // --- Test 6: Invalid sort field fails ---

    @Test
    void validateSort_invalidField_returnsError() {
        List<String> errors = validationService.validateSort("nonexistentField,asc");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("sort"));
    }

    @Test
    void validateSort_invalidDirection_returnsError() {
        List<String> errors = validationService.validateSort("id,invalid");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("sort"));
    }

    @Test
    void validateSort_missingDirection_returnsError() {
        List<String> errors = validationService.validateSort("id");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("sort"));
    }

    @Test
    void validateSort_emptyString_returnsError() {
        List<String> errors = validationService.validateSort("");
        assertEquals(1, errors.size());
    }

    // --- Test 7: Valid status parameter passes ---

    @Test
    void validateStatus_queued_passes() {
        List<String> errors = validationService.validateStatus("QUEUED");
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateStatus_rejectedTypeA_passes() {
        List<String> errors = validationService.validateStatus("REJECTED_TYPE_A");
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateStatus_rejectedTypeB_passes() {
        List<String> errors = validationService.validateStatus("REJECTED_TYPE_B");
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateStatus_multipleValidStatuses_passes() {
        List<String> errors = validationService.validateStatus("QUEUED,REJECTED_TYPE_A");
        assertTrue(errors.isEmpty());
    }

    // --- Test 8: Invalid status parameter fails ---

    @Test
    void validateStatus_invalidStatus_returnsError() {
        List<String> errors = validationService.validateStatus("INVALID_STATUS");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("status"));
    }

    @Test
    void validateStatus_oneInvalidInCommaList_returnsError() {
        List<String> errors = validationService.validateStatus("QUEUED,INVALID_STATUS");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("status"));
    }

    // --- Test 9: Valid search parameter passes ---

    @Test
    void validateSearch_emptyString_passes() {
        List<String> errors = validationService.validateSearch("");
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateSearch_256Chars_passes() {
        StringBuilder sb = new StringBuilder(256);
        for (int i = 0; i < 256; i++) {
            sb.append("a");
        }
        List<String> errors = validationService.validateSearch(sb.toString());
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateSearch_normalSearch_passes() {
        List<String> errors = validationService.validateSearch("INV-2026");
        assertTrue(errors.isEmpty());
    }

    // --- Test 10: Search exceeding 256 chars fails ---

    @Test
    void validateSearch_257Chars_returnsError() {
        StringBuilder sb = new StringBuilder(257);
        for (int i = 0; i < 257; i++) {
            sb.append("a");
        }
        List<String> errors = validationService.validateSearch(sb.toString());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("search"));
    }

    // --- Test 11: Valid id passes ---

    @Test
    void validateId_positive_returnsTrue() {
        assertTrue(validationService.validateId(1L));
    }

    @Test
    void validateId_largePositive_returnsTrue() {
        assertTrue(validationService.validateId(Long.MAX_VALUE));
    }

    // --- Test 12: Invalid id fails ---

    @Test
    void validateId_zero_returnsFalse() {
        assertFalse(validationService.validateId(0L));
    }

    @Test
    void validateId_negative_returnsFalse() {
        assertFalse(validationService.validateId(-1L));
    }

    // --- Test 13: Multiple invalid parameters produce multiple errors ---

    @Test
    void validateAllMultipleInvalidParams_returnsMultipleErrors() {
        Set<String> errors = validationService.validateAll("-1", "201", "bad,asc", "INVALID", "x".repeat(300));
        assertTrue(errors.size() >= 2, "Should have errors for multiple invalid parameters");
    }

    // --- Test 14: All valid parameters produce no errors ---

    @Test
    void validateAllAllValidParams_returnsNoErrors() {
        Set<String> errors = validationService.validateAll("0", "50", "id,asc", "QUEUED", "test");
        assertTrue(errors.isEmpty());
    }

    // --- Test 15: null/empty sort is acceptable (use default) ---

    @Test
    void validateSort_null_sortPassedAsDefault() {
        // Null or empty sort should not fail validation; the controller handles defaults
        List<String> errors = validationService.validateSort(null);
        assertTrue(errors.isEmpty());
    }

    // --- Test 16: null/empty status is acceptable (no filter) ---

    @Test
    void validateStatus_null_statusPassedAsNoFilter() {
        List<String> errors = validationService.validateStatus(null);
        assertTrue(errors.isEmpty());
    }

    // --- Test 17: null search is acceptable ---

    @Test
    void validateSearch_null_passes() {
        List<String> errors = validationService.validateSearch(null);
        assertTrue(errors.isEmpty());
    }
}
