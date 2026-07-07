package com.gimmevettingsolution.poc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for PoCStoreService — PoC existence verification.
 * Red-first discipline: these tests must fail before production code is written.
 */
class PoCStoreServiceTest {

    private PoCStoreService pocStoreService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create a subdirectory to work in so JUnit's @TempDir is preserved
        Files.createDirectory(tempDir.resolve("poc"));
    }

    @Test
    void hasMatchingPoC_returnsTrue_whenExactCaseMatchExists() throws IOException {
        // Create a PoC file matching the invoice number exactly
        Path pocDir = tempDir.resolve("poc");
        Files.createFile(pocDir.resolve("INV-2026-0042.pdf"));

        pocStoreService = new FileBackedPoCStoreService(pocDir);

        assertTrue(pocStoreService.hasMatchingPoC("INV-2026-0042"));
    }

    @Test
    void hasMatchingPoC_returnsTrue_whenCaseInsensitiveMatchExists() throws IOException {
        // PoC file is lowercase, invoice number is uppercase
        Path pocDir = tempDir.resolve("poc");
        Files.createFile(pocDir.resolve("inv-2026-0044.pdf"));

        pocStoreService = new FileBackedPoCStoreService(pocDir);

        assertTrue(pocStoreService.hasMatchingPoC("INV-2026-0044"));
    }

    @Test
    void hasMatchingPoC_returnsTrue_whenSpecialCharsMatch() throws IOException {
        // Invoice number with hyphens and underscore
        Path pocDir = tempDir.resolve("poc");
        Files.createFile(pocDir.resolve("INV-2026-0045-EU.pdf"));

        pocStoreService = new FileBackedPoCStoreService(pocDir);

        assertTrue(pocStoreService.hasMatchingPoC("INV-2026-0045-EU"));
    }

    @Test
    void hasMatchingPoC_returnsTrue_whenMultipleMatchesExist() throws IOException {
        // Multiple PoC files for one invoice — one match is sufficient
        Path pocDir = tempDir.resolve("poc");
        Files.createFile(pocDir.resolve("INV-2026-0046.pdf"));
        Files.createFile(pocDir.resolve("INV-2026-0046-copy.pdf"));

        pocStoreService = new FileBackedPoCStoreService(pocDir);

        assertTrue(pocStoreService.hasMatchingPoC("INV-2026-0046"));
    }

    @Test
    void hasMatchingPoC_returnsFalse_whenNoPoCExists() throws IOException {
        Path pocDir = tempDir.resolve("poc");

        pocStoreService = new FileBackedPoCStoreService(pocDir);

        assertFalse(pocStoreService.hasMatchingPoC("INV-2026-0043"));
    }

    @Test
    void hasMatchingPoC_returnsFalse_whenPoCStoreDirectoryDoesNotExist() {
        Path nonExistentDir = Path.of("/nonexistent/path/that/does/not/exist");

        pocStoreService = new FileBackedPoCStoreService(nonExistentDir);

        assertFalse(pocStoreService.hasMatchingPoC("INV-2026-0042"));
    }

    @Test
    void hasMatchingPoC_returnsFalse_whenOnlyPrefixMatches() throws IOException {
        // Partial match should NOT count — only full filename match
        Path pocDir = tempDir.resolve("poc");
        Files.createFile(pocDir.resolve("INV-2026-0042-partial.pdf"));

        pocStoreService = new FileBackedPoCStoreService(pocDir);

        assertFalse(pocStoreService.hasMatchingPoC("INV-2026-0042"));
    }

    @Test
    void hasMatchingPoC_rejectsPathTraversalInInvoiceNumber() throws IOException {
        Path pocDir = tempDir.resolve("poc");
        Files.createFile(pocDir.resolve("INV-2026-0042.pdf"));

        pocStoreService = new FileBackedPoCStoreService(pocDir);

        assertFalse(pocStoreService.hasMatchingPoC("../etc/passwd"));
    }

    @Test
    void hasMatchingPoC_rejectsSlashInInvoiceNumber() throws IOException {
        Path pocDir = tempDir.resolve("poc");
        Files.createFile(pocDir.resolve("INV-2026-0042.pdf"));

        pocStoreService = new FileBackedPoCStoreService(pocDir);

        assertFalse(pocStoreService.hasMatchingPoC("INV/2026/0042"));
    }

    @Test
    void hasMatchingPoC_rejectsBackslashInInvoiceNumber() throws IOException {
        Path pocDir = tempDir.resolve("poc");
        Files.createFile(pocDir.resolve("INV-2026-0042.pdf"));

        pocStoreService = new FileBackedPoCStoreService(pocDir);

        assertFalse(pocStoreService.hasMatchingPoC("INV\\2026\\0042"));
    }

}
