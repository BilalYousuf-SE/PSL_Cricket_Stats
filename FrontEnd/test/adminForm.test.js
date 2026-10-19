import assert from 'node:assert/strict';
import { test } from 'node:test';
import { serializeForm } from '../src/adminForm.js';
import { ENTITIES, MATCH_ENTITIES } from '../src/adminEntities.js';

test('cleared optional IDs and text fields are sent as null', () => {
  const fields = [{ n: 'winnerId', t: 'select', optionsFrom: 'teams' }, { n: 'extraType', t: 'text' }];
  assert.deepEqual(serializeForm(fields, { winnerId: '', extraType: '' }), { winnerId: null, extraType: null });
});

test('converts foreign keys and runs to numbers while retaining booleans', () => {
  const fields = [{ n: 'batterId', t: 'select', optionsFrom: 'squad' }, { n: 'runsBatter', t: 'number' }, { n: 'isPlaying', t: 'bool' }];
  assert.deepEqual(serializeForm(fields, { batterId: '12', runsBatter: '0', isPlaying: false }), { batterId: 12, runsBatter: 0, isPlaying: false });
});

test('delivery edit retains player IDs even when players share a name', () => {
  const entity = MATCH_ENTITIES.find((entry) => entry.k === 'deliveries');
  const row = { batterId: 12, nonStrikerId: 14, bowlerId: 16, playerDismissedId: 14, extrasType: 'no_ball' };
  const values = { ...row, ...entity.resolveInitial(row) };
  const result = serializeForm(entity.f, values);
  assert.equal(result.batterId, 12);
  assert.equal(result.nonStrikerId, 14);
  assert.equal(result.bowlerId, 16);
  assert.equal(result.playerDismissedId, 14);
  assert.equal(result.extraType, 'no_ball');
});

test('derived scorecards expose no write forms', () => {
  for (const key of ['batting', 'bowling']) {
    const entity = MATCH_ENTITIES.find((entry) => entry.k === key);
    assert.equal(entity.readOnly, true);
    assert.equal(entity.f, undefined);
  }
});

test('fullName is derived by the backend and new squad members default to playing', () => {
  assert.equal(ENTITIES.find((entry) => entry.k === 'players').f.some((field) => field.n === 'fullName'), false);
  assert.equal(MATCH_ENTITIES.find((entry) => entry.k === 'squad').f.find((field) => field.n === 'isPlaying').defaultValue, true);
});


test('player forms separate playing hand, bowling technique, and the four roles', () => {
  const fields = ENTITIES.find((entry) => entry.k === 'players').f;
  assert.deepEqual(fields.find((field) => field.n === 'role').options, ['Batsman', 'Baller', 'Keeper', 'Allrounder']);
  assert.equal(fields.some((field) => field.n === 'battingStyle'), false);
  assert.deepEqual(fields.find((field) => field.n === 'playingStyle').options, ['Right', 'Left']);
  const bowling = fields.find((field) => field.n === 'bowlingStyle');
  assert.ok(bowling.options.some((option) => option.value === 'LeftArmOrthodox'));
  assert.ok(bowling.options.every((option) => option.value !== 'Right' && option.value !== 'Left'));
  assert.deepEqual(serializeForm(fields.filter((field) => ['role', 'playingStyle', 'bowlingStyle'].includes(field.n)), {
    role: 'Baller', playingStyle: 'Right', bowlingStyle: 'LeftArmOrthodox',
  }), { role: 'Baller', playingStyle: 'Right', bowlingStyle: 'LeftArmOrthodox' });
});
