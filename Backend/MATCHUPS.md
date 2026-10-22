# Matchups

Matchups aggregate recorded deliveries on demand. They do not create stored matchup
records and need no data backfill. Updating a delivery changes subsequent results.
The aggregation supports PostgreSQL and MySQL 8; scoring and filters are identical.
The backend's existing `ddl-auto=update` setup creates the two delivery lookup
indexes declared in `Delivery`; no manual SQL import is required.

## Shared history and filters

A batter/bowler pair has the same statistics in direct comparisons, player opponent
lists, and team matrices under the same encounter filters. The historical teams
represented by either player never restrict the delivery calculation.

Every matchup endpoint accepts these optional parameters:

| Parameter | Meaning |
| --- | --- |
| `seasonYear` | The match's assigned season year, not the year extracted from its date |
| `fromDate` | Include match dates on or after this ISO date (`YYYY-MM-DD`) |
| `toDate` | Include match dates on or before this ISO date |
| `venue` | Exact stored venue name; choices are provided by the options endpoint |
| `innings` | Positive innings number |
| `bowlingStyle` | A `BowlingStyle` enum value belonging to the bowler |

Filters intersect. Either date bound can be used alone. No filters means all
recorded history. Matches without a date are included in all-time results and
excluded when a date bound is requested. An inverted date range returns HTTP 400.
Batting-position and over-phase filters are deferred; existing scorecard batting
positions are not innings-specific.

## Read-only APIs

| GET | Response |
| --- | --- |
| `/matchups/options` | Available seasons, venue names, bowling-style values |
| `/matchups/players/{batterId}/{bowlerId}` | Batter, bowler, and combined statistics |
| `/matchups/players/{playerId}?perspective=batting` | Paged opponents, most balls faced first |
| `/matchups/players/{playerId}?perspective=bowling` | Paged batters faced by this bowler |
| `/matchups/players/{batterId}/{bowlerId}/matches` | Paged match/innings breakdown, newest first |
| `/matchups/teams?battingTeamId=1&bowlingTeamId=2` | Complete squad rows/columns and observed pair cells |

Opponent and breakdown pages use zero-based `page` and `size` (default 25, maximum
100). The matrix returns complete `batters` and `bowlers` arrays, plus all observed
pair cells for those rosters, in one response. It has no paging parameters or
roster truncation. One aggregation computes the cells; there is no query for each
individual cell. Matrix statistics support both bowling directions by swapping
teams. Identical player or team selections return 400;
unknown player/team IDs return 404.

### Team matrix membership

Teams determine roster membership only. Rows are distinct recorded playing squad
members for the batting team, across all seasons or the selected `seasonYear`.
Columns are members of the bowling team with a recorded bowling delivery, a
bowling style, or a bowling/all-rounder role. This includes occasional bowlers
regardless of their normal role. Non-playing reserves are excluded.

Dates, venue, innings, and bowling style narrow encounters without changing the
selected squad membership. Thus a season's roster remains selectable when a date
range predates that team's own matches. Every cell uses that pair's encounters
across **all** teams, not only matches between the selected teams. A player who has
represented both teams can appear on both axes; their self-cell is `N/A`.

An absent cell means no recorded encounter under the filters. An observed
zero-run encounter is still a populated, selectable cell. The API returns empty
statistics for a valid direct pair with no encounters and empty lists for empty
filters. The complete roster arrays supply both matrix axes, including players
without encounters under the selected filters.

## Scoring definitions

- `deliveries`: all recorded encounters, including wides and no-balls.
- `matches`/`innings`: distinct matches and distinct match/innings combinations.
- `runs`: batter runs only; `fours`/`sixes`: deliveries with 4/6 batter runs.
- `ballsFaced`: excludes wides, includes no-balls, matching the scorecard calculator.
- `legalBalls`: excludes wides and no-balls.
- `dotBalls`: legal deliveries with zero total runs, including zero-run wickets.
- `dismissals`: this batter dismissed with a bowler-credited wicket: bowled, caught,
  caught-and-bowled, LBW, stumped, or hit wicket. No-balls cannot credit a bowler
  wicket. Wides can credit stumped/hit-wicket dismissals. Retirements and run-outs
  are excluded, and a non-striker's dismissal is never charged to the striker.
- `runOuts`: run-outs of the striker, reported separately.
- `runsConceded`: batter runs plus wide/no-ball extras; excludes byes, leg-byes,
  and penalty extras. The current delivery model records a single extras type.
- `strikeRate`: `100 × runs / ballsFaced`.
- `average`: `runs / dismissals` (bowler-credited dismissals of this batter).
- `economy`: `6 × runsConceded / legalBalls`.
- `bowlingAverage`: `runsConceded / dismissals`.

Rates are rounded to two decimal places, half up. A zero denominator produces
JSON `null`, displayed as `—`, rather than an invented zero or infinity. Bowling
style is player metadata; individual delivery length, speed, and shot type are
not recorded. Results describe the recorded delivery coverage, not inferred
statistics for missing matches.

## UI and checks

The public navigation includes `/matchups` with player and team-matrix views.
Player profiles have batting/bowling opponent lists, and team profiles have a
matrix with their batting team preselected. Selecting an opponent or populated
matrix cell opens the pair summary and match/innings history under the same
filters. The full matrix scrolls vertically through batters and horizontally
through bowlers, with names fixed on both axes. Every populated cell shows
`dismissals/runs (balls faced)`, with `Avg:` and `SR:` immediately below; undefined
rates remain `—`. There are no matrix page controls or statistic selector.

Backend regression coverage runs with `mvn verify` against an isolated MySQL 8+
database. It includes the 45-runs/30-balls/2-dismissals example, extras and wicket
rules, historical team changes, assigned seasons, inclusive/open date ranges,
combined filters, complete matrices beyond the former roster limits, opponent
and breakdown pagination, empty encounters, and delivery edits.
Frontend tests run with `npm test`; the production build uses `npm run build`.
