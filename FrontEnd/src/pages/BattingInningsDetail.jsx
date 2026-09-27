import { useParams, Link } from 'react-router-dom';
import { useApi } from '../useApi.js';
import Loading from '../components/Loading.jsx';
import { ErrorState, EmptyState } from '../components/ErrorState.jsx';
import DataTable from '../components/DataTable.jsx';
import { pick, toArray } from '../fields.js';
import { AnimatedNumber } from '../motion.jsx';
import '../motion.css';

// GET /matches/{id}/batting-scorecard/{playerId} -> List<DeliveryDTO>, every
// ball this player faced in that innings (despite the endpoint's name, it's
// filtered ball-by-ball data, not an aggregate scorecard row).
export default function BattingInningsDetail() {
  const { id, playerId } = useParams();

  const { data, loading, error, reload } = useApi(
    () => [`/matches/${id}`, `/matches/${id}/deliveries/batter/${playerId}`],
    [id, playerId]
  );
  const [match, deliveries] = data || [];
  const list = toArray(deliveries);

  const batterName = list.length > 0 ? pick(list[0], ['batterName']) : `Player #${playerId}`;
  const totalRuns = list.reduce((sum, d) => sum + (Number(pick(d, ['runsBatter'], 0)) || 0), 0);
  const ballsFaced = list.filter((d) => !String(pick(d, ['extrasType'], '')).toUpperCase().includes('WIDE')).length;
  const dismissalBall = list.find((d) => pick(d, ['wicketType'], null) && pick(d, ['playerDismissedName']) === batterName);

  return (
    <div>
      <Link to={`/matches/${id}`} className="back-link">← Back to match</Link>

      {loading && <Loading rows={6} label="Pulling up the ball-by-ball…" />}
      {error && <ErrorState message={error} onRetry={reload} />}

      {!loading && !error && (
        <>
          <h1 className="page-title">{batterName}</h1>
          <p className="page-subtitle">
            Ball-by-ball for this innings · {pick(match, ['teamAName'])} vs {pick(match, ['teamBName'])} · {pick(match, ['matchDate'])}
          </p>

          <div className="stat-grid">
            <div className="stat-cell">
              <span className="stat-num"><AnimatedNumber value={totalRuns} /></span>
              <span className="stat-label">Runs</span>
            </div>
            <div className="stat-cell">
              <span className="stat-num"><AnimatedNumber value={ballsFaced} /></span>
              <span className="stat-label">Balls faced</span>
            </div>
            <div className="stat-cell">
              <span className="stat-num">{dismissalBall ? pick(dismissalBall, ['wicketType']) : 'Not out'}</span>
              <span className="stat-label">How out</span>
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
                    { key: 'bowler', label: 'Bowler', render: (r) => pick(r, ['bowlerName']) },
                    { key: 'runs', label: 'Runs', align: 'right', render: (r) => pick(r, ['runsBatter']) },
                    {
                      key: 'notes',
                      label: 'Notes',
                      render: (r) => {
                        const wicket = pick(r, ['wicketType'], null);
                        const extra = pick(r, ['extrasType'], null);
                        if (wicket) return <span className="pill loss">{wicket}</span>;
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
