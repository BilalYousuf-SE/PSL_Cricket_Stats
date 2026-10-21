import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { FormModal } from './adminCore.jsx';
import { adminApi as api } from '../api.js';
import { toArray as asRows } from '../fields.js';
import { useApi } from '../useApi.js';
import '../motion.css';
import '../admin.css';
import { ENTITIES, MATCH_ENTITIES } from '../adminEntities.js';

/* ---------- config ---------- */
const PASS = import.meta.env.VITE_ADMIN_PASSWORD;

/* ---------- login ---------- */
function Login({ onOk }) {
  const [pw, setPw] = useState('');
  const [err, setErr] = useState('');
  const submit = (e) => {
    e.preventDefault();
    if (pw && pw === PASS) { sessionStorage.setItem('cd_admin', pw); onOk(); }
    else setErr('Wrong password');
  };
  return (
    <div className="admin-login">
      <form className="scorecard admin-login-card" onSubmit={submit}>
        <div className="scorecard-header"><h3>Admin sign-in</h3></div>
        <div className="admin-form">
          <input className="admin-input" type="password" autoFocus placeholder="Password"
            value={pw} onChange={(e) => { setPw(e.target.value); setErr(''); }} />
          {err && <div className="admin-err">{err}</div>}
          {!PASS && <div className="admin-err">VITE_ADMIN_PASSWORD is not set.</div>}
          <button className="admin-btn primary" type="submit">Enter</button>
        </div>
      </form>
    </div>
  );
}

/* ---------- shared table+modal panel for one entity ---------- */
function EntityPanel({ ent, url, refData, onRowClick, onChanged }) {
  const { data, loading, error, reload } = useApi(url, [], { enabled: !!url });
  const rows = asRows(data);
  const groups = ent.groupRows ? ent.groupRows(rows) : [{ key: 'all', rows }];
  const [err, setErr] = useState('');
  const [modal, setModal] = useState(null);
  const state = loading ? 'loading' : error || err ? 'error' : 'idle';
  const load = async () => { setErr(''); reload(); };

  const save = async (body) => {
    if (modal.row) await api('PUT', `${url}/${modal.row[ent.id]}`, body);
    else await api('POST', url, body);
    await load();
    if (onChanged) onChanged();
  };
  const del = async (r) => {
    if (!window.confirm(`Delete this ${ent.l.toLowerCase().replace(/s$/, '')}? This can't be undone.`)) return;
    try {
      await api('DELETE', `${url}/${r[ent.id]}`);
      await load();
      if (onChanged) onChanged();
    } catch (e) { setErr(e.message); }
  };

  return (
    <>
      {ent.description && <p className="subtle admin-panel-hint">{ent.description}</p>}
      <div className="admin-toolbar">
        <span className="subtle admin-count">{rows.length} rows</span>
        {!ent.readOnly && (
          <button className="admin-btn primary press-fx" disabled={!url} onClick={() => setModal({ row: null })}>
            + Add {ent.l.replace(/s$/, '')}
          </button>
        )}
      </div>

      <div className="tab-panel" style={{ overflowX: 'auto' }}>
        {!url ? (
          <div className="admin-empty">Pick a match to manage its {ent.l.toLowerCase()}.</div>
        ) : state === 'loading' ? (
          <div style={{ padding: 20 }}>{[0, 1, 2, 3].map((i) => <div key={i} className="skeleton-row" />)}</div>
        ) : state === 'error' ? (
          <div className="admin-empty admin-err">{error || err}</div>
        ) : rows.length === 0 ? (
          <div className="admin-empty">Nothing here yet.</div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                {ent.cols.map(([key, label]) => <th key={key}>{label}</th>)}
                {!ent.readOnly && <th className="num">Actions</th>}
              </tr>
            </thead>
            {groups.map((group) => (
              <tbody key={group.key}>
                {group.label && (
                  <tr className="admin-group-heading">
                    <th scope="rowgroup" colSpan={ent.cols.length + (ent.readOnly ? 0 : 1)}>{group.label}</th>
                  </tr>
                )}
                {group.rows.map((r, i) => (
                  <tr
                    key={r[ent.id] ?? i}
                    className="row-reveal"
                    style={{ '--i': i, cursor: onRowClick ? 'pointer' : 'default' }}
                    onClick={onRowClick ? () => onRowClick(r) : undefined}
                  >
                    {ent.cols.map(([key]) => (
                      <td key={key}>{typeof r[key] === 'boolean' ? (r[key] ? '✓' : '—') : String(r[key] ?? '—')}</td>
                    ))}
                    {!ent.readOnly && (
                      <td className="num admin-row-actions" onClick={(e) => e.stopPropagation()}>
                        <button className="admin-btn sm" onClick={() => setModal({ row: r })}>Edit</button>
                        <button className="admin-btn sm danger" onClick={() => del(r)}>Delete</button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            ))}
          </table>
        )}
      </div>

      {modal && (
        <FormModal ent={ent} row={modal.row} refData={refData} onClose={() => setModal(null)} onSave={save} />
      )}
    </>
  );
}

/* ---------- dashboard ---------- */
export default function Admin() {
  const { matchId } = useParams();
  const navigate = useNavigate();
  const [routeError, setRouteError] = useState('');
  const [authed, setAuthed] = useState(() => !!PASS && sessionStorage.getItem('cd_admin') === PASS);
  const [tab, setTab] = useState('teams');
  const [refData, setRefData] = useState({ teams: [], seasons: [], players: [], squad: [] });
  const [activeMatch, setActiveMatch] = useState(null);
  const [matchTab, setMatchTab] = useState('squad');

  const loadRefData = useCallback(async () => {
    try {
      const [teams, seasons, players] = await Promise.all([
        api('GET', '/teams'), api('GET', '/season'), api('GET', '/players'),
      ]);
      setRefData((prev) => ({
        ...prev,
        teams: asRows(teams).map((t) => ({ value: t.teamId, label: t.teamName })),
        seasons: asRows(seasons).map((s) => ({ value: s.seasonId, label: String(s.year) })),
        players: asRows(players).map((p) => ({ value: p.id, label: p.fullName })),
      }));
    } catch { /* individual panels still surface their own load errors */ }
  }, []);

  // Delivery and partnership player dropdowns are scoped to the
  // active match's squad rather than every player ever recorded — reloaded
  // whenever the Squad tab changes so a just-added player shows up right away.
  const loadSquadRef = useCallback(async (matchId) => {
    try {
      const squad = await api('GET', `/matches/${matchId}/squad`);
      setRefData((prev) => ({
        ...prev,
        squad: asRows(squad).map((s) => ({
          value: s.playerId, label: s.playerName, teamId: s.teamId, teamName: s.teamName, isPlaying: s.isPlaying,
        })),
      }));
    } catch { /* player selects stay empty until the squad can be loaded */ }
  }, []);

  useEffect(() => { if (authed) loadRefData(); }, [authed, loadRefData]);
  useEffect(() => { if (activeMatch) loadSquadRef(activeMatch.matchId); }, [activeMatch, loadSquadRef]);

  useEffect(() => {
    if (!authed || !matchId) return;
    let active = true;
    setRouteError('');
    api('GET', `/matches/${matchId}`).then(
      (match) => { if (active) { setActiveMatch(match); setMatchTab('squad'); } },
      (error) => { if (active) setRouteError(error.message); }
    );
    return () => { active = false; };
  }, [authed, matchId]);

  if (!authed) return <Login onOk={() => setAuthed(true)} />;

  const logout = () => { sessionStorage.removeItem('cd_admin'); setAuthed(false); };

  // ---- Nested Match Admin view ----
  if (activeMatch) {
    const mEnt = MATCH_ENTITIES.find((e) => e.k === matchTab);
    const matchUrl = mEnt.p(activeMatch.matchId);
    return (
      <div className="app-shell">
        <div className="admin-top">
          <div>
            <button className="admin-btn" onClick={() => { setActiveMatch(null); navigate('/admin'); }} style={{ marginBottom: 10 }}>
              ← Back to Matches
            </button>
            <h1 className="page-title" style={{ marginBottom: 0 }}>
              {activeMatch.teamAName} vs {activeMatch.teamBName}
            </h1>
            <p className="page-subtitle" style={{ marginBottom: 0 }}>
              {activeMatch.venue} · {activeMatch.matchDate} · Match #{activeMatch.matchId}
            </p>
          </div>
          <button className="admin-btn" onClick={logout}>Sign out</button>
        </div>

        <div className="scorecard reveal reveal-visible">
          <div className="tab-row">
            {MATCH_ENTITIES.map((e) => (
              <button key={e.k} className={`tab-btn${e.k === matchTab ? ' active' : ''}`}
                onClick={() => setMatchTab(e.k)}>{e.l}</button>
            ))}
          </div>
          {matchTab === 'batting' || matchTab === 'bowling' ? (
            <p className="subtle" style={{ padding: '10px 20px 0' }}>
              Derived automatically from Deliveries — edit a delivery to correct these numbers.
            </p>
          ) : null}
          <EntityPanel
            key={matchTab}
            ent={mEnt}
            url={matchUrl}
            refData={refData}
            onChanged={matchTab === 'squad' ? () => loadSquadRef(activeMatch.matchId) : undefined}
          />
        </div>
      </div>
    );
  }

  // ---- Main Admin dashboard ----
  const ent = ENTITIES.find((e) => e.k === tab);
  return (
    <div className="app-shell">
      <div className="admin-top">
        <div>
          <h1 className="page-title">Admin</h1>
          <p className="page-subtitle" style={{ marginBottom: 0 }}>Create, update and delete league data.</p>
          {routeError && <div className="admin-err">{routeError}</div>}
        </div>
        <button className="admin-btn" onClick={logout}>Sign out</button>
      </div>

      <div className="scorecard reveal reveal-visible">
        <div className="tab-row">
          {ENTITIES.map((e) => (
            <button key={e.k} className={`tab-btn${e.k === tab ? ' active' : ''}`}
              onClick={() => setTab(e.k)}>{e.l}</button>
          ))}
        </div>
        <EntityPanel
          key={tab}
          ent={ent}
          url={ent.p}
          refData={refData}
          onChanged={loadRefData}
          onRowClick={ent.clickable ? (row) => { setActiveMatch(row); setMatchTab('squad'); } : undefined}
        />
      </div>
    </div>
  );
}
