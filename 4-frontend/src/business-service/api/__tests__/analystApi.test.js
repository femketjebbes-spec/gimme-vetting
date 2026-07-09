import { describe, it, expect, jest } from '@jest/globals';

// Mock fetch globally
const mockFetch = jest.fn();
global.fetch = mockFetch;

describe('analystApi service', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockFetch.mockReset();
  });

  describe('fetchInvoiceList', () => {
    it('calls GET /api/v1/analyst/invoices with default parameters', async () => {
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

      const { fetchInvoiceList } = await import('../analystApi.js');
      await fetchInvoiceList();

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/analyst/invoices?page=0&size=50&sort=id,asc',
        { method: 'GET' }
      );
    });

    it('calls GET /api/v1/analyst/invoices with custom query parameters', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          content: [],
          totalElements: 0,
          totalPages: 0,
          currentPage: 1,
          pageSize: 20,
        }),
      });

      const { fetchInvoiceList } = await import('../analystApi.js');
      await fetchInvoiceList({ page: 1, size: 20, status: 'QUEUED', search: 'INV' });

      const calledUrl = mockFetch.mock.calls[0][0];
      expect(calledUrl).toContain('/api/v1/analyst/invoices');
      expect(calledUrl).toContain('page=1');
      expect(calledUrl).toContain('size=20');
      expect(calledUrl).toContain('status=QUEUED');
      expect(calledUrl).toContain('search=INV');
    });

    it('returns parsed JSON response for successful request', async () => {
      const mockResponse = {
        content: [{ id: 1, invoiceNumber: 'INV-001' }],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 50,
      };
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockResponse,
      });

      const { fetchInvoiceList } = await import('../analystApi.js');
      const result = await fetchInvoiceList();

      expect(result).toEqual(mockResponse);
    });

    it('throws an error when API returns non-ok status', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        json: async () => ({ error: 'Internal Server Error' }),
      });

      const { fetchInvoiceList } = await import('../analystApi.js');

      await expect(fetchInvoiceList()).rejects.toThrow();
    });
  });

  describe('fetchInvoiceDetail', () => {
    it('calls GET /api/v1/analyst/invoices/{id} with valid id', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
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
        }),
      });

      const { fetchInvoiceDetail } = await import('../analystApi.js');
      await fetchInvoiceDetail(1);

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/analyst/invoices/1',
        { method: 'GET' }
      );
    });

    it('returns parsed JSON response for successful detail request', async () => {
      const mockDetail = {
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
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockDetail,
      });

      const { fetchInvoiceDetail } = await import('../analystApi.js');
      const result = await fetchInvoiceDetail(1);

      expect(result).toEqual(mockDetail);
    });

    it('throws an error when API returns 404 Not Found', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 404,
        json: async () => ({ error: 'Not Found', message: 'Invoice with id 9999 not found' }),
      });

      const { fetchInvoiceDetail } = await import('../analystApi.js');

      await expect(fetchInvoiceDetail(9999)).rejects.toThrow();
    });

    it('throws an error for invalid id parameter (zero)', async () => {
      const { fetchInvoiceDetail } = await import('../analystApi.js');

      await expect(fetchInvoiceDetail(0)).rejects.toThrow('Invoice id must be a positive integer');
    });

    it('throws an error for invalid id parameter (negative)', async () => {
      const { fetchInvoiceDetail } = await import('../analystApi.js');

      await expect(fetchInvoiceDetail(-1)).rejects.toThrow('Invoice id must be a positive integer');
    });
  });
});
