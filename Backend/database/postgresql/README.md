# Copy local MySQL to Neon PostgreSQL

This is a one-time copy into a **fresh, empty** PostgreSQL database. Local MySQL
remains the source and is not modified. Finish the existing player-enum and
partnership MySQL migrations first if they have not already been applied.

The tool copies `teams`, `season`, `player`, `matches`, `match_squads`, `deliveries`,
`partnerships`, `scorecard_batters`, and `scorecard_bowlers`. It creates the PostgreSQL
schema from `schema.sql` when needed. Existing application rows cause it to stop;
it never drops or replaces populated tables.

Use a current local backup and pause local admin edits during the copy so the
hosted snapshot includes the changes you intend to keep. The source transaction
is read-only and uses a consistent InnoDB snapshot. Destination rows and schema
creation commit together only after every table count matches. Failed copies
roll back; no partial imported rows are committed.

## Windows PowerShell commands

Run from the project root, where your current `.env` and `docker-compose.yml` live.
Docker handles the clients and Python dependencies; no Windows Python or database
client installation is needed. These commands require the prepared migration
files to be present in your checkout.

First take a backup using the existing MySQL container:

```powershell
docker compose up -d mysql
docker compose exec mysql mysqldump -u root -p --single-transaction --no-tablespaces --set-gtid-purged=OFF --result-file=/tmp/psl-before-postgres.sql psl
docker compose cp mysql:/tmp/psl-before-postgres.sql .\psl-before-postgres.sql
```

Enter your **local MySQL root password** when prompted. Keep the backup locally.

Create a file named `.env.postgres` beside `.env` and fill it with your **Neon**
connection values. Choose the direct hostname, without `https://` or `-pooler`:

```dotenv
PGHOST=your-direct-host.neon.tech
PGPORT=5432
PGDATABASE=neondb
PGUSER=your-neon-username
PGPASSWORD='your-neon-password'
```

Use the database name shown in Neon. This file and SQL backups are Git-ignored.
The existing `.env` continues to hold the local MySQL password; the two password
settings have different names. Enter real credentials only in these local files
and the provider dashboards.

You can first check the connections, source table counts, and destination:

```powershell
docker compose --env-file .env --env-file .env.postgres -f docker-compose.yml -f compose.migration.yml run --rm --build migrate-postgres --check
```

Run the copy:

```powershell
docker compose --env-file .env --env-file .env.postgres -f docker-compose.yml -f compose.migration.yml run --rm --build migrate-postgres
```

Wait for `Migration committed` and a verified count for each of the nine tables.
If any step fails, read the error and correct it before retrying. A completed copy
cannot be rerun against the same populated database; create another empty Neon
database for a fresh copy. The script prints database errors with passwords
redacted.

In Neon's SQL editor, verify:

```sql
SELECT COUNT(*) AS delivery_count FROM public.deliveries;
SELECT COUNT(*) AS partnership_count FROM public.partnerships;
```

Compare these counts to local MySQL. Set the Render backend's PostgreSQL variables
as described in [DEPLOYMENT.md](../../../DEPLOYMENT.md), then deploy the prepared
code when it has been pushed. Your public UI and admin APIs keep the same routes.

## What is preserved

- Explicit IDs and foreign-key relationships, including match squads and batting
  partners. Identity sequences are advanced so later creates do not reuse IDs.
- NULL values, dates, Unicode names, decimal bowling figures and MySQL BIT/TINYINT
  booleans. A boolean other than 0, 1 or NULL stops the copy.
- Source columns outside the JPA schema, such as archived player styles or
  enrichment IDs. They are stored per source record in
  `public.mysql_migration_legacy(table_name, source_key, values_json)` as JSONB.
  Decimal, binary and date/time archive values have an explicit type tag.

Other source tables outside the nine listed application tables are not copied.
MySQL SQL dumps, stored procedures and backfill scripts are not PostgreSQL SQL.
This tool copies the existing results; it does not execute those MySQL scripts on
Neon or rebuild partnerships.

`schema.sql` matches the current JPA mappings. Update it when those mappings
change. The app's existing `ddl-auto=update` setting remains enabled.
