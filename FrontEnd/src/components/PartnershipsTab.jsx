import { Link } from 'react-router-dom';
import { useApi } from '../useApi.js';
import { pick } from '../fields.js';
import { groupPartnerships, partnershipTeamName } from '../partnerships.js';
import DataTable from './DataTable.jsx';
import Loading from './Loading.jsx';
import { EmptyState, ErrorState } from './ErrorState.jsx';

function Batter({ row, number }) {
  const id = row[`batter${number}Id`];
  const name = pick(row, [`batter${number}Name`], id == null ? 'Unknown batter' : `Player #${id}`);
  return id == null ? <span>{name}</span> : <Link to={`/players/${id}`}>{name}</Link>;
}

export default function PartnershipsTab({ matchId, squad }) {
  const { data, loading, error, reload } = useApi(`/matches/${matchId}/partnerships`);
  const groups = groupPartnerships(data);

  return (
    <div className="partnerships-tab">
      <div className="partnerships-toolbar">
        <p className="subtle">Runs include extras. Balls count legal deliveries.</p>
        <button type="button" className="pill partnerships-refresh" disabled={loading} onClick={reload}>
          Refresh
        </button>
      </div>
      {loading ? <Loading rows={4} label="Loading partnerships…" />
        : error ? <ErrorState message={error} onRetry={reload} />
          : groups.length === 0 ? <EmptyState title="No partnerships yet" message="No partnerships have been recorded for this match." />
            : groups.map((group) => {
              const team = partnershipTeamName(group.rows, squad);
              const runs = group.rows.reduce((sum, row) => sum + (Number(row.runs) || 0), 0);
              const balls = group.rows.reduce((sum, row) => sum + (Number(row.ballsFaced) || 0), 0);
              return (
                <section className="innings-block" key={group.key} aria-label={group.label}>
                  <div className="innings-heading partnership-heading">
                    <h4>{group.label}{team ? ` — ${team}` : ''}</h4>
                    <p className="subtle partnership-summary">
                      {group.rows.length} {group.rows.length === 1 ? 'stand' : 'stands'} · {runs} runs · {balls} balls
                    </p>
                  </div>
                  <div className="partnership-table">
                    <DataTable
                      rows={group.rows.map((row) => ({ ...row, id: row.partnershipId }))}
                      columns={[
                        { key: 'wicket', label: 'Wicket', render: (row) => pick(row, ['wicketNumber']) },
                        {
                          key: 'batters', label: 'Batters', render: (row) => (
                            <div className="partnership-batters name-cell">
                              <Batter row={row} number={1} />
                              <span className="subtle" aria-hidden="true">&amp;</span>
                              <Batter row={row} number={2} />
                            </div>
                          ),
                        },
                        { key: 'runs', label: 'Runs', align: 'right', render: (row) => <strong>{pick(row, ['runs'])}</strong> },
                        { key: 'ballsFaced', label: 'Balls', align: 'right' },
                      ]}
                    />
                  </div>
                </section>
              );
            })}
    </div>
  );
}
