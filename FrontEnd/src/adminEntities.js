import { groupPartnerships, partnershipBatterOptions } from './partnerships.js';

const F = (n, t = 'text', req, extra = {}) => ({ n, t, req, ...extra });

const ROLE_OPTIONS = ['Batsman', 'Baller', 'Keeper', 'Allrounder'];
const STYLE_OPTIONS = ['Right', 'Left'];
const BOWLING_STYLE_OPTIONS = [
  { value: 'FastBaller', label: 'Fast baller' },
  { value: 'FastMedium', label: 'Fast-medium' },
  { value: 'MediumFast', label: 'Medium-fast' },
  { value: 'MediumPacer', label: 'Medium pacer' },
  { value: 'SlowMediumPacer', label: 'Slow-medium pacer' },
  { value: 'OffSpinner', label: 'Off spinner' },
  { value: 'LegSpinner', label: 'Leg spinner' },
  { value: 'LeftArmOrthodox', label: 'Left-arm orthodox' },
  { value: 'LeftArmWristSpin', label: 'Left-arm wrist spin' },
];
const FORMAT_OPTIONS = ['T20', 'ODI', 'Test', 'SUPEROVER'];
const TOSS_OPTIONS = ['bat', 'bowl'];
const RESULT_TYPE_OPTIONS = ['runs', 'wickets', 'tie', 'no_result'];

// Top-level entities managed from the main Admin tabs. `cols` is
// [fieldKey, columnLabel] and is independent of `f` (the create/edit form
// fields) — the table shows friendly read fields (names), the form uses
// the raw IDs the backend's write endpoints actually expect.
export const ENTITIES = [
  {
    k: 'teams', l: 'Teams', p: '/teams', id: 'teamId',
    cols: [['teamId', 'ID'], ['teamName', 'Name']],
    f: [F('teamName', 'text', 1)],
  },
  {
    k: 'players', l: 'Players', p: '/players', id: 'id',
    cols: [['id', 'ID'], ['fullName', 'Name'], ['role', 'Role'], ['nationality', 'Nationality']],
    f: [
      F('firstName', 'text', 1),
      F('lastName', 'text', 1),
      F('age', 'number'),
      F('role', 'select', 1, { options: ROLE_OPTIONS }),
      F('nationality', 'text', 1),
      F('playingStyle', 'select', 0, { options: STYLE_OPTIONS }),
      F('bowlingStyle', 'select', 0, { options: BOWLING_STYLE_OPTIONS }),
    ],
  },
  {
    k: 'seasons', l: 'Seasons', p: '/season', id: 'seasonId',
    cols: [['seasonId', 'ID'], ['year', 'Year']],
    f: [F('year', 'number', 1)],
  },
  {
    k: 'matches', l: 'Matches', p: '/matches', id: 'matchId',
    cols: [
      ['matchId', 'ID'], ['matchDate', 'Date'], ['teamAName', 'Team A'],
      ['teamBName', 'Team B'], ['venue', 'Venue'], ['winnerName', 'Winner'],
    ],
    f: [
      F('matchDate', 'date', 1),
      F('venue', 'text', 1),
      F('format', 'select', 1, { options: FORMAT_OPTIONS }),
      F('teamAId', 'select', 1, { optionsFrom: 'teams' }),
      F('teamBId', 'select', 1, { optionsFrom: 'teams' }),
      F('seasonId', 'select', 1, { optionsFrom: 'seasons' }),
      F('tossWinnerId', 'select', 0, { optionsFrom: 'teams' }),
      F('tossDecision', 'select', 0, { options: TOSS_OPTIONS }),
      F('winnerId', 'select', 0, { optionsFrom: 'teams' }),
      F('resultMargin', 'number'),
      F('resultType', 'select', 0, { options: RESULT_TYPE_OPTIONS }),
      F('playerOfMatchId', 'select', 0, { optionsFrom: 'players' }),
    ],
    clickable: true, // clicking a row (not the Edit/Delete buttons) opens the Match Admin view
  },
];

// Nested entities scoped to a single match, opened by clicking a match row.
// Batting/Bowling have no `f` (no Add button, no Edit/Delete) — they're
// derived automatically from Deliveries on the backend and shown read-only
// so you can sanity-check the numbers after editing deliveries.
export const MATCH_ENTITIES = [
  {
    k: 'squad', l: 'Squad', p: (m) => `/matches/${m}/squad`, id: 'playerId',
    cols: [
      ['playerId', 'Player ID'], ['playerName', 'Player'], ['teamName', 'Team'],
      ['isCaptain', 'Capt'], ['isKeeper', 'Keeper'], ['isPlaying', 'Playing XI'],
    ],
    f: [
      F('playerId', 'select', 1, { optionsFrom: 'players' }),
      F('teamId', 'select', 1, { optionsFrom: 'teams' }),
      F('isCaptain', 'bool'),
      F('isKeeper', 'bool'),
      F('isPlaying', 'bool', 0, { defaultValue: true }),
    ],
  },
  {
    k: 'batting', l: 'Batting', p: (m) => `/matches/${m}/batting-scorecard`, id: 'playerId',
    cols: [
      ['playerId', 'Player ID'], ['playerName', 'Player'], ['runs', 'Runs'],
      ['ballsFaced', 'Balls'], ['fours', '4s'], ['sixes', '6s'], ['isOut', 'Out'],
    ],
    readOnly: true,
  },
  {
    k: 'bowling', l: 'Bowling', p: (m) => `/matches/${m}/bowling-scorecard`, id: 'playerId',
    cols: [
      ['playerId', 'Player ID'], ['playerName', 'Player'], ['oversBowled', 'Overs'],
      ['maidens', 'Maidens'], ['runsConceded', 'Runs'], ['wickets', 'Wkts'], ['economy', 'Econ'],
    ],
    readOnly: true,
  },
  {
    k: 'partnerships', l: 'Partnerships', p: (m) => `/matches/${m}/partnerships`, id: 'partnershipId',
    cols: [
      ['partnershipId', 'ID'], ['wicketNumber', 'Wicket'], ['batter1Name', 'Batter 1'],
      ['batter2Name', 'Batter 2'], ['runs', 'Runs'], ['ballsFaced', 'Balls'],
    ],
    groupRows: groupPartnerships,
    description: 'Runs include extras. Balls count legal deliveries.',
    resolveInitial: (row) => ({ innings: row.innings ?? '' }),
    f: [
      F('innings', 'number', 1, { label: 'Innings', min: 1, step: 1, defaultValue: 1 }),
      F('wicketNumber', 'number', 1, { label: 'Wicket number', min: 1, max: 10, step: 1, defaultValue: 1 }),
      F('batter1Id', 'select', 1, {
        label: 'Batter 1', optionsFrom: 'squad',
        getOptions: (values, refs) => partnershipBatterOptions(values, refs, 'batter2Id'),
      }),
      F('batter2Id', 'select', 1, {
        label: 'Batter 2', optionsFrom: 'squad',
        getOptions: (values, refs) => partnershipBatterOptions(values, refs, 'batter1Id'),
      }),
      F('runs', 'number', 1, { label: 'Runs', min: 0, step: 1, defaultValue: 0 }),
      F('ballsFaced', 'number', 1, { label: 'Balls', min: 0, step: 1, defaultValue: 0 }),
    ],
    validate: (body) => body.batter1Id === body.batter2Id ? 'Choose two different batters.' : '',
  },
  {
    k: 'deliveries', l: 'Deliveries', p: (m) => `/matches/${m}/deliveries`, id: 'deliveryId',
    cols: [
      ['deliveryId', 'ID'], ['innings', 'Inn'], ['overNum', 'Over'], ['ballNum', 'Ball'],
      ['batterName', 'Batter'], ['bowlerName', 'Bowler'], ['runsBatter', 'Runs'], ['wicketType', 'Wicket'],
    ],
    // batterId/nonStrikerId/bowlerId/playerDismissedId are scoped to this
    // match's squad (refData.squad), not the whole player table — far
    // easier to pick the right person from ~22 names than from everyone
    // who's ever played.
    f: [
      F('innings', 'number', 1),
      F('overNum', 'number', 1),
      F('ballNum', 'number', 1),
      F('batterId', 'select', 1, { optionsFrom: 'squad' }),
      F('nonStrikerId', 'select', 0, { optionsFrom: 'squad' }),
      F('bowlerId', 'select', 1, { optionsFrom: 'squad' }),
      F('runsBatter', 'number'),
      F('runsExtras', 'number'),
      F('extraType', 'text'),
      F('wicketType', 'text'),
      F('playerDismissedId', 'select', 0, { optionsFrom: 'squad' }),
    ],
    resolveInitial: (row) => ({ extraType: row.extrasType ?? '' }),
  },
];

