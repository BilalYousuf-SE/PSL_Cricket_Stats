import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApi } from '../useApi.js';
import { api } from '../api.js';
import Loading from '../components/Loading.jsx';
import { ErrorState, EmptyState } from '../components/ErrorState.jsx';
import { teamName, teamId, toArray } from '../fields.js';
import { AnimatedNumber } from '../motion.jsx';
import '../motion.css';

// TeamController's list endpoint only returns { teamId, teamName }, so
// matches-played and wins (both plain Longs) are fetched per team.
function TeamCard({ team, index }) {
  const navigate = useNavigate();
  const id = teamId(team);
  const [played, setPlayed] = useState(null);
  const [wins, setWins] = useState(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      api.get(`/teams/${id}/matches-played`).catch(() => null),
      api.get(`/teams/${id}/wins`).catch(() => null),
    ]).then(([p, w]) => {
      if (!cancelled) {
        setPlayed(p);
        setWins(w);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [id]);

  return (
    <div
      className="entity-card card-pop"
      onClick={() => navigate(`/teams/${id}`)}
      style={{ cursor: 'pointer', '--i': index }}
    >
      <h4>{teamName(team)}</h4>
      <div className="meta">
        <span><b>{played === null ? '—' : <AnimatedNumber value={played} />}</b>played</span>
        <span><b>{wins === null ? '—' : <AnimatedNumber value={wins} />}</b>won</span>
      </div>
    </div>
  );
}

export default function TeamsList() {
  const { data, loading, error, reload } = useApi('/teams');
  const teams = useMemo(() => toArray(data), [data]);

  return (
    <div>
      <h1 className="page-title">Teams</h1>
      <p className="page-subtitle">Every side that's taken the field, with their overall win record.</p>

      {loading && <Loading rows={6} label="Bringing up the team sheet…" />}
      {error && <ErrorState message={error} onRetry={reload} />}
      {!loading && !error && teams.length === 0 && (
        <EmptyState message="No teams found. Add some via the backend to see them here." />
      )}
      {!loading && !error && teams.length > 0 && (
        <div className="card-grid">
          {teams.map((team, i) => (
            <TeamCard key={teamId(team) ?? i} team={team} index={i} />
          ))}
        </div>
      )}
    </div>
  );
}
