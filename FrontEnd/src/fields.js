// Shared accessors for DTO fields used by more than one API response.

export function pick(obj, keys, fallback = '—') {
  if (!obj) return fallback;
  for (const key of keys) {
    const val = key.split('.').reduce((o, k) => (o == null ? undefined : o[k]), obj);
    if (val !== undefined && val !== null && val !== '') return val;
  }
  return fallback;
}

// Accept bare lists and common paginated response envelopes.
export function toArray(value) {
  if (Array.isArray(value)) return value;
  if (value && Array.isArray(value.content)) return value.content;
  if (value && Array.isArray(value.data)) return value.data;
  if (value && Array.isArray(value.items)) return value.items;
  if (value && Array.isArray(value.results)) return value.results;
  return [];
}

export const teamName = (t) => pick(t, ['teamName']);
export const teamId = (t) => pick(t, ['teamId']);

export const playerName = (p) => pick(p, ['playerName', 'fullName']);
export const playerId = (p) => pick(p, ['playerId', 'id']);

export const matchId = (m) => pick(m, ['matchId']);

// MatchDTO: teamAId/teamAName vs teamBId/teamBName. Given "this" team's id,
// return the name of the other side.
export function opponentOf(match, thisTeamId) {
  const aId = pick(match, ['teamAId'], null);
  const aName = pick(match, ['teamAName']);
  const bName = pick(match, ['teamBName']);
  return String(aId) === String(thisTeamId) ? bName : aName;
}

// ResultType uses runs, wickets, tie, or no_result.
export function resultText(match) {
  const winner = pick(match, ['winnerName'], null);
  const margin = pick(match, ['resultMargin'], null);
  const type = pick(match, ['resultType'], null);
  const typeLabel = type ? String(type).replace(/_/g, ' ').toLowerCase() : null;
  if (!winner) return typeLabel || 'No result';
  if (margin != null) return `${winner} won by ${margin}${typeLabel ? ' ' + typeLabel : ''}`;
  return `${winner} won${typeLabel ? ` (${typeLabel})` : ''}`;
}
