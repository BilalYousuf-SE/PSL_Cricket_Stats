import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { serializeForm } from '../adminForm.js';

/* ---------- create / edit modal (rendered via portal) ---------- */
export function FormModal({ ent, row, refData, onClose, onSave }) {
  const editing = !!row;
  const [v, setV] = useState(() => {
    const base = Object.fromEntries(ent.f.map((f) => [f.n, row?.[f.n] ?? f.defaultValue ?? (f.t === 'bool' ? false : '')]));
    return row && ent.resolveInitial ? { ...base, ...ent.resolveInitial(row, refData) } : base;
  });
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');

  // Lock the background in place while the modal is open — besides being
  // correct modal behavior generally, this guarantees the fixed-position
  // overlay lines up with what's actually on screen even on a long table,
  // regardless of any transform motion.css may apply elsewhere on the page.
  useEffect(() => {
    const scrollY = window.scrollY;
    const { style } = document.body;
    style.position = 'fixed';
    style.top = `-${scrollY}px`;
    style.left = '0';
    style.right = '0';
    return () => {
      style.position = '';
      style.top = '';
      style.left = '';
      style.right = '';
      window.scrollTo(0, scrollY);
    };
  }, []);

  const submit = async (e) => {
    e.preventDefault();
    const body = serializeForm(ent.f, v);
    setBusy(true); setErr('');
    try { await onSave(body); onClose(); } catch (ex) { setErr(ex.message); setBusy(false); }
  };

  const optionsFor = (f) => {
    if (f.options) return f.options.map((o) => ({ value: o, label: o }));
    if (f.optionsFrom) return refData[f.optionsFrom] || [];
    return [];
  };

  return createPortal(
    <div className="admin-overlay" onMouseDown={onClose}>
      <form className="scorecard admin-modal" onMouseDown={(e) => e.stopPropagation()} onSubmit={submit}>
        <div className="scorecard-header">
          <h3>{editing ? 'Edit' : 'Add'} · {ent.l}</h3>
          <button type="button" className="admin-btn" onClick={onClose}>✕</button>
        </div>
        <div className="admin-form admin-grid">
          {ent.f.map((f) => (
            <label key={f.n} className={f.t === 'bool' ? 'admin-check' : ''}>
              <span>{f.n}{f.req ? ' *' : ''}</span>
              {f.t === 'bool' ? (
                <input type="checkbox" checked={v[f.n]} onChange={(e) => setV({ ...v, [f.n]: e.target.checked })} />
              ) : f.t === 'select' ? (
                <select className="admin-input" required={!!f.req} disabled={editing && f.n === ent.id}
                  value={v[f.n]} onChange={(e) => setV({ ...v, [f.n]: e.target.value })}>
                  <option value="">—</option>
                  {optionsFor(f).map((o) => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
              ) : (
                <input className="admin-input" type={f.t === 'number' ? 'number' : f.t === 'date' ? 'date' : 'text'}
                  step="any" required={!!f.req} disabled={editing && f.n === ent.id}
                  value={v[f.n]} onChange={(e) => setV({ ...v, [f.n]: e.target.value })} />
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
    </div>,
    document.body
  );
}
