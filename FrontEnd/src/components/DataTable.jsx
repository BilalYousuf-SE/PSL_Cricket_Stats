import '../motion.css';

// Generic scorecard-style table.
// columns: [{ key, label, align: 'left'|'right', render?: (row, i) => node }]
export default function DataTable({ columns, rows, onRowClick, rankFrom }) {
  return (
    <table className="data-table">
      <thead>
        <tr>
          {rankFrom !== undefined && <th style={{ width: 36 }}></th>}
          {columns.map((col) => (
            <th key={col.key} className={col.align === 'right' ? 'num' : undefined}>
              {col.label}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, i) => (
          <tr
            key={row.id ?? i}
            className={['row-reveal', onRowClick && 'clickable'].filter(Boolean).join(' ')}
            style={{ '--i': i }}
            onClick={onRowClick ? () => onRowClick(row) : undefined}
          >
            {rankFrom !== undefined && <td className="rank">{rankFrom + i}</td>}
            {columns.map((col) => (
              <td key={col.key} className={col.align === 'right' ? 'num' : undefined}>
                {col.render ? col.render(row, i) : row[col.key]}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
