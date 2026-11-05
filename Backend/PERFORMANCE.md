# Player performance

Player pages have a Performance panel with Batting, Bowling, Wicketkeeping and
Fielding views. Every player can use every view, including Fielding. The initial
view follows the player's role; all-rounders start with Batting. Career match
history stays below the panel and does not depend on performance filters.

## Read APIs

- `GET /players/{playerId}/performance?discipline=batting`
- `GET /players/{playerId}/performance/options`

`discipline` accepts `batting`, `bowling`, `keeping` or `fielding`. Optional filters
combine: `seasonYear`, `fromDate`, `toDate`, `teamId`, `oppositionId`, `venue`,
`innings`, `battingPosition`, `captaincy`, `stage`, `bowlingStyle`, `batterHand`.
Dates are inclusive; seasons use the match's assigned season. `captaincy` accepts
`captain`, `not-captain` or `unknown`; other enums use their Java enum names.

Responses contain totals, compact category breakdowns and missing-metadata counts.
Rates come from summed runs, balls and dismissals, rounded to two decimal places.
A rate with no denominator is `null`, displayed as an em dash; a defined zero
remains zero. Selecting a category in the UI adds its filter.

## Accounting and metadata

- Batting dismissals include non-striker run-outs and exclude retired hurt.
  Balls faced exclude wides and include no-balls. Bowling wickets exclude run-outs
  and invalid no-ball dismissals; overs and bowling strike rate use legal balls.
  Bowling runs conceded exclude byes, leg-byes and penalty extras.
- Batting positions are derived from the first striker/non-striker appearance in
  each innings, including extra innings. Bowling and fielding position filters
  refer to the opponent's position; fielding handedness refers to the dismissed
  batter, including non-striker run-outs.
- Keeper catches use `match_squads.is_keeper` for that match, not a career role.
  Stumpings use the named fielder, or the sole assigned playing keeper if the
  fielder is missing. Caught-and-bowled catches belong to the bowler as outfield
  catches. A recorded run-out credits the recorded fielder only.
- Catches without an established keeper assignment remain unclassified rather
  than being assumed to be outfield catches. Team dismissals without an identified
  fielder are reported as missing metadata and are not credited to a player.
- All-false captain flags without an assigned captain mean unrecorded captaincy.
  Bowling types use recorded bowling techniques; batting handedness does not
  establish a bowler's arm. Left/right-arm pace splits need bowling-arm metadata.
- Match and innings counts cover recorded contributions for the selected view;
  in Fielding/Keeping these are matches/innings with credited dismissals.

The feature reads existing `deliveries.fielder_id` and `matches.stage` columns;
it does not backfill data or change migration SQL. Admin match/delivery forms can
edit those fields. Older API updates that omit them preserve existing values;
explicit `null` clears the value. Fielder choices include substitutes.

## Verification

The backend tests exercise the same repository queries on MySQL 8 and PostgreSQL,
including extras, dismissals, per-innings positions, combined filters, keeper
assignments and preservation of existing metadata. Run `mvn test` against an
isolated test database, and `npm test` / `npm run build` in `FrontEnd`.
