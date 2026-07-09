import { StatusBadge } from './StatusBadge.jsx';

/**
 * InvoiceTable — renders the invoice list as a compact table.
 *
 * @param {object[]} invoices - Array of invoice objects from the API.
 * @param {function} onRowClick - Callback invoked when a row is clicked, passing the invoice object.
 * @param {number} currentPage - Current page number (0-indexed).
 * @param {number} totalPages - Total number of pages.
 * @param {number} pageSize - Items per page.
 * @param {number} totalElements - Total number of invoices matching the query.
 * @param {function} [onPageChange] - Callback for page navigation (previous/next).
 */
function InvoiceTable({
  invoices,
  onRowClick,
  currentPage,
  totalPages,
  pageSize,
  totalElements,
  onPageChange,
}) {
  const handleRowClick = (invoice) => {
    onRowClick(invoice);
  };

  const handlePrevPage = () => {
    if (onPageChange && currentPage > 0) {
      onPageChange(currentPage - 1);
    }
  };

  const handleNextPage = () => {
    if (onPageChange && currentPage < totalPages - 1) {
      onPageChange(currentPage + 1);
    }
  };

  if (!invoices || invoices.length === 0) {
    return (
      <div className="invoice-table" data-testid="invoice-table">
        <p className="empty-state">No invoices found matching the current criteria.</p>
      </div>
    );
  }

  const startRow = currentPage * pageSize + 1;
  const endRow = Math.min((currentPage + 1) * pageSize, totalElements);

  return (
    <div className="invoice-table" data-testid="invoice-table">
      <table>
        <thead>
          <tr>
            <th>Invoice Number</th>
            <th>Debtor Name</th>
            <th>Address</th>
            <th>Phone</th>
            <th>Account</th>
            <th>Status</th>
            <th>PoC Status</th>
            <th>Resubmissions</th>
          </tr>
        </thead>
        <tbody>
          {invoices.map((invoice) => (
            <tr
              key={invoice.id}
              onClick={() => handleRowClick(invoice)}
              className="invoice-row clickable"
              data-testid={`invoice-row-${invoice.id}`}
            >
              <td data-testid={`cell-invoice-number`}>{invoice.invoiceNumber}</td>
              <td data-testid={`cell-debtor-name`}>{invoice.debtorName}</td>
              <td data-testid={`cell-address`}>{invoice.address}</td>
              <td data-testid={`cell-phone`}>{invoice.phoneNumber}</td>
              <td data-testid={`cell-account`}>{invoice.bankAccountNumber}</td>
              <td>
                <StatusBadge status={invoice.status} />
              </td>
              <td>
                <StatusBadge status={invoice.poCStatus} />
              </td>
              <td data-testid={`cell-resubmissions`}>{invoice.resubmissionCount}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {totalPages > 1 && (
        <div className="pagination-controls" data-testid="pagination-controls">
          <button
            onClick={handlePrevPage}
            disabled={currentPage === 0}
            aria-label="Previous page"
            data-testid="prev-page-button"
          >
            Previous
          </button>
          <span className="pagination-info" data-testid="pagination-info">
            Showing {startRow}-{endRow} of {totalElements}
          </span>
          <button
            onClick={handleNextPage}
            disabled={currentPage >= totalPages - 1}
            aria-label="Next page"
            data-testid="next-page-button"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}

export { InvoiceTable };
export default InvoiceTable;
