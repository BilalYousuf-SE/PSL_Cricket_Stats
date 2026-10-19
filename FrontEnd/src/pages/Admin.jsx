import { useCallback, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import '../motion.css';
import '../admin.css';

/* ---------- config ---------- */
const API = import.meta.env.VITE_API_URL || '';
const PASS = import.meta.env.VITE_ADMIN_PASSWORD;

const F = (n, t = 'text', req, extra = {}) => ({ n, t, req, ...extra });

const ROLE_OPTIONS = ['Batsman', 'FastBaller', 'OffSpinner', 'LegSpinner', 'AllRounder', 'WicketKeeper'];
const STYLE_OPTIONS = ['Right', 'Left'];
const FORMAT_OPTIONS = ['T20', 'ODI', 'Test', 'SUPEROVER'];
const TOSS_OPTIONS = ['bat', 'bowl'];
const RESULT_TYPE_OPTIONS = ['runs', 'wickets', 'tie', 'no_result'];

// Top-level entities managed from the main Admin tabs. `cols` is
// [fieldKey, columnLabel] and is independent of `f` (the create/edit form
// fields) — the table shows friendly read fields (names), the form uses
// the raw IDs the backend's write endpoints actually expect.
const ENTITIES = [
  {
    k: 'teams', l: 'Teams', p: '/teams', id: 'teamId',
    cols: [['teamId', 'ID'], ['teamName', 'Name']],
    f: [F('teamName', 'text', 1)],
  },
  {
    k: 'players', l: 'Players', p: '/players', id: 'id',
    cols: [['id', 'ID'], ['fullName', 'Name'], ['role', 'Role'], ['nationality', 'Nationality']],
    f: [
      F('firstName', 'text', 1),
      F('lastName', 'text', 1),
      F('fullName', 'text', 1),
      F('age', 'number'),
      F('role', 'select', 1, { options: ROLE_OPTIONS }),
      F('nationality', 'text', 1),
      F('battingStyle', 'select', 0, { options: STYLE_OPTIONS }),
      F('bowlingStyle', 'select', 0, { options: STYLE_OPTIONS }),
    ],
  },
  {
    k: 'seasons', l: 'Seasons', p: '/season', id: 'seasonId',
    cols: [['seasonId', 'ID'], ['year', 'Year']],
    f: [F('year', 'number', 1)],
  },
  {
    k: 'matches', l: 'Matches', p: '/matches', id: 'matchId',
    cols: [
      ['matchId', 'ID'], ['matchDate', 'Date'], ['teamAName', 'Team A'],
      ['teamBName', 'Team B'], ['venue', 'Venue'], ['winnerName', 'Winner'],
    ],
    f: [
      F('matchDate', 'date', 1),
      F('venue', 'text', 1),
      F('format', 'select', 1, { options: FORMAT_OPTIONS }),
      F('teamAId', 'select', 1, { optionsFrom: 'teams' }),
      F('teamBId', 'select', 1, { optionsFrom: 'teams' }),
      F('seasonId', 'select', 1, { optionsFrom: 'seasons' }),
      F('tossWinnerId', 'select', 0, { optionsFrom: 'teams' }),
      F('tossDecision', 'select', 0, { options: TOSS_OPTIONS }),
      F('winnerId', 'select', 0, { optionsFrom: 'teams' }),
      F('resultMargin', 'number'),
      F('resultType', 'select', 0, { options: RESULT_TYPE_OPTIONS }),
      F('playerOfMatchId', 'select', 0, { optionsFrom: 'players' }),
    ],
    // GET /matches gives teamAId/teamBId/playerOfMatchId directly, but only
    // *names* for season/toss winner/winner (seasonYear, tossWinnerName,
    // winnerName) — resolved here against the loaded reference lists so the
    // edit form opens pre-filled instead of blank for those three.
    resolveInitial: (row, refData) => ({
      teamAId: row.teamAId ?? '',
      teamBId: row.teamBId ?? '',
      seasonId: row.seasonId ?? (refData.seasons || []).find((s) => s.label === String(row.seasonYear))?.value ?? '',
      tossWinnerId: row.tossWinnerId ?? (row.tossWinnerName
        ? (refData.teams || []).find((t) => t.label === row.tossWinnerName)?.value ?? ''
        : ''),
      winnerId: row.winnerId ?? (row.winnerName
        ? (refData.teams || []).find((t) => t.label === row.winnerName)?.value ?? ''
        : ''),
      playerOfMatchId: row.playerOfMatchId ?? '',
    }),
    clickable: true, // clicking a row (not the Edit/Delete buttons) opens the Match Admin view
  },
];

// Nested entities scoped to a single match, opened by clicking a match row.
// Batting/Bowling have no `f` (no Add button, no Edit/Delete) — they're
// derived automatically from Deliveries on the backend and shown read-only
// so you can sanity-check the numbers after editing deliveries.
const MATCH_ENTITIES = [
  {
    k: 'squad', l: 'Squad', p: (m) => `/matches/${m}/squad`, id: 'playerId',
    cols: [
      ['playerId', 'Player ID'], ['playerName', 'Player'], ['teamName', 'Team'],
      ['isCaptain', 'Capt'], ['isKeeper', 'Keeper'], ['isPlaying', 'Playing XI'],
    ],
    f: [
      F('playerId', 'select', 1, { optionsFrom: 'players' }),
      F('teamId', 'select', 1, { optionsFrom: 'teams' }),
      F('isCaptain', 'bool'),
      F('isKeeper', 'bool'),
      F('isPlaying', 'bool'),
    ],
  },
  {
    k: 'batting', l: 'Batting', p: (m) => `/matches/${m}/batting-scorecard`, id: 'playerId',
    cols: [
      ['playerId', 'Player ID'], ['playerName', 'Player'], ['runs', 'Runs'],
      ['ballsFaced', 'Balls'], ['fours', '4s'], ['sixes', '6s'], ['isOut', 'Out'],
    ],
    readOnly: true,
  },
  {
    k: 'bowling', l: 'Bowling', p: (m) => `/matches/${m}/bowling-scorecard`, id: 'playerId',
    cols: [
      ['playerId', 'Player ID'], ['playerName', 'Player'], ['oversBowled', 'Overs'],
      ['maidens', 'Maidens'], ['runsConceded', 'Runs'], ['wickets', 'Wkts'], ['economy', 'Econ'],
    ],
    readOnly: true,
  },
  {
    k: 'deliveries', l: 'Deliveries', p: (m) => `/matches/${m}/deliveries`, id: 'deliveryId',
    cols: [
      ['deliveryId', 'ID'], ['innings', 'Inn'], ['overNum', 'Over'], ['ballNum', 'Ball'],
      ['batterName', 'Batter'], ['bowlerName', 'Bowler'], ['runsBatter', 'Runs'], ['wicketType', 'Wicket'],
    ],
    // batterId/nonStrikerId/bowlerId/playerDismissedId are scoped to this
    // match's squad (refData.squad), not the whole player table — far
    // easier to pick the right person from ~22 names than from everyone
    // who's ever played.
    f: [
      F('innings', 'number', 1),
      F('overNum', 'number', 1),
      F('ballNum', 'number', 1),
      F('batterId', 'select', 1, { optionsFrom: 'squad' }),
      F('nonStrikerId', 'select', 0, { optionsFrom: 'squad' }),
      F('bowlerId', 'select', 1, { optionsFrom: 'squad' }),
      F('runsBatter', 'number'),
      F('runsExtras', 'number'),
      F('extraType', 'text'),
      F('wicketType', 'text'),
      F('playerDismissedId', 'select', 0, { optionsFrom: 'squad' }),
    ],
    // GET /matches/{id}/deliveries only returns names (batterName,
    // nonStrikerName, bowlerName, playerDismissedName), never the IDs the
    // write endpoint needs — resolved here against the match's squad. Also
    // bridges extrasType (what the read DTO calls it) to extraType (what
    // the entity's own field — and the write endpoint — actually expects).
    resolveInitial: (row, refData) => ({
      batterId: (refData.squad || []).find((s) => s.label === row.batterName)?.value ?? '',
      nonStrikerId: row.nonStrikerName
        ? (refData.squad || []).find((s) => s.label === row.nonStrikerName)?.value ?? ''
        : '',
      bowlerId: (refData.squad || []).find((s) => s.label === row.bowlerName)?.value ?? '',
      playerDismissedId: row.playerDismissedName
        ? (refData.squad || []).find((s) => s.label === row.playerDismissedName)?.value ?? ''
        : '',
      extraType: row.extraType ?? row.extrasType ?? '',
    }),
  },
];

/* ---------- api ---------- */
async function api(method, url, body) {
  const r = await fetch(API + url, {
    method,
    headers: { 'Content-Type': 'application/json', 'X-Admin-Password': sessionStorage.getItem('cd_admin') || '' },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!r.ok) throw new Error(`${r.status} ${(await r.text()).slice(0, 200)}`);
  const t = await r.text();
  return t ? JSON.parse(t) : null;
}
const asRows = (d) => (Array.isArray(d) ? d : d?.items || d?.data || d?.results || []);

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

/* ---------- create / edit modal (rendered via portal) ---------- */
function FormModal({ ent, row, refData, onClose, onSave }) {
  const editing = !!row;
  const [v, setV] = useState(() => {
    const base = Object.fromEntries(ent.f.map((f) => [f.n, row?.[f.n] ?? (f.t === 'bool' ? false : '')]));
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
    const body = {};
    for (const f of ent.f) {
      const x = v[f.n];
      if (f.t === 'bool') { body[f.n] = !!x; continue; }
      if (x === '' || x == null) continue;
      body[f.n] = (f.t === 'number' || (f.t === 'select' && f.optionsFrom)) ? Number(x) : x;
    }
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

/* ---------- shared table+modal panel for one entity ---------- */
function EntityPanel({ ent, url, refData, onRowClick, onChanged }) {
  const [rows, setRows] = useState([]);
  const [state, setState] = useState('idle');
  const [err, setErr] = useState('');
  const [modal, setModal] = useState(null);

  const load = useCallback(async () => {
    if (!url) { setRows([]); return; }
    setState('loading'); setErr('');
    try {
      setRows(asRows(await api('GET', url)));
      setState('idle');
    } catch (e) { setErr(e.message); setState('error'); }
  }, [url]);

  useEffect(() => { load(); }, [load]);

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
    } catch (e) { setErr(e.message); setState('error'); }
  };

  return (
    <>
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
          <div className="admin-empty admin-err">{err}</div>
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
            <tbody>
              {rows.map((r, i) => (
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

  // Deliveries' batter/bowler/dismissed-player dropdowns are scoped to the
  // active match's squad rather than every player ever recorded — reloaded
  // whenever the Squad tab changes so a just-added player shows up right away.
  const loadSquadRef = useCallback(async (matchId) => {
    try {
      const squad = await api('GET', `/matches/${matchId}/squad`);
      setRefData((prev) => ({
        ...prev,
        squad: asRows(squad).map((s) => ({ value: s.playerId, label: s.playerName })),
      }));
    } catch { /* deliveries' player selects just stay empty until this succeeds */ }
  }, []);

  useEffect(() => { if (authed) loadRefData(); }, [authed, loadRefData]);
  useEffect(() => { if (activeMatch) loadSquadRef(activeMatch.matchId); }, [activeMatch, loadSquadRef]);

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
            <button className="admin-btn" onClick={() => setActiveMatch(null)} style={{ marginBottom: 10 }}>
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
          onRowClick={ent.clickable ? (row) => { setActiveMatch(row); setMatchTab('squad'); } : undefined}
        />
      </div>
    </div>
  );
}