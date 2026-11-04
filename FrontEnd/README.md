# Cricket Stats UI

React and Vite frontend for the Spring Boot API in `../Backend`.

## Development

Use Node.js 20 or later. Install the locked dependencies with `npm ci`, then run:

```bash
npm run dev
npm test
npm run build
```

The development server uses port 5173. The API defaults to port 8080; set
`VITE_API_BASE_URL` in `.env.local` to use another backend. The backend needs a
MySQL or PostgreSQL database; its datasource can be configured using Spring's
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` environment variables. No dataset is included.

The repository currently tracks `node_modules` from a Windows installation.
In the prepared cloud environment, use `/workspace/.psl-setup/frontend` as the
npm working directory instead; it has Linux dependencies and a symlink to the
live source tree. Copy `test/` there before running `npm test` if tests change.
This avoids modifying the tracked dependency files during development.

## Code structure

- `src/api.js`: HTTP requests, backend errors, and cancellation signals.
- `src/useApi.js`: single or parallel requests, loading/error state, and reload.
  It cancels obsolete requests when paths change or the component unmounts.
- `src/fields.js`: shared DTO accessors and list normalization.
- `src/matchups.js` and `src/components/MatchupViews.jsx`: shared player
  comparisons, opponent lists, team matrices, and season/date filters.
- `src/components/`: shared tables, navigation, loading, and error states.
- `src/pages/`: route components. `InningsDetail.jsx` serves both the batting and
  bowling ball-by-ball routes, retaining their separate statistics.
- `src/motion.jsx`: shared animation and scrolling hooks.
- `test/`: request and hook regression tests using Node's test runner and the
  React 18 test renderer.

## API routes used by the UI

| Area | Backend routes |
| --- | --- |
| Dashboard and records | `/stats/*` |
| Teams | `/teams`, `/teams/{id}`, `/teams/{id}/matches-played`, `/teams/{id}/wins` |
| Players | `/players`, `/players/search?name=`, `/players/{id}/batting`, `/players/{id}/bowling`, `/players/{id}/matches`, `/players/{id}/matches-count` |
| Matches | `/matches`, `/matches/{id}`, `/matches/team/{id}`, `/matches/format/{format}`, `/matches/headtohead/{team1}/{team2}` |
| Match details | `/matches/{id}/squad`, `/matches/{id}/batting-scorecard`, `/matches/{id}/bowling-scorecard`, `/matches/{id}/extras`, `/matches/{id}/deliveries` |
| Player deliveries | `/matches/{id}/deliveries/batter/{playerId}`, `/matches/{id}/deliveries/bowler/{playerId}` |
| Matchups | `/matchups/options`, `/matchups/players/{batterId}/{bowlerId}`, `/matchups/players/{playerId}`, `/matchups/players/{batterId}/{bowlerId}/matches`, `/matchups/teams` |
| Seasons | `/season`, `/season/{year}/leaderboard`, `/season/{year}/top-runs`, `/season/{year}/top-wickets`, `/season/{year}/top-strike-rates`, `/season/{year}/highest-scores`, `/season/{year}/best-bowling`, `/season/{year}/matches`, `/season/{year}/winner` |

Admin routes use `pages/Admin.jsx`, the form in `pages/adminCore.jsx`, and entity
configuration in `adminEntities.js`. Both `/admin` and `/admin/matches/:matchId`
use the same editor. Forms send IDs directly from API responses, and clearing an
optional field sends `null`. Batting and bowling scorecards are read-only: edit
Deliveries to update them.

The `/matchups` page offers player comparisons and team matrices. Player profiles
also show batting/bowling opponent lists, and team profiles include a matrix.
Season and inclusive match-date filters intersect; encounters always follow the
players across every team they represented. Team selection chooses matrix squad
members, with season controlling roster membership. Matchups are read-only and
need no SQL backfill. See [backend matchup documentation](../Backend/MATCHUPS.md)
for scoring definitions, filters, and roster rules. Team matrices load every
batter and bowler into one grid that scrolls in both directions, with fixed name
headers. Each populated cell shows `dismissals/runs (balls)`, then `Avg:` and
`SR:` together. Opponent lists and match/innings histories retain pagination.

Delivery changes and scorecard rebuilds run in one backend transaction, with a
per-match lock for concurrent writes. The calculator is tested separately from
persistence; integration tests cover rollback and delivery create/update/delete.
Scorecards are stored per match and player, so separate innings by the same
player are combined by the existing schema.

`VITE_API_BASE_URL` and the Docker setup's `VITE_API_URL` are both supported;
`VITE_API_BASE_URL` takes precedence. Set `ADMIN_PASSWORD` on the backend for
admin sign-in and write access. The browser verifies sign-in with `/admin/auth`
and sends the credential with every mutation. A tab retains its credential in
session storage until sign-out or tab closure. Public GET requests stay open;
writes fail when the backend password is missing. Remove the obsolete frontend
`VITE_ADMIN_PASSWORD` variable. `.env` files should remain local and untracked.

See [DEPLOYMENT.md](../DEPLOYMENT.md) for free Render hosting with Neon PostgreSQL
and automatic deployment from the `work` branch.


## Player role/style migration

Before starting this version on an existing database, stop the backend, back up
MySQL, and run `../Backend/database/player-enums-migration.sql`. See that folder's
README for old/new value mappings and preserved archive columns. Player JSON now
uses `playingStyle` (`Right`/`Left`) and a separate bowling-technique `bowlingStyle`.
Roles are `Batsman`, `Baller`, `Keeper`, and `Allrounder`. Use the regenerated
player-enrichment SQL with these enums; the earlier enrichment file is incompatible.
