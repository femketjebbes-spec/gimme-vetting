/**
 * analystApi — service module for consuming the Case Analyst API.
 *
 * Endpoints consumed:
 *   GET /api/v1/analyst/invoices (paginated list)
 *   GET /api/v1/analyst/invoices/{id} (single invoice detail)
 *
 * Both endpoints are unauthenticated per D-CA-002 (MVP).
 */

const API_BASE = '/api/v1/analyst';

/**
 * Fetch the paginated invoice list.
 *
 * @param {object} [params] - Query parameters.
 * @param {number} [params.page=0] - Page number (0-indexed).
 * @param {number} [params.size=50] - Items per page (1-200).
 * @param {string} [params.sort='id,asc'] - Sort field and direction.
 * @param {string} [params.status] - Comma-separated status filter.
 * @param {string} [params.search] - Free-text search (max 256 characters).
 * @returns {Promise<object>} Parsed JSON response with content, totalElements, totalPages, currentPage, pageSize.
 */
export async function fetchInvoiceList(params = {}) {
  const {
    page = 0,
    size = 50,
    sort = 'id,asc',
    status,
    search,
  } = params;

  const parts = [`page=${page}`, `size=${size}`, `sort=${sort}`];

  if (status) {
    parts.push(`status=${status}`);
  }

  if (search) {
    parts.push(`search=${search.substring(0, 256)}`);
  }

  const url = `${API_BASE}/invoices?${parts.join('&')}`;

  const response = await fetch(url, { method: 'GET' });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    const error = new Error(
      errorBody?.message || `API error: ${response.status} ${response.statusText}`
    );
    error.status = response.status;
    error.details = errorBody?.details;
    throw error;
  }

  return response.json();
}

/**
 * Fetch the source Excel file for a given invoice.
 *
 * @param {number} id - Invoice ID (must be a positive integer).
 * @returns {Promise<object>} Resolves with { blob, contentType, filename }.
 */
export async function fetchSourceFile(id) {
  if (!Number.isInteger(id) || id <= 0) {
    throw new Error('Invoice id must be a positive integer');
  }

  const url = `${API_BASE}/invoices/${id}/source-file`;

  const response = await fetch(url, { method: 'GET' });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    const error = new Error(
      errorBody?.message || `API error: ${response.status} ${response.statusText}`
    );
    error.status = response.status;
    error.details = errorBody;
    throw error;
  }

  const blob = await response.blob();
  const contentType = response.headers.get('content-type') || '';
  const disposition = response.headers.get('content-disposition') || '';
  const filenameMatch = disposition.match(/filename="?([^";]+)"?/i);
  const filename = filenameMatch ? filenameMatch[1] : null;

  return { blob, contentType, filename };
}

/**
 * Fetch a single invoice by ID.
 *
 * @param {number} id - Invoice ID (must be a positive integer).
 * @returns {Promise<object>} Parsed JSON response with all 10 invoice fields.
 */
export async function fetchInvoiceDetail(id) {
  if (!Number.isInteger(id) || id <= 0) {
    throw new Error('Invoice id must be a positive integer');
  }

  const url = `${API_BASE}/invoices/${id}`;

  const response = await fetch(url, { method: 'GET' });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    const error = new Error(
      errorBody?.message || `API error: ${response.status} ${response.statusText}`
    );
    error.status = response.status;
    error.details = errorBody;
    throw error;
  }

  return response.json();
}
