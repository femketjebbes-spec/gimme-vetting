package com.gimmevettingsolution.intake.service;

/**
 * Request-scoped holder for the source file UUID from Excel batch upload.
 * Allows IntakeServiceImpl to associate single-invoice submissions with
 * the originating Excel batch file.
 */
public class SourceFileContext {

    private static final ThreadLocal<String> SOURCE_FILE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> SOURCE_FILENAME = new ThreadLocal<>();

    public static void setSourceFileId(String sourceFileId, String sourceFilename) {
        SOURCE_FILE_ID.set(sourceFileId);
        SOURCE_FILENAME.set(sourceFilename);
    }

    public static String getSourceFileId() {
        return SOURCE_FILE_ID.get();
    }

    public static String getSourceFilename() {
        return SOURCE_FILENAME.get();
    }

    public static void clear() {
        SOURCE_FILE_ID.remove();
        SOURCE_FILENAME.remove();
    }
}
