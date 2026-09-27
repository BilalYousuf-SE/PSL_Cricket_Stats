export default function Loading({ rows = 4, label = 'Pulling the numbers from the scorebook…' }) {
  return (
    <div className="state-block" role="status" aria-live="polite">
      <p className="state-title">{label}</p>
      {Array.from({ length: rows }).map((_, i) => (
        <div
          key={i}
          className="skeleton-row"
          style={{ width: `${88 - i * 10}%` }}
        />
      ))}
    </div>
  );
}
