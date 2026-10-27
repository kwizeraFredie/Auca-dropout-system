export default function DataTable({ columns, rows, emptyMessage = 'No records found.' }) {
  if (!rows.length) return <div className="table-empty">{emptyMessage}</div>;
  return <div className="table-scroll"><table className="data-table"><thead><tr>{columns.map((column) => <th key={column.key}>{column.label}</th>)}</tr></thead><tbody>
    {rows.map((row, index) => <tr key={row.id ?? index}>{columns.map((column) => <td key={column.key}>{column.render ? column.render(row) : row[column.key] ?? '—'}</td>)}</tr>)}
  </tbody></table></div>;
}
