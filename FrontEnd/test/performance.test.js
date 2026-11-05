import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  EMPTY_PERFORMANCE_FILTERS, PERFORMANCE_MODES, performanceColumns,
  performanceDimensions, performanceFilterError, performanceQuery,
  performanceRowFilter, performanceValue,
} from '../src/performance.js';

test('all player disciplines include fielding, batting, bowling and wicketkeeping', () => {
  assert.deepEqual(PERFORMANCE_MODES.map((mode) => mode.id), ['batting', 'bowling', 'keeping', 'fielding']);
});

test('season, dates and contextual filters are combined without dropping unknown captaincy', () => {
  const query = performanceQuery({ ...EMPTY_PERFORMANCE_FILTERS, seasonYear: '2025',
    fromDate: '2025-04-11', toDate: '2025-05-25', teamId: '3', captaincy: 'unknown' }, 'fielding');
  const params = new URLSearchParams(query);
  assert.equal(params.get('discipline'), 'fielding');
  assert.equal(params.get('seasonYear'), '2025');
  assert.equal(params.get('teamId'), '3');
  assert.equal(params.get('captaincy'), 'unknown');
  assert.equal(params.get('fromDate'), '2025-04-11');
  assert.equal(params.get('toDate'), '2025-05-25');
  assert.equal(params.has('battingPosition'), false);
});

test('invalid date order and positions are rejected while equal boundaries are valid', () => {
  assert.match(performanceFilterError({ fromDate: '2025-05-02', toDate: '2025-05-01' }), /From date/);
  assert.equal(performanceFilterError({ fromDate: '2025-05-01', toDate: '2025-05-01' }), '');
  assert.match(performanceFilterError({ battingPosition: '12' }), /between 1 and 11/);
  assert.match(performanceFilterError({ innings: '0' }), /positive/);
});

test('zero scores and undefined rates remain distinguishable', () => {
  assert.equal(performanceValue({ average: null }, 'average'), '—');
  assert.equal(performanceValue({ strikeRate: 0 }, 'strikeRate'), '0.00');
  assert.equal(performanceValue({ catches: 0 }, 'catches'), '0');
  assert.equal(performanceValue({ overs: '4.5' }, 'overs'), '4.5');
});

test('bowling uses bowling rates and each discipline offers appropriate breakdowns', () => {
  const keys = performanceColumns('bowling').map(([key]) => key);
  assert.ok(keys.includes('bowlingAverage'));
  assert.ok(keys.includes('bowlingStrikeRate'));
  assert.ok(!keys.includes('average'));
  assert.ok(performanceDimensions('batting').some(([key]) => key === 'bowlingStyle'));
  assert.ok(performanceDimensions('bowling').some(([key]) => key === 'batterHand'));
  assert.ok(!performanceDimensions('bowling').some(([key]) => key === 'bowlingStyle'));
});

test('category selection maps to filters without submitting invalid unknown enum values', () => {
  assert.deepEqual(performanceRowFilter('season', { key: '2025' }), { key: 'seasonYear', value: '2025' });
  assert.deepEqual(performanceRowFilter('opposition', { key: '2' }), { key: 'oppositionId', value: '2' });
  assert.equal(performanceRowFilter('stage', { key: 'unknown' }), null);
  assert.equal(performanceRowFilter('bowlingStyle', { key: 'unknown' }), null);
  assert.deepEqual(performanceRowFilter('captaincy', { key: 'unknown' }), { key: 'captaincy', value: 'unknown' });
});
