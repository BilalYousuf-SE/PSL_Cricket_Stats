import { useParams, Link } from 'react-router-dom';
import { useApi } from '../useApi.js';
import Loading from '../components/Loading.jsx';
import { ErrorState, EmptyState } from '../components/ErrorState.jsx';
import DataTable from '../components/DataTable.jsx';
import { pick, toArray } from '../fields.js';
import { AnimatedNumber } from '../motion.jsx';
import '../motion.css';

// GET /matches/{id}/bowling-scorecard/{playerId} -> List<DeliveryDTO>, every
// ball this player bowled in that innings.
export default function BowlingInningsDetail() {
  const { id, playerId } = useParams();

  const { data, loading, error, reload } = useApi(
    () => [`/matches/${id}`, `/matches/${id}/deliveries/bowler/${playerId}`],
    [id, playerId]
  );
  const [match, deliveries] = data || [];
  const list = toArray(deliveries);

  const bowlerName = list.length > 0 ? pick(list[0], ['bowlerName']) : `Player #${playerId}`;
  const runsConceded = list.reduce((sum, d) => sum + (Number(pick(d, ['totalRuns'], 0)) || 0), 0);
  const legalBalls = list.filter((d) => {
    const extra = String(pick(d, ['extrasType'], '')).toUpperCase();
    return !extra.includes('WIDE') && !extra.includes('NO BALL') && !extra.includes('NOBALL');
  }).length;
  const overs = `${Math.floor(legalBalls / 6)}.${legalBalls % 6}`;
  const wickets = list.filter((d) => {
    const wt = pick(d, ['wicketType'], null);
    // run outs aren't credited to the bowler
    return wt && !String(wt).toUpperCase().includes('RUN OUT') && !String(wt).toUpperCase().includes('RUNOUT');
  }).length;

  return (
    <div>
      <Link to={`/matches/${id}`} className="back-link">← Back to match</Link>

      {loading && <Loading rows={6} label="Pulling up the ball-by-ball…" />}
      {error && <ErrorState message={error} onRetry={reload} />}

      {!loading && !error && (
        <>
          <h1 className="page-title">{bowlerName}</h1>
          <p className="page-subtitle">
            Ball-by-ball for this innings · {pick(match, ['teamAName'])} vs {pick(match, ['teamBName'])} · {pick(match, ['matchDate'])}
          </p>

          <div className="stat-grid">
            <div className="stat-cell">
              <span className="stat-num">{overs}</span>
              <span className="stat-label">Overs</span>
            </div>
            <div className="stat-cell">
              <span className="stat-num"><AnimatedNumber value={runsConceded} /></span>
              <span className="stat-label">Runs conceded</span>
            </div>
            <div className="stat-cell">
              <span className="stat-num"><AnimatedNumber value={wickets} /></span>
              <span className="stat-label">Wickets</span>
            </div>
          </div>

          <div className="scorecard">
            <div className="scorecard-body">
              {list.length === 0 ? (
                <EmptyState message="No ball-by-ball data recorded for this innings." />
              ) : (
                <DataTable
                  rows={list}
                  columns={[
                    { key: 'overball', label: 'Over.Ball', render: (r) => `${pick(r, ['overNum'])}.${pick(r, ['ballNum'])}` },
                    { key: 'batter', label: 'Batter', render: (r) => pick(r, ['batterName']) },
                    { key: 'runs', label: 'Runs', align: 'right', render: (r) => pick(r, ['totalRuns']) },
                    {
                      key: 'notes',
                      label: 'Notes',
                      render: (r) => {
                        const wicket = pick(r, ['wicketType'], null);
                        const extra = pick(r, ['extrasType'], null);
                        if (wicket) return <span className="pill loss">{wicket} — {pick(r, ['playerDismissedName'], '')}</span>;
                        if (extra) return <span className="pill">{extra}</span>;
                        return <span className="subtle">—</span>;
                      },
                    },
                  ]}
                />
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
