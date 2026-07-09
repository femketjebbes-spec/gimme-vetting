import { describe, it, expect, jest } from '@jest/globals';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import '@testing-library/jest-dom';

// Mock fetch globally
const mockFetch = jest.fn();
global.fetch = mockFetch;

function renderWithRouter(ui) {
  return render(<MemoryRouter>{ui}</MemoryRouter>);
}

describe('AnalystDashboard component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockFetch.mockReset();
  });

  it('renders a search input field', async () => {
    const { AnalystDashboard } = await import('../AnalystDashboard.jsx');
    renderWithRouter(<AnalystDashboard />);
    expect(screen.getByPlaceholderText(/search/i)).toBeInTheDocument();
  });

  it('renders a status filter dropdown', async () => {
    const { AnalystDashboard } = await import('../AnalystDashboard.jsx');
    renderWithRouter(<AnalystDashboard />);
    const select = screen.getByRole('combobox', { name: /status/i }) || screen.getByRole('listbox') || screen.getByRole('menu');
    expect(select).toBeInTheDocument();
  });

  it('renders an invoice table area', async () => {
    const { AnalystDashboard } = await import('../AnalystDashboard.jsx');
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        content: [
          {
            id: 1,
            invoiceNumber: 'INV-2026-0001',
            debtorName: 'Test Debtor',
            address: 'Test Street 1, 1000AA Test',
            bankAccountNumber: 'NL00BUNQ0000000000',
            phoneNumber: '+31600000000',
            status: 'QUEUED',
            poCStatus: 'PENDING',
            rejectionType: null,
            resubmissionCount: 0,
          },
        ],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 50,
      }),
    });
    renderWithRouter(<AnalystDashboard />);
    await waitFor(() => {
      expect(screen.getByRole('table')).toBeInTheDocument();
    });
  });

  it('calls fetchInvoiceList on mount', async () => {
    const { AnalystDashboard } = await import('../AnalystDashboard.jsx');
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        content: [{ id: 1, invoiceNumber: 'INV-001' }],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 50,
      }),
    });
    renderWithRouter(<AnalystDashboard />);
    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalled();
    });
  });

  it('displays invoices after fetching', async () => {
    const { AnalystDashboard } = await import('../AnalystDashboard.jsx');
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        content: [
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
        ],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 50,
      }),
    });
    renderWithRouter(<AnalystDashboard />);
    await waitFor(() => {
      expect(screen.getByText('INV-2026-0042')).toBeInTheDocument();
    });
  });

  it('handles search input and triggers re-fetch', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        content: [],
        totalElements: 0,
        totalPages: 0,
        currentPage: 0,
        pageSize: 50,
      }),
    });

    const { AnalystDashboard } = await import('../AnalystDashboard.jsx');
    renderWithRouter(<AnalystDashboard />);

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalled();
    });

    const initialCallCount = mockFetch.mock.calls.length;

    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        content: [],
        totalElements: 0,
        totalPages: 0,
        currentPage: 0,
        pageSize: 50,
      }),
    });

    const searchInput = screen.getByPlaceholderText(/search/i);
    fireEvent.change(searchInput, { target: { value: 'INV-001' } });

    const applyButton = screen.getByRole('button', { name: /search/i }) || screen.getByText(/search/i);
    fireEvent.click(applyButton);

    await waitFor(() => {
      expect(mockFetch.mock.calls.length).toBeGreaterThan(initialCallCount);
      const lastCall = mockFetch.mock.calls[mockFetch.mock.calls.length - 1];
      expect(lastCall[0]).toContain('search=INV-001');
    });
  });

  it('handles filter change and triggers re-fetch', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        content: [],
        totalElements: 0,
        totalPages: 0,
        currentPage: 0,
        pageSize: 50,
      }),
    });

    const { AnalystDashboard } = await import('../AnalystDashboard.jsx');
    renderWithRouter(<AnalystDashboard />);

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalled();
    });

    const initialCallCount = mockFetch.mock.calls.length;

    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        content: [],
        totalElements: 0,
        totalPages: 0,
        currentPage: 0,
        pageSize: 50,
      }),
    });

    const statusFilter = screen.getByRole('combobox', { name: /status/i }) || screen.getByRole('listbox') || screen.getByRole('menu');
    fireEvent.change(statusFilter, { target: { value: 'QUEUED' } });

    const applyFilterButton = screen.getByRole('button', { name: /apply filter/i }) || screen.getByText(/apply/i);
    fireEvent.click(applyFilterButton);

    await waitFor(() => {
      expect(mockFetch.mock.calls.length).toBeGreaterThan(initialCallCount);
      const lastCall = mockFetch.mock.calls[mockFetch.mock.calls.length - 1];
      expect(lastCall[0]).toContain('status=QUEUED');
    });
  });

  it('renders a header bar', async () => {
    const { AnalystDashboard } = await import('../AnalystDashboard.jsx');
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        content: [],
        totalElements: 0,
        totalPages: 0,
        currentPage: 0,
        pageSize: 50,
      }),
    });
    renderWithRouter(<AnalystDashboard />);
    await waitFor(() => {
      expect(screen.getByRole('banner') || screen.getByRole('heading')).toBeInTheDocument();
    });
  });

  it('renders navigation link to client upload page', async () => {
    const { AnalystDashboard } = await import('../AnalystDashboard.jsx');
    renderWithRouter(<AnalystDashboard />);
    const link = screen.getByRole('link', { name: /upload/i }) || screen.getByText(/upload/i);
    expect(link).toBeInTheDocument();
  });
});
