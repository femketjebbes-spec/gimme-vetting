package com.gimmevettingsolution.analyst.controller;

import com.gimmevettingsolution.analyst.dto.AnalystInvoiceDTO;
import com.gimmevettingsolution.analyst.exception.InvoiceNotFoundException;
import com.gimmevettingsolution.analyst.service.AnalystService;
import com.gimmevettingsolution.analyst.service.InputValidationService;
import com.gimmevettingsolution.excel.FileBackedExcelStoreService;
import com.gimmevettingsolution.invoice.entity.Invoice;
import com.gimmevettingsolution.invoice.repository.InvoiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for the Case Analyst dashboard.
 * Provides paginated invoice list and single invoice detail endpoints.
 * Both endpoints are unauthenticated per D-CA-002.
 */
@RestController
@RequestMapping("/api/v1/analyst")
public class AnalystController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;
    private static final String DEFAULT_SORT = "id,asc";

    private final AnalystService analystService;
    private final InputValidationService validationService;
    private final InvoiceRepository invoiceRepository;
    private final FileBackedExcelStoreService excelStoreService;

    /**
     * Constructor for backward compatibility with existing tests.
     * New endpoint (getSourceFile) requires invoiceRepository and excelStoreService.
     */
    public AnalystController(AnalystService analystService, InputValidationService validationService) {
        this(analystService, validationService, null, null);
    }

    /**
     * Full constructor with all dependencies. Used by Spring for dependency injection.
     */
    @Autowired
    public AnalystController(AnalystService analystService, InputValidationService validationService,
                             InvoiceRepository invoiceRepository, FileBackedExcelStoreService excelStoreService) {
        this.analystService = analystService;
        this.validationService = validationService;
        this.invoiceRepository = invoiceRepository;
        this.excelStoreService = excelStoreService;
    }

    /**
     * Endpoint 1: Paginated invoice list with filtering, sorting, and search.
     * GET /api/v1/analyst/invoices
     */
    @GetMapping("/invoices")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Page<AnalystInvoiceDTO>> listInvoices(
            @RequestParam(name = "page", defaultValue = "0") String pageParam,
            @RequestParam(name = "size", defaultValue = "50") String sizeParam,
            @RequestParam(name = "sort", defaultValue = "id,asc") String sortParam,
            @RequestParam(name = "status", required = false) String statusParam,
            @RequestParam(name = "search", required = false) String searchParam) {

        // Validate all parameters
        java.util.Set<String> errors = validationService.validateAll(pageParam, sizeParam, sortParam, statusParam, searchParam);
        if (!errors.isEmpty()) {
            return (ResponseEntity<Page<AnalystInvoiceDTO>>) (ResponseEntity<?>) buildValidationErrorResponse(errors);
        }

        // Parse pagination
        int page = Integer.parseInt(pageParam);
        int size = Integer.parseInt(sizeParam);
        PageRequest pageable = PageRequest.of(page, size);

        // Parse sort direction
        String[] sortParts = sortParam.split(",", -1);
        if (sortParts.length == 2) {
            String field = sortParts[0].trim();
            String direction = sortParts[1].trim();
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), field));
        }

        Page<AnalystInvoiceDTO> result = analystService.listInvoices(pageable, statusParam, searchParam, sortParam);
        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint 2: Single invoice detail.
     * GET /api/v1/analyst/invoices/{id}
     */
    @GetMapping("/invoices/{id}")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> getInvoiceDetail(@PathVariable String id) {
        try {
            long idValue = Long.parseLong(id);
            if (!validationService.validateId(idValue)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Bad Request",
                        "message", "Invalid invoice id parameter"
                ));
            }
            AnalystInvoiceDTO dto = analystService.getInvoiceDetail(idValue);
            return ResponseEntity.ok(dto);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Bad Request",
                    "message", "Invalid invoice id parameter"
            ));
        }
    }

    /**
     * Endpoint 3: Source file download.
     * GET /api/v1/analyst/invoices/{id}/source-file
     */
    @GetMapping("/invoices/{id}/source-file")
    public ResponseEntity<?> getSourceFile(@PathVariable String id) {
        try {
            long idValue = Long.parseLong(id);
            if (!validationService.validateId(idValue)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Bad Request",
                        "message", "Invalid invoice id parameter"
                ));
            }

            // Look up invoice
            Optional<Invoice> invoiceOpt = invoiceRepository.findById(idValue);
            if (invoiceOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "error", "Not Found",
                        "message", "No source file available for this invoice"
                ));
            }

            Invoice invoice = invoiceOpt.get();
            String sourceFileId = invoice.getSourceFileId();

            // No source file — return 404
            if (sourceFileId == null || sourceFileId.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "error", "Not Found",
                        "message", "No source file available for this invoice"
                ));
            }

            // Resolve file from store
            try {
                org.springframework.core.io.Resource resource = excelStoreService.getFile(sourceFileId);
                String contentType = excelStoreService.getContentType(invoice.getSourceFilename());
                String disposition = "inline; filename=\"" + invoice.getSourceFilename() + "\"";

                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.valueOf(contentType))
                        .header("Content-Disposition", disposition)
                        .body(resource);
            } catch (RuntimeException e) {
                return ResponseEntity.status(500).body(Map.of(
                        "error", "Internal Server Error",
                        "message", "Source file is unavailable"
                ));
            }

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Bad Request",
                    "message", "Invalid invoice id parameter"
            ));
        }
    }

    /**
     * Global exception handler for InvoiceNotFoundException.
     */
    @ExceptionHandler(InvoiceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleInvoiceNotFound(InvoiceNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of(
                "error", "Not Found",
                "message", ex.getMessage()
        ));
    }

    private ResponseEntity<?> buildValidationErrorResponse(java.util.Set<String> errors) {
        List<Map<String, Object>> details = new java.util.ArrayList<>();
        for (String msg : errors) {
            Map<String, Object> detail = new java.util.HashMap<>();
            detail.put("message", msg);
            details.add(detail);
        }
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("error", "Bad Request");
        body.put("message", "Parameter validation failed");
        body.put("details", details);
        return ResponseEntity.badRequest().body(body);
    }
}
