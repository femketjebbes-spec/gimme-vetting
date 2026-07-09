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
