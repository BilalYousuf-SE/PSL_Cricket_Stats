# Player roles and styles

The SQL files in this directory target MySQL. For a PostgreSQL deployment and a
copy of existing data, use [the PostgreSQL migration guide](postgresql/README.md).

Back up your database and stop the backend. Select the database in MySQL
Workbench and execute `player-enums-migration.sql`, then start the updated app.
Do not start the new app against unmigrated old enum data. MySQL DDL commits
implicitly, so keep the backup if you need to restore the previous schema.

The migration preserves all player IDs, cricsheet IDs, ages, nationalities,
names, and related tables. It is safe to rerun. It temporarily disables the
session's safe-update guard for the metadata conversion and restores its prior
value at the end; if execution stops on an error, reconnect before retrying.

| Old value | New role | New bowling style when unknown |
| --- | --- | --- |
| Batsman | Batsman | unchanged |
| FastBaller | Baller | FastBaller |
| OffSpinner | Baller | OffSpinner |
| LegSpinner | Baller | LegSpinner |
| WicketKeeper | Keeper | unchanged |
| AllRounder | Allrounder | unchanged |

API fields:

- `role`: `Batsman`, `Baller`, `Keeper`, or `Allrounder`.
- `playingStyle`: `Right`, `Left`, or `null`. This replaces `battingStyle` and
  retains the previous batting-hand meaning; it does not identify bowling arm.
- `bowlingStyle`: `FastBaller`, `FastMedium`, `MediumFast`, `MediumPacer`,
  `SlowMediumPacer`, `OffSpinner`, `LegSpinner`, `LeftArmOrthodox`,
  `LeftArmWristSpin`, or `null`.

Legacy `bowling_style` held a bowling arm (`Right`/`Left`), not a technique.
Those values are preserved in `legacy_bowling_arm`; a technique is never guessed
from arm. The old `batting_style` column also remains for reference, but the app
now uses `playing_style`. No archive column is read by the new API.

JSON clients must use the new names/values, including role filter URLs such as
`/players/role/Baller`. Human-readable labels in the admin form are separate
from the enum values sent to the API. The public player page and admin form
are updated together.

The earlier player-enrichment SQL uses the old enum values. Use the regenerated
version for this schema, after this migration. Do not apply the old enrichment
SQL after migrating.
