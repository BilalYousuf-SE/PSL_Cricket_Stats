// Accessors matched to the real backend DTOs (see /mnt/user-data/uploads DTOs).
// A couple of DTOs share a concept under different names depending on which
// endpoint produced them (e.g. player id is "id" on PlayerDTO but "playerId"
// on RunScorerDTO/WicketTakerDTO/etc), so these still try a short list —
// only one of the two is ever present on a given object, so there's no
// ambiguity in practice.

export function pick(obj, keys, fallback = '—') {
  if (!obj) return fallback;
  for (const key of keys) {
    const val = key.split('.').reduce((o, k) => (o == null ? undefined : o[k]), obj);
    if (val !== undefined && val !== null && val !== '') return val;
  }
  return fallback;
}

// Normalizes a list response into a plain array. Every page here was doing
// `Array.isArray(x) ? x : []`, which quietly renders as empty the moment an
// endpoint returns anything other than a bare JSON array — most commonly a
// Spring Data `Page<T>` (`{ content: [...], totalPages, ... }`) instead of a
// `List<T>`. This is very likely what was hiding the Matches tab: the fetch
// succeeds, `data` just isn't an array itself. Unwrap the common envelope
// shapes here once, in one place, instead of at every call site.
export function toArray(value) {
  if (Array.isArray(value)) return value;
  if (value && Array.isArray(value.content)) return value.content;
  if (value && Array.isArray(value.data)) return value.data;
  if (value && Array.isArray(value.items)) return value.items;
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

// MatchDTO has no plain "result" string — derive one from winnerName /
// resultMargin / resultType. resultType is assumed to carry the margin's
// unit (e.g. "RUNS", "WICKETS") — if your ResultType enum instead encodes
// the outcome kind (WIN/TIE/DRAW/NO_RESULT), adjust this to a lookup table.
export function resultText(match) {
  const winner = pick(match, ['winnerName'], null);
  const margin = pick(match, ['resultMargin'], null);
  const type = pick(match, ['resultType'], null);
  const typeLabel = type ? String(type).replace(/_/g, ' ').toLowerCase() : null;
  if (!winner) return typeLabel || 'No result';
  if (margin != null) return `${winner} won by ${margin}${typeLabel ? ' ' + typeLabel : ''}`;
  return `${winner} won${typeLabel ? ` (${typeLabel})` : ''}`;
}
