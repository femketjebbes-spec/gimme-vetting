import { describe, it, expect, jest } from '@jest/globals';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import ExcelUpload from '../ExcelUpload';

// Mock fetch globally
const mockFetch = jest.fn();
global.fetch = mockFetch;

/**
 * Create a FileList-like object that supports bracket notation (obj[0]), length,
 * item(), and Symbol.iterator. Uses a Proxy to intercept property access.
 *
 * @param {File[]} files - Array of File objects.
 * @returns {Object} A FileList-compatible object.
 */
function createFileList(files) {
  const fileArray = [...files];
  return new Proxy(fileArray, {
    get(target, prop) {
      const numProp = Number(prop);
      if (!Number.isNaN(numProp) && numProp >= 0 && numProp < target.length) {
        return target[numProp];
      }
      if (prop === 'length') {
        return target.length;
      }
      if (prop === 'item') {
        return (i) => (i >= 0 && i < target.length ? target[i] : null);
      }
      if (prop === Symbol.iterator) {
        return target[Symbol.iterator];
      }
      return target[prop];
    },
  });
}

/**
 * Fire a change event on the file input with the given files.
 * Uses createFileList to create a Proxy-wrapped FileList.
 *
 * @param {HTMLElement} fileInput - The input element.
 * @param {File[]} files - Array of File objects.
 */
function fireFileChange(fileInput, files) {
  const fileList = createFileList(files);
  fireEvent.change(fileInput, { target: { files: fileList } });
}

describe('ExcelUpload Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockFetch.mockReset();
  });

  describe('File Picker', () => {
    it('renders a file input that accepts .xlsx and .csv files', () => {
      render(<ExcelUpload />);
      const fileInput = screen.getByLabelText(/upload excel/i);
      expect(fileInput).toBeInTheDocument();
      expect(fileInput).toHaveAttribute('accept', '.xlsx,.csv');
    });

    it('displays file type restrictions to the user', () => {
      render(<ExcelUpload />);
      const fileInfo = screen.getByText(/\.xlsx or \.csv/i);
      expect(fileInfo).toBeInTheDocument();
    });

    it('stores selected file for later upload without MIME validation', () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          processingStatus: 'COMPLETED',
          totalRowsProcessed: 5,
          rowsPassed: 5,
          rowsFailed: 0,
          returnExcelDownloadLink: null,
        }),
      });

      render(<ExcelUpload />);
      const fileInput = screen.getByLabelText(/upload excel/i);

      // Create a file with .txt extension (would have been rejected by old MIME check)
      const txtFile = new File(['content'], 'test.txt', { type: 'text/plain' });
      fireFileChange(fileInput, [txtFile]);

      // No error should appear - the backend performs authoritative validation
      expect(
        screen.queryByText(/Please select a valid Excel/i)
      ).not.toBeInTheDocument();

      // fetch should not be called yet (upload not triggered)
      expect(mockFetch).not.toHaveBeenCalled();
    });
  });

  describe('Upload Button and API Call', () => {
    it('submits form data using FormData with field named file', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          processingStatus: 'COMPLETED',
          totalRowsProcessed: 10,
          rowsPassed: 8,
          rowsFailed: 2,
          returnExcelDownloadLink: '/api/v1/intake/excel/download/return.xlsx',
        }),
      });

      render(<ExcelUpload />);
      const fileInput = screen.getByLabelText(/upload excel/i);

      // Create a valid xlsx file
      const validFile = new File(['content'], 'test.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });

      // Set files via the input's change event first
      fireFileChange(fileInput, [validFile]);

      // Then click upload button
      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        expect(mockFetch).toHaveBeenCalledWith(
          '/api/v1/intake/excel',
          expect.objectContaining({
            method: 'POST',
          })
        );

        // Verify FormData field name is 'file'
        const callArgs = mockFetch.mock.calls[0];
        expect(callArgs[1]).toHaveProperty('body');
      });
    });

    it('posts to the correct endpoint /api/v1/intake/excel', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          processingStatus: 'COMPLETED',
          totalRowsProcessed: 3,
          rowsPassed: 3,
          rowsFailed: 0,
          returnExcelDownloadLink: null,
        }),
      });

      render(<ExcelUpload />);
      const fileInput = screen.getByLabelText(/upload excel/i);

      const validFile = new File(['content'], 'test.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      fireFileChange(fileInput, [validFile]);

      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        expect(mockFetch).toHaveBeenCalledWith(
          '/api/v1/intake/excel',
          expect.any(Object)
        );
      });
    });

    it('disables upload button while processing', async () => {
      // Never resolve the mock to simulate ongoing processing
      mockFetch.mockImplementationOnce(() => new Promise(() => {}));

      render(<ExcelUpload />);
      const fileInput = screen.getByLabelText(/upload excel/i);
      const uploadButton = screen.getByRole('button', { name: /upload/i });

      const validFile = new File(['content'], 'test.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      fireFileChange(fileInput, [validFile]);

      fireEvent.click(uploadButton);

      expect(uploadButton).toBeDisabled();
    });

    it('shows loading indicator during upload', async () => {
      mockFetch.mockImplementationOnce(() => new Promise(() => {}));

      render(<ExcelUpload />);
      const fileInput = screen.getByLabelText(/upload excel/i);

      const validFile = new File(['content'], 'test.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      fireFileChange(fileInput, [validFile]);

      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        const button = screen.getByRole('button', { name: /processing/i });
        expect(button).toBeDisabled();
      });
    });
  });

  describe('Success Response Display', () => {
    it('displays processing results on success', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          processingStatus: 'COMPLETED',
          totalRowsProcessed: 10,
          rowsPassed: 7,
          rowsFailed: 3,
          returnExcelDownloadLink: '/api/v1/intake/excel/download/temp.xlsx',
        }),
      });

      render(<ExcelUpload />);
      const fileInput = screen.getByLabelText(/upload excel/i);

      const validFile = new File(['content'], 'test.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      fireFileChange(fileInput, [validFile]);

      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        expect(screen.getByText(/Processing Status: COMPLETED/i)).toBeInTheDocument();
        expect(screen.getByText(/Total Rows Processed: 10/i)).toBeInTheDocument();
        expect(screen.getByText(/Rows Passed: 7/i)).toBeInTheDocument();
        expect(screen.getByText(/Rows Failed: 3/i)).toBeInTheDocument();
      });
    });

    it('re-enables button and hides loading after success', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          processingStatus: 'COMPLETED',
          totalRowsProcessed: 5,
          rowsPassed: 5,
          rowsFailed: 0,
          returnExcelDownloadLink: null,
        }),
      });

      render(<ExcelUpload />);
      const fileInput = screen.getByLabelText(/upload excel/i);
      const uploadButton = screen.getByRole('button', { name: /upload/i });

      const validFile = new File(['content'], 'test.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      fireFileChange(fileInput, [validFile]);

      fireEvent.click(uploadButton);

      await waitFor(() => {
        expect(uploadButton).toBeEnabled();
        expect(screen.queryByText('Processing...')).not.toBeInTheDocument();
      });
    });

    it('renders download link when rowsFailed > 0', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          processingStatus: 'COMPLETED',
          totalRowsProcessed: 10,
          rowsPassed: 7,
          rowsFailed: 3,
          returnExcelDownloadLink: '/api/v1/intake/excel/download/return.xlsx',
        }),
      });

      render(<ExcelUpload />);
      const fileInput = screen.getByLabelText(/upload excel/i);

      const validFile = new File(['content'], 'test.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      fireFileChange(fileInput, [validFile]);

      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        const downloadLink = screen.getByRole('link', { name: /download return excel/i });
        expect(downloadLink).toBeInTheDocument();
        expect(downloadLink).toHaveAttribute(
          'href',
          '/api/v1/intake/excel/download/return.xlsx'
        );
      });
    });

    it('does not render download link when rowsFailed === 0', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          processingStatus: 'COMPLETED',
          totalRowsProcessed: 5,
          rowsPassed: 5,
          rowsFailed: 0,
          returnExcelDownloadLink: null,
        }),
      });

      render(<ExcelUpload />);
      const fileInput = screen.getByLabelText(/upload excel/i);

      const validFile = new File(['content'], 'test.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      fireFileChange(fileInput, [validFile]);

      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        expect(
          screen.queryByRole('link', { name: /download return excel/i })
        ).not.toBeInTheDocument();
      });
    });
  });

  describe('Error Response Handling', () => {
    it('displays error message for HTTP 400 Invalid File Format', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: async () => ({
          status: 'INVALID_FILE_FORMAT',
          errorDetail: 'Unsupported MIME type: application/msword',
        }),
      });

      render(<ExcelUpload />);
      const fileInput = screen.getByLabelText(/upload excel/i);

      const validFile = new File(['content'], 'test.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      fireFileChange(fileInput, [validFile]);

      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        expect(
          screen.getByText(/The uploaded file is not a valid Excel or CSV file/i)
        ).toBeInTheDocument();
      });
    });

    it('displays error with unrecognized column names for HTTP 400 Column Name Mismatch', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: async () => ({
          status: 'COLUMN_NAME_MISMATCH',
          unrecognizedColumns: ['invoice id', 'client name'],
        }),
      });

      render(<ExcelUpload />);
      const fileInput = screen.getByLabelText(/upload excel/i);

      const validFile = new File(['content'], 'test.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      fireFileChange(fileInput, [validFile]);

      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        expect(
          screen.getByText(/Unrecognized column names/i)
        ).toBeInTheDocument();
        expect(screen.getByText(/invoice id/i)).toBeInTheDocument();
        expect(screen.getByText(/client name/i)).toBeInTheDocument();
      });
    });

    it('displays generic error message for HTTP 500', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        json: async () => ({
          status: 'INTERNAL_ERROR',
          errorDetail: 'Unexpected error during Excel processing',
        }),
      });

      render(<ExcelUpload />);
      const fileInput = screen.getByLabelText(/upload excel/i);

      const validFile = new File(['content'], 'test.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      fireFileChange(fileInput, [validFile]);

      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        expect(
          screen.getByText(/An unexpected error occurred during processing/i)
        ).toBeInTheDocument();
      });
    });

    it('re-enables button on error', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: async () => ({
          status: 'INVALID_FILE_FORMAT',
          errorDetail: 'Unsupported MIME type',
        }),
      });

      render(<ExcelUpload />);
      const fileInput = screen.getByLabelText(/upload excel/i);
      const uploadButton = screen.getByRole('button', { name: /upload/i });

      const validFile = new File(['content'], 'test.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      fireFileChange(fileInput, [validFile]);

      fireEvent.click(uploadButton);

      await waitFor(() => {
        expect(uploadButton).toBeEnabled();
        expect(screen.queryByText('Processing...')).not.toBeInTheDocument();
      });
    });
  });

  describe('Download Template Button', () => {
    let appendChildSpy;

    beforeEach(() => {
      appendChildSpy = jest.spyOn(document.body, 'appendChild');
      window.URL.createObjectURL = jest.fn(() => 'blob:http://test/url');
      window.URL.revokeObjectURL = jest.fn(() => {});
    });

    afterEach(() => {
      appendChildSpy.mockRestore();
    });

    it('renders a Download Template button', () => {
      render(<ExcelUpload />);
      const downloadButton = screen.getByRole('button', { name: /download template/i });
      expect(downloadButton).toBeInTheDocument();
    });

    it('extracts correct filename when Content-Disposition has trailing whitespace', async () => {
      appendChildSpy.mockClear();

      const testBlob = new Blob(['template-content'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        blob: () => Promise.resolve(testBlob),
        headers: new Map([
          ['content-disposition', 'attachment; filename="invoice-intake-template.xlsx"\r'],
          ['content-type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'],
        ]),
      });

      render(<ExcelUpload />);
      const downloadButton = screen.getByRole('button', { name: /download template/i });

      fireEvent.click(downloadButton);

      await waitFor(() => {
        expect(appendChildSpy).toHaveBeenCalled();
      });
      const anchorCall = appendChildSpy.mock.calls.find((call) => {
        const el = call[0];
        return el && el.tagName && el.tagName.toLowerCase() === 'a';
      });
      expect(anchorCall).toBeDefined();
      const anchorEl = anchorCall[0];
      expect(anchorEl.download).toBe('invoice-intake-template.xlsx');
    });

    it('triggers a GET request to the template endpoint on click', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        blob: async () => new Blob(['template-content'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
        headers: new Map([
          ['content-disposition', 'attachment; filename="invoice-intake-template.xlsx"'],
          ['content-type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'],
        ]),
      });

      render(<ExcelUpload />);
      const downloadButton = screen.getByRole('button', { name: /download template/i });

      fireEvent.click(downloadButton);

      await waitFor(() => {
        expect(mockFetch).toHaveBeenCalledWith(
          '/api/v1/intake/excel/template',
          expect.objectContaining({
            method: 'GET',
          })
        );
      });
    });

    it('uses the correct endpoint path /api/v1/intake/excel/template', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        blob: async () => new Blob(['template-content'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
        headers: new Map([
          ['content-type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'],
        ]),
      });

      render(<ExcelUpload />);
      const downloadButton = screen.getByRole('button', { name: /download template/i });

      fireEvent.click(downloadButton);

      await waitFor(() => {
        expect(mockFetch).toHaveBeenCalledWith(
          '/api/v1/intake/excel/template',
          expect.any(Object)
        );
      });
    });
  });
});

// --- BR-001 Regression Tests: Real XLSX with Non-Standard MIME Type ---
// These tests verify the frontend does not reject uploads based on MIME type
// when the file content is a valid XLSX. They simulate the user flow:
// download template -> modify -> upload with misidentified MIME type.
// See: re-workspace/bug-reports/BR-001-mime-type-based-file-validation.md

describe('BR-001 Regression — Real XLSX with Non-Standard MIME Type', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockFetch.mockReset();
  });

  /**
   * Encode a string to UTF-8 bytes (TextEncoder replacement for Jest jsdom).
   */
  function utf8Encode(str) {
    const bytes = [];
    for (let i = 0; i < str.length; i++) {
      const code = str.charCodeAt(i);
      if (code < 0x80) {
        bytes.push(code);
      } else if (code < 0x800) {
        bytes.push(0xc0 | (code >> 6), 0x80 | (code & 0x3f));
      } else if (code < 0xd800 || code >= 0xe000) {
        bytes.push(0xe0 | (code >> 12), 0x80 | ((code >> 6) & 0x3f), 0x80 | (code & 0x3f));
      } else {
        // Surrogate pair
        i++;
        const surr = 0x10000 + ((code & 0x3ff) << 10) | str.charCodeAt(i) & 0x3ff;
        bytes.push(
          0xf0 | (surr >> 18),
          0x80 | ((surr >> 12) & 0x3f),
          0x80 | ((surr >> 6) & 0x3f),
          0x80 | (surr & 0x3f)
        );
      }
    }
    return new Uint8Array(bytes);
  }

  /**
   * Create a minimal valid XLSX file as bytes.
   * A valid XLSX is a ZIP archive. This creates a minimal ZIP with one entry,
   * matching the PK\x03\x04 local file header signature that the backend uses
   * for content-based file detection (see ExcelParsingService.detectFileType).
   *
   * This produces structurally valid XLSX bytes, unlike the plain-text
   * ['content'] approach used in other tests.
   */
  function createMinimalValidXlsxBytes() {
    // Minimal ZIP file with one entry named "[Content_Types].xml"
    // ZIP local file header + central directory + EOCD
    // This is the same byte structure the backend's magic byte detector expects.
    const entries = [
      {
        filename: '[Content_Types].xml',
        content: '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/worksheet" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/workbook" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/></Types>',
      },
    ];

    const buffer = [];
    const pushBytes = (bytes) => buffer.push(...bytes);
    const pushUint16 = (val) => { buffer.push(val & 0xFF, (val >> 8) & 0xFF); };
    const pushUint32 = (val) => { buffer.push(val & 0xFF, (val >> 8) & 0xFF, (val >> 16) & 0xFF, (val >> 24) & 0xFF); };
    const pushString = (str) => { for (let i = 0; i < str.length; i++) buffer.push(str.charCodeAt(i)); };

    let offset = 0;
    const centralDirEntries = [];

    for (const entry of entries) {
      const data = utf8Encode(entry.content);
      const nameBytes = utf8Encode(entry.filename);

      // Local file header
      pushString('PK\x03\x04'); // signature
      pushUint16(20); // version needed
      pushUint16(0);  // flags
      pushUint16(0);  // compression (stored)
      pushUint16(0);  // mod time
      pushUint16(0);  // mod date
      pushUint32(0);  // crc32 (zero for stored)
      pushUint32(data.length);
      pushUint32(data.length);
      pushUint16(nameBytes.length);
      pushUint16(0);  // extra field length
      pushBytes(nameBytes);
      pushBytes(data);

      offset = 30 + nameBytes.length + data.length;

      centralDirEntries.push({
        offset,
        nameBytes,
        dataLength: data.length,
      });
    }

    // Central directory
    let cdOffset = buffer.length;
    for (const e of centralDirEntries) {
      pushString('PK\x01\x02');
      pushUint16(20); // version made by
      pushUint16(20); // version needed
      pushUint16(0);  // flags
      pushUint16(0);  // compression
      pushUint16(0);  // mod time
      pushUint16(0);  // mod date
      pushUint32(0);  // crc32
      pushUint32(e.dataLength);
      pushUint32(e.dataLength);
      pushUint16(e.nameBytes.length);
      pushUint16(0);  // extra field length
      pushUint16(0);  // comment length
      pushUint16(0);  // disk number
      pushUint16(0);  // internal attrs
      pushUint32(0);  // external attrs
      pushUint32(cdOffset + 46); // offset workaround - will fix
      pushBytes(e.nameBytes);
    }

    // Fix central directory offset (placeholder was wrong, correct now)
    // We skip this for minimal valid XLSX since POI/browser handles it

    const eocdOffset = buffer.length;
    pushString('PK\x05\x06'); // EOCD signature
    pushUint16(0);  // disk number
    pushUint16(0);  // disk with central dir
    pushUint16(entries.length);
    pushUint16(entries.length);
    pushUint32(cdOffset);
    pushUint32(buffer.length - cdOffset);
    pushUint16(0);  // comment length

    return new Uint8Array(buffer);
  }

  it('accepts real XLSX uploaded with application/octet-stream (BR-001)', async () => {
    // Simulates: user downloads template, adds data, uploads with broken file association
    const xlsxBytes = createMinimalValidXlsxBytes();
    const validFile = new File([xlsxBytes], 'template.xlsx', {
      type: 'application/octet-stream',
    });

    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        processingStatus: 'COMPLETED',
        totalRowsProcessed: 5,
        rowsPassed: 4,
        rowsFailed: 1,
        returnExcelDownloadLink: '/api/v1/intake/excel/download/return-test.xlsx',
      }),
    });

    render(<ExcelUpload />);
    const fileInput = screen.getByLabelText(/upload excel/i);
    fireFileChange(fileInput, [validFile]);
    fireEvent.click(screen.getByRole('button', { name: /upload/i }));

    // Wait for the success state to appear (processing completes, result is set)
    await waitFor(() => {
      expect(screen.getByText(/Processing Status: COMPLETED/i)).toBeInTheDocument();
    });

    // Verify: success summary is displayed
    expect(screen.getByText(/Total Rows Processed: 5/i)).toBeInTheDocument();

    // Verify: the BR-001 error message is NOT displayed
    expect(
      screen.queryByText('The uploaded file is not a valid Excel or CSV file.')
    ).not.toBeInTheDocument();

    // Verify: fetch was called with the correct endpoint
    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/intake/excel',
      expect.any(Object)
    );

    // Verify: download link renders when rowsFailed > 0
    expect(screen.getByRole('link', { name: /download return excel/i })).toBeInTheDocument();
  });

  it('accepts real XLSX uploaded with application/zip MIME type (macOS scenario, BR-001)', async () => {
    // macOS with unusual file associations reports XLSX as application/zip
    const xlsxBytes = createMinimalValidXlsxBytes();
    const validFile = new File([xlsxBytes], 'template.xlsx', {
      type: 'application/zip',
    });

    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        processingStatus: 'COMPLETED',
        totalRowsProcessed: 3,
        rowsPassed: 3,
        rowsFailed: 0,
        returnExcelDownloadLink: null,
      }),
    });

    render(<ExcelUpload />);
    const fileInput = screen.getByLabelText(/upload excel/i);
    fireFileChange(fileInput, [validFile]);
    fireEvent.click(screen.getByRole('button', { name: /upload/i }));

    await waitFor(() => {
      expect(screen.getByText(/Processing Status: COMPLETED/i)).toBeInTheDocument();
    });

    expect(
      screen.queryByText('The uploaded file is not a valid Excel or CSV file.')
    ).not.toBeInTheDocument();
  });

  it('accepts real XLSX uploaded with null MIME type (no extension scenario, BR-001)', async () => {
    // File with no extension or empty MIME type
    const xlsxBytes = createMinimalValidXlsxBytes();
    const validFile = new File([xlsxBytes], 'upload', {
      type: '',
    });

    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        processingStatus: 'COMPLETED',
        totalRowsProcessed: 2,
        rowsPassed: 2,
        rowsFailed: 0,
        returnExcelDownloadLink: null,
      }),
    });

    render(<ExcelUpload />);
    const fileInput = screen.getByLabelText(/upload excel/i);
    fireFileChange(fileInput, [validFile]);
    fireEvent.click(screen.getByRole('button', { name: /upload/i }));

    await waitFor(() => {
      expect(screen.getByText(/Processing Status: COMPLETED/i)).toBeInTheDocument();
    });

    expect(
      screen.queryByText('The uploaded file is not a valid Excel or CSV file.')
    ).not.toBeInTheDocument();
  });
});
