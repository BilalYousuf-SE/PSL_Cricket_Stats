import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApi } from '../useApi.js';
import Loading from '../components/Loading.jsx';
import { ErrorState, EmptyState } from '../components/ErrorState.jsx';
import DataTable from '../components/DataTable.jsx';
import { pick, matchId, teamId, teamName, resultText, toArray } from '../fields.js';
import { Reveal } from '../motion.jsx';
import '../motion.css';

export default function MatchesList() {
  const navigate = useNavigate();
  const [format, setFormat] = useState('');
  const [team, setTeam] = useState('');

  const { data: teams } = useApi('/teams');
  const teamList = toArray(teams);

  // Full unfiltered list, used only to derive the real MatchFormat enum
  // values in use (avoids guessing enum constant names for the
  // /matches/format/{format} path variable).
  const { data: allMatches } = useApi('/matches');
  const formats = useMemo(() => {
    const list = toArray(allMatches);
    return [...new Set(list.map((m) => pick(m, ['format'], null)).filter(Boolean))].sort();
  }, [allMatches]);

  const endpoint = useMemo(() => {
    if (format) return `/matches/format/${format}`;
    if (team) return `/matches/team/${team}`;
    return '/matches';
  }, [format, team]);

  // Fixed: this used to be `Array.isArray(data) ? data : []`, which renders
  // as empty the instant this endpoint returns anything other than a bare
  // array (e.g. a Spring Data Page envelope `{ content: [...] }`). toArray()
  // unwraps that case too — see fields.js.
  const { data, loading, error, reload } = useApi(endpoint, [endpoint]);
  const matches = toArray(data);
  console.log('MATCHES DEBUG', { data, loading, error, matches });

  return (
    <div>
      <h1 className="page-title">Matches</h1>
      <p className="page-subtitle">Every fixture on record. Filter by format or team, or open one for the full scorecard.</p>

      <div className="controls-row">
        <select
          className="control-select"
          value={format}
          onChange={(e) => {
            setFormat(e.target.value);
            setTeam('');
          }}
        >
          <option value="">All formats</option>
          {formats.map((f) => (
            <option key={f} value={f}>{f}</option>
          ))}
        </select>
        <select
          className="control-select"
          value={team}
          onChange={(e) => {
            setTeam(e.target.value);
            setFormat('');
          }}
        >
          <option value="">All teams</option>
          {teamList.map((t, i) => (
            <option key={i} value={teamId(t)}>{teamName(t)}</option>
          ))}
        </select>
      </div>

      {loading && <Loading rows={8} label="Pulling the fixture list…" />}
      {error && <ErrorState message={error} onRetry={reload} />}
      {!loading && !error && matches.length === 0 && (
        <EmptyState message="No matches found for this filter." />
      )}
      {!loading && !error && matches.length > 0 && (
        <Reveal className="scorecard">
          <div className="scorecard-body">
            <DataTable
              rows={matches}
              onRowClick={(row) => navigate(`/matches/${matchId(row)}`)}
              columns={[
                { key: 'date', label: 'Date', render: (r) => pick(r, ['matchDate']) },
                { key: 'format', label: 'Format', render: (r) => <span className="pill">{pick(r, ['format'])}</span> },
                { key: 'teams', label: 'Fixture', render: (r) => `${pick(r, ['teamAName'])} vs ${pick(r, ['teamBName'])}` },
                { key: 'venue', label: 'Venue', render: (r) => pick(r, ['venue']) },
                { key: 'result', label: 'Result', render: (r) => resultText(r) },
              ]}
            />
          </div>
        </Reveal>
      )}
    </div>
  );
}
