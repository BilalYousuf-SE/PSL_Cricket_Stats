import { useId, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useApi } from '../useApi.js';
import { playerId, playerName, teamId, teamName, toArray } from '../fields.js';
import { EMPTY_MATCHUP_FILTERS, MATCHUP_METRICS, bowlingStyleLabel, canComparePlayers, matchupCells, matchupFilterError, matchupQuery, matchupValue } from '../matchups.js';
import MatchupFilters from './MatchupFilters.jsx';
import DataTable from './DataTable.jsx';
import Loading from './Loading.jsx';
import { EmptyState, ErrorState } from './ErrorState.jsx';
import '../matchups.css';

function Pagination({ data, onChange, label = 'Results' }) {
  if (!data || data.totalPages <= 1) return null;
  return (
    <nav className="matchup-pagination" aria-label={`${label} pagination`}>
      <span>{label}: page {data.number + 1} of {data.totalPages} · {data.totalElements} total</span>
      <button type="button" className="matchup-button" disabled={data.number === 0} onClick={() => onChange(data.number - 1)}>Previous {label.toLowerCase()}</button>
      <button type="button" className="matchup-button" disabled={data.number + 1 >= data.totalPages} onClick={() => onChange(data.number + 1)}>Next {label.toLowerCase()}</button>
    </nav>
  );
}

function MetricCards({ stats }) {
  return (
    <div className="matchup-stat-grid">
      {MATCHUP_METRICS.map((metric) => (
        <div className="matchup-stat" key={metric.key}>
          <strong>{matchupValue(stats, metric.key)}</strong><span>{metric.label}</span>
        </div>
      ))}
    </div>
  );
}

function ScoringNotes() {
  return (
    <details className="matchup-scoring">
      <summary>How these statistics are counted</summary>
      <p>Runs are scored off the bat. Balls faced exclude wides and include no-balls; legal balls exclude both. Runs conceded include wides and no-balls, and exclude byes, leg-byes, and penalty extras.</p>
      <p>Dismissals count wickets credited to this bowler against this batter. Striker run-outs are shown separately. Dot balls are legal deliveries with no runs scored. Batting average is runs per bowler-credited dismissal; an undefined rate is shown as —.</p>
      <p>Statistics cover recorded deliveries within your filters, across every team the players represented.</p>
    </details>
  );
}

function PairMatches({ batterId, bowlerId, filters }) {
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useApi(`/matchups/players/${batterId}/${bowlerId}/matches${matchupQuery(filters, { page, size: 25 })}`);
  return (
    <div className="matchup-breakdown">
      <h4>Match-by-match · each innings</h4>
      {loading ? <Loading rows={3} label="Loading encounter history…" />
        : error ? <ErrorState message={error} onRetry={reload} />
          : <>
            <div className="matchup-table-scroll" tabIndex="0" role="region" aria-label="Encounter history table">
              <DataTable rows={toArray(data).map((row) => ({ ...row, id: `${row.matchId}:${row.innings}` }))} columns={[
                { key: 'match', label: 'Match', render: (row) => <Link className="matchup-link" to={`/matches/${row.matchId}`}>{row.teamAName || 'Team A'} vs {row.teamBName || 'Team B'}</Link> },
                { key: 'matchDate', label: 'Date', render: (row) => row.matchDate || '—' },
                { key: 'seasonYear', label: 'Season', render: (row) => row.seasonYear ?? '—' },
                { key: 'venue', label: 'Venue', render: (row) => row.venue || '—' },
                { key: 'innings', label: 'Innings', align: 'right', render: (row) => row.innings ?? '—' },
                { key: 'runs', label: 'Runs', align: 'right', render: (row) => matchupValue(row.stats, 'runs') },
                { key: 'balls', label: 'Balls', align: 'right', render: (row) => matchupValue(row.stats, 'ballsFaced') },
                { key: 'dismissals', label: 'Dismissals', align: 'right', render: (row) => matchupValue(row.stats, 'dismissals') },
              ]} />
            </div>
            <Pagination data={data} label="Encounters" onChange={setPage} />
          </>}
    </div>
  );
}

export function PairMatchup({ batterId, bowlerId, filters, onClose }) {
  const valid = canComparePlayers(batterId, bowlerId) && !matchupFilterError(filters);
  const { data, loading, error, reload } = useApi(`/matchups/players/${batterId}/${bowlerId}${matchupQuery(filters)}`, [], { enabled: valid });
  if (!valid) return null;
  return (
    <section className="scorecard matchup-pair" aria-label="Player comparison">
      <div className="scorecard-header">
        <h3>{data ? <><Link to={`/players/${data.batter.playerId}`}>{data.batter.playerName}</Link> <span className="matchup-versus">batting vs</span> <Link to={`/players/${data.bowler.playerId}`}>{data.bowler.playerName}</Link></> : 'Player comparison'}</h3>
        {onClose && <button type="button" className="matchup-button" onClick={onClose}>Close comparison</button>}
      </div>
      <div className="scorecard-body">
        {loading ? <Loading rows={4} label="Calculating this matchup…" />
          : error ? <ErrorState message={error} onRetry={reload} />
            : data && (data.stats.deliveries === 0
              ? <EmptyState title="No encounters in this selection" message="These players have no recorded deliveries against each other within the selected filters." />
              : <>
                <p className="matchup-pair-meta">{data.stats.matches} {data.stats.matches === 1 ? 'match' : 'matches'} · {data.stats.innings} innings · {data.stats.deliveries} deliveries
                  {data.bowler.bowlingStyle ? ` · ${bowlingStyleLabel(data.bowler.bowlingStyle)}` : ''}</p>
                <MetricCards stats={data.stats} />
                <PairMatches key={JSON.stringify(filters)} batterId={batterId} bowlerId={bowlerId} filters={filters} />
              </>)}
        <ScoringNotes />
      </div>
    </section>
  );
}

function OpponentResults({ id, perspective, filters }) {
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);
  const { data, loading, error, reload } = useApi(`/matchups/players/${id}${matchupQuery(filters, { perspective, page, size: 25 })}`, [], { enabled: !matchupFilterError(filters) });
  const batting = perspective === 'batting';
  return (
    <>
      {loading ? <Loading rows={4} label="Loading opponents…" />
        : error ? <ErrorState message={error} onRetry={reload} />
          : data && (toArray(data).length === 0
            ? <EmptyState message="No recorded matchups within the selected filters." />
            : <div className="scorecard">
              <div className="scorecard-header"><h3>{batting ? 'Against each bowler' : 'Against each batter'}</h3><span>{data.totalElements} opponents</span></div>
              <div className="matchup-table-scroll" tabIndex="0" role="region" aria-label="Opponent matchups table">
                <DataTable rows={toArray(data).map((row) => ({ ...row, id: batting ? row.bowler.playerId : row.batter.playerId }))} columns={[
                  { key: 'opponent', label: batting ? 'Bowler' : 'Batter', render: (row) => <button type="button" className="matchup-link" onClick={() => setSelected(row)}>{(batting ? row.bowler : row.batter).playerName}</button> },
                  { key: 'runs', label: 'Runs', align: 'right', render: (row) => matchupValue(row.stats, 'runs') },
                  { key: 'balls', label: 'Balls', align: 'right', render: (row) => matchupValue(row.stats, 'ballsFaced') },
                  { key: 'dismissals', label: 'Dismissals', align: 'right', render: (row) => matchupValue(row.stats, 'dismissals') },
                  { key: 'strikeRate', label: 'SR', align: 'right', render: (row) => matchupValue(row.stats, 'strikeRate') },
                  { key: 'average', label: 'Avg', align: 'right', render: (row) => matchupValue(row.stats, 'average') },
                  { key: 'fours', label: '4s', align: 'right', render: (row) => matchupValue(row.stats, 'fours') },
                  { key: 'sixes', label: '6s', align: 'right', render: (row) => matchupValue(row.stats, 'sixes') },
                ]} />
              </div>
              <Pagination data={data} label="Opponents" onChange={(next) => { setPage(next); setSelected(null); }} />
            </div>)}
      {selected && <PairMatchup key={`${selected.batter.playerId}:${selected.bowler.playerId}`} batterId={selected.batter.playerId} bowlerId={selected.bowler.playerId} filters={filters} onClose={() => setSelected(null)} />}
    </>
  );
}

export function PlayerMatchups({ id }) {
  const [filters, setFilters] = useState({ ...EMPTY_MATCHUP_FILTERS });
  const [perspective, setPerspective] = useState('batting');
  return (
    <section className="matchup-section" aria-label="Player matchups">
      <h3 className="section-heading section-spacer">Matchups</h3>
      <div className="matchup-mode-controls" aria-label="Player perspective">
        <button type="button" className="matchup-button" aria-pressed={perspective === 'batting'} onClick={() => setPerspective('batting')}>Batting matchups</button>
        <button type="button" className="matchup-button" aria-pressed={perspective === 'bowling'} onClick={() => setPerspective('bowling')}>Bowling matchups</button>
        <Link className="matchup-link" to={`/matchups?batterId=${id}`}>Compare a specific pair →</Link>
      </div>
      <MatchupFilters value={filters} onChange={setFilters} />
      <OpponentResults key={`${id}:${perspective}:${JSON.stringify(filters)}`} id={id} perspective={perspective} filters={filters} />
    </section>
  );
}

export function PlayerComparison({ filters, initialBatterId = '', initialBowlerId = '' }) {
  const id = useId();
  const [batter, setBatter] = useState(initialBatterId);
  const [bowler, setBowler] = useState(initialBowlerId);
  const { data, loading, error, reload } = useApi('/players');
  const players = [...toArray(data)].sort((a, b) => playerName(a).localeCompare(playerName(b)));
  const valid = canComparePlayers(batter, bowler);
  return (
    <div>
      {loading && <Loading rows={2} label="Loading players…" />}
      {error && <ErrorState message={error} onRetry={reload} />}
      <div className="matchup-controls matchup-selectors">
        <label htmlFor={`${id}-batter`}>Batter
          <select id={`${id}-batter`} className="control-select" value={batter} onChange={(e) => setBatter(e.target.value)}>
            <option value="">Choose a batter…</option>
            {players.map((player) => <option key={playerId(player)} value={playerId(player)}>{playerName(player)}</option>)}
          </select>
        </label>
        <label htmlFor={`${id}-bowler`}>Bowler
          <select id={`${id}-bowler`} className="control-select" value={bowler} onChange={(e) => setBowler(e.target.value)}>
            <option value="">Choose a bowler…</option>
            {players.map((player) => <option key={playerId(player)} value={playerId(player)}>{playerName(player)}</option>)}
          </select>
        </label>
        <button type="button" className="matchup-button" disabled={!valid} onClick={() => { setBatter(bowler); setBowler(batter); }}>Swap players</button>
      </div>
      {batter && bowler && !valid && <p role="alert" className="matchup-filter-error">Select two different players.</p>}
      {valid ? <PairMatchup key={`${batter}:${bowler}`} batterId={batter} bowlerId={bowler} filters={filters} />
        : !batter || !bowler ? <EmptyState title="Choose a batter and a bowler" message="Their encounters across all represented teams will appear here." /> : null}
    </div>
  );
}

function TeamMatrix({ battingTeam, bowlingTeam, filters }) {
  const legendId = useId();
  const [selected, setSelected] = useState(null);
  const { data, loading, error, reload } = useApi(`/matchups/teams${matchupQuery(filters, { battingTeamId: battingTeam, bowlingTeamId: bowlingTeam })}`, [], { enabled: !matchupFilterError(filters) });
  const cells = useMemo(() => matchupCells(data?.cells), [data?.cells]);
  const batters = toArray(data?.batters);
  const bowlers = toArray(data?.bowlers);
  return (
    <>
      {loading ? <Loading rows={5} label="Building the matchup matrix…" />
        : error ? <ErrorState message={error} onRetry={reload} />
          : data && (batters.length === 0 || bowlers.length === 0
            ? <EmptyState title="No squad members in this selection" message="Try another season or team. Squads use recorded playing members; encounters can come from any represented team." />
            : <div className="scorecard">
              <div className="scorecard-header matchup-matrix-heading"><h3>{data.battingTeamName} batting vs {data.bowlingTeamName} bowling</h3><span>{batters.length} batters × {bowlers.length} bowlers</span></div>
              <p className="matchup-matrix-legend" id={legendId}><strong>Dismissals / runs (balls)</strong> · Avg and SR below. Select a cell for details.</p>
              <div className="matchup-table-scroll matchup-matrix-scroll" tabIndex="0" role="region" aria-label="Scrollable matchup matrix">
                <table className="data-table matchup-matrix" aria-label={`${data.battingTeamName} batters against ${data.bowlingTeamName} bowlers`} aria-describedby={legendId}>
                  <thead><tr><th scope="col">Batter ↓ / Bowler →</th>{bowlers.map((bowler) => <th scope="col" key={bowler.playerId}><Link to={`/players/${bowler.playerId}`}>{bowler.playerName}</Link></th>)}</tr></thead>
                  <tbody>{batters.map((batter) => <tr key={batter.playerId}>
                    <th scope="row"><Link to={`/players/${batter.playerId}`}>{batter.playerName}</Link></th>
                    {bowlers.map((bowler) => {
                      const cell = cells.get(`${batter.playerId}:${bowler.playerId}`);
                      const self = batter.playerId === bowler.playerId;
                      return <td key={bowler.playerId} className="num">
                        {self ? <span className="matchup-no-encounter" title="A player cannot face themselves">N/A</span>
                          : cell?.stats.deliveries > 0 ? <button type="button" className="matchup-cell"
                            aria-label={`${batter.playerName} against ${bowler.playerName}: ${cell.stats.dismissals} dismissals, ${cell.stats.runs} runs (${cell.stats.ballsFaced} balls); Avg: ${matchupValue(cell.stats, 'average')}; SR: ${matchupValue(cell.stats, 'strikeRate')}`}
                            onClick={() => setSelected(cell)}>
                            <strong className="matchup-cell-score">{cell.stats.dismissals}/{cell.stats.runs} ({cell.stats.ballsFaced})</strong>
                            <span className="matchup-cell-rates"><span>Avg: {matchupValue(cell.stats, 'average')}</span><span>SR: {matchupValue(cell.stats, 'strikeRate')}</span></span>
                          </button>
                            : <span className="matchup-no-encounter" title="No recorded encounters within these filters">—</span>}
                      </td>;
                    })}
                  </tr>)}</tbody>
                </table>
              </div>
              <p className="matchup-matrix-note">Scroll down for batters and across for bowlers. — means no recorded encounter or an undefined rate. A recorded zero remains selectable.</p>
            </div>)}
      {selected && <PairMatchup key={`${selected.batter.playerId}:${selected.bowler.playerId}`} batterId={selected.batter.playerId} bowlerId={selected.bowler.playerId} filters={filters} onClose={() => setSelected(null)} />}
    </>
  );
}

export function TeamComparison({ filters, initialBattingTeamId = '', initialBowlingTeamId = '' }) {
  const id = useId();
  const [battingTeam, setBattingTeam] = useState(initialBattingTeamId);
  const [bowlingTeam, setBowlingTeam] = useState(initialBowlingTeamId);
  const { data, loading, error, reload } = useApi('/teams');
  const teams = [...toArray(data)].sort((a, b) => teamName(a).localeCompare(teamName(b)));
  const valid = canComparePlayers(battingTeam, bowlingTeam);
  return (
    <div>
      {loading && <Loading rows={2} label="Loading teams…" />}
      {error && <ErrorState message={error} onRetry={reload} />}
      <div className="matchup-controls matchup-selectors">
        <label htmlFor={`${id}-batting-team`}>Batting team
          <select id={`${id}-batting-team`} className="control-select" value={battingTeam} onChange={(e) => setBattingTeam(e.target.value)}>
            <option value="">Choose a batting team…</option>
            {teams.map((team) => <option key={teamId(team)} value={teamId(team)}>{teamName(team)}</option>)}
          </select>
        </label>
        <label htmlFor={`${id}-bowling-team`}>Bowling team
          <select id={`${id}-bowling-team`} className="control-select" value={bowlingTeam} onChange={(e) => setBowlingTeam(e.target.value)}>
            <option value="">Choose a bowling team…</option>
            {teams.map((team) => <option key={teamId(team)} value={teamId(team)}>{teamName(team)}</option>)}
          </select>
        </label>
        <button type="button" className="matchup-button" disabled={!valid} onClick={() => { setBattingTeam(bowlingTeam); setBowlingTeam(battingTeam); }}>Swap teams</button>
      </div>
      <p className="matchup-note">Teams choose the players shown. Each pair includes encounters across every represented team, within your filters.</p>
      <p className="matchup-note">Squads use the selected season, or all recorded seasons. Dates, venue, innings, and bowling style filter the encounters.</p>
      {battingTeam && bowlingTeam && !valid && <p className="matchup-filter-error" role="alert">Select two different teams.</p>}
      {valid ? <TeamMatrix key={`${battingTeam}:${bowlingTeam}:${JSON.stringify(filters)}`} battingTeam={battingTeam} bowlingTeam={bowlingTeam} filters={filters} />
        : !battingTeam || !bowlingTeam ? <EmptyState title="Choose two teams" message="Compare their batters and bowlers using the players’ encounter histories." /> : null}
    </div>
  );
}

export function TeamMatchups({ id }) {
  const [filters, setFilters] = useState({ ...EMPTY_MATCHUP_FILTERS });
  return (
    <section className="matchup-section" aria-label="Team matchups">
      <h3 className="section-heading section-spacer">Matchup matrix</h3>
      <MatchupFilters value={filters} onChange={setFilters} />
      <TeamComparison key={id} filters={filters} initialBattingTeamId={id} />
    </section>
  );
}
