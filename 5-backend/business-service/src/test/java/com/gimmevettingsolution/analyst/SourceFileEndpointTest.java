package com.gimmevettingsolution.analyst;

import com.gimmevettingsolution.analyst.controller.AnalystController;
import com.gimmevettingsolution.analyst.dto.AnalystInvoiceDTO;
import com.gimmevettingsolution.analyst.exception.InvoiceNotFoundException;
import com.gimmevettingsolution.analyst.service.AnalystService;
import com.gimmevettingsolution.analyst.service.InputValidationService;
import com.gimmevettingsolution.excel.FileBackedExcelStoreService;
import com.gimmevettingsolution.invoice.entity.Invoice;
import com.gimmevettingsolution.invoice.repository.InvoiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Tests for GET /api/v1/analyst/invoices/{id}/source-file endpoint.
 *
 * Covers: 200 OK (xlsx and csv), 404 Not Found (no sourceFileId),
 * 400 Bad Request (invalid id), 500 Internal Server Error (file missing).
 *
 * Red-first discipline: these tests must fail before production code is written.
 */
class SourceFileEndpointTest {

    private MockMvc mockMvc;

    private AnalystService analystService;
    private InputValidationService inputValidationService;
    private InvoiceRepository invoiceRepository;
    private FileBackedExcelStoreService excelStoreService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectory(tempDir.resolve("excel-store"));
        excelStoreService = new FileBackedExcelStoreService(tempDir.resolve("excel-store"));
        analystService = mock(AnalystService.class);
        inputValidationService = mock(InputValidationService.class);
        when(inputValidationService.validateId(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0, Long.class);
            return id != null && id > 0;
        });
        invoiceRepository = mock(InvoiceRepository.class);
        AnalystController controller = new AnalystController(
                analystService, inputValidationService, invoiceRepository, excelStoreService);
        mockMvc = standaloneSetup(controller)
                .build();
    }

    // --- Test 1: 200 OK for xlsx source file ---

    @Test
    void getSourceFile_xlsx_returns200WithCorrectContentType() throws Exception {
        // Save an xlsx file to the store
        byte[] xlsxContent = new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04};
        MockMultipartFile file = new MockMultipartFile(
                "file", "batch-001.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxContent);
        String uuid = excelStoreService.save(file);

        Invoice invoice = createInvoiceWithSourceFile(1L, uuid, "batch-001.xlsx");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        mockMvc.perform(get("/api/v1/analyst/invoices/1/source-file"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        "inline; filename=\"batch-001.xlsx\""))
                .andExpect(content().bytes(xlsxContent));
    }

    // --- Test 2: 200 OK for csv source file ---

    @Test
    void getSourceFile_csv_returns200WithCorrectContentType() throws Exception {
        // Save a csv file to the store
        String csvContent = "invoice number,debtor name\nINV-001,Test";
        MockMultipartFile file = new MockMultipartFile(
                "file", "batch.csv",
                "text/csv",
                csvContent.getBytes());
        String uuid = excelStoreService.save(file);

        Invoice invoice = createInvoiceWithSourceFile(2L, uuid, "batch.csv");
        when(invoiceRepository.findById(2L)).thenReturn(Optional.of(invoice));

        mockMvc.perform(get("/api/v1/analyst/invoices/2/source-file"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition",
                        "inline; filename=\"batch.csv\""))
                .andExpect(content().string(csvContent));
    }

    // --- Test 3: 404 Not Found when sourceFileId is null ---

    @Test
    void getSourceFile_noSourceFileId_returns404() throws Exception {
        Invoice invoice = createInvoiceWithNoSourceFile(3L);
        when(invoiceRepository.findById(3L)).thenReturn(Optional.of(invoice));

        mockMvc.perform(get("/api/v1/analyst/invoices/3/source-file"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("No source file available for this invoice"));
    }

    // --- Test 4: 400 Bad Request for zero id ---

    @Test
    void getSourceFile_zeroId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/analyst/invoices/0/source-file"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid invoice id parameter"));
    }

    // --- Test 5: 400 Bad Request for non-numeric id ---

    @Test
    void getSourceFile_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/analyst/invoices/abc/source-file"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid invoice id parameter"));
    }

    // --- Test 6: 500 Internal Server Error when file missing from store ---

    @Test
    void getSourceFile_fileMissingFromStore_returns500() throws Exception {
        Invoice invoice = createInvoiceWithSourceFile(4L, "nonexistent-uuid-string", "missing.xlsx");
        when(invoiceRepository.findById(4L)).thenReturn(Optional.of(invoice));

        mockMvc.perform(get("/api/v1/analyst/invoices/4/source-file"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Source file is unavailable"));
    }

    // --- Test 7: invoice not found returns 404 ---

    @Test
    void getSourceFile_invoiceNotFound_returns404() throws Exception {
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/analyst/invoices/999/source-file"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("No source file available for this invoice"));
    }

    // --- Test 8: Error response does not expose UUID or store path ---

    @Test
    void getSourceFile_fileMissingFromStore_noLeakOfStorePath() throws Exception {
        Invoice invoice = createInvoiceWithSourceFile(5L, "fake-uuid", "file.xlsx");
        when(invoiceRepository.findById(5L)).thenReturn(Optional.of(invoice));

        mockMvc.perform(get("/api/v1/analyst/invoices/5/source-file"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Source file is unavailable"));
    }

    // --- Test 9: Negative id returns 400 ---

    @Test
    void getSourceFile_negativeId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/analyst/invoices/-1/source-file"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid invoice id parameter"));
    }

    // --- Test 10: store path not leaked in error message ---

    @Test
    void getSourceFile_fileMissing_noStorePathInResponse() throws Exception {
        Invoice invoice = createInvoiceWithSourceFile(6L, "some-uuid", "file.xlsx");
        when(invoiceRepository.findById(6L)).thenReturn(Optional.of(invoice));

        mockMvc.perform(get("/api/v1/analyst/invoices/6/source-file"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Source file is unavailable"));
    }

    // --- Helper methods ---

    private Invoice createInvoiceWithSourceFile(Long id, String sourceFileId, String sourceFilename) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setInvoiceNumber("INV-" + id);
        invoice.setDebtorName("Test Debtor");
        invoice.setAddress("Test Address");
        invoice.setBankAccountNumber("NL12BUNQ0000000000");
        invoice.setPhoneNumber("+31600000000");
        invoice.setPoCStatus("VERIFIED");
        invoice.setRejectionType(null);
        invoice.setStatus("QUEUED");
        invoice.setResubmissionCount(0);
        invoice.setSourceFileId(sourceFileId);
        invoice.setSourceFilename(sourceFilename);
        return invoice;
    }

    private Invoice createInvoiceWithNoSourceFile(Long id) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setInvoiceNumber("INV-" + id);
        invoice.setDebtorName("Test Debtor");
        invoice.setAddress("Test Address");
        invoice.setBankAccountNumber("NL12BUNQ0000000000");
        invoice.setPhoneNumber("+31600000000");
        invoice.setPoCStatus("VERIFIED");
        invoice.setRejectionType(null);
        invoice.setStatus("QUEUED");
        invoice.setResubmissionCount(0);
        invoice.setSourceFileId(null);
        invoice.setSourceFilename(null);
        return invoice;
    }
}
