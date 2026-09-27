# Cricket Stats UI

A React + Vite frontend for your Spring Boot cricket stats API, matching the real
DTOs/controllers (BattingStatsDTO, BowlingStatsDTO, DeliveryDTO, MatchDTO,
MatchSquadDTO, PlayerDTO, PlayerOfMatchDTO, RunScorerDTO, ScorecardBatterDTO,
ScorecardBowlerDTO, SeasonDTO, TeamDTO, TeamLeaderboardDTO, WicketTakerDTO).

## Run it

```bash
npm install
npm run dev
```

Opens at `http://localhost:5173`. It talks to your backend at `http://localhost:8080`
by default. To point it elsewhere, create a `.env` file:

```
VITE_API_BASE_URL=http://localhost:9000
```

**CORS:** since the UI and API run on different ports, make sure your Spring Boot app
allows cross-origin requests from `http://localhost:5173` (`@CrossOrigin` or a global
`WebMvcConfigurer` CORS mapping).

## Structure

- `src/api.js` — single fetch wrapper all pages go through (base URL, error handling)
- `src/useApi.js` — hook that fetches one or several endpoints in parallel and gives
  back `{ data, loading, error, reload }`, so every page gets consistent loading/error
  states without repeating boilerplate
- `src/fields.js` — accessors matched to your real DTOs, plus two derived helpers:
  `opponentOf(match, teamId)` (MatchDTO only stores teamA/teamB, not "opponent") and
  `resultText(match)` (built from `winnerName` + `resultMargin` + `resultType`)
- `src/components/` — shared UI: `Navbar`, `Loading`, `ErrorState`/`EmptyState`,
  `DataTable` (used by every table in the app)
- `src/pages/` — one file per route, matching the spec's routing table

## Design

Pitch-green chrome with scoreboard-amber numerals (nav, headings, hero stats), warm
paper-cream scorecard panels for actual data tables. "Big Shoulders Display" for scores
and headlines, "IBM Plex Sans" for body/table text with tabular figures. Tokens live at
the top of `src/index.css`.

## How the pages map to your controllers

- **Dashboard** — `/stats/team-leaderboard` (`TeamLeaderboardDTO`: teamId/teamName/wins
  only, no matches-played or points), `/stats/top-run-scorers`, `/stats/top-wicket-takers`,
  `/stats/player-of-match-awards`.
- **Teams / Team Detail** — `/teams`, `/teams/{id}`, `/teams/{id}/matches-played` and
  `/teams/{id}/wins` (both plain `Long`s, not wrapped objects), `/matches/team/{id}`
  for history. `TeamController` also exposes `/teams/season/{year}` and
  `/teams/leaderboard` (duplicate of the stats one) if you want to use them later.
- **Head-to-Head** — `/matches/headtohead/{team1}/{team2}`.
- **Players / Player Detail** — `/players`, `/players/search?name=` (query param is
  `name`, not `q`), `/players/role/{role}`, `/players/team/{teamId}`,
  `/players/{id}/batting` (`BattingStatsDTO`), `/players/{id}/bowling`
  (`BowlingStatsDTO`), `/players/{id}/matches`.
  **Important:** `/players/{id}/matches` returns a plain count, not a match list — there's
  currently no endpoint for a player's individual match history, so Player Detail shows
  the count only. Also note `PlayerDTO` has no team field (players aren't tied to one
  team), so player cards show role/nationality instead.
  Batting/bowling average, strike rate, and economy aren't returned by the stats DTOs
  (no balls-faced-derived average or overs-bowled economy at the career level) — strike
  rate and a runs/innings average are computed client-side in `PlayerDetail.jsx`;
  economy is left out since overs bowled isn't available at that granularity.
- **Matches / Match Detail** — `/matches`, `/matches/format/{format}`,
  `/matches/team/{teamId}` (there's also `/matches/team/{teamId}/wins` if you want a
  "wins only" filter later), `/matches/{id}/squad`, `/matches/{id}/batting-scorecard`,
  `/matches/{id}/bowling-scorecard`, `/matches/{id}/deliveries`.
  Format and role filter dropdowns (Matches, Players) derive their option list from
  values actually seen in the data rather than a hardcoded enum, since `MatchFormat`
  and `Roles` enum constant names weren't in the DTOs you shared — if a filter's list
  looks off, check `MatchFormat`/`Roles` in `com.cricket.stats.entity` and hardcode
  the real constants in `MatchesList.jsx` / `PlayersList.jsx` instead if you'd rather
  not do the extra `/matches` or `/players` fetch just to sniff values.
  Deliveries are grouped by `innings` first, then `overNum`, since over numbers restart
  each innings.
- **Records / Season Stats** — `/stats/highest-scores` and `/stats/best-bowling` return
  `ScorecardBatterDTO`/`ScorecardBowlerDTO` (same shape as a match scorecard row), which
  has no opponent field — rows link through to the match itself via `matchId` instead.
  The season picker uses the real `/season` endpoint (`SeasonDTO`: seasonId/year).
  The Season Stats page (`/records/season/:year`) pulls from the newer `SeasonController`
  endpoints: `/season/{year}/leaderboard`, `/season/{year}/top-runs`,
  `/season/{year}/top-wickets`, `/season/{year}/Top-Strike-Rates` (note the exact
  casing — Spring path matching is case-sensitive), `/season/{year}/highest-scores`,
  `/season/{year}/best-bowling`, and `/season/{year}/matches`. `/season/{year}/winner`
  is fetched separately from the rest, since an in-progress or undecided season will
  likely 404 there and that shouldn't take down the whole page.
  **Known backend gap:** as of the code shared so far, `/season/{year}/highest-scores`
  and `/season/{year}/best-bowling` call the same service methods as the global
  `/stats/highest-scores` / `/stats/best-bowling` — the `{year}` isn't actually used to
  filter, so those two panels may show all-time records rather than season-specific ones
  until that's filtered server-side.
- **Match Detail — ball-by-ball per innings** — each batter/bowler row in the Scorecard
  tab links to `/matches/:id/batting/:playerId` or `/matches/:id/bowling/:playerId`,
  backed by `/matches/{id}/batting-scorecard/{playerId}` and
  `/matches/{id}/bowling-scorecard/{playerId}` (both return `List<DeliveryDTO>` — every
  ball that player faced/bowled in that innings).
- **Player Detail — match history** — `/players/{id}/matches` now returns
  `List<PlayerMatchDTO>`, grouped in the UI by season, then by the team the player
  represented that season (handles both playing for one team across seasons and
  switching teams within a season). The "Matches played" stat number comes from the
  separate `/players/{id}/matches-count`.

As you add more APIs, drop new pages into `src/pages/`, add a `<Route>` in `src/App.jsx`,
and reuse `useApi` + `DataTable` to keep things consistent.
