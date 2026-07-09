/**
 * StatusBadge — renders a status indicator with appropriate color class.
 *
 * Status values (per API contract):
 *   QUEUED        → blue tint (status-badge-queued)
 *   REJECTED_TYPE_A → red tint (status-badge-rejected)
 *   REJECTED_TYPE_B → red tint (status-badge-rejected)
 */

function StatusBadge({ status }) {
  const displayStatus = status || 'QUEUED';
  const className = displayStatus === 'QUEUED'
    ? 'status-badge status-badge-queued'
    : 'status-badge status-badge-rejected';

  return (
    <span className={className} data-testid="status-badge">
      {displayStatus}
    </span>
  );
}

export { StatusBadge };
export default StatusBadge;
