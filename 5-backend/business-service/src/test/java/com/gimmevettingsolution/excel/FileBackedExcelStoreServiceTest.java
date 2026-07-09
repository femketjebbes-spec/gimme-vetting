package com.gimmevettingsolution.excel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileBackedExcelStoreService.
 * Covers: save operation, getFile operation, format validation (.xlsx and .csv only),
 * MIME type validation, header injection prevention, and file size limit (50MB).
 *
 * Red-first discipline: these tests must fail before production code is written.
 */
class FileBackedExcelStoreServiceTest {

    private FileBackedExcelStoreService excelStoreService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectory(tempDir.resolve("excel-store"));
    }

    // --- save() tests ---

    @Test
    void save_acceptsXlsxFile_andReturnsUuid() throws IOException {
        Path storePath = tempDir.resolve("excel-store");
        byte[] xlsxContent = new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04}; // ZIP header
        MockMultipartFile file = new MockMultipartFile(
                "file", "batch-001.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxContent);

        excelStoreService = new FileBackedExcelStoreService(storePath);

        String uuid = excelStoreService.save(file);

        assertNotNull(uuid);
        assertDoesNotThrow(() -> UUID.fromString(uuid));
        assertTrue(Files.exists(storePath.resolve(uuid)));
    }

    @Test
    void save_acceptsCsvFile_andReturnsUuid() throws IOException {
        Path storePath = tempDir.resolve("excel-store");
        String csvContent = "invoice number,debtor name\nINV-001,Test";
        MockMultipartFile file = new MockMultipartFile(
                "file", "batch-002.csv",
                "text/csv",
                csvContent.getBytes());

        excelStoreService = new FileBackedExcelStoreService(storePath);

        String uuid = excelStoreService.save(file);

        assertNotNull(uuid);
        assertDoesNotThrow(() -> UUID.fromString(uuid));
        assertTrue(Files.exists(storePath.resolve(uuid)));
    }

    @Test
    void save_rejectsNonExcelMimeType() throws IOException {
        Path storePath = tempDir.resolve("excel-store");
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf",
                "application/pdf",
                "pdf-content".getBytes());

        excelStoreService = new FileBackedExcelStoreService(storePath);

        assertThrows(IllegalArgumentException.class, () -> excelStoreService.save(file));
    }

    @Test
    void save_rejectsImageFile() throws IOException {
        Path storePath = tempDir.resolve("excel-store");
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        excelStoreService = new FileBackedExcelStoreService(storePath);

        assertThrows(IllegalArgumentException.class, () -> excelStoreService.save(file));
    }

    @Test
    void save_rejectsExceeding50MB() throws IOException {
        Path storePath = tempDir.resolve("excel-store");
        // Create a file just over 50MB (50 * 1024 * 1024 + 1 bytes)
        long fiftyMB = 50L * 1024 * 1024;
        byte[] bigContent = new byte[(int) (fiftyMB + 1)];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bigContent);

        excelStoreService = new FileBackedExcelStoreService(storePath);

        assertThrows(IllegalArgumentException.class, () -> excelStoreService.save(file));
    }

    @Test
    void save_acceptsExactly50MB() throws IOException {
        Path storePath = tempDir.resolve("excel-store");
        long fiftyMB = 50L * 1024 * 1024;
        byte[] content = new byte[(int) fiftyMB];
        MockMultipartFile file = new MockMultipartFile(
                "file", "exact.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                content);

        excelStoreService = new FileBackedExcelStoreService(storePath);

        String uuid = excelStoreService.save(file);

        assertNotNull(uuid);
        assertDoesNotThrow(() -> UUID.fromString(uuid));
    }

    @Test
    void save_rejectsFilenameWithNewlinesAndSemicolons() throws IOException {
        Path storePath = tempDir.resolve("excel-store");
        byte[] xlsxContent = new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04};
        MockMultipartFile file = new MockMultipartFile(
                "file", "batch;evil\n.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxContent);

        excelStoreService = new FileBackedExcelStoreService(storePath);

        // Must throw IllegalArgumentException for header-injection filename
        assertThrows(IllegalArgumentException.class, () -> excelStoreService.save(file));
    }

    @Test
    void save_generatesUniqueUuidPerFile() throws IOException {
        Path storePath = tempDir.resolve("excel-store");
        byte[] xlsxContent = new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04};
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "batch-001.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxContent);
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "batch-002.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxContent);

        excelStoreService = new FileBackedExcelStoreService(storePath);

        String uuid1 = excelStoreService.save(file1);
        String uuid2 = excelStoreService.save(file2);

        assertNotEquals(uuid1, uuid2);
        assertTrue(Files.exists(storePath.resolve(uuid1)));
        assertTrue(Files.exists(storePath.resolve(uuid2)));
    }

    // --- getFile() tests ---

    @Test
    void getFile_returnsResourceForSavedUuid() throws IOException {
        Path storePath = tempDir.resolve("excel-store");
        byte[] xlsxContent = new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04};
        MockMultipartFile file = new MockMultipartFile(
                "file", "batch-001.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxContent);

        excelStoreService = new FileBackedExcelStoreService(storePath);
        String uuid = excelStoreService.save(file);

        Resource resource = excelStoreService.getFile(uuid);

        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void getFile_throwsExceptionForNonExistentUuid() throws IOException {
        Path storePath = tempDir.resolve("excel-store");
        excelStoreService = new FileBackedExcelStoreService(storePath);

        assertThrows(RuntimeException.class, () -> excelStoreService.getFile("nonexistent-uuid"));
    }

    @Test
    void getFile_returnsCorrectContent() throws IOException {
        Path storePath = tempDir.resolve("excel-store");
        String csvContent = "invoice number,debtor name\nINV-001,Test";
        MockMultipartFile file = new MockMultipartFile(
                "file", "batch.csv",
                "text/csv",
                csvContent.getBytes());

        excelStoreService = new FileBackedExcelStoreService(storePath);
        String uuid = excelStoreService.save(file);

        Resource resource = excelStoreService.getFile(uuid);

        String readContent = Files.readString(resource.getFile().toPath());
        assertEquals(csvContent, readContent);
    }

    // --- sanitizeFilename() tests ---

    @Test
    void sanitizeFilename_rejectsNewlines() {
        excelStoreService = new FileBackedExcelStoreService(tempDir.resolve("excel-store"));

        assertThrows(IllegalArgumentException.class, () -> excelStoreService.sanitizeFilename("batch\r\n.xlsx"));
    }

    @Test
    void sanitizeFilename_rejectsSemicolons() {
        excelStoreService = new FileBackedExcelStoreService(tempDir.resolve("excel-store"));

        assertThrows(IllegalArgumentException.class, () -> excelStoreService.sanitizeFilename("batch;download.xlsx"));
    }

    @Test
    void sanitizeFilename_rejectsControlCharacters() {
        excelStoreService = new FileBackedExcelStoreService(tempDir.resolve("excel-store"));

        assertThrows(IllegalArgumentException.class, () -> excelStoreService.sanitizeFilename("batch\u0001.xlsx"));
    }

    @Test
    void sanitizeFilename_allowsAlphanumericWithHyphensUnderscoresSpaces() {
        excelStoreService = new FileBackedExcelStoreService(tempDir.resolve("excel-store"));

        String sanitized = excelStoreService.sanitizeFilename("batch-001_valid file.xlsx");

        assertEquals("batch-001_valid file.xlsx", sanitized);
    }

    @Test
    void sanitizeFilename_nullInput_rejects() {
        excelStoreService = new FileBackedExcelStoreService(tempDir.resolve("excel-store"));

        assertThrows(IllegalArgumentException.class, () -> excelStoreService.sanitizeFilename(null));
    }

    @Test
    void sanitizeFilename_emptyInput_rejects() {
        excelStoreService = new FileBackedExcelStoreService(tempDir.resolve("excel-store"));

        assertThrows(IllegalArgumentException.class, () -> excelStoreService.sanitizeFilename(""));
    }
}
