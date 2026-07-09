package com.gimmevettingsolution.analyst;

import com.gimmevettingsolution.analyst.controller.AnalystController;
import com.gimmevettingsolution.analyst.dto.AnalystInvoiceDTO;
import com.gimmevettingsolution.analyst.service.AnalystService;
import com.gimmevettingsolution.analyst.service.InputValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Controller tests for AnalystController covering:
 * - GET /api/v1/analyst/invoices with valid parameters
 * - GET /api/v1/analyst/invoices with invalid page (negative)
 * - GET /api/v1/analyst/invoices with invalid size (exceeds 200)
 * - GET /api/v1/analyst/invoices with invalid sort field
 * - GET /api/v1/analyst/invoices with search exceeding 256 chars
 * - GET /api/v1/analyst/invoices with invalid status enum
 * - GET /api/v1/analyst/invoices/{id} with valid id
 * - GET /api/v1/analyst/invoices/{id} with non-existent id (404)
 * - GET /api/v1/analyst/invoices/{id} with invalid id (negative)
 */
class AnalystControllerTest {

    private MockMvc mockMvc;

    private AnalystService analystService;

    private InputValidationService inputValidationService;

    private AnalystInvoiceDTO createSampleInvoice() {
        AnalystInvoiceDTO dto = new AnalystInvoiceDTO();
        dto.setId(1L);
        dto.setInvoiceNumber("INV-2026-0042");
        dto.setDebtorName("Jan de Vries");
        dto.setAddress("Voorbeeldstraat 1, 1234AB Amsterdam");
        dto.setBankAccountNumber("NL12BUNQ0123456789");
        dto.setPhoneNumber("+31612345678");
        dto.setStatus("QUEUED");
        dto.setPoCStatus("VERIFIED");
        dto.setRejectionType(null);
        dto.setResubmissionCount(0);
        return dto;
    }

    @BeforeEach
    void setUp() {
        analystService = mock(AnalystService.class);
        inputValidationService = mock(InputValidationService.class);
        AnalystController controller = new AnalystController(analystService, inputValidationService);
        mockMvc = standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
                .build();
    }

    // --- Test 1: List endpoint returns paginated response with 200 ---

    @Test
    void listInvoices_validRequest_returns200WithPagination() throws Exception {
        AnalystInvoiceDTO invoice = createSampleInvoice();
        Page<AnalystInvoiceDTO> page = new PageImpl<>(List.of(invoice), PageRequest.of(0, 50), 1);

        when(inputValidationService.validateAll(any(), any(), any(), any(), any()))
                .thenReturn(Set.of());
        when(analystService.listInvoices(any(PageRequest.class), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/analyst/invoices")
                        .param("page", "0")
                        .param("size", "50")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-2026-0042"))
                .andExpect(jsonPath("$.content[0].debtorName").value("Jan de Vries"))
                .andExpect(jsonPath("$.content[0].address").value("Voorbeeldstraat 1, 1234AB Amsterdam"))
                .andExpect(jsonPath("$.content[0].bankAccountNumber").value("NL12BUNQ0123456789"))
                .andExpect(jsonPath("$.content[0].phoneNumber").value("+31612345678"))
                .andExpect(jsonPath("$.content[0].status").value("QUEUED"))
                .andExpect(jsonPath("$.content[0].poCStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.content[0].rejectionType").doesNotExist())
                .andExpect(jsonPath("$.content[0].resubmissionCount").value(0))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(50));
    }

    // --- Test 2: List endpoint rejects negative page ---

    @Test
    void listInvoices_negativePage_returns400() throws Exception {
        when(inputValidationService.validateAll(any(), any(), any(), any(), any()))
                .thenReturn(Set.of("Parameter 'page' must not be less than 0"));

        mockMvc.perform(get("/api/v1/analyst/invoices")
                        .param("page", "-1")
                        .param("size", "50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details").isArray());
    }

    // --- Test 3: List endpoint rejects size exceeding 200 ---

    @Test
    void listInvoices_sizeExceeds200_returns400() throws Exception {
        when(inputValidationService.validateAll(any(), any(), any(), any(), any()))
                .thenReturn(Set.of("Parameter 'size' must be between 1 and 200"));

        mockMvc.perform(get("/api/v1/analyst/invoices")
                        .param("page", "0")
                        .param("size", "201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details").isArray());
    }

    // --- Test 4: List endpoint rejects invalid sort field ---

    @Test
    void listInvoices_invalidSortField_returns400() throws Exception {
        when(inputValidationService.validateAll(any(), any(), any(), any(), any()))
                .thenReturn(Set.of("Parameter 'sort' contains invalid field: nonexistentField"));

        mockMvc.perform(get("/api/v1/analyst/invoices")
                        .param("sort", "nonexistentField,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details").isArray());
    }

    // --- Test 5: List endpoint rejects search exceeding 256 chars ---

    @Test
    void listInvoices_searchExceeds256_returns400() throws Exception {
        StringBuilder longSearch = new StringBuilder();
        for (int i = 0; i < 257; i++) {
            longSearch.append("a");
        }
        String searchParam = longSearch.toString();

        when(inputValidationService.validateAll(any(), any(), any(), any(), eq(searchParam)))
                .thenReturn(Set.of("search parameter must not exceed 256 characters"));

        mockMvc.perform(get("/api/v1/analyst/invoices")
                        .param("search", searchParam))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details").isArray());
    }

    // --- Test 6: List endpoint with status filter ---

    @Test
    void listInvoices_withStatusFilter_returns200() throws Exception {
        AnalystInvoiceDTO invoice = createSampleInvoice();
        invoice.setStatus("REJECTED_TYPE_A");
        Page<AnalystInvoiceDTO> page = new PageImpl<>(List.of(invoice), PageRequest.of(0, 50), 1);

        when(inputValidationService.validateAll(any(), any(), any(), any(), any()))
                .thenReturn(Set.of());
        when(analystService.listInvoices(any(PageRequest.class), eq("REJECTED_TYPE_A"), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/analyst/invoices")
                        .param("status", "REJECTED_TYPE_A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("REJECTED_TYPE_A"));
    }

    // --- Test 7: List endpoint with search parameter ---

    @Test
    void listInvoices_withSearch_returns200() throws Exception {
        AnalystInvoiceDTO invoice = createSampleInvoice();
        Page<AnalystInvoiceDTO> page = new PageImpl<>(List.of(invoice), PageRequest.of(0, 50), 1);

        when(inputValidationService.validateAll(any(), any(), any(), any(), eq("INV-2026")))
                .thenReturn(Set.of());
        when(analystService.listInvoices(any(PageRequest.class), any(), eq("INV-2026"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/analyst/invoices")
                        .param("search", "INV-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-2026-0042"));
    }

    // --- Test 8: Detail endpoint returns 200 for valid id ---

    @Test
    void getInvoiceDetail_validId_returns200() throws Exception {
        AnalystInvoiceDTO invoice = createSampleInvoice();
        when(inputValidationService.validateId(1L)).thenReturn(true);
        when(analystService.getInvoiceDetail(eq(1L))).thenReturn(invoice);

        mockMvc.perform(get("/api/v1/analyst/invoices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-2026-0042"))
                .andExpect(jsonPath("$.debtorName").value("Jan de Vries"))
                .andExpect(jsonPath("$.address").value("Voorbeeldstraat 1, 1234AB Amsterdam"))
                .andExpect(jsonPath("$.bankAccountNumber").value("NL12BUNQ0123456789"))
                .andExpect(jsonPath("$.phoneNumber").value("+31612345678"))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.poCStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.rejectionType").doesNotExist())
                .andExpect(jsonPath("$.resubmissionCount").value(0));
    }

    // --- Test 9: Detail endpoint returns 404 for non-existent id ---

    @Test
    void getInvoiceDetail_nonExistentId_returns404() throws Exception {
        when(inputValidationService.validateId(999L)).thenReturn(true);
        when(analystService.getInvoiceDetail(eq(999L)))
                .thenThrow(new com.gimmevettingsolution.analyst.exception.InvoiceNotFoundException(999L));

        mockMvc.perform(get("/api/v1/analyst/invoices/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Invoice with id 999 not found"));
    }

    // --- Test 10: Detail endpoint rejects negative id ---

    @Test
    void getInvoiceDetail_negativeId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/analyst/invoices/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid invoice id parameter"));
    }

    // --- Test 11: List endpoint with default parameters ---

    @Test
    void listInvoices_defaultParams_returns200WithDefaults() throws Exception {
        AnalystInvoiceDTO invoice = createSampleInvoice();
        Page<AnalystInvoiceDTO> page = new PageImpl<>(List.of(invoice), PageRequest.of(0, 50), 1);

        when(inputValidationService.validateAll(any(), any(), any(), any(), any()))
                .thenReturn(Set.of());
        when(analystService.listInvoices(any(PageRequest.class), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/analyst/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-2026-0042"));
    }

    // --- Test 12: Detail endpoint with string id returns 400 ---

    @Test
    void getInvoiceDetail_stringId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/analyst/invoices/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid invoice id parameter"));
    }

    // --- Test 13: List endpoint rejects invalid status enum ---

    @Test
    void listInvoices_invalidStatus_returns400() throws Exception {
        when(inputValidationService.validateAll(any(), any(), any(), any(), any()))
                .thenReturn(Set.of("Parameter 'status' contains invalid value: INVALID_STATUS"));

        mockMvc.perform(get("/api/v1/analyst/invoices")
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details").isArray());
    }
}
