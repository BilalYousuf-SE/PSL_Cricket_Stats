import { useId } from 'react';
import { useApi } from '../useApi.js';
import { EMPTY_MATCHUP_FILTERS, bowlingStyleLabel, matchupFilterError } from '../matchups.js';

export default function MatchupFilters({ value, onChange }) {
  const id = useId();
  const { data: options, loading, error, reload } = useApi('/matchups/options');
  const update = (key, next) => onChange({ ...value, [key]: next });
  const validation = matchupFilterError(value);

  return (
    <div className="matchup-filters">
      <div className="matchup-controls">
        <label htmlFor={`${id}-season`}>Season
          <select id={`${id}-season`} className="control-select" value={value.seasonYear} onChange={(e) => update('seasonYear', e.target.value)}>
            <option value="">All seasons</option>
            {(options?.seasons || []).map((year) => <option key={year} value={year}>{year}</option>)}
          </select>
        </label>
        <label htmlFor={`${id}-from`}>From date
          <input id={`${id}-from`} className="control-input" type="date" value={value.fromDate} onChange={(e) => update('fromDate', e.target.value)} />
        </label>
        <label htmlFor={`${id}-to`}>To date
          <input id={`${id}-to`} className="control-input" type="date" value={value.toDate} onChange={(e) => update('toDate', e.target.value)} />
        </label>
        <label htmlFor={`${id}-venue`}>Venue
          <select id={`${id}-venue`} className="control-select" value={value.venue} onChange={(e) => update('venue', e.target.value)}>
            <option value="">All venues</option>
            {(options?.venues || []).map((venue) => <option key={venue} value={venue}>{venue}</option>)}
          </select>
        </label>
        <label htmlFor={`${id}-innings`}>Innings
          <input id={`${id}-innings`} className="control-input matchup-innings" type="number" min="1" step="1" placeholder="All innings"
            value={value.innings} onChange={(e) => update('innings', e.target.value)} />
        </label>
        <label htmlFor={`${id}-style`}>Bowling style
          <select id={`${id}-style`} className="control-select" value={value.bowlingStyle} onChange={(e) => update('bowlingStyle', e.target.value)}>
            <option value="">All styles</option>
            {(options?.bowlingStyles || []).map((style) => <option key={style} value={style}>{bowlingStyleLabel(style)}</option>)}
          </select>
        </label>
        <button type="button" className="matchup-button" onClick={() => onChange({ ...EMPTY_MATCHUP_FILTERS })}>Reset filters</button>
      </div>
      <p className="matchup-note">Season and dates work together. Both boundary dates are included.</p>
      {loading && <p className="matchup-note" role="status">Loading filter choices…</p>}
      {error && <p className="matchup-filter-error" role="alert">Filter choices could not load. <button className="matchup-link" type="button" onClick={reload}>Retry filters</button></p>}
      {validation && <p className="matchup-filter-error" role="alert">{validation}</p>}
    </div>
  );
}
