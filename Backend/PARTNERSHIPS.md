# Partnership APIs

Partnerships are stored records. These endpoints do not calculate stands or
populate the table from deliveries. Historical backfilling through SQL is a
separate step; delivery edits currently rebuild scorecards only.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/matches/{matchId}/partnerships` | All stands, ordered by innings, wicket number, then ID |
| GET | `/matches/{matchId}/partnerships?innings=1` | Stands in one innings |
| GET | `/partnerships/{id}` | One record with batter names and match context |
| GET | `/partnerships` | Paginated history with optional filters |
| POST | `/matches/{matchId}/partnerships` | Create a record; returns 201 |
| PUT | `/matches/{matchId}/partnerships/{id}` | Replace the record's editable fields |
| DELETE | `/matches/{matchId}/partnerships/{id}` | Delete a record; returns 204 |

History filters can be combined: `matchId`, `innings`, `seasonYear`, `playerId`,
and `partnerId`. A player matches either batter column; a pair matches both
orientations. `partnerId` requires a different `playerId`.

History returns a Spring page with `content`, `totalElements`, `totalPages`,
`number`, and `size`. Pages are zero-based; size defaults to 50 and is limited
to 200. Results are ordered by match date descending, match ID descending,
innings, wicket number, and partnership ID.

For example, `/partnerships?playerId=12&partnerId=34&seasonYear=2026&page=0&size=20`.

POST and PUT accept the same complete body:

```json
{
  "innings": 1,
  "batter1Id": 12,
  "batter2Id": 34,
  "runs": 45,
  "ballsFaced": 30,
  "wicketNumber": 1
}
```

All fields are required. Runs include extras; balls count legal deliveries in
the stand. Wicket number is 1 for the opening stand, through 10. Both batters
must be distinct players in the match squad, playing for the same match team.
Updates and deletes cannot address a record belonging to another match.

The response includes IDs and names for both batters, match ID/date, season
year, venue, innings, runs, balls, and wicket number. Missing records/matches
return 404; invalid bodies, filters, or squad membership return 400. Existing
matches with no partnerships return an empty list.

## Schema and backfill

The entity maps a nullable integer `partnerships.innings` column. The repeatable
`database/partnership-schema.sql` file adds it and a match/innings/wicket index;
it also creates the partnership table if missing and enables `AUTO_INCREMENT`
on an existing `partnership_id` column while preserving its integer type,
signedness, and existing IDs. Existing rows keep
`innings = NULL` until backfilled. They remain visible in unfiltered reads and
are excluded from a specific innings filter. New API writes require an explicit
positive innings number.

Wicket number and the batter pair are not a unique key: retirements can produce
multiple stands for a wicket, including resumed pairs. Records retain separate
partnership IDs. The SQL backfill must segment deliveries chronologically and
avoid merging these stands.

## Update and populate MySQL

Requires MySQL 8.0+ and a database user allowed to alter tables, create routines,
create temporary tables, and modify partnerships. Run from the repository root.
Stop the backend and any delivery imports until the update and checks finish.
No database changes are performed merely by committing these files.

Back up the database, then open a fresh MySQL session (replace `root` and `psl`
if your connection uses different values):

```sh
mysqldump -u root -p --single-transaction --no-tablespaces --result-file=psl-before-partnerships.sql psl
mysql -u root -p --default-character-set=utf8mb4 psl
```

In the MySQL prompt, run each step separately. If any command reports an
`ERROR`, stop and resolve it before continuing to apply mode:

```sql
SOURCE Backend/database/partnership-schema.sql;

-- Preview all matches; this does not change partnership rows.
SET @partnership_match_id = NULL;
SET @partnership_apply = 0;
SOURCE Backend/database/partnership-backfill.sql;

-- Apply after inspecting the preview.
SET @partnership_apply = 1;
SOURCE Backend/database/partnership-backfill.sql;

-- First two result sets should be empty for backfilled matches.
SOURCE Backend/database/partnership-verify.sql;
```

For a single match, set `@partnership_match_id = 123` instead of NULL before
preview/apply/checks. Reset it to NULL to process all matches. Each successful
backfill invocation resets apply mode to 0. Restart the backend after checking.

Apply mode replaces **all existing partnership rows for the selected matches
with deliveries**, including manual edits. It regenerates partnership IDs.
Matches without deliveries retain their existing records. Repeated runs produce
the same stands/totals without duplicates, but new IDs. Data replacement uses
an InnoDB transaction and rolls back on failure; schema DDL commits separately.
Restore the backup if you need to undo a committed replacement.

The calculation sorts deliveries by innings, over, ball, and delivery ID. When
illegal deliveries share a ball label, delivery IDs must reflect their original
order within that label. Strike changes do not split stands. A pair change or
a retirement/dismissal ends a stand; retired hurt/not out does not count as a
wicket. Runs include extras; wides/no-balls do not count as legal balls. Batter
columns are stored by ascending player ID, without implying batting position.

The backfill stops without changing partnership rows if source data is missing
either batter, contains inconsistent squads/innings/dismissals, duplicates a
legal ball label, or continues batting immediately after a recorded dismissal.
Fix the reported source rows rather than filling missing batters from the toss.
An innings with deliveries after ten dismissals is rejected. Wicket types must
use the recognized cricket dismissal names listed in the script.

Verification compares each innings' runs and legal balls with deliveries, and
reports malformed/orphan records. It also lists preserved matches without
deliveries; those may still have unknown innings and need separate review.
Delivery corrections made later require another backfill for that match.

If an older table produces `ERROR 1364: Field 'partnership_id' doesn't have a
default value`, rerun the latest schema file before applying the backfill. An
older schema file may have added innings without enabling ID generation on an
existing table. After updating, `SHOW COLUMNS FROM partnerships LIKE
'partnership_id';` should include `auto_increment` in `Extra`. The backfill
also checks this prerequisite before calculating stands.

## Verification

Run the database-independent checks with:

```sh
bash mvnw -Dtest=PartnershipServiceTest,PartnershipControllerTest,RepositoryQueriesTest,ScorecardCalculatorTest,PlayerMapperTest test
```

These cover API validation, write rules, filters, mapping, and Hibernate JPQL
validation. Executing the queries against MySQL needs a separate integration
run with a configured test database. Tests use Mockito's subclass mock maker
so they do not require JVM agent attachment.

The full backend suite includes `PartnershipIntegrationTest`, which verifies
role ordering and partnership CRUD/filter queries against the configured MySQL
test database. Run `bash mvnw verify` with the datasource variables pointing
to a disposable database.

The standalone SQL regression test must also use its own **empty** database:

```sh
mysql -u root -p -e "CREATE DATABASE psl_partnership_test_local"
mysql -u root -p psl_partnership_test_local
```

At that MySQL prompt, run
`SOURCE Backend/database/partnership-fixture-test.sql;` from the repository
root. It checks creation/migration (including legacy ID generation), preview,
repeat runs, single-match scope, extras, run-outs, retirement/resumed stands, and preserved matches without
deliveries. It ends with `Partnership SQL fixture checks passed`. The fixture
database is left in place for inspection; do not use it as your application
database.
