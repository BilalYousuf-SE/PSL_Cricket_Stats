export const EMPTY_MATCHUP_FILTERS = Object.freeze({
  seasonYear: '', fromDate: '', toDate: '', venue: '', innings: '', bowlingStyle: '',
});

export const MATCHUP_METRICS = [
  { key: 'runs', label: 'Runs' },
  { key: 'ballsFaced', label: 'Balls faced' },
  { key: 'dismissals', label: 'Dismissals' },
  { key: 'strikeRate', label: 'Strike rate', rate: true },
  { key: 'average', label: 'Batting average', rate: true },
  { key: 'fours', label: 'Fours' },
  { key: 'sixes', label: 'Sixes' },
  { key: 'dotBalls', label: 'Dot balls' },
  { key: 'runsConceded', label: 'Runs conceded' },
  { key: 'legalBalls', label: 'Legal balls' },
  { key: 'economy', label: 'Economy', rate: true },
  { key: 'runOuts', label: 'Striker run-outs' },
];

export function matchupFilterError(filters) {
  if (filters.fromDate && filters.toDate && filters.fromDate > filters.toDate) {
    return 'From date must be on or before to date.';
  }
  if (filters.innings && (!/^\d+$/.test(String(filters.innings)) || Number(filters.innings) < 1)) {
    return 'Innings must be a positive whole number.';
  }
  return '';
}

export function matchupQuery(filters, extra = {}) {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries({ ...filters, ...extra })) {
    if (value !== '' && value !== null && value !== undefined) params.set(key, String(value));
  }
  const query = params.toString();
  return query ? `?${query}` : '';
}

export function matchupValue(stats, key) {
  const value = stats?.[key];
  if (value === null || value === undefined) return '—';
  return MATCHUP_METRICS.find((metric) => metric.key === key)?.rate
    ? Number(value).toFixed(2) : String(value);
}

export function matchupCells(cells = []) {
  return new Map(cells.map((cell) => [`${cell.batter.playerId}:${cell.bowler.playerId}`, cell]));
}

export function canComparePlayers(first, second) {
  return /^[1-9]\d*$/.test(String(first)) && /^[1-9]\d*$/.test(String(second)) && String(first) !== String(second);
}

export const bowlingStyleLabel = (style) => String(style || '').replace(/([a-z])([A-Z])/g, '$1 $2');
