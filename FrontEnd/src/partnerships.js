import { toArray } from './fields.js';

function inningsNumber(value) {
  const number = Number(value);
  return Number.isInteger(number) && number > 0 ? number : null;
}

export function inningsLabel(value) {
  const number = inningsNumber(value);
  if (number === null) return 'Innings not recorded';
  const lastTwo = number % 100;
  const suffix = lastTwo >= 11 && lastTwo <= 13 ? 'th'
    : ({ 1: 'st', 2: 'nd', 3: 'rd' }[number % 10] || 'th');
  return `${number}${suffix} Innings`;
}

// Keep every stand, including resumed pairs and multiple stands for one wicket.
export function groupPartnerships(value) {
  const groups = new Map();
  for (const row of toArray(value)) {
    const innings = inningsNumber(row.innings);
    if (!groups.has(innings)) {
      groups.set(innings, { key: innings ?? 'unknown', innings, label: inningsLabel(innings), rows: [] });
    }
    groups.get(innings).rows.push(row);
  }
  return [...groups.values()]
    .sort((a, b) => (a.innings ?? Infinity) - (b.innings ?? Infinity))
    .map((group) => ({
      ...group,
      rows: group.rows.sort((a, b) =>
        (Number(a.wicketNumber) || Infinity) - (Number(b.wicketNumber) || Infinity)
        || (Number(a.partnershipId) || 0) - (Number(b.partnershipId) || 0)),
    }));
}

// Derive the team from match-specific squad membership rather than the toss.
export function partnershipTeamName(rows, squad) {
  const players = new Map(toArray(squad).map((player) => [String(player.playerId), player]));
  const teams = new Map();
  for (const row of rows) {
    for (const id of [row.batter1Id, row.batter2Id]) {
      const player = players.get(String(id));
      if (player?.teamId != null && player.teamName) teams.set(String(player.teamId), player.teamName);
    }
  }
  return teams.size === 1 ? [...teams.values()][0] : '';
}

export function partnershipBatterOptions(values, refData, otherField) {
  const squad = refData.squad || [];
  const otherId = values[otherField];
  const other = squad.find((player) => String(player.value) === String(otherId));
  return squad.filter((player) => player.isPlaying !== false
    && String(player.value) !== String(otherId)
    && (!other || other.teamId == null || String(player.teamId) === String(other.teamId)))
    .map((player) => ({ ...player, label: player.teamName ? `${player.label} — ${player.teamName}` : player.label }));
}
