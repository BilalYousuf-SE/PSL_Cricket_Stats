import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApi } from '../useApi.js';
import Loading from '../components/Loading.jsx';
import { ErrorState, EmptyState } from '../components/ErrorState.jsx';
import DataTable from '../components/DataTable.jsx';
import { pick, teamName, teamId, matchId, resultText, toArray } from '../fields.js';
import { AnimatedNumber, Reveal } from '../motion.jsx';
import '../motion.css';

export default function HeadToHead() {
  const navigate = useNavigate();
  const { data: teams, loading: teamsLoading, error: teamsError } = useApi('/teams');
  const [team1, setTeam1] = useState('');
  const [team2, setTeam2] = useState('');

  const canCompare = team1 && team2 && team1 !== team2;

  const { data: results, loading, error, reload } = useApi(
    () => (canCompare ? `/matches/headtohead/${team1}/${team2}` : null),
    [team1, team2],
    { enabled: !!canCompare }
  );

  const teamList = toArray(teams);
  const nameOf = (id) => teamName(teamList.find((t) => String(teamId(t)) === String(id)));
  const resultList = toArray(results);

  return (
    <div>
      <h1 className="page-title">Head-to-Head</h1>
      <p className="page-subtitle">Pick two teams to see every meeting between them.</p>

      {teamsLoading && <Loading rows={2} label="Loading the team list…" />}
      {teamsError && <ErrorState message={teamsError} />}

      {!teamsLoading && !teamsError && (
        <div className="controls-row">
          <select className="control-select" value={team1} onChange={(e) => setTeam1(e.target.value)}>
            <option value="">Select Team A…</option>
            {teamList.map((t, i) => (
              <option key={teamId(t) ?? i} value={teamId(t)}>
                {teamName(t)}
              </option>
            ))}
          </select>
          <span style={{ alignSelf: 'center', color: 'var(--gold)', fontFamily: 'var(--font-display)', fontWeight: 600 }}>
            vs
          </span>
          <select className="control-select" value={team2} onChange={(e) => setTeam2(e.target.value)}>
            <option value="">Select Team B…</option>
            {teamList.map((t, i) => (
              <option key={teamId(t) ?? i} value={teamId(t)}>
                {teamName(t)}
              </option>
            ))}
          </select>
        </div>
      )}

      {team1 && team2 && team1 === team2 && (
        <EmptyState title="Pick two different teams" message="A team can't play itself — choose a different opponent." />
      )}

      {canCompare && loading && <Loading rows={4} label="Digging up past meetings…" />}
      {canCompare && error && <ErrorState message={error} onRetry={reload} />}

      {canCompare && !loading && !error && (
        <Reveal className="scorecard">
          <div className="scorecard-header">
            <h3>{nameOf(team1)} vs {nameOf(team2)}</h3>
          </div>
          <div className="scorecard-body">
            {resultList.length === 0 ? (
              <EmptyState message="These teams haven't met yet in the records." />
            ) : (
              <>
                {(() => {
                  const t1Name = nameOf(team1);
                  const t2Name = nameOf(team2);
                  const t1Wins = resultList.filter((r) => pick(r, ['winnerName'], null) === t1Name).length;
                  const t2Wins = resultList.filter((r) => pick(r, ['winnerName'], null) === t2Name).length;
                  const noResult = resultList.length - t1Wins - t2Wins;
                  return (
                    <div className="stat-grid" style={{ margin: '16px 20px', width: 'calc(100% - 40px)' }}>
                      <div className="stat-cell">
                        <span className="stat-num"><AnimatedNumber value={resultList.length} /></span>
                        <span className="stat-label">Matches played</span>
                      </div>
                      <div className="stat-cell">
                        <span className="stat-num"><AnimatedNumber value={t1Wins} /></span>
                        <span className="stat-label">{t1Name} wins</span>
                      </div>
                      <div className="stat-cell">
                        <span className="stat-num"><AnimatedNumber value={t2Wins} /></span>
                        <span className="stat-label">{t2Name} wins</span>
                      </div>
                      {noResult > 0 && (
                        <div className="stat-cell">
                          <span className="stat-num"><AnimatedNumber value={noResult} /></span>
                          <span className="stat-label">No result / tied</span>
                        </div>
                      )}
                    </div>
                  );
                })()}
                <DataTable
                  rows={resultList}
                  onRowClick={(row) => navigate(`/matches/${matchId(row)}`)}
                  columns={[
                    { key: 'date', label: 'Date', render: (r) => pick(r, ['matchDate']) },
                    { key: 'format', label: 'Format', render: (r) => <span className="pill">{pick(r, ['format'])}</span> },
                    { key: 'venue', label: 'Venue', render: (r) => pick(r, ['venue']) },
                    { key: 'result', label: 'Result', render: (r) => <span className="pill">{resultText(r)}</span> },
                  ]}
                />
              </>
            )}
          </div>
        </Reveal>
      )}

      {!canCompare && !team1 && !team2 && (
        <div className="state-block">
          <p className="state-title">Choose two teams above</p>
          <p style={{ margin: 0 }}>Their full match history will appear here.</p>
        </div>
      )}
    </div>
  );
}
