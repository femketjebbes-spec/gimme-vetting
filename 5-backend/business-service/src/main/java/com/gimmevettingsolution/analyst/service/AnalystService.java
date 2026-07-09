package com.gimmevettingsolution.analyst.service;

import com.gimmevettingsolution.analyst.dto.AnalystInvoiceDTO;
import com.gimmevettingsolution.analyst.exception.InvoiceNotFoundException;
import com.gimmevettingsolution.invoice.entity.Invoice;
import com.gimmevettingsolution.invoice.repository.InvoiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for the Analyst API.
 * Provides paginated invoice list with filtering and single invoice detail retrieval.
 * Uses JPA Specifications for dynamic query construction.
 */
@Service
public class AnalystService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;
    private static final String DEFAULT_SORT_FIELD = "id";
    private static final String DEFAULT_SORT_DIRECTION = "asc";

    private final InvoiceRepository invoiceRepository;

    public AnalystService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Lists invoices with pagination, status filter, search filter, and sort parameter.
     * The sort parameter is parsed into a Sort object for PageRequest.
     */
    public Page<AnalystInvoiceDTO> listInvoices(
            PageRequest pageRequest,
            String status,
            String search,
            String sort) {

        // Apply defaults for null/empty page request
        if (pageRequest == null) {
            Sort sortObj = Sort.by(Sort.Direction.fromString(DEFAULT_SORT_DIRECTION), DEFAULT_SORT_FIELD);
            pageRequest = PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE, sortObj);
        }

        // Build combined Specification
        Specification<Invoice> spec = Specification.where(null);

        // Apply status filter if provided
        if (status != null && !status.isBlank()) {
            String[] statuses = status.split(",");
            Specification<Invoice> statusSpec = (root, query, cb) -> root.get("status").in(statuses);
            spec = spec.and(statusSpec);
        }

        // Apply search filter if provided
        if (search != null && !search.isBlank()) {
            String searchTerm = "%" + search.toLowerCase() + "%";
            Specification<Invoice> searchSpec = (root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("invoiceNumber")), searchTerm),
                    cb.like(cb.lower(root.get("debtorName")), searchTerm),
                    cb.like(cb.lower(root.get("address")), searchTerm)
            );
            spec = spec.and(searchSpec);
        }

        Page<Invoice> invoicePage = invoiceRepository.findAll(spec, pageRequest);
        return invoicePage.map(this::toDTO);
    }

    /**
     * Gets a single invoice by ID.
     */
    public AnalystInvoiceDTO getInvoiceDetail(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id));
        return toDTO(invoice);
    }

    /**
     * Converts an Invoice entity to AnalystInvoiceDTO.
     */
    private AnalystInvoiceDTO toDTO(Invoice invoice) {
        AnalystInvoiceDTO dto = new AnalystInvoiceDTO();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setDebtorName(invoice.getDebtorName());
        dto.setAddress(invoice.getAddress());
        dto.setBankAccountNumber(invoice.getBankAccountNumber());
        dto.setPhoneNumber(invoice.getPhoneNumber());
        dto.setStatus(invoice.getStatus());
        dto.setPoCStatus(invoice.getPoCStatus());
        dto.setRejectionType(invoice.getRejectionType());
        dto.setResubmissionCount(invoice.getResubmissionCount());
        dto.setSourceFileId(invoice.getSourceFileId());
        dto.setSourceFilename(invoice.getSourceFilename());
        return dto;
    }
}
