import { describe, it, expect, jest } from '@jest/globals';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';

describe('InvoiceDrawer component', () => {
  const mockOnClose = jest.fn();

  const mockInvoice = {
    id: 1,
    invoiceNumber: 'INV-2026-0042',
    debtorName: 'Jan de Vries',
    address: 'Voorbeeldstraat 1, 1234AB Amsterdam',
    bankAccountNumber: 'NL12BUNQ0123456789',
    phoneNumber: '+31612345678',
    status: 'QUEUED',
    poCStatus: 'VERIFIED',
    rejectionType: null,
    resubmissionCount: 0,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockOnClose.mockClear();
  });

  it('renders the drawer when open', async () => {
    const { InvoiceDrawer } = await import('../InvoiceDrawer.jsx');
    render(
      <InvoiceDrawer
        invoice={mockInvoice}
        isOpen={true}
        onClose={mockOnClose}
      />
    );

    expect(screen.getByText('INV-2026-0042')).toBeInTheDocument();
  });

  it('does not render the drawer when closed', async () => {
    const { InvoiceDrawer } = await import('../InvoiceDrawer.jsx');
    render(
      <InvoiceDrawer
        invoice={mockInvoice}
        isOpen={false}
        onClose={mockOnClose}
      />
    );

    expect(screen.queryByText('INV-2026-0042')).not.toBeInTheDocument();
  });

  it('displays all invoice detail fields', async () => {
    const { InvoiceDrawer } = await import('../InvoiceDrawer.jsx');
    render(
      <InvoiceDrawer
        invoice={mockInvoice}
        isOpen={true}
        onClose={mockOnClose}
      />
    );

    expect(screen.getByText('Jan de Vries')).toBeInTheDocument();
    expect(screen.getByText('Voorbeeldstraat 1, 1234AB Amsterdam')).toBeInTheDocument();
    expect(screen.getByText('NL12BUNQ0123456789')).toBeInTheDocument();
    expect(screen.getByText('+31612345678')).toBeInTheDocument();
  });

  it('displays POc status as VERIFIED', async () => {
    const { InvoiceDrawer } = await import('../InvoiceDrawer.jsx');
    render(
      <InvoiceDrawer
        invoice={mockInvoice}
        isOpen={true}
        onClose={mockOnClose}
      />
    );

    expect(screen.getByText('VERIFIED')).toBeInTheDocument();
  });

  it('displays resubmission count when greater than zero', async () => {
    const { InvoiceDrawer } = await import('../InvoiceDrawer.jsx');
    const invoiceWithResubmissions = { ...mockInvoice, resubmissionCount: 3 };
    render(
      <InvoiceDrawer
        invoice={invoiceWithResubmissions}
        isOpen={true}
        onClose={mockOnClose}
      />
    );

    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('closes when close button is clicked', async () => {
    const { InvoiceDrawer } = await import('../InvoiceDrawer.jsx');
    render(
      <InvoiceDrawer
        invoice={mockInvoice}
        isOpen={true}
        onClose={mockOnClose}
      />
    );

    const closeButton = screen.getByRole('button', { name: /close/i });
    fireEvent.click(closeButton);

    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  it('renders status and PoC status badges', async () => {
    const { InvoiceDrawer } = await import('../InvoiceDrawer.jsx');
    render(
      <InvoiceDrawer
        invoice={mockInvoice}
        isOpen={true}
        onClose={mockOnClose}
      />
    );

    const statusElements = screen.getAllByText('QUEUED');
    expect(statusElements.length).toBeGreaterThanOrEqual(1);
  });
});
