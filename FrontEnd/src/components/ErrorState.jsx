export function ErrorState({ message, onRetry }) {
  return (
    <div className="state-block error" role="alert">
      <p className="state-title">The scoreboard didn't update</p>
      <p style={{ margin: '0 0 14px' }}>{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          style={{
            background: 'transparent',
            border: '1px solid rgba(225,58,75,0.6)',
            color: '#f3a3ac',
            padding: '8px 14px',
            borderRadius: 3,
            fontFamily: 'var(--font-display)',
            fontWeight: 600,
            fontSize: 14,
          }}
        >
          Try again
        </button>
      )}
    </div>
  );
}

export function EmptyState({ title = 'Nothing on the board yet', message }) {
  return (
    <div className="state-block">
      <p className="state-title">{title}</p>
      {message && <p style={{ margin: 0 }}>{message}</p>}
    </div>
  );
}
