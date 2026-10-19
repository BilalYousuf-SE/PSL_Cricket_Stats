import { useState } from 'react';

/* ---------- config ---------- */
export const API = import.meta.env.VITE_API_URL || '';

/* ---------- api ---------- */
export async function api(method, url, body) {
  const r = await fetch(API + url, {
    method,
    headers: { 'Content-Type': 'application/json', 'X-Admin-Password': sessionStorage.getItem('cd_admin') || '' },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!r.ok) throw new Error(`${r.status} ${(await r.text()).slice(0, 200)}`);
  const t = await r.text();
  return t ? JSON.parse(t) : null;
}

export const asRows = (d) => (Array.isArray(d) ? d : d?.items || d?.data || d?.results || []);

/* ---------- field descriptor ----------
   n: field name sent in the request body
   t: 'text' | 'number' | 'bool' | 'date' | 'select'
   req: required?
   opts: { options: [...strings] | [{value,label}], numeric: bool }
     - plain strings => enum-style select, value sent as-is
     - {value,label} objects => foreign-key select; numeric:true sends Number(value)
*/
export const F = (n, t = 'text', req, opts = {}) => ({ n, t, req, ...opts });

/* ---------- create / edit modal ---------- */
export function FormModal({ ent, row, onClose, onSave }) {
  const editing = !!row;
  const base = Object.fromEntries(ent.f.map((f) => [f.n, row?.[f.n] ?? (f.t === 'bool' ? false : '')]));
  const initial = ent.resolveInitial && row ? { ...base, ...ent.resolveInitial(row) } : base;
  const [v, setV] = useState(initial);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');

  const submit = async (e) => {
    e.preventDefault();
    const body = {};
    for (const f of ent.f) {
      const x = v[f.n];
      if (f.t === 'bool') body[f.n] = !!x;
      else if (x === '' || x == null) continue;
      else if (f.t === 'number' || f.numeric) body[f.n] = Number(x);
      else body[f.n] = x;
    }
    setBusy(true);
    setErr('');
    try {
      await onSave(body);
      onClose();
    } catch (ex) {
      setErr(ex.message);
      setBusy(false);
    }
  };

  return (
    <div className="admin-overlay" onMouseDown={onClose}>
      <form className="scorecard admin-modal" onMouseDown={(e) => e.stopPropagation()} onSubmit={submit}>
        <div className="scorecard-header">
          <h3>{editing ? 'Edit' : 'Add'} · {ent.l}</h3>
          <button type="button" className="admin-btn" onClick={onClose}>✕</button>
        </div>
        <div className="admin-form admin-grid">
          {ent.f.map((f) => (
            <label key={f.n} className={f.t === 'bool' ? 'admin-check' : ''}>
              <span>{f.label || f.n}{f.req ? ' *' : ''}</span>
              {f.t === 'bool' ? (
                <input type="checkbox" checked={v[f.n]} onChange={(e) => setV({ ...v, [f.n]: e.target.checked })} />
              ) : f.t === 'select' ? (
                <select
                  className="admin-input"
                  required={!!f.req}
                  disabled={editing && f.n === ent.id}
                  value={v[f.n]}
                  onChange={(e) => setV({ ...v, [f.n]: e.target.value })}
                >
                  <option value="">—</option>
                  {(f.options || []).map((opt) =>
                    typeof opt === 'string' ? (
                      <option key={opt} value={opt}>{opt}</option>
                    ) : (
                      <option key={opt.value} value={opt.value}>{opt.label}</option>
                    )
                  )}
                </select>
              ) : (
                <input
                  className="admin-input"
                  type={f.t === 'number' ? 'number' : f.t === 'date' ? 'date' : 'text'}
                  step="any"
                  required={!!f.req}
                  disabled={editing && f.n === ent.id}
                  value={v[f.n]}
                  onChange={(e) => setV({ ...v, [f.n]: e.target.value })}
                />
              )}
            </label>
          ))}
        </div>
        {err && <div className="admin-err" style={{ margin: '0 20px' }}>{err}</div>}
        <div className="admin-actions">
          <button type="button" className="admin-btn" onClick={onClose}>Cancel</button>
          <button className="admin-btn primary press-fx" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        </div>
      </form>
    </div>
  );
}

/* ---------- generic list + add/edit/delete panel ----------
   ent.cols (optional): [{key,label,render?(row)}] for the display table.
   Falls back to deriving columns straight from ent.f when not given, since
   several entities (e.g. a scorecard row) show computed fields like
   strikeRate/economy that aren't part of the edit form at all.
*/
export function AdminEntityPanel({ ent, rows, state, err, onSave, onDelete, toolbarExtra, onRowClick, disabled }) {
  const [modal, setModal] = useState(null);
  const cols = ent.cols || [ent.id, ...ent.f.map((f) => f.n).filter((n) => n !== ent.id)].map((k) => ({ key: k, label: k }));

  return (
    <>
      <div className="admin-toolbar">
        {toolbarExtra}
        <span className="subtle admin-count">{rows.length} rows</span>
        <button className="admin-btn primary press-fx" disabled={disabled} onClick={() => setModal({ row: null })}>
          + Add {ent.l.replace(/s$/, '')}
        </button>
      </div>

      <div className="tab-panel" style={{ overflowX: 'auto' }}>
        {state === 'loading' ? (
          <div style={{ padding: 20 }}>{[0, 1, 2, 3].map((i) => <div key={i} className="skeleton-row" />)}</div>
        ) : state === 'error' ? (
          <div className="admin-empty admin-err">{err}</div>
        ) : rows.length === 0 ? (
          <div className="admin-empty">Nothing here yet.</div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                {cols.map((c) => <th key={c.key}>{c.label}</th>)}
                <th className="num">Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r, i) => (
                <tr
                  key={r[ent.id] ?? i}
                  className="row-reveal"
                  style={{ '--i': i, cursor: onRowClick ? 'pointer' : undefined }}
                  onClick={onRowClick ? () => onRowClick(r) : undefined}
                >
                  {cols.map((c) => (
                    <td key={c.key}>
                      {c.render ? c.render(r) : typeof r[c.key] === 'boolean' ? (r[c.key] ? '✓' : '—') : String(r[c.key] ?? '—')}
                    </td>
                  ))}
                  <td className="num admin-row-actions" onClick={(e) => e.stopPropagation()}>
                    <button className="admin-btn sm" onClick={() => setModal({ row: r })}>Edit</button>
                    <button className="admin-btn sm danger" onClick={() => onDelete(r)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {modal && (
        <FormModal
          ent={ent}
          row={modal.row}
          onClose={() => setModal(null)}
          onSave={(body) => onSave(body, modal.row)}
        />
      )}
    </>
  );
}
