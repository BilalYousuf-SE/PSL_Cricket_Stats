import { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import '../motion.css';
import '../admin.css';
import { api, asRows, F, AdminEntityPanel } from './adminCore.jsx';

// One nested entity's list + add/edit/delete, scoped under a single match.
// ent.path(matchId) gives the base URL; update/delete append /{ent.id value}.
// onChanged fires after a successful save/delete so the parent can refresh
// the squad list (used to build player dropdowns on the other tabs).
function MatchNestedTab({ ent, matchId, onChanged }) {
  const [rows, setRows] = useState([]);
  const [state, setState] = useState('loading');
  const [err, setErr] = useState('');
  const path = ent.path(matchId);

  const load = useCallback(async () => {
    setState('loading');
    setErr('');
    try {
      setRows(asRows(await api('GET', path)));
      setState('idle');
    } catch (e) {
      setErr(e.message);
      setState('error');
    }
  }, [path]);

  useEffect(() => { load(); }, [load]);

  const save = async (body, row) => {
    if (row) await api('PUT', `${path}/${row[ent.id]}`, body);
    else await api('POST', path, body);
    await load();
    if (onChanged) onChanged();
  };
  const del = async (r) => {
    if (!window.confirm(`Delete this ${ent.l.toLowerCase().replace(/s$/, '')} entry? This can't be undone.`)) return;
    try {
      await api('DELETE', `${path}/${r[ent.id]}`);
      await load();
      if (onChanged) onChanged();
    } catch (e) {
      setErr(e.message);
      setState('error');
    }
  };

  return <AdminEntityPanel ent={ent} rows={rows} state={state} err={err} onSave={save} onDelete={del} />;
}

export default function AdminMatchDetail() {
  const { matchId } = useParams();
  const navigate = useNavigate();
  const [match, setMatch] = useState(null);
  const [squad, setSquad] = useState([]);
  const [players, setPlayers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState('');
  const [tab, setTab] = useState('squad');

  const loadHeader = useCallback(async () => {
    setLoading(true);
    setErr('');
    try {
      const [m, s, p] = await Promise.all([
        api('GET', `/matches/${matchId}`),
        api('GET', `/matches/${matchId}/squad`),
        api('GET', '/players'),
      ]);
      setMatch(m);
      setSquad(asRows(s));
      setPlayers(asRows(p));
    } catch (e) {
      setErr(e.message);
    }
    setLoading(false);
  }, [matchId]);

  useEffect(() => { loadHeader(); }, [loadHeader]);

  // Re-fetch just the squad after it changes, so Batting/Bowling/Deliveries'
  // player dropdowns (built from the squad, not the full player list) stay
  // current without reloading the whole page.
  const reloadSquad = useCallback(async () => {
    try {
      setSquad(asRows(await api('GET', `/matches/${matchId}/squad`)));
    } catch {
      /* non-fatal — the dropdown just won't reflect the latest change yet */
    }
  }, [matchId]);

  if (loading) {
    return (
      <div className="app-shell">
        <div style={{ padding: 20 }}>{[0, 1, 2].map((i) => <div key={i} className="skeleton-row" />)}</div>
      </div>
    );
  }

  const playerOptions = players.map((p) => ({ value: p.id, label: p.fullName }));
  const squadOptions = squad.map((s) => ({ value: s.playerId, label: s.playerName }));
  const teamOptions = match
    ? [
        { value: match.teamAId, label: match.teamAName },
        { value: match.teamBId, label: match.teamBName },
      ]
    : [];

  // MatchSquadDTO: playerId/teamId are plain values (no name-resolution
  // problem like the Matches form has), so this can stay generic.
  const squadEnt = {
    k: 'squad', l: 'Squad', id: 'playerId',
    path: (id) => `/matches/${id}/squad`,
    f: [
      F('playerId', 'select', 1, { options: playerOptions, numeric: true }),
      F('teamId', 'select', 1, { options: teamOptions, numeric: true }),
      F('isCaptain', 'bool'),
      F('isKeeper', 'bool'),
      F('isPlaying', 'bool'),
    ],
    cols: [
      { key: 'playerId', label: 'playerId' },
      { key: 'playerName', label: 'playerName' },
      { key: 'teamId', label: 'teamId' },
      { key: 'teamName', label: 'teamName' },
      { key: 'isCaptain', label: 'captain' },
      { key: 'isKeeper', label: 'keeper' },
      { key: 'isPlaying', label: 'playing' },
    ],
  };

  // ScorecardBatter has no 'innings' column on the entity — only
  // match+player+runs/balls/fours/sixes/outDescription/isOut/battingPosition.
  const battingEnt = {
    k: 'batting', l: 'Batting', id: 'playerId',
    path: (id) => `/matches/${id}/batting-scorecard`,
    f: [
      F('playerId', 'select', 1, { options: squadOptions, numeric: true }),
      F('runs', 'number'),
      F('ballsFaced', 'number'),
      F('fours', 'number'),
      F('sixes', 'number'),
      F('outDescription', 'text'),
      F('isOut', 'bool'),
      F('battingPosition', 'number'),
    ],
    cols: [
      { key: 'playerId', label: 'playerId' },
      { key: 'playerName', label: 'playerName' },
      { key: 'runs', label: 'runs' },
      { key: 'ballsFaced', label: 'balls' },
      { key: 'fours', label: '4s' },
      { key: 'sixes', label: '6s' },
      { key: 'strikeRate', label: 'SR' },
      { key: 'outDescription', label: 'dismissal' },
      { key: 'isOut', label: 'out' },
    ],
  };

  // ScorecardBowler: no 'innings', 'wides', or 'no_balls' columns on the
  // entity either — economy is a real stored field, not computed on save.
  const bowlingEnt = {
    k: 'bowling', l: 'Bowling', id: 'playerId',
    path: (id) => `/matches/${id}/bowling-scorecard`,
    f: [
      F('playerId', 'select', 1, { options: squadOptions, numeric: true }),
      F('oversBowled', 'number'),
      F('maidens', 'number'),
      F('runsConceded', 'number'),
      F('wickets', 'number'),
      F('economy', 'number'),
    ],
    cols: [
      { key: 'playerId', label: 'playerId' },
      { key: 'playerName', label: 'playerName' },
      { key: 'oversBowled', label: 'overs' },
      { key: 'maidens', label: 'maidens' },
      { key: 'runsConceded', label: 'runs' },
      { key: 'wickets', label: 'wkts' },
      { key: 'economy', label: 'econ' },
    ],
  };

  // Delivery entity field is "extraType" (singular) even though the read
  // DTO exposes it as "extrasType" (plural) — this sends the entity's real
  // field name. There's no "is_wicket" boolean on the backend; a wicket is
  // just wicketType being non-empty.
  const deliveriesEnt = {
    k: 'deliveries', l: 'Deliveries', id: 'deliveryId',
    path: (id) => `/matches/${id}/deliveries`,
    f: [
      F('innings', 'number', 1),
      F('overNum', 'number', 1),
      F('ballNum', 'number', 1),
      F('batterId', 'select', 1, { options: squadOptions, numeric: true }),
      F('nonStrikerId', 'select', 0, { options: squadOptions, numeric: true }),
      F('bowlerId', 'select', 1, { options: squadOptions, numeric: true }),
      F('runsBatter', 'number'),
      F('runsExtras', 'number'),
      F('extraType', 'text'),
      F('wicketType', 'text'),
      F('playerDismissedId', 'select', 0, { options: squadOptions, numeric: true }),
    ],
    cols: [
      { key: 'deliveryId', label: 'id' },
      { key: 'innings', label: 'inn' },
      { key: 'overNum', label: 'ov' },
      { key: 'ballNum', label: 'ball' },
      { key: 'batterName', label: 'batter' },
      { key: 'bowlerName', label: 'bowler' },
      { key: 'runsBatter', label: 'runs' },
      { key: 'runsExtras', label: 'extras' },
      { key: 'totalRuns', label: 'total' },
      { key: 'extrasType', label: 'extraType' },
      { key: 'wicketType', label: 'wicket' },
      { key: 'playerDismissedName', label: 'dismissed' },
    ],
  };

  const TABS = [
    { k: 'squad', l: 'Squad', ent: squadEnt },
    { k: 'batting', l: 'Batting', ent: battingEnt },
    { k: 'bowling', l: 'Bowling', ent: bowlingEnt },
    { k: 'deliveries', l: 'Deliveries', ent: deliveriesEnt },
  ];
  const activeEnt = TABS.find((t) => t.k === tab).ent;

  return (
    <div className="app-shell">
      <div className="admin-top">
        <div>
          <button
            className="back-link"
            style={{ background: 'none', border: 'none', padding: 0, cursor: 'pointer', font: 'inherit' }}
            onClick={() => navigate('/admin')}
          >
            ← All matches
          </button>
          <h1 className="page-title" style={{ marginTop: 6 }}>
            {match ? `${match.teamAName} vs ${match.teamBName}` : `Match #${matchId}`}
          </h1>
          <p className="page-subtitle" style={{ marginBottom: 0 }}>
            {match ? `${match.format} · ${match.venue} · ${match.matchDate}` : ''}
          </p>
        </div>
      </div>

      {err && <div className="admin-err" style={{ marginBottom: 16 }}>{err}</div>}

      <div className="scorecard reveal reveal-visible">
        <div className="tab-row">
          {TABS.map((t) => (
            <button key={t.k} className={`tab-btn${t.k === tab ? ' active' : ''}`} onClick={() => setTab(t.k)}>
              {t.l}
            </button>
          ))}
        </div>
        {tab === 'squad' ? (
          <MatchNestedTab ent={squadEnt} matchId={matchId} onChanged={reloadSquad} />
        ) : (
          <MatchNestedTab ent={activeEnt} matchId={matchId} />
        )}
      </div>
    </div>
  );
}
