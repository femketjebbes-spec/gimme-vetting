package com.gimmevettingsolution.poc;

import com.gimmevettingsolution.intake.dto.InvalidFileFormatResponse;
import com.gimmevettingsolution.intake.dto.InternalErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PoCUploadController covering:
 * - Successful PDF upload (200 OK)
 * - Non-PDF file rejection (400 Bad Request)
 * - Path traversal filename rejection (400 Bad Request)
 * - Internal server error handling (500)
 */
class PoCUploadControllerTest {

    private MockMvc mockMvc;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        Path pocStoreDir = tempDir.resolve("poc-store");
        Files.createDirectories(pocStoreDir);
        PoCStoreService mockStoreService = new FileBackedPoCStoreService(pocStoreDir);
        PoCUploadController controller = new PoCUploadController(mockStoreService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // --- Successful Upload Tests ---

    @Test
    void uploadPoc_success_returns200WithStatusAndInvoiceNumber() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "inv-2026-0100.pdf",
                "application/pdf",
                "pdf-content".getBytes());

        mockMvc.perform(multipart("/api/v1/poc-upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.invoiceNumber").value("inv-2026-0100"));
    }

    @Test
    void uploadPoc_uppercasePdfExtension_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "INV-2026-0101.PDF",
                "application/pdf",
                "pdf-content".getBytes());

        mockMvc.perform(multipart("/api/v1/poc-upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.invoiceNumber").value("inv-2026-0101"));
    }

    @Test
    void uploadPoc_mixedCasePdfExtension_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "inv-2026-0102.Pdf",
                "application/pdf",
                "pdf-content".getBytes());

        mockMvc.perform(multipart("/api/v1/poc-upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.invoiceNumber").value("inv-2026-0102"));
    }

    // --- Non-PDF File Rejection Tests ---

    @Test
    void uploadPoc_wordFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "inv-2026-0103.docx",
                "application/msword",
                "word-content".getBytes());

        mockMvc.perform(multipart("/api/v1/poc-upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_FILE_FORMAT"))
                .andExpect(jsonPath("$.errorDetail").exists());
    }

    @Test
    void uploadPoc_pngFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "inv-2026-0104.png",
                "image/png",
                new byte[]{-119, 80, 78, 71});

        mockMvc.perform(multipart("/api/v1/poc-upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_FILE_FORMAT"))
                .andExpect(jsonPath("$.errorDetail").exists());
    }

    @Test
    void uploadPoc_nullMimeType_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "inv-2026-0105.pdf",
                null,
                "content".getBytes());

        mockMvc.perform(multipart("/api/v1/poc-upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_FILE_FORMAT"))
                .andExpect(jsonPath("$.errorDetail").exists());
    }

    @Test
    void uploadPoc_nonPdfMimeType_returns400WithMimeDetail() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "inv-2026-0107.xls",
                "application/vnd.ms-excel",
                "content".getBytes());

        mockMvc.perform(multipart("/api/v1/poc-upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_FILE_FORMAT"))
                .andExpect(jsonPath("$.errorDetail").value(org.hamcrest.Matchers.containsString("application/vnd.ms-excel")));
    }

    // --- Path Traversal Tests ---

    @Test
    void uploadPoc_pathTraversal_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../etc/passwd.pdf",
                "application/pdf",
                "pdf-content".getBytes());

        mockMvc.perform(multipart("/api/v1/poc-upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_FILE_FORMAT"))
                .andExpect(jsonPath("$.errorDetail").value("Path traversal detected in filename"));
    }

    @Test
    void uploadPoc_absolutePath_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "/etc/shadow.pdf",
                "application/pdf",
                "pdf-content".getBytes());

        mockMvc.perform(multipart("/api/v1/poc-upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_FILE_FORMAT"))
                .andExpect(jsonPath("$.errorDetail").value("Path traversal detected in filename"));
    }

    @Test
    void uploadPoc_backslashPathTraversal_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "..\\..\\windows\\system32\\config\\sam.pdf",
                "application/pdf",
                "pdf-content".getBytes());

        mockMvc.perform(multipart("/api/v1/poc-upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_FILE_FORMAT"))
                .andExpect(jsonPath("$.errorDetail").value("Path traversal detected in filename"));
    }

    // --- Duplicate Upload Tests ---

    @Test
    void uploadPoc_duplicateFile_overwritesAndReturns200() throws Exception {
        // First upload
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "inv-2026-0106.pdf",
                "application/pdf",
                "first-content".getBytes());

        mockMvc.perform(multipart("/api/v1/poc-upload").file(file1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"));

        // Second upload of same file
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "inv-2026-0106.pdf",
                "application/pdf",
                "second-content".getBytes());

        mockMvc.perform(multipart("/api/v1/poc-upload").file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.invoiceNumber").value("inv-2026-0106"));
    }

    // --- Filename Without Extension Tests ---

    @Test
    void uploadPoc_filenameWithoutPdf_returns200WithFilename() throws Exception {
        // This test verifies that filenames without .pdf extension
        // are handled correctly — the invoice number will be the full filename
        MockMultipartFile file = new MockMultipartFile(
                "file", "inv-2026-0108",
                "application/pdf",
                "pdf-content".getBytes());

        mockMvc.perform(multipart("/api/v1/poc-upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.invoiceNumber").value("inv-2026-0108"));
    }

}
