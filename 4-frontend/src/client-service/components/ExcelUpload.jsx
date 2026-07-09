import { useState } from 'react';

// NOTE: Authentication is absent for the PoC phase. This endpoint is unauthenticated
// and should be protected in a future work item.

/**
 * ExcelUpload component.
 * Handles file selection (MIME validation only), upload via FormData triggered by button click,
 * and display of the API response (success summary + error messages).
 *
 * @param {object} [props] - Component props.
 * @param {function} [props.onUploadComplete] - Callback invoked on successful upload.
 * @param {function} [props.onUploadError] - Callback invoked on upload error.
 * @returns {JSX.Element} The rendered upload component.
 */
function ExcelUpload({ onUploadComplete, onUploadError }) {
  const [uploading, setUploading] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [selectedFile, setSelectedFile] = useState(null);

  /**
   * Handle file selection: store the file for later upload.
   * Does NOT perform client-side MIME type validation. The backend performs
   * authoritative content-based detection per BR-001 (MIME type is supplementary hint only).
   * Does NOT trigger the upload.
   *
   * @param {React.ChangeEvent<HTMLInputElement>} e - File input change event.
   */
  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) {
      return;
    }

    setError(null);
    setSelectedFile(file);
  };

  /**
   * Handle upload button click: build FormData and POST to the intake endpoint.
   */
  const handleUpload = async () => {
    const file = selectedFile;
    if (!file) {
      return;
    }

    setUploading(true);
    setResult(null);
    setError(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch('/api/v1/intake/excel', {
        method: 'POST',
        body: formData,
      });

      const data = await response.json();

      if (response.ok && data.processingStatus === 'COMPLETED') {
        setResult(data);
        if (onUploadComplete) {
          onUploadComplete(data);
        }
      } else if (response.status === 400 && data.status === 'INVALID_FILE_FORMAT') {
        setError('The uploaded file is not a valid Excel or CSV file.');
        if (onUploadError) {
          onUploadError(data);
        }
      } else if (response.status === 400 && data.status === 'COLUMN_NAME_MISMATCH') {
        const columnsList = data.unrecognizedColumns?.join(', ') ?? '';
        setError(`Unrecognized column names: ${columnsList}`);
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

  /**
   * Handle template download: fetch the template file and trigger browser download.
   */
  const handleDownloadTemplate = async () => {
    setDownloading(true);
    setError(null);

    try {
      const response = await fetch('/api/v1/intake/excel/template', {
        method: 'GET',
      });

      if (response.ok) {
        const blob = await response.blob();
<<<<<<< HEAD:4-frontend/src/client-service/components/ExcelUpload.jsx
<<<<<<< HEAD:4-frontend/src/client-service/components/ExcelUpload.jsx
<<<<<<< HEAD:4-frontend/src/client-service/components/ExcelUpload.jsx
=======
>>>>>>> 83497927bdf2b212763bf177e8af0bcca7746661:4-frontend/src/frontend/components/ExcelUpload.jsx
        const contentType = response.headers.get('content-type');
        const disposition = response.headers.get('content-disposition');
        let filename = 'invoice-intake-template.xlsx';
        if (disposition) {
          const filenameMatch = disposition.match(/filename="?([^";]+)"?/i);
          if (filenameMatch && filenameMatch[1]) {
            filename = filenameMatch[1].trim();
          }
        }
        const downloadBlob = contentType
          ? new Blob([blob], { type: contentType })
          : blob;
        const downloadUrl = window.URL.createObjectURL(downloadBlob);
<<<<<<< HEAD:4-frontend/src/client-service/components/ExcelUpload.jsx
=======
=======
        const contentType = response.headers.get('content-type');
>>>>>>> 3cacf7e (Bugfix waarbij de template excel niet een excel file was):4-frontend/src/frontend/components/ExcelUpload.jsx
        const disposition = response.headers.get('content-disposition');
        let filename = 'invoice-intake-template.xlsx';
        if (disposition) {
          const filenameMatch = disposition.match(/filename="?([^";]+)"?/i);
          if (filenameMatch && filenameMatch[1]) {
            filename = filenameMatch[1].trim();
          }
        }
<<<<<<< HEAD:4-frontend/src/client-service/components/ExcelUpload.jsx
        const downloadUrl = window.URL.createObjectURL(blob);
>>>>>>> 4a4153c (wi-007 af):4-frontend/src/frontend/components/ExcelUpload.jsx
=======
        const downloadBlob = contentType
          ? new Blob([blob], { type: contentType })
          : blob;
        const downloadUrl = window.URL.createObjectURL(downloadBlob);
>>>>>>> 3cacf7e (Bugfix waarbij de template excel niet een excel file was):4-frontend/src/frontend/components/ExcelUpload.jsx
=======
>>>>>>> 83497927bdf2b212763bf177e8af0bcca7746661:4-frontend/src/frontend/components/ExcelUpload.jsx
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(downloadUrl);
        document.body.removeChild(a);
      } else {
        setError('An unexpected error occurred during processing.');
      }
    } catch {
      setError('An unexpected error occurred during processing.');
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="excel-upload">
      <h2>Upload Excel File</h2>
      <p className="file-info">Accepted formats: .xlsx or .csv</p>

      <button
        type="button"
        onClick={handleDownloadTemplate}
        disabled={downloading}
        className="download-template-btn"
        aria-label="Download Template"
      >
        {downloading ? 'Downloading...' : 'Download Template'}
      </button>

      <input
        type="file"
        id="excel-file-input"
        accept=".xlsx,.csv"
        onChange={handleFileChange}
        disabled={uploading}
        aria-label="Upload Excel file"
      />

      <br />

      <button
        type="button"
        onClick={handleUpload}
        disabled={uploading || !selectedFile}
      >
        {uploading ? 'Processing...' : 'Upload'}
      </button>

      {uploading && <p className="loading">Processing...</p>}

      {error && (
        <div className="error-message" role="alert">
          <p>{error}</p>
        </div>
      )}

      {result && (
        <div className="success-message" data-testid="result-summary">
          <p>Processing Status: {result.processingStatus}</p>
          <p>Total Rows Processed: {result.totalRowsProcessed}</p>
          <p>Rows Passed: {result.rowsPassed}</p>
          <p>Rows Failed: {result.rowsFailed}</p>
          {result.rowsFailed > 0 && result.returnExcelDownloadLink && (
            <a
              href={result.returnExcelDownloadLink}
              download
              className="download-link"
            >
              Download Return Excel
            </a>
          )}
        </div>
      )}
    </div>
  );
}

export default ExcelUpload;
