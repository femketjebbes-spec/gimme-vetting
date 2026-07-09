import { StatusBadge } from './StatusBadge.jsx';

/**
 * InvoiceDrawer — slide-over detail panel that opens when a row is clicked.
 * Displays all 10 invoice fields in a vertical layout.
 *
 * @param {object} invoice - The selected invoice object.
 * @param {boolean} isOpen - Whether the drawer is currently open.
 * @param {function} onClose - Callback to close the drawer.
 */
function InvoiceDrawer({ invoice, isOpen, onClose }) {
  if (!invoice) {
    return null;
  }

  if (!isOpen) {
    return null;
  }

  return (
    <div className="invoice-drawer-overlay" data-testid="invoice-drawer-overlay" onClick={onClose}>
      <div className="invoice-drawer" data-testid="invoice-drawer" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-header">
          <h3 className="drawer-title" data-testid="drawer-invoice-number">
            {invoice.invoiceNumber}
          </h3>
          <button
            className="drawer-close-button"
            onClick={onClose}
            aria-label="Close"
            data-testid="drawer-close-button"
          >
            &times;
          </button>
        </div>

        <div className="drawer-content">
          <div className="detail-section">
            <h4>Invoice Details</h4>
            <div className="detail-row">
              <span className="detail-label">Debtor Name</span>
              <span className="detail-value" data-testid="detail-debtor-name">
                {invoice.debtorName}
              </span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Address</span>
              <span className="detail-value" data-testid="detail-address">
                {invoice.address}
              </span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Phone Number</span>
              <span className="detail-value" data-testid="detail-phone">
                {invoice.phoneNumber}
              </span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Bank Account</span>
              <span className="detail-value" data-testid="detail-account">
                {invoice.bankAccountNumber}
              </span>
            </div>
          </div>

          <div className="detail-section">
            <h4>Processing Status</h4>
            <div className="detail-row">
              <span className="detail-label">Status</span>
              <span data-testid="detail-status">
                <StatusBadge status={invoice.status} />
              </span>
            </div>
            <div className="detail-row">
              <span className="detail-label">PoC Status</span>
              <span data-testid="detail-poc-status">
                <StatusBadge status={invoice.poCStatus} />
              </span>
            </div>
            {invoice.rejectionType && (
              <div className="detail-row">
                <span className="detail-label">Rejection Type</span>
                <span className="detail-value" data-testid="detail-rejection-type">
                  {invoice.rejectionType}
                </span>
              </div>
            )}
            <div className="detail-row">
              <span className="detail-label">Resubmission Count</span>
              <span className="detail-value" data-testid="detail-resubmission-count">
                {invoice.resubmissionCount}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export { InvoiceDrawer };
export default InvoiceDrawer;
