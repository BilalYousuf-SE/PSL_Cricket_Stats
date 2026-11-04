import assert from 'node:assert/strict';
import { test } from 'node:test';
import { groupPartnerships, inningsLabel, partnershipBatterOptions, partnershipTeamName } from '../src/partnerships.js';
import { MATCH_ENTITIES } from '../src/adminEntities.js';
import { serializeForm } from '../src/adminForm.js';

test('groups innings in order without merging resumed pairs or guessing missing innings', () => {
  const rows = [
    { partnershipId: 8, innings: 2, wicketNumber: 1, batter1Id: 1, batter2Id: 2 },
    { partnershipId: 4, innings: 1, wicketNumber: 2, batter1Id: 1, batter2Id: 2 },
    { partnershipId: 3, innings: '1', wicketNumber: 1, batter1Id: 1, batter2Id: 2 },
    { partnershipId: 2, innings: 1, wicketNumber: 1, batter1Id: 1, batter2Id: 2 },
    { partnershipId: 9, innings: 3, wicketNumber: 1 },
    { partnershipId: 1, innings: null, wicketNumber: 1 },
  ];
  const groups = groupPartnerships(rows);
  assert.deepEqual(groups.map((group) => group.label), ['1st Innings', '2nd Innings', '3rd Innings', 'Innings not recorded']);
  assert.deepEqual(groups[0].rows.map((row) => row.partnershipId), [2, 3, 4]);
  assert.equal(groups.reduce((sum, group) => sum + group.rows.length, 0), rows.length);
  assert.equal(rows[0].partnershipId, 8);
  assert.deepEqual(groupPartnerships(null), []);
});

test('supports extra innings and leaves invalid innings unassigned', () => {
  for (const [number, expected] of [[4, '4th'], [11, '11th'], [12, '12th'], [13, '13th'], [21, '21st']]) {
    assert.equal(inningsLabel(number), `${expected} Innings`);
  }
  for (const value of [null, '', 0, -1, 1.5, 'unknown']) assert.equal(inningsLabel(value), 'Innings not recorded');
});

test('team labels use historical squad membership and avoid ambiguous teams', () => {
  const squad = [
    { playerId: 1, teamId: 10, teamName: 'Team A' },
    { playerId: 2, teamId: 10, teamName: 'Team A' },
    { playerId: 3, teamId: 20, teamName: 'Team B' },
  ];
  assert.equal(partnershipTeamName([{ batter1Id: '1', batter2Id: 2 }], squad), 'Team A');
  assert.equal(partnershipTeamName([{ batter1Id: 1, batter2Id: 3 }], squad), '');
  assert.equal(partnershipTeamName([{ batter1Id: 1, batter2Id: 2 }], []), '');
});

test('batter choices exclude the selected partner, opponents, and non-playing squad members', () => {
  const refs = { squad: [
    { value: 1, label: 'Same Name', teamId: 10, teamName: 'Team A', isPlaying: true },
    { value: 2, label: 'Same Name', teamId: 10, teamName: 'Team A', isPlaying: true },
    { value: 3, label: 'Opponent', teamId: 20, teamName: 'Team B', isPlaying: true },
    { value: 4, label: 'Bench', teamId: 10, teamName: 'Team A', isPlaying: false },
  ] };
  assert.deepEqual(partnershipBatterOptions({}, refs, 'batter1Id').map((option) => option.value), [1, 2, 3]);
  const choices = partnershipBatterOptions({ batter1Id: '1' }, refs, 'batter1Id');
  assert.deepEqual(choices.map((option) => option.value), [2]);
  assert.equal(choices[0].label, 'Same Name — Team A');
});

test('the admin writes the partnership API payload with numeric IDs and zero totals intact', () => {
  const entity = MATCH_ENTITIES.find((entry) => entry.k === 'partnerships');
  assert.equal(entity.p(17), '/matches/17/partnerships');
  assert.equal(entity.id, 'partnershipId');
  const body = serializeForm(entity.f, {
    innings: '2', wicketNumber: '3', batter1Id: '12', batter2Id: '14', runs: '0', ballsFaced: '0',
  });
  assert.deepEqual(body, { innings: 2, wicketNumber: 3, batter1Id: 12, batter2Id: 14, runs: 0, ballsFaced: 0 });
  assert.equal(entity.validate(body), '');
  assert.match(entity.validate({ ...body, batter2Id: 12 }), /different batters/);
  assert.equal(entity.resolveInitial({ innings: null }).innings, '');
});
