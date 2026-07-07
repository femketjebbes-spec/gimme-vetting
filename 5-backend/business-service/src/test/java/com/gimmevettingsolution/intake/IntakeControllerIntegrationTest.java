package com.gimmevettingsolution.intake;

import com.gimmevettingsolution.intake.service.IntakeService;
import com.gimmevettingsolution.intake.service.RejectedTypeAException;
import com.gimmevettingsolution.intake.service.ValidationException;
import com.gimmevettingsolution.intake.dto.IntakeRequest;
import com.gimmevettingsolution.intake.dto.PoCVerifiedResponse;
import com.gimmevettingsolution.poc.PoCStoreService;
import com.gimmevettingsolution.invoice.entity.Invoice;
import com.gimmevettingsolution.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for the intake pipeline.
 * Covers all 6 test cases from WI-001 test strategy.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntakeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @MockBean
    private PoCStoreService pocStoreService;

    @TempDir
    Path tempDir;

    @Test
    void happyPath_pocVerified_returns202WithPoCVerified() throws Exception {
        // Test case 1: Invoice INV-2026-0042 with PoC file -> 202 Accepted
        when(pocStoreService.hasMatchingPoC("INV-2026-0042")).thenReturn(true);

        mockMvc.perform(post("/api/v1/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "invoiceNumber": "INV-2026-0042",
                                  "debtorName": "Test Debtor",
                                  "address": "Test Street 1, 1234 AB, NL",
                                  "bankAccountNumber": "NL12TEST0123456789",
                                  "phoneNumber": "+31612345678"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("POC_VERIFIED"))
                .andExpect(jsonPath("$.nextStep").value("BUSINESS_RULE_CHECK"))
                .andExpect(jsonPath("$.invoiceId").isNotEmpty());
    }

    @Test
    void noPoC_returns400WithRejectedTypeA() throws Exception {
        // Test case 2: Invoice INV-2026-0043, no PoC -> 400 REJECTED_TYPE_A
        when(pocStoreService.hasMatchingPoC("INV-2026-0043")).thenReturn(false);

        mockMvc.perform(post("/api/v1/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "invoiceNumber": "INV-2026-0043",
                                  "debtorName": "Test Debtor",
                                  "address": "Test Street 1, 1234 AB, NL",
                                  "bankAccountNumber": "NL12TEST0123456789",
                                  "phoneNumber": "+31612345678"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED_TYPE_A"))
                .andExpect(jsonPath("$.rejectionReason").value("No PoC linked to invoice INV-2026-0043"))
                .andExpect(jsonPath("$.resubmitAllowed").value(true));
    }

    @Test
    void caseVariation_pocFileLowercase_returns202WithPoCVerified() throws Exception {
        // Test case 3: Invoice INV-2026-0044, PoC file inv-2026-0044.pdf (lowercase) -> 202 Accepted
        when(pocStoreService.hasMatchingPoC("INV-2026-0044")).thenReturn(true);

        mockMvc.perform(post("/api/v1/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "invoiceNumber": "INV-2026-0044",
                                  "debtorName": "Test Debtor",
                                  "address": "Test Street 1, 1234 AB, NL",
                                  "bankAccountNumber": "NL12TEST0123456789",
                                  "phoneNumber": "+31612345678"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("POC_VERIFIED"))
                .andExpect(jsonPath("$.invoiceId").isNotEmpty());
    }

    @Test
    void specialChars_pocVerified_returns202WithPoCVerified() throws Exception {
        // Test case 4: Invoice INV-2026-0045-EU -> 202 Accepted
        when(pocStoreService.hasMatchingPoC("INV-2026-0045-EU")).thenReturn(true);

        mockMvc.perform(post("/api/v1/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "invoiceNumber": "INV-2026-0045-EU",
                                  "debtorName": "Test Debtor",
                                  "address": "Test Street 1, 1234 AB, NL",
                                  "bankAccountNumber": "NL12TEST0123456789",
                                  "phoneNumber": "+31612345678"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("POC_VERIFIED"))
                .andExpect(jsonPath("$.invoiceId").isNotEmpty());
    }

    @Test
    void multiMatch_pocVerified_returns202WithPoCVerified() throws Exception {
        // Test case 5: Invoice INV-2026-0046, multiple PoC files -> 202 Accepted
        when(pocStoreService.hasMatchingPoC("INV-2026-0046")).thenReturn(true);

        mockMvc.perform(post("/api/v1/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "invoiceNumber": "INV-2026-0046",
                                  "debtorName": "Test Debtor",
                                  "address": "Test Street 1, 1234 AB, NL",
                                  "bankAccountNumber": "NL12TEST0123456789",
                                  "phoneNumber": "+31612345678"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("POC_VERIFIED"))
                .andExpect(jsonPath("$.invoiceId").isNotEmpty());
    }

    @Test
    void pathTraversal_returns400WithValidationError() throws Exception {
        // Test case 6: Invoice number "../etc/passwd" -> 400 validation error
        mockMvc.perform(post("/api/v1/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "invoiceNumber": "../etc/passwd",
                                  "debtorName": "Test Debtor",
                                  "address": "Test Street 1, 1234 AB, NL",
                                  "bankAccountNumber": "NL12TEST0123456789",
                                  "phoneNumber": "+31612345678"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.resubmitAllowed").value(false));
    }

    @Test
    void missingField_returns400WithValidationError() throws Exception {
        // Additional test: missing invoice number
        mockMvc.perform(post("/api/v1/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "debtorName": "Test Debtor",
                                  "address": "Test Street 1, 1234 AB, NL",
                                  "bankAccountNumber": "NL12TEST0123456789",
                                  "phoneNumber": "+31612345678"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.resubmitAllowed").value(false));
    }

    @Test
    void emptyInvoiceNumber_returns400WithValidationError() throws Exception {
        // Additional test: empty invoice number
        mockMvc.perform(post("/api/v1/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "invoiceNumber": "",
                                  "debtorName": "Test Debtor",
                                  "address": "Test Street 1, 1234 AB, NL",
                                  "bankAccountNumber": "NL12TEST0123456789",
                                  "phoneNumber": "+31612345678"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.resubmitAllowed").value(false));
    }

    @Test
    void happyPath_savesInvoiceWithQueuedStatus() throws Exception {
        // Verify that invoice is saved to database when PoC is verified
        when(pocStoreService.hasMatchingPoC("INV-2026-0099")).thenReturn(true);

        mockMvc.perform(post("/api/v1/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "invoiceNumber": "INV-2026-0099",
                                  "debtorName": "Test Debtor",
                                  "address": "Test Street 1, 1234 AB, NL",
                                  "bankAccountNumber": "NL12TEST0123456789",
                                  "phoneNumber": "+31612345678"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("POC_VERIFIED"));

        // Verify invoice was saved to database
        Optional<Invoice> savedInvoice = invoiceRepository.findByInvoiceNumber("INV-2026-0099");
        assertTrue(savedInvoice.isPresent());
        Invoice invoice = savedInvoice.get();
        assertEquals("INV-2026-0099", invoice.getInvoiceNumber());
        assertEquals("QUEUED", invoice.getStatus());
        assertEquals("VERIFIED", invoice.getPoCStatus());
    }
}
