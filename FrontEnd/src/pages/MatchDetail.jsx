import { useRef, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useApi } from '../useApi.js';
import Loading from '../components/Loading.jsx';
import { ErrorState, EmptyState } from '../components/ErrorState.jsx';
import DataTable from '../components/DataTable.jsx';
import { pick, playerName, resultText } from '../fields.js';
import { AnimatedNumber, StickyHeader, TabRow, useScrolledPast } from '../motion.jsx';
import '../motion.css';

const TABS = ['Summary', 'Squad', 'Scorecard', 'Deliveries'];

function SummaryTab({ match }) {
  if (!match) return <EmptyState message="No summary available." />;

  const aName = pick(match, ['teamAName']);
  const bName = pick(match, ['teamBName']);
  const winner = pick(match, ['winnerName'], null);
  const potm = pick(match, ['playerOfMatchName'], null);

  return (
    <div className="summary-panel">
      <div className="stat-grid">
        <div className="stat-cell">
          <span className="stat-num">{pick(match, ['format'])}</span>
          <span className="stat-label">Format</span>
        </div>
        <div className="stat-cell">
          <span className="stat-num">{pick(match, ['matchDate'])}</span>
          <span className="stat-label">Date</span>
        </div>
        <div className="stat-cell">
          <span className="stat-num">{pick(match, ['seasonYear'])}</span>
          <span className="stat-label">Season</span>
        </div>
      </div>

      <p className="summary-fixture">
        <span className={winner === aName ? 'summary-team win' : 'summary-team'}>{aName}</span>
        <span className="summary-vs">vs</span>
        <span className={winner === bName ? 'summary-team win' : 'summary-team'}>{bName}</span>
      </p>
      <p className="subtle" style={{ margin: '2px 0 16px' }}>{pick(match, ['venue'])}</p>

      <p style={{ fontSize: 14, margin: '0 0 18px', color: 'var(--ink-soft)' }}>
        Toss: {pick(match, ['tossWinnerName'])} chose to {String(pick(match, ['tossDecision'], '')).toLowerCase()}
      </p>

      <div className="result-banner">
        <span className="result-banner-icon" aria-hidden="true">🏆</span>
        <span>{resultText(match)}</span>
      </div>

      {potm && (
        <div className="potm-card">
          <span className="potm-label">Player of the Match</span>
          <span className="potm-name">{potm}</span>
        </div>
      )}
    </div>
  );
}

// MatchSquadDTO carries teamId/teamName per player, so the squad splits
// cleanly into a column per side, ordered to match teamA/teamB.
function SquadTab({ squad, match }) {
  const list = Array.isArray(squad) ? squad : [];
  if (list.length === 0) return <EmptyState message="No squad list available." />;

  const aId = pick(match, ['teamAId']);
  const aName = pick(match, ['teamAName'], 'Team A');
  const bId = pick(match, ['teamBId']);
  const bName = pick(match, ['teamBName'], 'Team B');

  const teamAPlayers = list.filter((p) => String(pick(p, ['teamId'])) === String(aId));
  const teamBPlayers = list.filter((p) => String(pick(p, ['teamId'])) === String(bId));
  const otherPlayers = list.filter((p) => !teamAPlayers.includes(p) && !teamBPlayers.includes(p));

  const Column = ({ title, players }) => (
    <div className="squad-column">
      <h4>{title}</h4>
      {players.length === 0 ? (
        <p className="subtle" style={{ padding: '0 20px' }}>No players listed.</p>
      ) : (
        <ul className="squad-list">
          {players.map((p, i) => (
            <li key={i}>
              <span className="name-cell">{playerName(p)}</span>
              {pick(p, ['isCaptain'], false) === true && <span className="pill" style={{ marginLeft: 8 }}>C</span>}
              {pick(p, ['isKeeper'], false) === true && <span className="pill" style={{ marginLeft: 6 }}>WK</span>}
              {pick(p, ['isPlaying'], true) === false && <span className="pill loss" style={{ marginLeft: 6 }}>Not playing</span>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );

  return (
    <div className="squad-columns">
      <Column title={aName} players={teamAPlayers} />
      <div className="squad-column-divider" />
      <Column title={bName} players={teamBPlayers} />
      {otherPlayers.length > 0 && (
        <>
          <div className="squad-column-divider" />
          <Column title="Unassigned" players={otherPlayers} />
        </>
      )}
    </div>
  );
}

function BattingScorecardTable({ rows, matchId }) {
  if (rows.length === 0) return <div className="innings-empty"><EmptyState message="No batting data recorded." /></div>;
  return (
    <DataTable
      rows={rows}
      columns={[
        { key: 'name', label: 'Batter', render: (r) => <span className="name-cell">{playerName(r)}</span> },
        {
          key: 'dismissal',
          label: 'Dismissal',
          render: (r) => <span className="subtle">{pick(r, ['isOut'], false) === false ? 'not out' : pick(r, ['outDescription'], '—')}</span>,
        },
        { key: 'runs', label: 'R', align: 'right', render: (r) => pick(r, ['runs']) },
        { key: 'balls', label: 'B', align: 'right', render: (r) => pick(r, ['ballsFaced']) },
        { key: 'fours', label: '4s', align: 'right', render: (r) => pick(r, ['fours']) },
        { key: 'sixes', label: '6s', align: 'right', render: (r) => pick(r, ['sixes']) },
        { key: 'sr', label: 'SR', align: 'right', render: (r) => pick(r, ['strikeRate']) },
        {
          key: 'detail',
          label: '',
          render: (r) => (
            <Link to={`/matches/${matchId}/batting/${pick(r, ['playerId'])}`} className="pill" style={{ textDecoration: 'none' }}>
              Ball-by-ball →
            </Link>
          ),
        },
      ]}
    />
  );
}

function BowlingScorecardTable({ rows, matchId }) {
  if (rows.length === 0) return <div className="innings-empty"><EmptyState message="No bowling data recorded." /></div>;
  return (
    <DataTable
      rows={rows}
      columns={[
        { key: 'name', label: 'Bowler', render: (r) => <span className="name-cell">{playerName(r)}</span> },
        { key: 'overs', label: 'O', align: 'right', render: (r) => pick(r, ['oversBowled']) },
        { key: 'maidens', label: 'M', align: 'right', render: (r) => pick(r, ['maidens']) },
        { key: 'runs', label: 'R', align: 'right', render: (r) => pick(r, ['runsConceded']) },
        { key: 'wickets', label: 'W', align: 'right', render: (r) => pick(r, ['wickets']) },
        { key: 'econ', label: 'Econ', align: 'right', render: (r) => pick(r, ['economy']) },
        {
          key: 'detail',
          label: '',
          render: (r) => (
            <Link to={`/matches/${matchId}/bowling/${pick(r, ['playerId'])}`} className="pill" style={{ textDecoration: 'none' }}>
              Ball-by-ball →
            </Link>
          ),
        },
      ]}
    />
  );
}

function buildPlayerTeamMap(squad) {
  const map = {};
  (Array.isArray(squad) ? squad : []).forEach((s) => {
    const pid = pick(s, ['playerId'], null);
    if (pid != null) map[String(pid)] = pick(s, ['teamId'], null);
  });
  return map;
}

// Neither ScorecardBatterDTO nor ScorecardBowlerDTO carry a team or innings
// field, so team membership comes from the squad list, and which team batted
// in which innings is derived from tossWinnerName + tossDecision (whoever
// didn't choose to bat, bats second).
function battingFirstTeamId(match) {
  const tossWinnerName = pick(match, ['tossWinnerName'], null);
  const decision = String(pick(match, ['tossDecision'], '')).toUpperCase();
  const aId = pick(match, ['teamAId']);
  const aName = pick(match, ['teamAName']);
  const bId = pick(match, ['teamBId']);
  if (!tossWinnerName) return aId;
  const tossWinnerIsA = tossWinnerName === aName;
  const decisionIsBat = decision.includes('BAT');
  if (decisionIsBat) return tossWinnerIsA ? aId : bId;
  return tossWinnerIsA ? bId : aId;
}

// InningsExtrasDTO: { innings, totalExtras, extrasByType }. extrasByType is a
// Map<String, Long> (e.g. {"WIDE": 4, "NO_BALL": 2}) — rendered as a plain
// comma list since the exact key strings depend on however extrasType is
// spelled in your data.
function formatExtrasBreakdown(entry) {
  const byType = pick(entry, ['extrasByType'], null);
  if (!byType || typeof byType !== 'object' || Object.keys(byType).length === 0) return '';
  return Object.entries(byType)
    .map(([k, v]) => `${String(k).replace(/_/g, ' ').toLowerCase()} ${v}`)
    .join(', ');
}

function ScorecardTab({ match, squad, battingRows, bowlingRows, extrasRows, matchId }) {
  const battingList = Array.isArray(battingRows) ? battingRows : [];
  const bowlingList = Array.isArray(bowlingRows) ? bowlingRows : [];

  if (battingList.length === 0 && bowlingList.length === 0) {
    return <EmptyState message="No scorecard available for this match yet." />;
  }

  const teamMap = buildPlayerTeamMap(squad);
  const teamIdOfPlayer = (playerId) => teamMap[String(playerId)];

  const aId = pick(match, ['teamAId']);
  const aName = pick(match, ['teamAName'], 'Team A');
  const bId = pick(match, ['teamBId']);
  const bName = pick(match, ['teamBName'], 'Team B');

  const firstBatId = battingFirstTeamId(match);
  const firstBatIsA = String(firstBatId) === String(aId);
  const firstBatName = firstBatIsA ? aName : bName;
  const secondBatId = firstBatIsA ? bId : aId;
  const secondBatName = firstBatIsA ? bName : aName;

  const battingFor = (teamId) => battingList.filter((r) => String(teamIdOfPlayer(pick(r, ['playerId']))) === String(teamId));
  const bowlingFor = (teamId) => bowlingList.filter((r) => String(teamIdOfPlayer(pick(r, ['playerId']))) === String(teamId));

  const extrasList = Array.isArray(extrasRows) ? extrasRows : [];
  const extrasFor = (inningsNumber) =>
    extrasList.find((e) => Number(pick(e, ['innings'], null)) === inningsNumber);

  const innings = [
    { label: '1st Innings', inningsNumber: 1, battingTeamId: firstBatId, battingTeamName: firstBatName, bowlingTeamId: secondBatId, bowlingTeamName: secondBatName },
    { label: '2nd Innings', inningsNumber: 2, battingTeamId: secondBatId, battingTeamName: secondBatName, bowlingTeamId: firstBatId, bowlingTeamName: firstBatName },
  ];

  return (
    <div>
      {innings.map((inn, idx) => {
        const battingRowsForInnings = battingFor(inn.battingTeamId);
        const bowlingRowsForInnings = bowlingFor(inn.bowlingTeamId);
        const totalRuns = battingRowsForInnings.reduce((sum, r) => sum + (Number(pick(r, ['runs'], 0)) || 0), 0);
        const totalWickets = battingRowsForInnings.filter((r) => pick(r, ['isOut'], false) === true).length;
        const extrasEntry = extrasFor(inn.inningsNumber);
        const extrasBreakdown = extrasEntry ? formatExtrasBreakdown(extrasEntry) : '';

        return (
          <div className="innings-block" key={idx}>
            <div className="innings-heading">
              <h4>{inn.label} — {inn.battingTeamName} batting</h4>
              {battingRowsForInnings.length > 0 && (
                <div className="innings-score">
                  <span className="innings-score-figure">
                    <AnimatedNumber value={totalRuns} />/{totalWickets}
                  </span>
                  {extrasEntry && (
                    <span className="subtle" style={{ marginLeft: 8 }}>
                      · {pick(extrasEntry, ['totalExtras'], 0)} extras
                      {extrasBreakdown ? ` (${extrasBreakdown})` : ''}
                    </span>
                  )}
                </div>
              )}
            </div>
            <BattingScorecardTable rows={battingRowsForInnings} matchId={matchId} />

            <div className="innings-subheading">
              <h5>Bowling — {inn.bowlingTeamName}</h5>
            </div>
            <BowlingScorecardTable rows={bowlingRowsForInnings} matchId={matchId} />
          </div>
        );
      })}
    </div>
  );
}

function isWicketBall(d) {
  const wt = pick(d, ['wicketType'], null);
  return !!wt && wt !== '—';
}

function ballClass(delivery) {
  const runs = Number(pick(delivery, ['runsBatter'], 0)) || 0;
  if (isWicketBall(delivery)) return 'ball-chip wicket';
  if (runs === 4 || runs === 6) return 'ball-chip boundary';
  return 'ball-chip';
}

function ballLabel(delivery) {
  if (isWicketBall(delivery)) return 'W';
  const extra = pick(delivery, ['extrasType'], null);
  const runs = pick(delivery, ['totalRuns'], 0);
  if (extra && extra !== '—') return extra[0]?.toUpperCase() || '•';
  return String(runs);
}

// Deliveries are grouped by innings first, then by over. Every expected
// innings (2 for limited-overs, 4 for Test) is always rendered — even with
// no balls yet — so a missing innings reads as "no data returned by the
// API for this innings" rather than looking like it silently disappeared.
function DeliveriesTab({ data, match }) {
  const [openKey, setOpenKey] = useState(null);
  const list = Array.isArray(data) ? data : [];
  if (list.length === 0) return <EmptyState message="No ball-by-ball data recorded for this match." />;

  const format = String(pick(match, ['format'], '')).toUpperCase();
  const expectedInningsCount = format.includes('TEST') ? 4 : 2;

  const inningsMap = {};
  list.forEach((d) => {
    const inn = String(pick(d, ['innings'], '1'));
    if (!inningsMap[inn]) inningsMap[inn] = {};
    const overNum = pick(d, ['overNum'], '0');
    if (!inningsMap[inn][overNum]) inningsMap[inn][overNum] = [];
    inningsMap[inn][overNum].push(d);
  });

  const dataInnings = Object.keys(inningsMap).map(Number);
  const maxInnings = Math.max(expectedInningsCount, ...(dataInnings.length ? dataInnings : [0]));
  const inningsToShow = Array.from({ length: maxInnings }, (_, i) => String(i + 1));

  return (
    <div>
      {inningsToShow.map((inn) => {
        const overs = inningsMap[inn];
        if (!overs) {
          return (
            <div key={inn} className="innings-heading">
              <h4>Innings {inn}</h4>
              <p className="subtle" style={{ margin: '4px 0 0' }}>No deliveries recorded for this innings yet.</p>
            </div>
          );
        }
        const overNumbers = Object.keys(overs).sort((a, b) => Number(a) - Number(b));
        return (
          <div key={inn}>
            <div className="innings-heading"><h4>Innings {inn}</h4></div>
            {overNumbers.map((overNum) => {
              const balls = overs[overNum];
              const totalRuns = balls.reduce((sum, d) => sum + (Number(pick(d, ['totalRuns'], 0)) || 0), 0);
              const wickets = balls.filter(isWicketBall).length;
              const bowler = pick(balls[0], ['bowlerName'], '');
              const key = `${inn}-${overNum}`;
              const isOpen = openKey === key;
              return (
                <div className="over-block" key={key}>
                  <button
                    type="button"
                    className="over-header"
                    onClick={() => setOpenKey(isOpen ? null : key)}
                    aria-expanded={isOpen}
                  >
                    <span>
                      <span className="over-chevron" aria-hidden="true">▸</span>
                      <span className="over-num">Ov {overNum}</span>
                      <span className="over-summary">
                        {bowler ? `${bowler} · ` : ''}
                        {wickets > 0 ? `${wickets} wkt${wickets > 1 ? 's' : ''} · ` : ''}
                        {balls.length} balls
                      </span>
                    </span>
                    <span className="over-runs">{totalRuns} runs</span>
                  </button>
                  <div className={`over-collapse ${isOpen ? 'open' : ''}`}>
                    <div className="over-collapse-inner">
                      <div className="ball-list">
                        {balls.map((d, i) => (
                          <span key={i} className={ballClass(d)} style={{ '--i': i }} title={pick(d, ['bowlerName'], '')}>
                            {ballLabel(d)}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        );
      })}
    </div>
  );
}

export default function MatchDetail() {
  const { id } = useParams();
  const [activeTab, setActiveTab] = useState('Summary');
  const heroEndRef = useRef(null);

  const { data, loading, error, reload } = useApi(
    () => [
      `/matches/${id}`,
      `/matches/${id}/squad`,
      `/matches/${id}/batting-scorecard`,
      `/matches/${id}/bowling-scorecard`,
      `/matches/${id}/deliveries`,
      `/matches/${id}/extras`,
    ],
    [id]
  );
  const [match, squad, batting, bowling, deliveries, extras] = data || [];

  const ready = !loading && !error && !!match;
  const scrolledPast = useScrolledPast(heroEndRef, ready);

  return (
    <div className="match-detail">
      {ready && (
        <StickyHeader visible={scrolledPast}>
          <span className="condensed-title">
            {pick(match, ['teamAName'])}
            <span className="condensed-sep">vs</span>
            {pick(match, ['teamBName'])}
          </span>
          <span className="condensed-meta">{resultText(match)}</span>
          <TabRow tabs={TABS} active={activeTab} onChange={setActiveTab} compact />
        </StickyHeader>
      )}

      <Link to="/matches" className="back-link">← All matches</Link>

      {loading && <Loading rows={6} label="Loading the full scorecard…" />}
      {error && <ErrorState message={error} onRetry={reload} />}

      {ready && (
        <>
          <h1 className="page-title">
            {pick(match, ['teamAName'])} vs {pick(match, ['teamBName'])}
          </h1>
          <p className="page-subtitle">
            {pick(match, ['format'])} · {pick(match, ['venue'])} · {pick(match, ['matchDate'])}
          </p>
          <div ref={heroEndRef} />

          <div className="scorecard">
            <TabRow tabs={TABS} active={activeTab} onChange={setActiveTab} />
            <div className="scorecard-body">
              <div key={activeTab} className="tab-panel">
                {activeTab === 'Summary' && <SummaryTab match={match} />}
                {activeTab === 'Squad' && <SquadTab squad={squad} match={match} />}
                {activeTab === 'Scorecard' && (
                  <ScorecardTab match={match} squad={squad} battingRows={batting} bowlingRows={bowling} extrasRows={extras} matchId={id} />
                )}
                {activeTab === 'Deliveries' && <DeliveriesTab data={deliveries} match={match} />}
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
