import { useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useApi } from '../useApi.js';
import Loading from '../components/Loading.jsx';
import { ErrorState, EmptyState } from '../components/ErrorState.jsx';
import DataTable from '../components/DataTable.jsx';
import { TeamMatchups } from '../components/MatchupViews.jsx';
import { pick, teamName, matchId, opponentOf, resultText, toArray } from '../fields.js';
import { AnimatedNumber, Reveal, StickyHeader, useScrolledPast } from '../motion.jsx';
import '../motion.css';

export default function TeamDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const heroEndRef = useRef(null);

  // /teams/{id}/matches-played and /teams/{id}/wins both return a plain Long.
  const { data, loading, error, reload } = useApi(
    () => [`/teams/${id}`, `/teams/${id}/matches-played`, `/teams/${id}/wins`, `/matches/team/${id}`],
    [id]
  );
  const [team, played, wins, matches] = data || [];
  const matchList = toArray(matches);

  const ready = !loading && !error;
  const scrolledPast = useScrolledPast(heroEndRef, ready);

  return (
    <div>
      {ready && (
        <StickyHeader visible={scrolledPast}>
          <span className="condensed-title">{teamName(team) || `Team #${id}`}</span>
          <span className="condensed-meta">{played ?? '—'} played · {wins ?? '—'} won</span>
        </StickyHeader>
      )}

      <Link to="/teams" className="back-link">← All teams</Link>

      {loading && <Loading rows={6} label="Loading the team file…" />}
      {error && <ErrorState message={error} onRetry={reload} />}

      {ready && (
        <>
          <h1 className="page-title">{teamName(team) || `Team #${id}`}</h1>
          <p className="page-subtitle">Full record and match history for this side.</p>

          <div className="stat-grid">
            <div className="stat-cell">
              <span className="stat-num">{played != null ? <AnimatedNumber value={played} /> : '—'}</span>
              <span className="stat-label">Matches played</span>
            </div>
            <div className="stat-cell">
              <span className="stat-num">{wins != null ? <AnimatedNumber value={wins} /> : '—'}</span>
              <span className="stat-label">Wins</span>
            </div>
            <div className="stat-cell">
              <span className="stat-num">
                {played && wins != null ? `${Math.round((wins / played) * 100)}%` : '—'}
              </span>
              <span className="stat-label">Win rate</span>
            </div>
          </div>
          <div ref={heroEndRef} />

          <TeamMatchups key={id} id={id} />

          <h3 className="section-heading">Match History</h3>
          <Reveal className="scorecard">
            <div className="scorecard-body">
              {matchList.length === 0 ? (
                <EmptyState message="No matches recorded for this team yet." />
              ) : (
                <DataTable
                  rows={matchList}
                  onRowClick={(row) => navigate(`/matches/${matchId(row)}`)}
                  columns={[
                    { key: 'date', label: 'Date', render: (r) => pick(r, ['matchDate']) },
                    { key: 'format', label: 'Format', render: (r) => <span className="pill">{pick(r, ['format'])}</span> },
                    { key: 'opponent', label: 'Opponent', render: (r) => opponentOf(r, id) },
                    { key: 'venue', label: 'Venue', render: (r) => pick(r, ['venue']) },
                    {
                      key: 'result',
                      label: 'Result',
                      render: (r) => {
                        const winner = pick(r, ['winnerName'], null);
                        const won = winner && teamName(team) && winner === teamName(team);
                        const lost = winner && winner !== teamName(team);
                        return (
                          <span className={`pill ${won ? 'win' : lost ? 'loss' : ''}`}>
                            {resultText(r)}
                          </span>
                        );
                      },
                    },
                  ]}
                />
              )}
            </div>
          </Reveal>
        </>
      )}
    </div>
  );
}
