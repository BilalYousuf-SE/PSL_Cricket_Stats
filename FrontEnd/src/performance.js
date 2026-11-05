export const EMPTY_PERFORMANCE_FILTERS = Object.freeze({
  seasonYear: '', fromDate: '', toDate: '', teamId: '', oppositionId: '', venue: '',
  innings: '', battingPosition: '', captaincy: '', stage: '', bowlingStyle: '', batterHand: '',
});

export const PERFORMANCE_MODES = [
  { id: 'batting', label: 'Batting' }, { id: 'bowling', label: 'Bowling' },
  { id: 'keeping', label: 'Wicketkeeping' }, { id: 'fielding', label: 'Fielding' },
];

const common = [
  ['venue', 'Venue'], ['team', 'Team represented'], ['opposition', 'Opposition'], ['season', 'Season'],
  ['battingPosition', 'Batting position'], ['captaincy', 'Captaincy'], ['innings', 'Innings / match situation'], ['stage', 'Match stage'],
];
export function performanceDimensions(mode) {
  return [...(mode !== 'bowling' ? [['bowlingStyle', 'Bowling type']] : []),
    ...(mode !== 'batting' ? [['batterHand', 'Batter handedness']] : []), ...common];
}

export function performanceQuery(filters, discipline) {
  const params = new URLSearchParams({ discipline });
  for (const [key, value] of Object.entries(filters)) {
    if (value !== '' && value !== null && value !== undefined) params.set(key, String(value));
  }
  return `?${params}`;
}

export function performanceFilterError(filters) {
  if (filters.fromDate && filters.toDate && filters.fromDate > filters.toDate) return 'From date must be on or before to date.';
  if (filters.innings && (!/^\d+$/.test(String(filters.innings)) || Number(filters.innings) < 1)) return 'Innings must be a positive whole number.';
  if (filters.battingPosition && (!/^\d+$/.test(String(filters.battingPosition)) || Number(filters.battingPosition) < 1 || Number(filters.battingPosition) > 11)) return 'Batting position must be between 1 and 11.';
  return '';
}

export function performanceValue(stats, key) {
  const value = stats?.[key];
  if (value === null || value === undefined) return '—';
  return ['average', 'strikeRate', 'economy', 'bowlingAverage', 'bowlingStrikeRate'].includes(key)
    ? Number(value).toFixed(2) : String(value);
}

export function performanceColumns(mode) {
  if (mode === 'batting') return [['runs', 'Runs'], ['dismissals', 'Dismissals'], ['ballsFaced', 'Balls'], ['strikeRate', 'SR'], ['average', 'Avg']];
  if (mode === 'bowling') return [['dismissals', 'Wickets'], ['runsConceded', 'Runs conceded'], ['overs', 'Overs'], ['economy', 'Econ'], ['bowlingAverage', 'Avg'], ['bowlingStrikeRate', 'SR (balls/wkt)']];
  return [['catches', 'Catches'], ['stumpings', 'Stumpings'], ...(mode === 'fielding' ? [['runOuts', 'Run-outs']] : []), ['dismissals', 'Dismissals']];
}

export function performanceMetrics(mode) {
  if (mode === 'batting') return [['runs', 'Runs'], ['average', 'Batting average'], ['strikeRate', 'Strike rate'], ['dismissals', 'Dismissals'], ['ballsFaced', 'Balls faced'], ['innings', 'Innings batted']];
  if (mode === 'bowling') return [['dismissals', 'Wickets'], ['bowlingAverage', 'Bowling average'], ['economy', 'Economy'], ['bowlingStrikeRate', 'Balls per wicket'], ['overs', 'Overs bowled'], ['innings', 'Innings bowled']];
  if (mode === 'keeping') return [['dismissals', 'Keeper dismissals'], ['catches', 'Keeper catches'], ['stumpings', 'Stumpings'], ['innings', 'Innings with dismissals']];
  return [['catches', 'Total catches'], ['keeperCatches', 'Keeper catches'], ['outfieldCatches', 'Outfield catches'], ['unclassifiedCatches', 'Catches: keeper role unknown'], ['stumpings', 'Stumpings'], ['runOuts', 'Recorded run-outs']];
}

export function performanceRowFilter(dimension, row) {
  if (row.key === 'unknown' && dimension !== 'captaincy') return null;
  const key = { season: 'seasonYear', team: 'teamId', opposition: 'oppositionId' }[dimension] || dimension;
  return { key, value: row.key };
}

export const COVERAGE_LABELS = {
  missingStageMatches: 'matches without a recorded stage',
  missingCaptainMatches: 'matches without an established captain assignment',
  missingBattingPositions: 'deliveries without a known batting position',
  missingBowlingStyles: 'deliveries without a recorded bowling type',
  missingBattingHands: 'deliveries without a recorded batter hand',
  unclassifiedKeeperCatches: 'catches without an established keeper assignment',
  unidentifiedFieldingDismissals: 'team dismissals without an identified fielder',
};
