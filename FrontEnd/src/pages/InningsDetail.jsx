import { useParams, Link } from 'react-router-dom';
import { useApi } from '../useApi.js';
import Loading from '../components/Loading.jsx';
import { ErrorState, EmptyState } from '../components/ErrorState.jsx';
import DataTable from '../components/DataTable.jsx';
import { pick, toArray } from '../fields.js';
import { AnimatedNumber } from '../motion.jsx';
import '../motion.css';

// Both routes use the same delivery table and request lifecycle.
export default function InningsDetail({ type }) {
  const isBatting = type === 'batting';
  const { id, playerId } = useParams();

  const { data, loading, error, reload } = useApi(
    () => [`/matches/${id}`, `/matches/${id}/deliveries/${isBatting ? 'batter' : 'bowler'}/${playerId}`],
    [id, playerId]
  );
  const [match, deliveries] = data || [];
  const list = toArray(deliveries);

  const playerName = list.length > 0
    ? pick(list[0], [isBatting ? 'batterName' : 'bowlerName'])
    : `Player #${playerId}`;
  const stats = isBatting ? battingStats(list, playerName) : bowlingStats(list);

  return (
    <div>
      <Link to={`/matches/${id}`} className="back-link">← Back to match</Link>

      {loading && <Loading rows={6} label="Pulling up the ball-by-ball…" />}
      {error && <ErrorState message={error} onRetry={reload} />}

      {!loading && !error && (
        <>
          <h1 className="page-title">{playerName}</h1>
          <p className="page-subtitle">
            Ball-by-ball for this innings · {pick(match, ['teamAName'])} vs {pick(match, ['teamBName'])} · {pick(match, ['matchDate'])}
          </p>

          <div className="stat-grid">
            {stats.map(({ label, value, animate }) => (
              <div className="stat-cell" key={label}>
                <span className="stat-num">{animate ? <AnimatedNumber value={value} /> : value}</span>
                <span className="stat-label">{label}</span>
              </div>
            ))}
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
                    { key: 'opponent', label: isBatting ? 'Bowler' : 'Batter', render: (r) => pick(r, [isBatting ? 'bowlerName' : 'batterName']) },
                    { key: 'runs', label: 'Runs', align: 'right', render: (r) => pick(r, [isBatting ? 'runsBatter' : 'totalRuns']) },
                    {
                      key: 'notes',
                      label: 'Notes',
                      render: (r) => {
                        const wicket = pick(r, ['wicketType'], null);
                        const extra = pick(r, ['extrasType'], null);
                        if (wicket) return <span className="pill loss">{wicket}{!isBatting && ` — ${pick(r, ['playerDismissedName'], '')}`}</span>;
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

function battingStats(list, playerName) {
  const totalRuns = list.reduce((sum, d) => sum + (Number(pick(d, ['runsBatter'], 0)) || 0), 0);
  const ballsFaced = list.filter((d) => !String(pick(d, ['extrasType'], '')).toUpperCase().includes('WIDE')).length;
  const dismissalBall = list.find((d) => pick(d, ['wicketType'], null) && pick(d, ['playerDismissedName']) === playerName);

  return [
    { label: 'Runs', value: totalRuns, animate: true },
    { label: 'Balls faced', value: ballsFaced, animate: true },
    { label: 'How out', value: dismissalBall ? pick(dismissalBall, ['wicketType']) : 'Not out' },
  ];
}

function bowlingStats(list) {
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

  return [
    { label: 'Overs', value: overs },
    { label: 'Runs conceded', value: runsConceded, animate: true },
    { label: 'Wickets', value: wickets, animate: true },
  ];
}
