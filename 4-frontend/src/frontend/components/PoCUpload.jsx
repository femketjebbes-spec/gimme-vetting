import { useState } from 'react';

/**
 * PoCUpload component.
 * Handles PoC (Proof of Correspondence) file upload for a specific invoice.
 * Displays the invoice number, accepts PDF files, and posts to the PoC upload endpoint.
 *
 * @param {object} [props] - Component props.
 * @param {string} [props.invoiceNumber] - The invoice number to display and work on.
 * @param {function} [props.onUploadComplete] - Callback invoked on successful upload.
 * @param {function} [props.onUploadError] - Callback invoked on upload error.
 * @returns {JSX.Element} The rendered upload component.
 */
function PoCUpload({ invoiceNumber, onUploadComplete, onUploadError }) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploadResult, setUploadResult] = useState(null);

  const ACCEPTED_MIME_TYPES = new Set([
    'application/pdf',
  ]);

  /**
   * Handle file selection: validate MIME type and store the file for later upload.
   * Does NOT trigger the upload.
   *
   * @param {React.ChangeEvent<HTMLInputElement>} e - File input change event.
   */
  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) {
      return;
    }

    // Client-side MIME type validation (server-side validation enforced by backend)
    if (!ACCEPTED_MIME_TYPES.has(file.type)) {
      setError('Please select a valid PDF file.');
      setSelectedFile(null);
    } else {
      setError(null);
      setUploadResult(null);
      setSelectedFile(file);
    }
  };

  /**
   * Handle upload button click: build FormData and POST to the PoC upload endpoint.
   */
  const handleUpload = async () => {
    const file = selectedFile;
    if (!file) {
      return;
    }

    setUploading(true);
    setError(null);
    setUploadResult(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch('/api/v1/poc-upload', {
        method: 'POST',
        body: formData,
      });

      const data = await response.json();

      if (response.ok && data.status === 'UPLOADED') {
        setUploadResult(data);
        if (onUploadComplete) {
          onUploadComplete(data);
        }
      } else if (response.status === 400 && data.status === 'INVALID_FILE_FORMAT') {
        if (data.errorDetail?.includes('Path traversal detected')) {
          setError('Path traversal detected in filename.');
        } else if (data.errorDetail?.includes('Only PDF')) {
          setError('Only PDF files are accepted.');
        } else {
          setError(data.errorDetail || 'Invalid file format.');
        }
        if (onUploadError) {
          onUploadError(data);
        }
      } else if (response.status === 500) {
        setError('An unexpected error occurred during processing.');
        if (onUploadError) {
          onUploadError(data);
        }
      } else {
        setError('An unexpected error occurred during processing.');
        if (onUploadError) {
          onUploadError(data);
        }
      }
    } catch {
      setError('An unexpected error occurred during processing.');
      if (onUploadError) {
        onUploadError({ error: 'Network error' });
      }
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="poc-upload">
      <h2>Upload PoC File</h2>
      {invoiceNumber && <p className="invoice-display">{invoiceNumber}</p>}
      <p className="file-info">Only PDF files are accepted.</p>

      <input
        type="file"
        id="poc-file-input"
        accept=".pdf"
        onChange={handleFileChange}
        disabled={uploading}
        aria-label="Upload PoC file"
      />

      <br />

      <button
        type="button"
        onClick={handleUpload}
        disabled={uploading || !selectedFile}
      >
        {uploading ? 'Uploading...' : 'Upload'}
      </button>

      {uploading && <p className="loading">Uploading...</p>}

      {error && (
        <div className="error-message" role="alert">
          <p>{error}</p>
        </div>
      )}

      {uploadResult && (
        <div className="success-message" data-testid="result-summary">
          <p>PoC uploaded successfully.</p>
          <p>Invoice: {uploadResult.invoiceNumber}</p>
        </div>
      )}
    </div>
  );
}

export default PoCUpload;
