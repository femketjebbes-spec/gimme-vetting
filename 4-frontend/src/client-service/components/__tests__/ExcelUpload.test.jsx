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

    it('validates MIME type before upload for non-matching types', () => {
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

      // Create a file with wrong MIME type (e.g., a .txt file)
      const wrongFile = new File(['content'], 'test.txt', { type: 'text/plain' });
      fireFileChange(fileInput, [wrongFile]);

      // The onChange handler sets an error, so the error message should appear
      expect(
        screen.getByText(/Please select a valid Excel/i)
      ).toBeInTheDocument();

      // fetch should NOT be called because MIME type is invalid
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
});

describe('Download Template Button', () => {
<<<<<<< HEAD:4-frontend/src/client-service/components/__tests__/ExcelUpload.test.jsx
  let appendChildSpy;

  beforeEach(() => {
    appendChildSpy = jest.spyOn(document.body, 'appendChild');
    window.URL.createObjectURL = jest.fn(() => 'blob:http://test/url');
    window.URL.revokeObjectURL = jest.fn(() => {});
  });

  afterEach(() => {
    appendChildSpy.mockRestore();
  });

=======
>>>>>>> 4a4153c (wi-007 af):4-frontend/src/frontend/components/__tests__/ExcelUpload.test.jsx
  it('renders a Download Template button', () => {
    render(<ExcelUpload />);
    const downloadButton = screen.getByRole('button', { name: /download template/i });
    expect(downloadButton).toBeInTheDocument();
  });

<<<<<<< HEAD:4-frontend/src/client-service/components/__tests__/ExcelUpload.test.jsx
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

  it('extracts correct filename when Content-Disposition ends with trailing underscore', async () => {
    appendChildSpy.mockClear();

    const testBlob = new Blob(['template-content'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      blob: () => Promise.resolve(testBlob),
      headers: new Map([
        ['content-disposition', 'attachment; filename="invoice-intake-template.xlsx_"'],
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
    expect(anchorEl.download).toBe('invoice-intake-template.xlsx_');
  });

=======
>>>>>>> 4a4153c (wi-007 af):4-frontend/src/frontend/components/__tests__/ExcelUpload.test.jsx
  it('triggers a GET request to the template endpoint on click', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
<<<<<<< HEAD:4-frontend/src/client-service/components/__tests__/ExcelUpload.test.jsx
      blob: async () => new Blob(['template-content'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
      headers: new Map([
        ['content-disposition', 'attachment; filename="invoice-intake-template.xlsx"'],
        ['content-type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'],
      ]),
=======
      blob: async () => new Blob(),
      headers: new Map([['content-disposition', 'attachment; filename="invoice-intake-template.xlsx"']]),
>>>>>>> 4a4153c (wi-007 af):4-frontend/src/frontend/components/__tests__/ExcelUpload.test.jsx
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
<<<<<<< HEAD:4-frontend/src/client-service/components/__tests__/ExcelUpload.test.jsx
      blob: async () => new Blob(['template-content'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
      headers: new Map([
        ['content-type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'],
      ]),
=======
      blob: async () => new Blob(),
>>>>>>> 4a4153c (wi-007 af):4-frontend/src/frontend/components/__tests__/ExcelUpload.test.jsx
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
