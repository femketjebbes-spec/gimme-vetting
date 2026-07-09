import { describe, it, expect } from '@jest/globals';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

describe('StatusBadge component', () => {
  it('renders with QUEUED status', async () => {
    const { StatusBadge } = await import('../StatusBadge.jsx');
    const { container } = render(<StatusBadge status="QUEUED" />);
    const badge = screen.getByText('QUEUED');
    expect(badge).toBeInTheDocument();
  });

  it('renders with REJECTED_TYPE_A status', async () => {
    const { StatusBadge } = await import('../StatusBadge.jsx');
    const { container } = render(<StatusBadge status="REJECTED_TYPE_A" />);
    const badge = screen.getByText('REJECTED_TYPE_A');
    expect(badge).toBeInTheDocument();
  });

  it('renders with REJECTED_TYPE_B status', async () => {
    const { StatusBadge } = await import('../StatusBadge.jsx');
    const { container } = render(<StatusBadge status="REJECTED_TYPE_B" />);
    const badge = screen.getByText('REJECTED_TYPE_B');
    expect(badge).toBeInTheDocument();
  });

  it('applies correct CSS class for status color', async () => {
    const { StatusBadge } = await import('../StatusBadge.jsx');
    const { container } = render(<StatusBadge status="QUEUED" />);
    const badge = container.querySelector('.status-badge');
    expect(badge).toHaveClass('status-badge-queued');
  });

  it('applies correct CSS class for rejection status', async () => {
    const { StatusBadge } = await import('../StatusBadge.jsx');
    const { container } = render(<StatusBadge status="REJECTED_TYPE_A" />);
    const badge = container.querySelector('.status-badge');
    expect(badge).toHaveClass('status-badge-rejected');
  });

  it('handles null status gracefully', async () => {
    const { StatusBadge } = await import('../StatusBadge.jsx');
    render(<StatusBadge status={null} />);
    expect(screen.getByText('QUEUED')).toBeInTheDocument();
  });
});
