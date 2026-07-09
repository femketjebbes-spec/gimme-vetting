import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { fetchInvoiceList } from '../api/analystApi.js';
import InvoiceTable from './InvoiceTable.jsx';
import InvoiceDrawer from './InvoiceDrawer.jsx';

/**
 * AnalystDashboard — main case analyst view.
 *
 * Features:
 *   - Search input (bounded to 256 characters, per contract constraint)
 *   - Status filter dropdown
 *   - Paginated invoice table
 *   - Click-to-open detail drawer
 *
 * Consumes: GET /api/v1/analyst/invoices (unauthenticated per D-CA-002)
 */

const VALID_STATUSES = ['QUEUED', 'REJECTED_TYPE_A', 'REJECTED_TYPE_B'];

function AnalystDashboard() {
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [pagination, setPagination] = useState({
    currentPage: 0,
    totalPages: 0,
    pageSize: 50,
    totalElements: 0,
  });
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const loadInvoices = useCallback(async (page = 0, searchVal, statusVal) => {
    setLoading(true);
    setError(null);

    try {
      const params = { page, size: pagination.pageSize };
      if (searchVal) params.search = searchVal;
      if (statusVal) params.status = statusVal;

      const response = await fetchInvoiceList(params);

      setInvoices(response.content || []);
      setPagination({
        currentPage: response.currentPage || page,
        totalPages: response.totalPages || 0,
        pageSize: response.pageSize || 50,
        totalElements: response.totalElements || 0,
      });
    } catch (err) {
      setError(err.message || 'Failed to load invoices');
    } finally {
      setLoading(false);
    }
  }, [pagination.pageSize]);

  useEffect(() => {
    loadInvoices(0, search, statusFilter);
  }, []);

  const handleSearchChange = (e) => {
    const value = e.target.value;
    if (value.length <= 256) {
      setSearch(value);
    }
  };

  const handleSearchSubmit = () => {
    loadInvoices(0, search, statusFilter);
  };

  const handleStatusFilterChange = (e) => {
    setStatusFilter(e.target.value);
  };

  const handleFilterApply = () => {
    loadInvoices(0, search, statusFilter);
  };

  const handleRowClick = (invoice) => {
    setSelectedInvoice(invoice);
    setDrawerOpen(true);
  };

  const handleDrawerClose = () => {
    setDrawerOpen(false);
    setSelectedInvoice(null);
  };

  const handlePageChange = (newPage) => {
    loadInvoices(newPage, search, statusFilter);
  };

  return (
    <div className="analyst-dashboard">
      <header className="dashboard-header" role="banner">
        <div className="header-left">
          <h1>Case Analyst Dashboard</h1>
        </div>
        <div className="header-right">
          <Link to="/" className="upload-link" aria-label="Upload Invoice">
            Upload Invoice
          </Link>
        </div>
      </header>

      <div className="filter-bar" data-testid="filter-bar">
        <div className="search-input-group">
          <input
            type="text"
            value={search}
            onChange={handleSearchChange}
            placeholder="Search by invoice number, debtor, or address..."
            aria-label="Search invoices"
            data-testid="search-input"
          />
          <button
            onClick={handleSearchSubmit}
            aria-label="Search"
            data-testid="search-button"
          >
            Search
          </button>
        </div>

        <div className="status-filter-group">
          <select
            value={statusFilter}
            onChange={handleStatusFilterChange}
            aria-label="Filter by status"
            data-testid="status-filter"
          >
            <option value="">All Statuses</option>
            {VALID_STATUSES.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
          <button
            onClick={handleFilterApply}
            aria-label="Apply filter"
            data-testid="filter-apply-button"
          >
            Apply
          </button>
        </div>
      </div>

      {loading && <div className="loading-indicator" data-testid="loading">Loading...</div>}

      {error && (
        <div className="error-message" role="alert" data-testid="error-message">
          <p>{error}</p>
        </div>
      )}

      <InvoiceTable
        invoices={invoices}
        onRowClick={handleRowClick}
        currentPage={pagination.currentPage}
        totalPages={pagination.totalPages}
        pageSize={pagination.pageSize}
        totalElements={pagination.totalElements}
        onPageChange={handlePageChange}
      />

      <InvoiceDrawer
        invoice={selectedInvoice}
        isOpen={drawerOpen}
        onClose={handleDrawerClose}
      />
    </div>
  );
}

export { AnalystDashboard };
export default AnalystDashboard;
