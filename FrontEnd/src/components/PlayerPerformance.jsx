import { useId, useState } from 'react';
import { useApi } from '../useApi.js';
import { bowlingStyleLabel } from '../matchups.js';
import {
  EMPTY_PERFORMANCE_FILTERS, PERFORMANCE_MODES, COVERAGE_LABELS,
  performanceDimensions, performanceQuery, performanceFilterError,
  performanceValue, performanceColumns, performanceMetrics, performanceRowFilter,
} from '../performance.js';
import DataTable from './DataTable.jsx';
import Loading from './Loading.jsx';
import { EmptyState, ErrorState } from './ErrorState.jsx';
import '../performance.css';

export default function PlayerPerformance({ playerId, role }) {
  const id = useId();
  const initialMode = role === 'Baller' ? 'bowling' : role === 'Keeper' ? 'keeping' : 'batting';
  const [mode, setMode] = useState(initialMode);
  const [filters, setFilters] = useState({ ...EMPTY_PERFORMANCE_FILTERS });
  const [dimension, setDimension] = useState(initialMode === 'bowling' ? 'batterHand' : 'bowlingStyle');
  const validation = performanceFilterError(filters);
  const { data, loading, error, reload } = useApi(`/players/${playerId}/performance${performanceQuery(filters, mode)}`, [], { enabled: !validation });
  const choices = useApi(`/players/${playerId}/performance/options`);
  const options = choices.data || {};
  const update = (key, value) => setFilters((current) => ({ ...current, [key]: value }));
  const chooseMode = (next) => {
    setMode(next);
    setDimension(next === 'bowling' ? 'batterHand' : 'bowlingStyle');
    setFilters((current) => ({ ...current, bowlingStyle: next === 'bowling' ? '' : current.bowlingStyle, batterHand: next === 'batting' ? '' : current.batterHand }));
  };
  const select = (key, label, values, empty = 'All') => (
    <label className="performance-control" htmlFor={`${id}-${key}`}>
      <span>{label}</span>
      <select id={`${id}-${key}`} className="control-select" value={filters[key]} onChange={(event) => update(key, event.target.value)}>
        <option value="">{empty}</option>
        {values.map((item) => <option key={String(item.value ?? item)} value={item.value ?? item}>{item.label ?? item}</option>)}
      </select>
    </label>
  );
  const active = Object.entries(filters).filter(([, value]) => value !== '');
  const rows = (data?.breakdowns?.[dimension] || []).map((row) => ({ ...row, id: row.key }));
  const coverage = Object.entries(data?.coverage || {}).filter(([, count]) => count > 0);
  const teamOptions = (options.teams || []).map((team) => ({ value: team.id, label: team.name || `Team #${team.id}` }));
  const filterLabel = (key, value) => {
    if (key === 'teamId' || key === 'oppositionId') return `${key === 'teamId' ? 'Team' : 'Opposition'}: ${teamOptions.find((team) => String(team.value) === String(value))?.label || value}`;
    const labels = { seasonYear: 'Season', fromDate: 'From', toDate: 'To', battingPosition: 'Position', captaincy: 'Captaincy', bowlingStyle: 'Bowling type', batterHand: 'Batter hand', stage: 'Stage', innings: 'Innings', venue: 'Venue' };
    return `${labels[key]}: ${bowlingStyleLabel(value)}`;
  };

  return (
    <section className="player-performance" aria-label="Player performance">
      <div className="performance-mode-row" aria-label="Performance discipline">
        {PERFORMANCE_MODES.map((item) => <button key={item.id} type="button" className="performance-button" aria-pressed={mode === item.id} onClick={() => chooseMode(item.id)}>{item.label}</button>)}
      </div>
      <div className="performance-filters">
        <div className="performance-filter-grid">
          {select('seasonYear', 'Season', options.seasons || [], 'All seasons')}
          {['fromDate', 'toDate'].map((key) => <label key={key} className="performance-control" htmlFor={`${id}-${key}`}><span>{key === 'fromDate' ? 'From date' : 'To date'}</span><input id={`${id}-${key}`} className="control-input" type="date" value={filters[key]} onChange={(event) => update(key, event.target.value)} /></label>)}
          <button type="button" className="performance-button" onClick={() => setFilters({ ...EMPTY_PERFORMANCE_FILTERS })}>Reset filters</button>
        </div>
        <details className="performance-advanced">
          <summary>More filters</summary>
          <div className="performance-filter-grid">
            {select('teamId', 'Team represented', teamOptions, 'All represented teams')}
            {select('oppositionId', 'Opposition', teamOptions, 'All opposition teams')}
            {select('venue', 'Venue', options.venues || [], 'All venues')}
            {select('innings', 'Innings', [{ value: 1, label: mode === 'batting' ? '1 · Batting first' : '1 · Bowling first' }, { value: 2, label: mode === 'batting' ? '2 · Chasing' : '2 · Defending' }, { value: 3, label: '3 · Extra innings' }, { value: 4, label: '4 · Extra innings' }])}
            {select('battingPosition', mode === 'batting' ? 'Your batting position' : 'Opponent batting position', Array.from({ length: 11 }, (_, i) => i + 1))}
            {select('captaincy', 'Captaincy', [{ value: 'captain', label: 'Captain' }, { value: 'not-captain', label: 'Not captain' }, { value: 'unknown', label: 'Unrecorded' }])}
            {select('stage', 'Match stage', (options.stages || []).map((stage) => ({ value: stage, label: stage === 'League' ? 'League / group' : bowlingStyleLabel(stage) })), 'All stages')}
            {mode !== 'bowling' && select('bowlingStyle', 'Bowling type', (options.bowlingStyles || []).map((style) => ({ value: style, label: bowlingStyleLabel(style) })), 'All bowling types')}
            {mode !== 'batting' && select('batterHand', 'Opponent batting hand', [{ value: 'Right', label: 'Right-handed' }, { value: 'Left', label: 'Left-handed' }], 'Both hands')}
          </div>
        </details>
        <p className="performance-filter-note">Season and date filters work together; both boundary dates are included.</p>
        {choices.error && <p className="performance-filter-note" role="alert">Filter choices could not load. <button className="performance-text-button" type="button" onClick={choices.reload}>Retry choices</button></p>}
        {validation && <p className="performance-filter-error" role="alert">{validation}</p>}
        {active.length > 0 && <div className="performance-active-filters" aria-label="Active performance filters">{active.map(([key, value]) => <button key={key} type="button" onClick={() => update(key, '')} aria-label={`Clear ${filterLabel(key, value)}`}>{filterLabel(key, value)} ×</button>)}</div>}
      </div>
      {loading ? <Loading rows={5} label="Calculating player performance…" />
        : error ? <ErrorState message={error} onRetry={reload} />
          : data && <div className="scorecard performance-results">
            <div className="scorecard-header"><h3>{PERFORMANCE_MODES.find((item) => item.id === mode)?.label} performance</h3></div>
            <div className="performance-metrics">{performanceMetrics(mode).map(([key, label]) => <div key={key}><strong>{performanceValue(data.totals, key)}</strong><span>{label}</span></div>)}</div>
            {mode === 'keeping' && data.coverage?.unclassifiedKeeperCatches > 0 && <p className="performance-data-note">{data.coverage.unclassifiedKeeperCatches} catches lack a match keeper assignment and are excluded from keeper totals.</p>}
            <div className="performance-breakdown-toolbar">
              <label htmlFor={`${id}-breakdown`}>Break down by</label>
              <select id={`${id}-breakdown`} className="control-select" value={dimension} onChange={(event) => setDimension(event.target.value)}>{performanceDimensions(mode).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select>
              <span>{data.totals.matches} matches with recorded contributions</span>
            </div>
            {rows.length > 0 ? <div className="performance-table-scroll" role="region" tabIndex="0" aria-label="Performance breakdown table">
              <DataTable rows={rows} columns={[
                { key: 'label', label: performanceDimensions(mode).find(([key]) => key === dimension)?.[1], render: (row) => {
                  const selected = performanceRowFilter(dimension, row);
                  return selected ? <button type="button" className="performance-text-button" onClick={() => update(selected.key, String(filters[selected.key]) === String(selected.value) ? '' : selected.value)}>{row.label}</button> : row.label;
                } },
                ...performanceColumns(mode).map(([key, label]) => ({ key, label, align: 'right', render: (row) => performanceValue(row.stats, key) })),
              ]} />
            </div> : <EmptyState title="No recorded contributions in this selection" message="Try another discipline or broaden the filters." />}
            {rows.length > 0 && <p className="performance-data-note">Select a recorded category to filter the statistics. Rates are calculated from the selected totals.</p>}
            {coverage.length > 0 && <details className="performance-coverage"><summary>Unrecorded historical details</summary><ul>{coverage.map(([key, count]) => <li key={key}>{count} {COVERAGE_LABELS[key] || key}</li>)}</ul><p>Unidentified fielders are not credited to a player.</p></details>}
            <details className="performance-coverage"><summary>How performance is counted</summary>
              <p>Batting average includes all recorded dismissals, including non-striker run-outs. Balls faced exclude wides and include no-balls. Bowling wickets exclude run-outs and invalid no-ball dismissals; bowling strike rate is legal balls per wicket. Bowling runs conceded exclude byes, leg-byes and penalty extras.</p>
              <p>Keeper catches use the player’s keeper assignment in that match squad. Stumpings use the identified keeper, or a single assigned playing keeper when the fielder is missing. Caught-and-bowled catches belong to the bowler. Unassigned captaincy, stages and player techniques remain unrecorded.</p>
              <p>Batting positions follow first appearance at the crease within each innings, including the non-striker. Bowling types use recorded player techniques. Match and innings counts cover recorded contributions in the selected discipline.</p>
            </details>
          </div>}
    </section>
  );
}
