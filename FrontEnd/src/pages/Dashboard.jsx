import { useNavigate } from 'react-router-dom';
import { useApi } from '../useApi.js';
import Loading from '../components/Loading.jsx';
import { ErrorState, EmptyState } from '../components/ErrorState.jsx';
import DataTable from '../components/DataTable.jsx';
import { pick, teamName, teamId, playerName, playerId } from '../fields.js';

function Panel({ title, children }) {
  return (
    <div className="scorecard">
      <div className="scorecard-header">
        <h3>{title}</h3>
      </div>
      <div className="scorecard-body">{children}</div>
    </div>
  );
}

// TeamLeaderboardDTO: { teamId, teamName, wins, losses, matchesPlayed, points, netRunRate }
// — Dashboard only shows wins; the full breakdown lives on the season page.
function LeaderboardPanel() {
  const { data, loading, error, reload } = useApi('/stats/team-leaderboard');
  const navigate = useNavigate();
  return (
    <Panel title="Team Leaderboard">
      {loading && <Loading rows={5} label="Tallying team standings…" />}
      {error && <ErrorState message={error} onRetry={reload} />}
      {!loading && !error && (!data || data.length === 0) && (
        <EmptyState message="No leaderboard data yet." />
      )}
      {!loading && !error && data?.length > 0 && (
        <DataTable
          rankFrom={1}
          rows={data}
          onRowClick={(row) => navigate(`/teams/${teamId(row)}`)}
          columns={[
            { key: 'team', label: 'Team', render: (r) => <span className="name-cell">{teamName(r)}</span> },
            { key: 'wins', label: 'Wins', align: 'right', render: (r) => pick(r, ['wins']) },
          ]}
        />
      )}
    </Panel>
  );
}

// RunScorerDTO: { playerId, playerName, totalRuns }
function TopScorersPanel() {
  const { data, loading, error, reload } = useApi('/stats/top-run-scorers');
  const navigate = useNavigate();
  return (
    <Panel title="Top Scorers">
      {loading && <Loading rows={5} label="Adding up the runs…" />}
      {error && <ErrorState message={error} onRetry={reload} />}
      {!loading && !error && (!data || data.length === 0) && (
        <EmptyState message="No run-scoring data yet." />
      )}
      {!loading && !error && data?.length > 0 && (
        <DataTable
          rankFrom={1}
          rows={data.slice(0, 8)}
          onRowClick={(row) => navigate(`/players/${playerId(row)}`)}
          columns={[
            { key: 'player', label: 'Player', render: (r) => <span className="name-cell">{playerName(r)}</span> },
            { key: 'runs', label: 'Runs', align: 'right', render: (r) => pick(r, ['totalRuns']) },
          ]}
        />
      )}
    </Panel>
  );
}

// WicketTakerDTO: { playerId, playerName, totalWickets }
function TopWicketTakersPanel() {
  const { data, loading, error, reload } = useApi('/stats/top-wicket-takers');
  const navigate = useNavigate();
  return (
    <Panel title="Top Wicket-Takers">
      {loading && <Loading rows={5} label="Counting the scalps…" />}
      {error && <ErrorState message={error} onRetry={reload} />}
      {!loading && !error && (!data || data.length === 0) && (
        <EmptyState message="No bowling data yet." />
      )}
      {!loading && !error && data?.length > 0 && (
        <DataTable
          rankFrom={1}
          rows={data.slice(0, 8)}
          onRowClick={(row) => navigate(`/players/${playerId(row)}`)}
          columns={[
            { key: 'player', label: 'Player', render: (r) => <span className="name-cell">{playerName(r)}</span> },
            { key: 'wkts', label: 'Wkts', align: 'right', render: (r) => pick(r, ['totalWickets']) },
          ]}
        />
      )}
    </Panel>
  );
}

// PlayerOfMatchDTO: { playerId, playerName, awards }
function PotmPanel() {
  const { data, loading, error, reload } = useApi('/stats/player-of-match-awards');
  const navigate = useNavigate();
  return (
    <Panel title="Player of the Match — Leaders">
      {loading && <Loading rows={5} label="Reviewing the presentations…" />}
      {error && <ErrorState message={error} onRetry={reload} />}
      {!loading && !error && (!data || data.length === 0) && (
        <EmptyState message="No POTM data yet." />
      )}
      {!loading && !error && data?.length > 0 && (
        <DataTable
          rankFrom={1}
          rows={data.slice(0, 8)}
          onRowClick={(row) => navigate(`/players/${playerId(row)}`)}
          columns={[
            { key: 'player', label: 'Player', render: (r) => <span className="name-cell">{playerName(r)}</span> },
            { key: 'awards', label: 'Awards', align: 'right', render: (r) => pick(r, ['awards']) },
          ]}
        />
      )}
    </Panel>
  );
}

export default function Dashboard() {
  return (
    <div>
      <h1 className="page-title">The Scoreboard</h1>
      <p className="page-subtitle">
        Standings and leading performers across every match in the books. Tap any name to open its full record.
      </p>
      <div className="panel-grid">
        <LeaderboardPanel />
        <TopScorersPanel />
        <TopWicketTakersPanel />
        <PotmPanel />
      </div>
    </div>
  );
}