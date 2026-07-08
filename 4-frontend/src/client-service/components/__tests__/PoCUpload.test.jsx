import { describe, it, expect, jest } from '@jest/globals';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import PoCUpload from '../PoCUpload';

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
 *
 * @param {HTMLElement} fileInput - The input element.
 * @param {File[]} files - Array of File objects.
 */
function fireFileChange(fileInput, files) {
  const fileList = createFileList(files);
  fireEvent.change(fileInput, { target: { files: fileList } });
}

describe('PoCUpload Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockFetch.mockReset();
  });

  describe('File Picker', () => {
    it('renders a file input that accepts only PDF files', () => {
      render(<PoCUpload invoiceNumber="INV-2026-0042" />);
      const fileInput = screen.getByLabelText(/upload poc file/i);
      expect(fileInput).toBeInTheDocument();
      expect(fileInput).toHaveAttribute('accept', '.pdf');
    });

    it('displays PDF file type restrictions to the user', () => {
      render(<PoCUpload invoiceNumber="INV-2026-0042" />);
      const fileInfo = screen.getByText(/only pdf files are accepted/i);
      expect(fileInfo).toBeInTheDocument();
    });

    it('displays the invoice number being worked on', () => {
      render(<PoCUpload invoiceNumber="INV-2026-0042" />);
      expect(screen.getByText(/INV-2026-0042/i)).toBeInTheDocument();
    });

    it('validates MIME type before upload for non-PDF files', () => {
      render(<PoCUpload invoiceNumber="INV-2026-0042" />);
      const fileInput = screen.getByLabelText(/upload poc file/i);

      const wrongFile = new File(['content'], 'test.docx', { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' });
      fireFileChange(fileInput, [wrongFile]);

      expect(
        screen.getByText(/Please select a valid PDF file/i)
      ).toBeInTheDocument();

      expect(mockFetch).not.toHaveBeenCalled();
    });
  });

  describe('Upload Button and API Call', () => {
    it('submits form data using FormData with field named file', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          status: 'UPLOADED',
          invoiceNumber: 'inv-2026-0042',
        }),
      });

      render(<PoCUpload invoiceNumber="INV-2026-0042" />);
      const fileInput = screen.getByLabelText(/upload poc file/i);

      const pdfFile = new File(['pdf content'], 'inv-2026-0042.pdf', {
        type: 'application/pdf',
      });

      fireFileChange(fileInput, [pdfFile]);
      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        expect(mockFetch).toHaveBeenCalledWith(
          '/api/v1/poc-upload',
          expect.objectContaining({
            method: 'POST',
          })
        );

        const callArgs = mockFetch.mock.calls[0];
        expect(callArgs[1]).toHaveProperty('body');
      });
    });

    it('posts to the correct endpoint /api/v1/poc-upload', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          status: 'UPLOADED',
          invoiceNumber: 'inv-2026-0042',
        }),
      });

      render(<PoCUpload invoiceNumber="INV-2026-0042" />);
      const fileInput = screen.getByLabelText(/upload poc file/i);

      const pdfFile = new File(['pdf content'], 'inv-2026-0042.pdf', {
        type: 'application/pdf',
      });

      fireFileChange(fileInput, [pdfFile]);
      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        expect(mockFetch).toHaveBeenCalledWith(
          '/api/v1/poc-upload',
          expect.any(Object)
        );
      });
    });

    it('disables upload button while processing', async () => {
      mockFetch.mockImplementationOnce(() => new Promise(() => {}));

      render(<PoCUpload invoiceNumber="INV-2026-0042" />);
      const fileInput = screen.getByLabelText(/upload poc file/i);
      const uploadButton = screen.getByRole('button', { name: /upload/i });

      const pdfFile = new File(['pdf content'], 'inv-2026-0042.pdf', {
        type: 'application/pdf',
      });

      fireFileChange(fileInput, [pdfFile]);
      fireEvent.click(uploadButton);

      expect(uploadButton).toBeDisabled();
    });

    it('shows loading indicator during upload', async () => {
      mockFetch.mockImplementationOnce(() => new Promise(() => {}));

      render(<PoCUpload invoiceNumber="INV-2026-0042" />);
      const fileInput = screen.getByLabelText(/upload poc file/i);

      const pdfFile = new File(['pdf content'], 'inv-2026-0042.pdf', {
        type: 'application/pdf',
      });

      fireFileChange(fileInput, [pdfFile]);
      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        const button = screen.getByRole('button', { name: /uploading/i });
        expect(button).toBeDisabled();
      });
    });
  });

  describe('Success Response Display', () => {
    it('displays success message when upload returns 200 UPLOADED', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          status: 'UPLOADED',
          invoiceNumber: 'inv-2026-0042',
        }),
      });

      render(<PoCUpload invoiceNumber="INV-2026-0042" />);
      const fileInput = screen.getByLabelText(/upload poc file/i);

      const pdfFile = new File(['pdf content'], 'inv-2026-0042.pdf', {
        type: 'application/pdf',
      });

      fireFileChange(fileInput, [pdfFile]);
      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        expect(screen.getByText(/Poc uploaded successfully/i)).toBeInTheDocument();
        expect(screen.getByText(/invoice: inv-2026-0042/i)).toBeInTheDocument();
      });
    });

    it('re-enables button and hides loading after success', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          status: 'UPLOADED',
          invoiceNumber: 'inv-2026-0042',
        }),
      });

      render(<PoCUpload invoiceNumber="INV-2026-0042" />);
      const fileInput = screen.getByLabelText(/upload poc file/i);
      const uploadButton = screen.getByRole('button', { name: /upload/i });

      const pdfFile = new File(['pdf content'], 'inv-2026-0042.pdf', {
        type: 'application/pdf',
      });

      fireFileChange(fileInput, [pdfFile]);
      fireEvent.click(uploadButton);

      await waitFor(() => {
        expect(uploadButton).toBeEnabled();
        expect(screen.queryByText('Uploading...')).not.toBeInTheDocument();
      });
    });

    it('invokes onUploadComplete callback on success', async () => {
      const mockCallback = jest.fn();

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          status: 'UPLOADED',
          invoiceNumber: 'inv-2026-0042',
        }),
      });

      render(<PoCUpload invoiceNumber="INV-2026-0042" onUploadComplete={mockCallback} />);
      const fileInput = screen.getByLabelText(/upload poc file/i);

      const pdfFile = new File(['pdf content'], 'inv-2026-0042.pdf', {
        type: 'application/pdf',
      });

      fireFileChange(fileInput, [pdfFile]);
      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        expect(mockCallback).toHaveBeenCalledTimes(1);
        expect(mockCallback).toHaveBeenCalledWith({
          status: 'UPLOADED',
          invoiceNumber: 'inv-2026-0042',
        });
      });
    });
  });

  describe('Error Response Handling', () => {
    it('displays error message for HTTP 400 Invalid File Format (non-PDF)', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: async () => ({
          status: 'INVALID_FILE_FORMAT',
          errorDetail: 'Only PDF files are accepted. Uploaded file type: application/msword',
        }),
      });

      // This test simulates a scenario where the server rejects a PDF that failed MIME check
      // or when client-side validation is bypassed
      render(<PoCUpload invoiceNumber="INV-2026-0042" />);
      const fileInput = screen.getByLabelText(/upload poc file/i);

      const pdfFile = new File(['pdf content'], 'inv-2026-0042.pdf', {
        type: 'application/pdf',
      });

      fireFileChange(fileInput, [pdfFile]);
      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        expect(
          screen.getByText(/Only PDF files are accepted/i)
        ).toBeInTheDocument();
      });
    });

    it('displays path traversal error for HTTP 400 Path Traversal Detected', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: async () => ({
          status: 'INVALID_FILE_FORMAT',
          errorDetail: 'Path traversal detected in filename',
        }),
      });

      render(<PoCUpload invoiceNumber="INV-2026-0042" />);
      const fileInput = screen.getByLabelText(/upload poc file/i);

      const maliciousFile = new File(['content'], '../../etc/passwd.pdf', {
        type: 'application/pdf',
      });

      fireFileChange(fileInput, [maliciousFile]);
      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        expect(
          screen.getByText(/Path traversal detected/i)
        ).toBeInTheDocument();
      });
    });

    it('displays generic error message for HTTP 500', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        json: async () => ({
          status: 'INTERNAL_ERROR',
          errorDetail: 'Unexpected error during PoC upload',
        }),
      });

      render(<PoCUpload invoiceNumber="INV-2026-0042" />);
      const fileInput = screen.getByLabelText(/upload poc file/i);

      const pdfFile = new File(['pdf content'], 'inv-2026-0042.pdf', {
        type: 'application/pdf',
      });

      fireFileChange(fileInput, [pdfFile]);
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
          errorDetail: 'Only PDF files are accepted',
        }),
      });

      render(<PoCUpload invoiceNumber="INV-2026-0042" />);
      const fileInput = screen.getByLabelText(/upload poc file/i);
      const uploadButton = screen.getByRole('button', { name: /upload/i });

      const pdfFile = new File(['pdf content'], 'inv-2026-0042.pdf', {
        type: 'application/pdf',
      });

      fireFileChange(fileInput, [pdfFile]);
      fireEvent.click(uploadButton);

      await waitFor(() => {
        expect(uploadButton).toBeEnabled();
        expect(screen.queryByText('Uploading...')).not.toBeInTheDocument();
      });
    });

    it('invokes onUploadError callback on error', async () => {
      const mockErrorCallback = jest.fn();

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: async () => ({
          status: 'INVALID_FILE_FORMAT',
          errorDetail: 'Only PDF files are accepted',
        }),
      });

      render(<PoCUpload invoiceNumber="INV-2026-0042" onUploadError={mockErrorCallback} />);
      const fileInput = screen.getByLabelText(/upload poc file/i);

      const pdfFile = new File(['pdf content'], 'inv-2026-0042.pdf', {
        type: 'application/pdf',
      });

      fireFileChange(fileInput, [pdfFile]);
      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        expect(mockErrorCallback).toHaveBeenCalledTimes(1);
        expect(mockErrorCallback).toHaveBeenCalledWith({
          status: 'INVALID_FILE_FORMAT',
          errorDetail: 'Only PDF files are accepted',
        });
      });
    });
  });

  describe('Network Error Handling', () => {
    it('displays generic error message when fetch fails with network error', async () => {
      mockFetch.mockRejectedValueOnce(new Error('Network request failed'));

      render(<PoCUpload invoiceNumber="INV-2026-0042" />);
      const fileInput = screen.getByLabelText(/upload poc file/i);

      const pdfFile = new File(['pdf content'], 'inv-2026-0042.pdf', {
        type: 'application/pdf',
      });

      fireFileChange(fileInput, [pdfFile]);
      fireEvent.click(screen.getByRole('button', { name: /upload/i }));

      await waitFor(() => {
        expect(
          screen.getByText(/An unexpected error occurred during processing/i)
        ).toBeInTheDocument();
      });
    });
  });
});
