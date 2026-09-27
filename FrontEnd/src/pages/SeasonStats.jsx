import { useEffect, useRef, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useApi } from '../useApi.js';
import Loading from '../components/Loading.jsx';
import { ErrorState, EmptyState } from '../components/ErrorState.jsx';
import DataTable from '../components/DataTable.jsx';
import { pick, playerName, teamName, matchId, resultText, toArray } from '../fields.js';
import { AnimatedNumber, Reveal, StickyHeader, useScrolledPast } from '../motion.jsx';
import '../motion.css';

// /season/{year}/winner is fetched on its own: an in-progress or unresolved
// season will likely 404 here, and that shouldn't take down the rest of the
// page the way a failure inside a combined Promise.all would.
function SeasonChampionStat({ year }) {
  const { data: winner, loading } = useApi(`/season/${year}/winner`, [year]);
  return (
    <div className="stat-cell">
      <span className="stat-num">{loading ? '…' : winner ? teamName(winner) : 'TBD'}</span>
      <span className="stat-label">Season champions</span>
    </div>
  );
}

// Season picker now lives on the page itself (moved out of the navbar).
// Click the current year to open a dropdown of every season on record and
// jump straight to it. Reused both in the hero and in the condensed bar
// that appears once you've scrolled past it, so switching seasons never
// requires scrolling back up on a long page.
function SeasonSwitcher({ currentYear }) {
  const navigate = useNavigate();
  const { data: seasons } = useApi('/season');
  const seasonList = toArray(seasons);

  const [open, setOpen] = useState(false);
  const containerRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(e) {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="nav-dropdown" ref={containerRef} style={{ display: 'inline-block' }}>
      <button
        type="button"
        className={'nav-link' + (open ? ' active' : '')}
        onClick={() => setOpen((o) => !o)}
      >
        {currentYear} ▾
      </button>
      {open && (
        <div className="nav-dropdown-menu">
          {seasonList.length === 0 ? (
            <span className="nav-dropdown-item subtle">No seasons yet</span>
          ) : (
            seasonList.map((s, i) => (
              <button
                key={pick(s, ['seasonId'], i)}
                type="button"
                className="nav-dropdown-item"
                onClick={() => {
                  setOpen(false);
                  navigate(`/records/season/${pick(s, ['year'])}`);
                }}
              >
                {pick(s, ['year'])}
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}

export default function SeasonStats() {
  const { year } = useParams();
  const navigate = useNavigate();
  const heroEndRef = useRef(null);

  const { data, loading, error, reload } = useApi(
    () => [
      `/season/${year}/leaderboard`,
      `/season/${year}/top-runs`,
      `/season/${year}/top-wickets`,
      `/season/${year}/top-strike-rates`,
      `/season/${year}/highest-scores`,
      `/season/${year}/best-bowling`,
      `/season/${year}/matches`,
    ],
    [year]
  );
  const [leaderboard, topRuns, topWickets, topStrikeRates, highestScores, bestBowling, matches] = data || [];

  const leaderboardList = toArray(leaderboard);
  const topRunsList = toArray(topRuns).slice(0, 10);
  const topWicketsList = toArray(topWickets).slice(0, 10);
  const strikeRateList = toArray(topStrikeRates);
  const highestScoresList = toArray(highestScores);
  const bestBowlingList = toArray(bestBowling);
  const matchList = toArray(matches);

  const ready = !loading && !error;
  const scrolledPast = useScrolledPast(heroEndRef, ready);

  return (
    <div>
      {ready && (
        <StickyHeader visible={scrolledPast}>
          <span className="condensed-title">{year} Season</span>
          <span className="condensed-meta">{matchList.length} matches</span>
          <SeasonSwitcher currentYear={year} />
        </StickyHeader>
      )}

      <Link to="/records" className="back-link">← All records</Link>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, flexWrap: 'wrap' }}>
        <h1 className="page-title" style={{ marginBottom: 0 }}>{year} Season</h1>
        <SeasonSwitcher currentYear={year} />
      </div>
      <p className="page-subtitle">Standings, leaders and every fixture from the {year} season.</p>

      {loading && <Loading rows={8} label={`Pulling up the ${year} scorebook…`} />}
      {error && <ErrorState message={error} onRetry={reload} />}

      {ready && (
        <>
          <div className="stat-grid">
            <SeasonChampionStat year={year} />
            <div className="stat-cell">
              <span className="stat-num"><AnimatedNumber value={matchList.length} /></span>
              <span className="stat-label">Matches played</span>
            </div>
          </div>
          <div ref={heroEndRef} />

          <h3 className="section-heading">Team Standings</h3>
          <Reveal className="scorecard section-spacer">
            <div className="scorecard-body">
              {leaderboardList.length === 0 ? (
                <EmptyState message="No standings recorded for this season yet." />
              ) : (
                <DataTable
                  rankFrom={1}
                  rows={leaderboardList}
                  onRowClick={(row) => navigate(`/teams/${pick(row, ['teamId'])}`)}
                  columns={[
                    { key: 'team', label: 'Team', render: (r) => <span className="name-cell">{teamName(r)}</span> },
                    { key: 'played', label: 'Played', align: 'right', render: (r) => pick(r, ['matchesPlayed']) },
                    { key: 'wins', label: 'Wins', align: 'right', render: (r) => pick(r, ['wins']) },
                    { key: 'losses', label: 'Losses', align: 'right', render: (r) => pick(r, ['losses']) },
                    { key: 'points', label: 'Points', align: 'right', render: (r) => pick(r, ['points']) },
                  ]}
                />
              )}
            </div>
          </Reveal>

          <div className="panel-grid section-spacer">
            <Reveal className="scorecard">
              <div className="scorecard-header"><h3>Top Run-Scorers</h3></div>
              <div className="scorecard-body">
                {topRunsList.length === 0 ? (
                  <EmptyState message={`No batting data recorded for ${year}.`} />
                ) : (
                  <DataTable
                    rankFrom={1}
                    rows={topRunsList}
                    onRowClick={(row) => navigate(`/players/${pick(row, ['playerId'])}`)}
                    columns={[
                      { key: 'name', label: 'Player', render: (r) => <span className="name-cell">{playerName(r)}</span> },
                      { key: 'runs', label: 'Runs', align: 'right', render: (r) => pick(r, ['totalRuns']) },
                    ]}
                  />
                )}
              </div>
            </Reveal>

            <Reveal className="scorecard" delay={80}>
              <div className="scorecard-header"><h3>Top Wicket-Takers</h3></div>
              <div className="scorecard-body">
                {topWicketsList.length === 0 ? (
                  <EmptyState message={`No bowling data recorded for ${year}.`} />
                ) : (
                  <DataTable
                    rankFrom={1}
                    rows={topWicketsList}
                    onRowClick={(row) => navigate(`/players/${pick(row, ['playerId'])}`)}
                    columns={[
                      { key: 'name', label: 'Player', render: (r) => <span className="name-cell">{playerName(r)}</span> },
                      { key: 'wkts', label: 'Wkts', align: 'right', render: (r) => pick(r, ['totalWickets']) },
                    ]}
                  />
                )}
              </div>
            </Reveal>
          </div>

          <div className="panel-grid section-spacer">
            <Reveal className="scorecard">
              <div className="scorecard-header"><h3>Top Strike Rates</h3></div>
              <div className="scorecard-body">
                {strikeRateList.length === 0 ? (
                  <EmptyState message={`No qualifying batters recorded for ${year}.`} />
                ) : (
                  <DataTable
                    rankFrom={1}
                    rows={strikeRateList}
                    onRowClick={(row) => navigate(`/players/${pick(row, ['playerId'])}`)}
                    columns={[
                      { key: 'name', label: 'Player', render: (r) => <span className="name-cell">{playerName(r)}</span> },
                      { key: 'sr', label: 'SR', align: 'right', render: (r) => {
                        const sr = pick(r, ['strikeRate'], null);
                        return sr == null ? '—' : Number(sr).toFixed(2);
                      } },
                      { key: 'runs', label: 'Runs', align: 'right', render: (r) => pick(r, ['runs']) },
                      { key: 'balls', label: 'Balls', align: 'right', render: (r) => pick(r, ['ballsFaced']) },
                    ]}
                  />
                )}
              </div>
            </Reveal>

            <Reveal className="scorecard" delay={80}>
              <div className="scorecard-header"><h3>Highest Scores</h3></div>
              <div className="scorecard-body">
                {highestScoresList.length === 0 ? (
                  <EmptyState message={`No record scores on file for ${year}.`} />
                ) : (
                  <DataTable
                    rankFrom={1}
                    rows={highestScoresList}
                    onRowClick={(row) => navigate(`/matches/${matchId(row)}`)}
                    columns={[
                      { key: 'name', label: 'Player', render: (r) => <span className="name-cell">{playerName(r)}</span> },
                      { key: 'runs', label: 'Runs', align: 'right', render: (r) => pick(r, ['runs']) },
                      { key: 'balls', label: 'Balls', align: 'right', render: (r) => pick(r, ['ballsFaced']) },
                    ]}
                  />
                )}
              </div>
            </Reveal>
          </div>

          <h3 className="section-heading section-spacer">Best Bowling Figures</h3>
          <Reveal className="scorecard">
            <div className="scorecard-body">
              {bestBowlingList.length === 0 ? (
                <EmptyState message={`No record bowling figures on file for ${year}.`} />
              ) : (
                <DataTable
                  rankFrom={1}
                  rows={bestBowlingList}
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

          <h3 className="section-heading section-spacer">All Matches — {year}</h3>
          <Reveal className="scorecard">
            <div className="scorecard-body">
              {matchList.length === 0 ? (
                <EmptyState message={`No matches recorded for ${year}.`} />
              ) : (
                <DataTable
                  rows={matchList}
                  onRowClick={(row) => navigate(`/matches/${matchId(row)}`)}
                  columns={[
                    { key: 'date', label: 'Date', render: (r) => pick(r, ['matchDate']) },
                    { key: 'format', label: 'Format', render: (r) => <span className="pill">{pick(r, ['format'])}</span> },
                    { key: 'teams', label: 'Fixture', render: (r) => `${pick(r, ['teamAName'])} vs ${pick(r, ['teamBName'])}` },
                    { key: 'venue', label: 'Venue', render: (r) => pick(r, ['venue']) },
                    { key: 'result', label: 'Result', render: (r) => resultText(r) },
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