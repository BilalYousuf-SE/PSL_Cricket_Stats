import assert from 'node:assert/strict';
import { test } from 'node:test';
import { EMPTY_MATCHUP_FILTERS, canComparePlayers, matchupCells, matchupFilterError, matchupQuery, matchupValue } from '../src/matchups.js';

test('season and inclusive date bounds are sent together without any team-affiliation restriction', () => {
  const filters = { ...EMPTY_MATCHUP_FILTERS, seasonYear: '2025', fromDate: '2025-02-01', toDate: '2025-02-01' };
  assert.equal(matchupFilterError(filters), '');
  const params = new URLSearchParams(matchupQuery(filters, { page: 0, size: 25 }));
  assert.deepEqual(Object.fromEntries(params), { seasonYear: '2025', fromDate: '2025-02-01', toDate: '2025-02-01', page: '0', size: '25' });
  assert.equal(matchupQuery(EMPTY_MATCHUP_FILTERS), '');
  assert.equal(matchupFilterError({ ...filters, fromDate: '2025-02-02' }), 'From date must be on or before to date.');
});

test('open-ended dates and venue punctuation survive query serialization', () => {
  const venue = "National Stadium, Karachi & Annex 'A'";
  const params = new URLSearchParams(matchupQuery({ ...EMPTY_MATCHUP_FILTERS, venue, toDate: '2025-03-01' }));
  assert.equal(params.get('venue'), venue);
  assert.equal(params.get('toDate'), '2025-03-01');
  assert.equal(params.has('fromDate'), false);
  assert.equal(params.has('seasonYear'), false);
});

test('zero-run encounters, wides-only encounters, and absent encounters remain distinguishable', () => {
  const zero = { batter: { playerId: 1 }, bowler: { playerId: 2 }, stats: { runs: 0, ballsFaced: 1, deliveries: 1, strikeRate: 0 } };
  const wides = { batter: { playerId: 1 }, bowler: { playerId: 3 }, stats: { runs: 0, ballsFaced: 0, deliveries: 1, strikeRate: null } };
  const cells = matchupCells([zero, wides]);
  assert.equal(cells.get('1:2').stats.deliveries, 1);
  assert.equal(matchupValue(cells.get('1:2').stats, 'runs'), '0');
  assert.equal(matchupValue(cells.get('1:2').stats, 'strikeRate'), '0.00');
  assert.equal(cells.get('1:3').stats.deliveries, 1);
  assert.equal(matchupValue(cells.get('1:3').stats, 'strikeRate'), '—');
  assert.equal(cells.has('1:4'), false);
});

test('undefined averages stay undefined while recorded zero averages are displayed', () => {
  assert.equal(matchupValue({ average: null }, 'average'), '—');
  assert.equal(matchupValue({ average: 0 }, 'average'), '0.00');
  assert.equal(matchupValue({ average: 22.5 }, 'average'), '22.50');
});

test('comparison inputs reject self-pairs and invalid IDs and innings', () => {
  assert.equal(canComparePlayers(1, '2'), true);
  for (const [a, b] of [[1, '1'], ['', 2], [-1, 2], ['undefined', 2], [1.5, 2]]) assert.equal(canComparePlayers(a, b), false);
  for (const innings of ['0', '-1', '1.5']) assert.match(matchupFilterError({ ...EMPTY_MATCHUP_FILTERS, innings }), /positive whole/);
});
