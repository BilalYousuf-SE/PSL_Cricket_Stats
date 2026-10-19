import { useMemo, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useApi } from '../useApi.js';
import Loading from '../components/Loading.jsx';
import { ErrorState, EmptyState } from '../components/ErrorState.jsx';
import DataTable from '../components/DataTable.jsx';
import { pick, playerName, toArray } from '../fields.js';
import { AnimatedNumber, Reveal, StickyHeader, useScrolledPast } from '../motion.jsx';
import '../motion.css';

// BattingStatsDTO/BowlingStatsDTO give raw totals but no pre-computed
// average/strike-rate/economy — derived here since balls/overs data is
// only available at career-total granularity.
function computeBattingAverage(batting) {
  const runs = pick(batting, ['totalRuns'], null);
  const innings = pick(batting, ['innings'], null);
  if (runs == null || !innings) return '—';
  return (runs / innings).toFixed(2);
}

function computeStrikeRate(batting) {
  const runs = pick(batting, ['totalRuns'], null);
  const balls = pick(batting, ['totalBallsFaced'], null);
  if (runs == null || !balls) return '—';
  return ((runs / balls) * 100).toFixed(2);
}

// PlayerMatchDTO has no "opponent"/"result" field directly — both are
// derived from teamAName/teamBName/winnerName relative to playerTeam.
function opponentOf(m) {
  const playerTeam = pick(m, ['playerTeam']);
  const aName = pick(m, ['teamAName']);
  const bName = pick(m, ['teamBName']);
  return playerTeam === aName ? bName : aName;
}

function resultFor(m) {
  const winner = pick(m, ['winnerName'], null);
  const playerTeam = pick(m, ['playerTeam']);
  if (!winner) return { label: 'No result', cls: '' };
  if (winner === playerTeam) return { label: 'Won', cls: 'win' };
  return { label: 'Lost', cls: 'loss' };
}

// A player can represent different teams in different seasons, or even
// switch mid-career — so matches are grouped by season first, then by the
// team he played for within that season (a player rarely, but sometimes,
// plays for more than one side in a season, e.g. a mid-season trade).
function groupMatches(matches) {
  const bySeason = new Map();
  matches.forEach((m) => {
    const season = pick(m, ['seasonYear'], 'Unknown season');
    const team = pick(m, ['playerTeam'], 'Unknown team');
    if (!bySeason.has(season)) bySeason.set(season, new Map());
    const byTeam = bySeason.get(season);
    if (!byTeam.has(team)) byTeam.set(team, []);
    byTeam.get(team).push(m);
  });
  // newest season first
  return [...bySeason.entries()].sort((a, b) => String(b[0]).localeCompare(String(a[0]), undefined, { numeric: true }));
}

function MatchHistorySection({ matches }) {
  const navigate = useNavigate();
  const grouped = useMemo(() => groupMatches(matches), [matches]);

  if (matches.length === 0) {
    return <EmptyState message="No match history recorded for this player yet." />;
  }

  return (
    <div>
      {grouped.map(([season, byTeam]) => (
        <div key={season} className="innings-block">
          <div className="innings-heading">
            <h4>{season}</h4>
          </div>
          {[...byTeam.entries()].map(([team, teamMatches]) => (
            <div key={team}>
              <div className="innings-subheading">
                <h5>{team}</h5>
              </div>
              <DataTable
                rows={teamMatches}
                onRowClick={(row) => navigate(`/matches/${pick(row, ['matchId'])}`)}
                columns={[
                  { key: 'date', label: 'Date', render: (r) => pick(r, ['matchDate']) },
                  { key: 'format', label: 'Format', render: (r) => <span className="pill">{pick(r, ['format'])}</span> },
                  { key: 'opponent', label: 'Opponent', render: (r) => opponentOf(r) },
                  { key: 'venue', label: 'Venue', render: (r) => pick(r, ['venue']) },
                  {
                    key: 'result',
                    label: 'Result',
                    render: (r) => {
                      const { label, cls } = resultFor(r);
                      return <span className={`pill ${cls}`}>{label}</span>;
                    },
                  },
                ]}
              />
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}

export default function PlayerDetail() {
  const { id } = useParams();
  const heroEndRef = useRef(null);

  const { data, loading, error, reload } = useApi(
    () => [
      `/players/${id}`,
      `/players/${id}/batting`,
      `/players/${id}/bowling`,
      `/players/${id}/matches-count`,
      `/players/${id}/matches`,
    ],
    [id]
  );
  const [player, batting, bowling, matchesCount, matches] = data || [];
  const matchList = toArray(matches);

  const ready = !loading && !error;
  const scrolledPast = useScrolledPast(heroEndRef, ready);

  return (
    <div>
      {ready && (
        <StickyHeader visible={scrolledPast}>
          <span className="condensed-title">{playerName(player) || `Player #${id}`}</span>
          <span className="condensed-meta">{matchesCount ?? '—'} matches</span>
        </StickyHeader>
      )}

      <Link to="/players" className="back-link">← All players</Link>

      {loading && <Loading rows={6} label="Pulling up the player card…" />}
      {error && <ErrorState message={error} onRetry={reload} />}

      {ready && (
        <>
          <h1 className="page-title">{playerName(player) || `Player #${id}`}</h1>
          <p className="page-subtitle">
            {pick(player, ['role'], 'Role unknown')} · {pick(player, ['nationality'], 'Nationality unknown')}
            {pick(player, ['age'], null) ? ` · Age ${pick(player, ['age'])}` : ''}
          </p>

          <div className="stat-grid">
            <div className="stat-cell">
              <span className="stat-num">{matchesCount != null ? <AnimatedNumber value={matchesCount} /> : '—'}</span>
              <span className="stat-label">Matches played</span>
            </div>
            <div className="stat-cell">
              <span className="stat-num">{pick(player, ['playingStyle'], '—')}</span>
              <span className="stat-label">Playing style</span>
            </div>
            <div className="stat-cell">
              <span className="stat-num">{String(pick(player, ['bowlingStyle'], '—')).replace(/([a-z])([A-Z])/g, '$1 $2')}</span>
              <span className="stat-label">Bowling style</span>
            </div>
          </div>
          <div ref={heroEndRef} />

          <div className="panel-grid">
            <Reveal className="scorecard">
              <div className="scorecard-header"><h3>Batting</h3></div>
              <div className="scorecard-body">
                <div className="stat-grid" style={{ margin: '14px 20px', width: 'calc(100% - 40px)' }}>
                  <div className="stat-cell">
                    <span className="stat-num"><AnimatedNumber value={pick(batting, ['totalRuns'])} /></span>
                    <span className="stat-label">Runs</span>
                  </div>
                  <div className="stat-cell">
                    <span className="stat-num"><AnimatedNumber value={pick(batting, ['innings'])} /></span>
                    <span className="stat-label">Innings</span>
                  </div>
                  <div className="stat-cell">
                    <span className="stat-num"><AnimatedNumber value={pick(batting, ['highestScore'])} /></span>
                    <span className="stat-label">Highest score</span>
                  </div>
                  <div className="stat-cell">
                    <span className="stat-num"><AnimatedNumber value={pick(batting, ['totalFours'])} /></span>
                    <span className="stat-label">Fours</span>
                  </div>
                  <div className="stat-cell">
                    <span className="stat-num"><AnimatedNumber value={pick(batting, ['totalSixes'])} /></span>
                    <span className="stat-label">Sixes</span>
                  </div>
                  <div className="stat-cell">
                    <span className="stat-num">{computeStrikeRate(batting)}</span>
                    <span className="stat-label">Strike rate</span>
                  </div>
                  <div className="stat-cell">
                    <span className="stat-num">{computeBattingAverage(batting)}</span>
                    <span className="stat-label">Avg (runs/innings)</span>
                  </div>
                </div>
              </div>
            </Reveal>

            <Reveal className="scorecard" delay={80}>
              <div className="scorecard-header"><h3>Bowling</h3></div>
              <div className="scorecard-body">
                <div className="stat-grid" style={{ margin: '14px 20px', width: 'calc(100% - 40px)' }}>
                  <div className="stat-cell">
                    <span className="stat-num"><AnimatedNumber value={pick(bowling, ['totalWickets'])} /></span>
                    <span className="stat-label">Wickets</span>
                  </div>
                  <div className="stat-cell">
                    <span className="stat-num"><AnimatedNumber value={pick(bowling, ['innings'])} /></span>
                    <span className="stat-label">Innings</span>
                  </div>
                  <div className="stat-cell">
                    <span className="stat-num"><AnimatedNumber value={pick(bowling, ['totalMaidens'])} /></span>
                    <span className="stat-label">Maidens</span>
                  </div>
                  <div className="stat-cell">
                    <span className="stat-num"><AnimatedNumber value={pick(bowling, ['totalRunsConceded'])} /></span>
                    <span className="stat-label">Runs conceded</span>
                  </div>
                  <div className="stat-cell">
                    <span className="stat-num">{pick(bowling, ['bestWickets'])}</span>
                    <span className="stat-label">Best (single innings)</span>
                  </div>
                </div>
              </div>
            </Reveal>
          </div>

          <h3 className="section-heading section-spacer">Matches Played</h3>
          <Reveal className="scorecard">
            <div className="scorecard-body">
              <MatchHistorySection matches={matchList} />
            </div>
          </Reveal>
        </>
      )}
    </div>
  );
}
