"""Copy the PSL application's MySQL data into an empty PostgreSQL database.

Source reads use a consistent, read-only snapshot. Destination writes, schema
creation and count verification share one transaction. Existing data is refused.
"""

import argparse
import base64
from datetime import date, datetime, time
from decimal import Decimal
import os
from pathlib import Path
import sys

import psycopg
from psycopg import sql
from psycopg.types.json import Jsonb
import pymysql


# Parents precede their foreign-key dependants. Composite keys have no sequence.
TABLE_KEYS = {
    "teams": ("team_id",),
    "season": ("season_id",),
    "player": ("id",),
    "matches": ("match_id",),
    "match_squads": ("match_id", "player_id"),
    "deliveries": ("delivery_id",),
    "partnerships": ("partnership_id",),
    "scorecard_batters": ("match_id", "player_id"),
    "scorecard_bowlers": ("match_id", "player_id"),
}


def mysql_identifier(value):
    return "`" + value.replace("`", "``") + "`"


def table_identifier(table):
    return sql.Identifier("public", table)


def legacy_json(value):
    """Keep archive-column types that JSON does not represent natively."""
    if isinstance(value, (bytes, bytearray)):
        return {"type": "bytes", "base64": base64.b64encode(value).decode("ascii")}
    if isinstance(value, Decimal):
        return {"type": "decimal", "value": str(value)}
    if isinstance(value, (date, datetime, time)):
        return {"type": type(value).__name__, "value": value.isoformat()}
    return value


def postgres_boolean(value):
    if value is None:
        return None
    if isinstance(value, (bytes, bytearray)):
        value = int.from_bytes(value, "big")
    if value not in (0, 1):
        raise ValueError("MySQL boolean must be 0, 1, or NULL; encountered an invalid source value")
    return bool(value)


def source_columns(source):
    with source.cursor() as cursor:
        cursor.execute("SELECT table_name, column_name FROM information_schema.columns "
                       "WHERE table_schema = DATABASE() ORDER BY ordinal_position")
        result = {table: [] for table in TABLE_KEYS}
        for table, column in cursor:
            if table in result:
                result[table].append(column)
        missing = [table for table, columns in result.items() if not columns]
        if missing:
            raise ValueError("Source tables missing: " + ", ".join(missing))
        cursor.execute("SELECT table_name, engine FROM information_schema.tables WHERE table_schema = DATABASE()")
        unsupported = [table for table, engine in cursor if table in TABLE_KEYS and engine != "InnoDB"]
        if unsupported:
            raise ValueError("A consistent snapshot requires InnoDB source tables: " + ", ".join(unsupported))
    return result


def destination_columns(cursor):
    cursor.execute("SELECT table_name, column_name, data_type FROM information_schema.columns "
                   "WHERE table_schema = 'public' ORDER BY ordinal_position")
    result = {table: [] for table in TABLE_KEYS}
    for table, column, kind in cursor:
        if table in result:
            result[table].append((column, kind))
    return result


def source_count(source, table):
    with source.cursor() as cursor:
        cursor.execute("SELECT COUNT(*) FROM " + mysql_identifier(table))
        return cursor.fetchone()[0]


def migrate(source, destination, check_only=False):
    with source.cursor() as cursor:
        cursor.execute("SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ")
        cursor.execute("START TRANSACTION WITH CONSISTENT SNAPSHOT, READ ONLY")
    try:
        columns = source_columns(source)
        counts = {table: source_count(source, table) for table in TABLE_KEYS}
        with destination.transaction(), destination.cursor() as target:
            target.execute("SET LOCAL lock_timeout = '5s'")
            target.execute("SET LOCAL statement_timeout = '120s'")
            target_columns = destination_columns(target)
            present = [table for table, fields in target_columns.items() if fields]
            if present and len(present) != len(TABLE_KEYS):
                raise ValueError("Destination has a partial application schema; use a fresh database")
            if not present:
                if check_only:
                    print("Destination schema will be created from schema.sql.")
                else:
                    target.execute(Path(__file__).with_name("schema.sql").read_text(encoding="utf-8"))
                    target_columns = destination_columns(target)

            if present or not check_only:
                # Serialize migrations and prevent concurrent admin writes during import.
                target.execute(sql.SQL("LOCK TABLE {} IN ACCESS EXCLUSIVE MODE").format(
                    sql.SQL(", ").join(table_identifier(table) for table in TABLE_KEYS)))
                for table in TABLE_KEYS:
                    target.execute(sql.SQL("SELECT COUNT(*) FROM {}").format(table_identifier(table)))
                    if target.fetchone()[0]:
                        raise ValueError("Destination table " + table + " is not empty; no rows were replaced")
                    missing = set(field for field, _ in target_columns[table]) - set(columns[table])
                    if missing:
                        raise ValueError("Source " + table + " is missing columns: " + ", ".join(sorted(missing))
                                         + ". Apply the existing MySQL schema migrations first.")

            if check_only:
                for table, count in counts.items():
                    print(f"{table}: {count} source rows")
                print("Check complete. No source or destination rows changed.")
                return counts

            for table, fields in target_columns.items():
                names = [name for name, _ in fields]
                boolean_positions = [i for i, (_, kind) in enumerate(fields) if kind == "boolean"]
                select = "SELECT " + ", ".join(map(mysql_identifier, names)) + " FROM " + mysql_identifier(table)
                copied = 0
                with source.cursor(pymysql.cursors.SSCursor) as rows, destination.cursor() as writer:
                    rows.execute(select)
                    with writer.copy(sql.SQL("COPY {} ({}) FROM STDIN").format(
                            table_identifier(table), sql.SQL(", ").join(map(sql.Identifier, names)))) as copy:
                        for values in rows:
                            values = list(values)
                            for position in boolean_positions:
                                values[position] = postgres_boolean(values[position])
                            copy.write_row(values)
                            copied += 1
                target.execute(sql.SQL("SELECT COUNT(*) FROM {}").format(table_identifier(table)))
                if copied != counts[table] or target.fetchone()[0] != counts[table]:
                    raise ValueError("Row count verification failed for " + table)
                extra = set(columns[table]) - set(names)
                if extra:
                    archive_columns(source, target, table, sorted(extra))
                print(f"{table}: {copied} rows copied and verified", flush=True)

            for table, keys in TABLE_KEYS.items():
                if len(keys) == 1:
                    target.execute(sql.SQL("SELECT setval(pg_get_serial_sequence(%s, %s), "
                                           "GREATEST(COALESCE(MAX({key}), 1), 1), MAX({key}) IS NOT NULL) FROM {table}")
                                   .format(key=sql.Identifier(keys[0]), table=table_identifier(table)),
                                   ("public." + table, keys[0]))
        print("Migration committed. All nine table counts match; identity sequences are ready for new records.")
        return counts
    finally:
        source.rollback()


def archive_columns(source, target, table, extras):
    """Keep legacy/enrichment columns that are not part of the JPA schema."""
    target.execute("CREATE TABLE IF NOT EXISTS public.mysql_migration_legacy ("
                   "table_name text NOT NULL, source_key jsonb NOT NULL, values_json jsonb NOT NULL, "
                   "PRIMARY KEY (table_name, source_key))")
    keys = TABLE_KEYS[table]
    selected = list(keys) + extras
    statement = "SELECT " + ", ".join(map(mysql_identifier, selected)) + " FROM " + mysql_identifier(table)
    with source.cursor(pymysql.cursors.SSCursor) as rows:
        rows.execute(statement)
        for values in rows:
            source_key = dict(zip(keys, values[:len(keys)]))
            archive = {name: legacy_json(value) for name, value in zip(extras, values[len(keys):])}
            target.execute("INSERT INTO public.mysql_migration_legacy VALUES (%s, %s, %s)",
                           (table, Jsonb(source_key), Jsonb(archive)))
    print(table + ": preserved extra columns in mysql_migration_legacy: " + ", ".join(extras), flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Validate connections and source counts without copying")
    args = parser.parse_args()
    source = pymysql.connect(host=os.environ["MYSQL_HOST"], port=int(os.getenv("MYSQL_PORT", "3306")),
                             database=os.environ["MYSQL_DATABASE"], user=os.environ["MYSQL_USER"],
                             password=os.environ["MYSQL_PASSWORD"], charset="utf8mb4", autocommit=True)
    try:
        # libpq reads PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD and TLS settings.
        with psycopg.connect() as destination:
            migrate(source, destination, args.check)
    finally:
        source.close()


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        message = str(error)
        for key in ("MYSQL_PASSWORD", "PGPASSWORD"):
            password = os.getenv(key)
            if password:
                message = message.replace(password, "[redacted]")
        print(f"Migration failed ({type(error).__name__}): {message}", file=sys.stderr)
        sys.exit(1)
