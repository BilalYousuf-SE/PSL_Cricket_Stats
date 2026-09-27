# CoverDrive — what changed

## 1. The Matches bug

`MatchesList.jsx` (and every other page) parsed list responses with
`Array.isArray(x) ? x : []`. That's correct only if the endpoint always
returns a bare JSON array. If your Spring Boot side returns a `Page<T>`
for any of `/matches`, `/matches/format/{format}`, or `/matches/team/{team}`
(very common with Spring Data pagination), the JSON body looks like:

```json
{ "content": [ ... ], "totalPages": 1, "totalElements": 12, ... }
```

`Array.isArray()` on that is `false`, so `matches` silently became `[]` and
the table rendered the empty state — no error, nothing in the console,
just "not visible."

**Fix:** added `toArray()` to `fields.js`, which accepts a raw array or
unwraps `.content` / `.data` / `.items`, and swapped it in everywhere a page
was doing the old `Array.isArray(x) ? x : []` dance (Matches, Players,
Teams, Head-to-Head, Records, Season Stats, Team/Player detail, both
innings-detail pages, and the Navbar's season dropdown).

If matches are still missing after this, it's not that bug — check the
Network tab for the actual response shape/status code on `/matches`, and
the browser console for a CORS or fetch error. Happy to dig in further with
that info.

**Not touched:** `MatchDetail.jsx` wasn't included in what you shared, so
I couldn't check or update it. If it does its own array handling, send it
over and I'll apply the same fix + the styling pass below to it.

## 2. The redesign — "CoverDrive"

New name, new palette, same bones. Every existing class name and component
API is unchanged, so this is a drop-in replacement — no other code needs to
change to pick it up.

- **Palette:** swapped the dark-pitch-green/amber scheme for a night-match
  look — deep navy sky (`--midnight`), warm ivory scorecards (`--paper`,
  unchanged concept), floodlight gold (`--gold`) and an electric teal
  accent (`--teal`), cricket-ball red for losses (`--ball-red`). Old
  variable names (`--pitch`, `--amber`, `--leather`, etc.) are kept as
  aliases in `index.css`, so `motion.css` didn't need any color edits.
- **Brand:** "Scorecard." → "CoverDrive.", with an animated gold→teal
  gradient wordmark in the navbar.
- **Motion, upgraded:**
  - Scroll-reveal (`.reveal`) now eases in with a blur + scale, not just a
    fade — feels like content coming into focus rather than sliding up.
  - Every scorecard panel now has a slow-sweeping gold→teal accent line
    across its top edge.
  - Table rows get a colored left-edge accent on hover instead of a flat
    background tint.
  - Team/player cards lift and glow on hover; ball chips pop on hover.
  - Nav/tab underlines now glow subtly instead of being a flat bar.
  - All of this respects `prefers-reduced-motion` (same as before, extended
    to the new effects).
- **Small UI additions:** sticky/glassy navbar, a "live from the scorebook"
  hero kicker on the Dashboard, a redesigned scrollbar, refreshed focus
  states and skeleton-loader shimmer to match the new palette.
- **No new dependencies.** Everything above is CSS plus the one JS helper
  (`toArray`) — nothing to install.

## Files in this bundle

Everything under `src/` mirrors your original structure. Files with real
logic changes: `fields.js`, `MatchesList.jsx`, `PlayersList.jsx`,
`TeamsList.jsx`, `HeadToHead.jsx`, `Records.jsx`, `SeasonStats.jsx`,
`TeamDetail.jsx`, `PlayerDetail.jsx`, `BattingInningsDetail.jsx`,
`BowlingInningsDetail.jsx`, `Navbar.jsx`, `Dashboard.jsx`, `App.jsx`.
Files included unchanged for completeness: `api.js`, `useApi.js`,
`main.jsx`, `motion.jsx`, `components/DataTable.jsx`,
`components/Loading.jsx`. `components/ErrorState.jsx` got one color
tweak (retry button border/text to match the new palette).
`index.css` and `motion.css` are full rewrites of the visual layer.
