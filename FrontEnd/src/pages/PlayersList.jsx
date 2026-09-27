import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApi } from '../useApi.js';
import Loading from '../components/Loading.jsx';
import { ErrorState, EmptyState } from '../components/ErrorState.jsx';
import { pick, playerName, playerId, toArray } from '../fields.js';
import '../motion.css';

export default function PlayersList() {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [role, setRole] = useState('');
  const [team, setTeam] = useState('');

  const { data: teams } = useApi('/teams');
  const teamList = toArray(teams);

  // Full unfiltered list, used only to derive the real Roles enum values
  // in use (avoids guessing enum constant names for the /players/role/{role}
  // path variable).
  const { data: allPlayers } = useApi('/players');
  const roles = useMemo(() => {
    const list = toArray(allPlayers);
    return [...new Set(list.map((p) => pick(p, ['role'], null)).filter(Boolean))].sort();
  }, [allPlayers]);

  const endpoint = useMemo(() => {
    if (search.trim()) return `/players/search?name=${encodeURIComponent(search.trim())}`;
    if (role) return `/players/role/${encodeURIComponent(role)}`;
    if (team) return `/players/team/${team}`;
    return '/players';
  }, [search, role, team]);

  const { data, loading, error, reload } = useApi(endpoint, [endpoint]);
  const players = toArray(data);

  return (
    <div>
      <h1 className="page-title">Players</h1>
      <p className="page-subtitle">Search the full roster, or narrow it down by role and team.</p>

      <div className="controls-row">
        <input
          className="control-input"
          placeholder="Search players by name…"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setRole('');
            setTeam('');
          }}
          style={{ minWidth: 220 }}
        />
        <select
          className="control-select"
          value={role}
          onChange={(e) => {
            setRole(e.target.value);
            setSearch('');
            setTeam('');
          }}
        >
          <option value="">All roles</option>
          {roles.map((r) => (
            <option key={r} value={r}>{r}</option>
          ))}
        </select>
        <select
          className="control-select"
          value={team}
          onChange={(e) => {
            setTeam(e.target.value);
            setSearch('');
            setRole('');
          }}
        >
          <option value="">All teams</option>
          {teamList.map((t, i) => (
            <option key={i} value={pick(t, ['teamId'])}>
              {pick(t, ['teamName'])}
            </option>
          ))}
        </select>
      </div>

      {loading && <Loading rows={6} label="Calling the roster…" />}
      {error && <ErrorState message={error} onRetry={reload} />}
      {!loading && !error && players.length === 0 && (
        <EmptyState message="No players match that search." />
      )}
      {!loading && !error && players.length > 0 && (
        <div className="card-grid">
          {players.map((p, i) => (
            <div
              key={playerId(p) ?? i}
              className="entity-card card-pop"
              style={{ cursor: 'pointer', '--i': i }}
              onClick={() => navigate(`/players/${playerId(p)}`)}
            >
              <h4>{playerName(p)}</h4>
              <div className="meta">
                <span>{pick(p, ['role'], 'Role unknown')}</span>
                <span>{pick(p, ['nationality'], '')}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
