import { describe, it, expect, jest } from '@jest/globals';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';

describe('InvoiceTable component', () => {
  const mockOnRowClick = jest.fn();

  const mockInvoices = [
    {
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
    },
    {
      id: 2,
      invoiceNumber: 'INV-2026-0043',
      debtorName: 'Pieter Jansen',
      address: 'Testweg 5, 1000AA Rotterdam',
      bankAccountNumber: 'NL23INGB0987654321',
      phoneNumber: '+31698765432',
      status: 'REJECTED_TYPE_A',
      poCStatus: 'MISSING',
      rejectionType: 'REJECTED_TYPE_A',
      resubmissionCount: 2,
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    mockOnRowClick.mockClear();
  });

  it('renders a table header row with all column titles', async () => {
    const { InvoiceTable } = await import('../InvoiceTable.jsx');
    render(
      <InvoiceTable
        invoices={mockInvoices}
        onRowClick={mockOnRowClick}
        currentPage={0}
        totalPages={1}
        pageSize={50}
        totalElements={2}
      />
    );

    expect(screen.getByText('Invoice Number')).toBeInTheDocument();
    expect(screen.getByText('Debtor Name')).toBeInTheDocument();
    expect(screen.getByText('Status')).toBeInTheDocument();
    expect(screen.getByText('PoC Status')).toBeInTheDocument();
    expect(screen.getByText('Resubmissions')).toBeInTheDocument();
  });

  it('renders all invoice rows with correct data', async () => {
    const { InvoiceTable } = await import('../InvoiceTable.jsx');
    render(
      <InvoiceTable
        invoices={mockInvoices}
        onRowClick={mockOnRowClick}
        currentPage={0}
        totalPages={1}
        pageSize={50}
        totalElements={2}
      />
    );

    expect(screen.getByText('INV-2026-0042')).toBeInTheDocument();
    expect(screen.getByText('Jan de Vries')).toBeInTheDocument();
    expect(screen.getByText('INV-2026-0043')).toBeInTheDocument();
    expect(screen.getByText('Pieter Jansen')).toBeInTheDocument();
  });

  it('calls onRowClick with the correct invoice when a row is clicked', async () => {
    const { InvoiceTable } = await import('../InvoiceTable.jsx');
    render(
      <InvoiceTable
        invoices={mockInvoices}
        onRowClick={mockOnRowClick}
        currentPage={0}
        totalPages={1}
        pageSize={50}
        totalElements={2}
      />
    );

    const firstRow = screen.getByText('INV-2026-0042').closest('tr');
    fireEvent.click(firstRow);

    expect(mockOnRowClick).toHaveBeenCalledWith(mockInvoices[0]);
  });

  it('displays pagination controls with correct information', async () => {
    const { InvoiceTable } = await import('../InvoiceTable.jsx');
    render(
      <InvoiceTable
        invoices={mockInvoices}
        onRowClick={mockOnRowClick}
        currentPage={1}
        totalPages={5}
        pageSize={10}
        totalElements={50}
      />
    );

    expect(screen.getByText('Showing 11-20 of 50')).toBeInTheDocument();
  });

  it('enables page navigation when more than one page exists', async () => {
    const { InvoiceTable } = await import('../InvoiceTable.jsx');
    render(
      <InvoiceTable
        invoices={mockInvoices}
        onRowClick={mockOnRowClick}
        currentPage={1}
        totalPages={5}
        pageSize={10}
        totalElements={50}
      />
    );

    const prevButton = screen.getByRole('button', { name: /previous/i });
    expect(prevButton).toBeEnabled();
  });

  it('disables previous page button when on first page', async () => {
    const { InvoiceTable } = await import('../InvoiceTable.jsx');
    render(
      <InvoiceTable
        invoices={mockInvoices}
        onRowClick={mockOnRowClick}
        currentPage={0}
        totalPages={5}
        pageSize={10}
        totalElements={50}
      />
    );

    const prevButton = screen.getByRole('button', { name: /previous/i });
    expect(prevButton).toBeDisabled();
  });

  it('disables next page button when on last page', async () => {
    const { InvoiceTable } = await import('../InvoiceTable.jsx');
    render(
      <InvoiceTable
        invoices={mockInvoices}
        onRowClick={mockOnRowClick}
        currentPage={4}
        totalPages={5}
        pageSize={10}
        totalElements={50}
      />
    );

    const nextButton = screen.getByRole('button', { name: /next/i });
    expect(nextButton).toBeDisabled();
  });

  it('shows empty state message when no invoices are provided', async () => {
    const { InvoiceTable } = await import('../InvoiceTable.jsx');
    render(
      <InvoiceTable
        invoices={[]}
        onRowClick={mockOnRowClick}
        currentPage={0}
        totalPages={0}
        pageSize={50}
        totalElements={0}
      />
    );

    expect(screen.getByText(/no invoices/i)).toBeInTheDocument();
  });
});
