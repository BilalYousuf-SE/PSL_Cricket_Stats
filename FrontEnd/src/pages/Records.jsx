import { useNavigate } from 'react-router-dom';
import { useApi } from '../useApi.js';
import Loading from '../components/Loading.jsx';
import { ErrorState, EmptyState } from '../components/ErrorState.jsx';
import DataTable from '../components/DataTable.jsx';
import { pick, playerName, matchId, toArray } from '../fields.js';
import { Reveal } from '../motion.jsx';
import '../motion.css';

export default function Records() {
  const navigate = useNavigate();

  const { data: scores, loading: scoresLoading, error: scoresError, reload: reloadScores } = useApi('/stats/highest-scores');
  const { data: bowling, loading: bowlingLoading, error: bowlingError, reload: reloadBowling } = useApi('/stats/best-bowling');

  const scoreList = toArray(scores);
  const bowlingList = toArray(bowling);

  return (
    <div>
      <h1 className="page-title">Records</h1>
      <p className="page-subtitle">The all-time high-water marks.</p>

      <div className="panel-grid">
        <Reveal className="scorecard">
          <div className="scorecard-header"><h3>Highest Individual Scores</h3></div>
          <div className="scorecard-body">
            {scoresLoading && <Loading rows={5} label="Digging through the archives…" />}
            {scoresError && <ErrorState message={scoresError} onRetry={reloadScores} />}
            {!scoresLoading && !scoresError && scoreList.length === 0 && (
              <EmptyState message="No record scores on file yet." />
            )}
            {!scoresLoading && !scoresError && scoreList.length > 0 && (
              <DataTable
                rankFrom={1}
                rows={scoreList}
                onRowClick={(row) => navigate(`/matches/${matchId(row)}`)}
                columns={[
                  { key: 'name', label: 'Player', render: (r) => <span className="name-cell">{playerName(r)}</span> },
                  { key: 'runs', label: 'Runs', align: 'right', render: (r) => pick(r, ['runs']) },
                  { key: 'balls', label: 'Balls', align: 'right', render: (r) => pick(r, ['ballsFaced']) },
                  { key: 'sr', label: 'SR', align: 'right', render: (r) => pick(r, ['strikeRate']) },
                ]}
              />
            )}
          </div>
        </Reveal>

        <Reveal className="scorecard" delay={80}>
          <div className="scorecard-header"><h3>Best Bowling Figures</h3></div>
          <div className="scorecard-body">
            {bowlingLoading && <Loading rows={5} label="Digging through the archives…" />}
            {bowlingError && <ErrorState message={bowlingError} onRetry={reloadBowling} />}
            {!bowlingLoading && !bowlingError && bowlingList.length === 0 && (
              <EmptyState message="No record bowling figures on file yet." />
            )}
            {!bowlingLoading && !bowlingError && bowlingList.length > 0 && (
              <DataTable
                rankFrom={1}
                rows={bowlingList}
                onRowClick={(row) => navigate(`/matches/${matchId(row)}`)}
                columns={[
                  { key: 'name', label: 'Player', render: (r) => <span className="name-cell">{playerName(r)}</span> },
                  { key: 'figures', label: 'W-R', align: 'right', render: (r) => `${pick(r, ['wickets'])}-${pick(r, ['runsConceded'])}` },
                  { key: 'overs', label: 'Overs', align: 'right', render: (r) => pick(r, ['oversBowled']) },
                  { key: 'econ', label: 'Econ', align: 'right', render: (r) => pick(r, ['economy']) },
                ]}
              />
            )}
          </div>
        </Reveal>
      </div>
    </div>
  );
}
