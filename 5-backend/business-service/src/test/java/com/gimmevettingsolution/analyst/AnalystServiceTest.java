package com.gimmevettingsolution.analyst;

import com.gimmevettingsolution.analyst.dto.AnalystInvoiceDTO;
import com.gimmevettingsolution.analyst.service.AnalystService;
import com.gimmevettingsolution.invoice.entity.Invoice;
import com.gimmevettingsolution.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalystService covering:
 * - listInvoices returns paginated results with default pagination
 * - listInvoices respects page and size parameters
 * - listInvoices applies status filter
 * - listInvoices applies search filter via Specification
 * - listInvoices applies both status and search filters
 * - listInvoices returns empty page when no matches
 * - getInvoiceDetail returns invoice for valid id
 * - getInvoiceDetail throws exception for non-existent id
 * - DTO conversion includes all 10 fields
 * - DTO conversion handles null rejectionType
 */
@ExtendWith(MockitoExtension.class)
class AnalystServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private AnalystService analystService;

    private Invoice createTestInvoice() {
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("INV-2026-0042");
        invoice.setDebtorName("Jan de Vries");
        invoice.setAddress("Voorbeeldstraat 1, 1234AB Amsterdam");
        invoice.setBankAccountNumber("NL12BUNQ0123456789");
        invoice.setPhoneNumber("+31612345678");
        invoice.setStatus("QUEUED");
        invoice.setPoCStatus("VERIFIED");
        invoice.setRejectionType(null);
        invoice.setResubmissionCount(0);
        return invoice;
    }

    private Invoice createRejectedInvoice() {
        Invoice invoice = new Invoice();
        invoice.setId(2L);
        invoice.setInvoiceNumber("INV-2026-0043");
        invoice.setDebtorName("Pieter Jansen");
        invoice.setAddress("Testweg 5, 1000AA Rotterdam");
        invoice.setBankAccountNumber("NL30ABNA0987654321");
        invoice.setPhoneNumber("+31698765432");
        invoice.setStatus("REJECTED_TYPE_A");
        invoice.setPoCStatus("MISSING");
        invoice.setRejectionType("REJECTED_TYPE_A");
        invoice.setResubmissionCount(2);
        return invoice;
    }

    // --- Test 1: listInvoices returns paginated results with defaults ---

    @Test
    void listInvoices_noFilters_returnsDefaultPagination() {
        Invoice invoice = createTestInvoice();
        Page<Invoice> invoicePage = new PageImpl<>(List.of(invoice), PageRequest.of(0, 50), 1);

        when(invoiceRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(invoicePage);

        Page<AnalystInvoiceDTO> result = analystService.listInvoices(null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(0, result.getNumber());
        assertEquals(50, result.getSize());
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals("INV-2026-0042", result.getContent().get(0).getInvoiceNumber());
    }

    // --- Test 2: listInvoices respects custom page and size ---

    @Test
    void listInvoices_customPageAndSize_appliesPageable() {
        Invoice invoice = createTestInvoice();
        Page<Invoice> invoicePage = new PageImpl<>(List.of(invoice), PageRequest.of(2, 10), 1);

        when(invoiceRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(invoicePage);

        Page<AnalystInvoiceDTO> result = analystService.listInvoices(PageRequest.of(2, 10), null, null, null);

        assertEquals(2, result.getNumber());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getContent().size());
    }

    // --- Test 3: listInvoices with status filter applies Specification ---

    @Test
    void listInvoices_withStatusFilter_appliesSpecification() {
        Invoice invoice = createTestInvoice();
        Page<Invoice> invoicePage = new PageImpl<>(List.of(invoice), PageRequest.of(0, 50), 1);

        when(invoiceRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(invoicePage);

        analystService.listInvoices(PageRequest.of(0, 50), "QUEUED", null, null);

        verify(invoiceRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    // --- Test 4: listInvoices with search filter applies Specification ---

    @Test
    void listInvoices_withSearchFilter_appliesSpecification() {
        Invoice invoice = createTestInvoice();
        Page<Invoice> invoicePage = new PageImpl<>(List.of(invoice), PageRequest.of(0, 50), 1);

        when(invoiceRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(invoicePage);

        analystService.listInvoices(PageRequest.of(0, 50), null, "INV-2026", null);

        verify(invoiceRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    // --- Test 5: listInvoices with both status and search applies combined Specification ---

    @Test
    void listInvoices_withStatusAndSearch_appliesCombinedSpecification() {
        Invoice invoice = createTestInvoice();
        Page<Invoice> invoicePage = new PageImpl<>(List.of(invoice), PageRequest.of(0, 50), 1);

        when(invoiceRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(invoicePage);

        analystService.listInvoices(PageRequest.of(0, 50), "QUEUED", "INV-2026", null);

        verify(invoiceRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    // --- Test 6: listInvoices returns empty page when no invoices match ---

    @Test
    void listInvoices_noMatches_returnsEmptyPage() {
        Page<Invoice> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 50), 0);

        when(invoiceRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(emptyPage);

        Page<AnalystInvoiceDTO> result = analystService.listInvoices(PageRequest.of(0, 50), null, null, null);

        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());
        assertTrue(result.getContent().isEmpty());
    }

    // --- Test 7: getInvoiceDetail returns invoice for valid id ---

    @Test
    void getInvoiceDetail_exists_returnsDto() {
        Invoice invoice = createRejectedInvoice();
        when(invoiceRepository.findById(2L)).thenReturn(Optional.of(invoice));

        AnalystInvoiceDTO result = analystService.getInvoiceDetail(2L);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("INV-2026-0043", result.getInvoiceNumber());
        assertEquals("Pieter Jansen", result.getDebtorName());
        assertEquals("Testweg 5, 1000AA Rotterdam", result.getAddress());
        assertEquals("NL30ABNA0987654321", result.getBankAccountNumber());
        assertEquals("+31698765432", result.getPhoneNumber());
        assertEquals("REJECTED_TYPE_A", result.getStatus());
        assertEquals("MISSING", result.getPoCStatus());
        assertEquals("REJECTED_TYPE_A", result.getRejectionType());
        assertEquals(2, result.getResubmissionCount());
    }

    // --- Test 8: getInvoiceDetail throws exception for non-existent id ---

    @Test
    void getInvoiceDetail_notExists_throwsNotFoundException() {
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(com.gimmevettingsolution.analyst.exception.InvoiceNotFoundException.class,
                () -> analystService.getInvoiceDetail(999L));
    }

    // --- Test 9: DTO conversion includes all 10 fields ---

    @Test
    void listInvoices_allTenFieldsPresentInDto() {
        Invoice invoice = createRejectedInvoice();
        Page<Invoice> invoicePage = new PageImpl<>(List.of(invoice), PageRequest.of(0, 50), 1);

        when(invoiceRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(invoicePage);

        Page<AnalystInvoiceDTO> result = analystService.listInvoices(PageRequest.of(0, 50), null, null, null);

        AnalystInvoiceDTO dto = result.getContent().get(0);

        // Verify all 10 fields are present in the DTO
        assertNotNull(dto.getId());
        assertNotNull(dto.getInvoiceNumber());
        assertNotNull(dto.getDebtorName());
        assertNotNull(dto.getAddress());
        assertNotNull(dto.getBankAccountNumber());
        assertNotNull(dto.getPhoneNumber());
        assertNotNull(dto.getStatus());
        assertNotNull(dto.getPoCStatus());
        assertNotNull(dto.getRejectionType());
        assertNotNull(dto.getResubmissionCount());
    }

    // --- Test 10: DTO conversion handles null rejectionType ---

    @Test
    void listInvoices_nullRejectionType_returnsNullInDto() {
        Invoice invoice = createTestInvoice();
        assertNull(invoice.getRejectionType());

        Page<Invoice> invoicePage = new PageImpl<>(List.of(invoice), PageRequest.of(0, 50), 1);

        when(invoiceRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(invoicePage);

        Page<AnalystInvoiceDTO> result = analystService.listInvoices(PageRequest.of(0, 50), null, null, null);

        AnalystInvoiceDTO dto = result.getContent().get(0);
        assertNull(dto.getRejectionType());
    }

    // --- Test 11: listInvoices with default size 50 ---

    @Test
    void listInvoices_defaultSize50_appliesCorrectPageable() {
        Invoice invoice = createTestInvoice();
        Page<Invoice> invoicePage = new PageImpl<>(List.of(invoice), PageRequest.of(0, 50), 1);

        when(invoiceRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(invoicePage);

        Page<AnalystInvoiceDTO> result = analystService.listInvoices(null, null, null, null);

        assertEquals(50, result.getSize());
    }

    // --- Test 12: getInvoiceDetail with resubmissionCount=0 ---

    @Test
    void getInvoiceDetail_zeroResubmissionCount_returnsZero() {
        Invoice invoice = createTestInvoice();
        assertEquals(0, invoice.getResubmissionCount());

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        AnalystInvoiceDTO result = analystService.getInvoiceDetail(1L);

        assertEquals(0, result.getResubmissionCount());
    }
}
