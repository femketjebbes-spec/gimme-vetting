package com.gimmevettingsolution.analyst.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

/**
 * Validates query parameters for the Analyst API endpoints.
 * Enforces constraints from the WI-CA-001 API contract.
 */
@Service
public class InputValidationService {

    private static final int MAX_SEARCH_LENGTH = 256;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 200;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "invoiceNumber", "debtorName", "status", "poCStatus", "rejectionType", "resubmissionCount"
    );
    private static final Set<String> VALID_STATUSES = Set.of("QUEUED", "REJECTED_TYPE_A", "REJECTED_TYPE_B");

    /**
     * Validates the page parameter. Must be a non-negative integer.
     * Returns a list of error messages (empty if valid).
     */
    public List<String> validatePage(String page) {
        List<String> errors = new ArrayList<>();
        if (page != null && !page.isBlank()) {
            try {
                int pageNum = Integer.parseInt(page);
                if (pageNum < 0) {
                    errors.add("Parameter 'page' must not be less than 0");
                }
            } catch (NumberFormatException e) {
                errors.add("Parameter 'page' must be a valid integer");
            }
        }
        return errors;
    }

    /**
     * Validates the size parameter. Must be an integer between 1 and 200.
     * Returns a list of error messages (empty if valid).
     */
    public List<String> validateSize(String size) {
        List<String> errors = new ArrayList<>();
        if (size != null && !size.isBlank()) {
            try {
                int sizeVal = Integer.parseInt(size);
                if (sizeVal < MIN_SIZE || sizeVal > MAX_SIZE) {
                    errors.add("Parameter 'size' must be between " + MIN_SIZE + " and " + MAX_SIZE);
                }
            } catch (NumberFormatException e) {
                errors.add("Parameter 'size' must be a valid integer");
            }
        }
        return errors;
    }

    /**
     * Validates the sort parameter. Format must be "field,direction" where field is allowlisted and direction is asc/desc.
     * Null is accepted as "use default". Returns a list of error messages (empty if valid).
     */
    public List<String> validateSort(String sort) {
        List<String> errors = new ArrayList<>();
        if (sort == null) {
            return errors;
        }
        if (sort.isBlank()) {
            errors.add("Parameter 'sort' must not be empty");
            return errors;
        }
        String[] parts = sort.split(",", -1);
        if (parts.length != 2) {
            errors.add("Parameter 'sort' must be in format 'field,direction'");
            return errors;
        }
        String field = parts[0].trim();
        String direction = parts[1].trim();
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            errors.add("Parameter 'sort' contains invalid field: " + field);
        }
        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            errors.add("Parameter 'sort' direction must be 'asc' or 'desc'");
        }
        return errors;
    }

    /**
     * Validates the status parameter. Comma-separated list of QUEUED, REJECTED_TYPE_A, or REJECTED_TYPE_B.
     * Null is accepted as "no filter". Returns a list of error messages (empty if valid).
     */
    public List<String> validateStatus(String status) {
        List<String> errors = new ArrayList<>();
        if (status == null || status.isBlank()) {
            return errors;
        }
        String[] statuses = status.split(",");
        for (String s : statuses) {
            if (!VALID_STATUSES.contains(s.trim())) {
                errors.add("Parameter 'status' contains invalid value: " + s.trim());
                break;
            }
        }
        return errors;
    }

    /**
     * Validates the search parameter. Must not exceed MAX_SEARCH_LENGTH characters.
     * Null and empty are accepted. Returns a list of error messages (empty if valid).
     */
    public List<String> validateSearch(String search) {
        List<String> errors = new ArrayList<>();
        if (search != null && search.length() > MAX_SEARCH_LENGTH) {
            errors.add("search parameter must not exceed " + MAX_SEARCH_LENGTH + " characters");
        }
        return errors;
    }

    /**
     * Validates the invoice id. Must be a positive long.
     */
    public boolean validateId(long id) {
        return id > 0;
    }

    /**
     * Validates all query parameters at once. Returns a set of error messages.
     */
    public Set<String> validateAll(String page, String size, String sort, String status, String search) {
        Set<String> errors = new HashSet<>();
        errors.addAll(validatePage(page));
        errors.addAll(validateSize(size));
        errors.addAll(validateSort(sort));
        errors.addAll(validateStatus(status));
        errors.addAll(validateSearch(search));
        return errors;
    }
}
