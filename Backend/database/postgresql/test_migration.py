"""Integration tests; require explicit PSL_TEST_* database connection settings."""

from datetime import date
from decimal import Decimal
import os
from pathlib import Path
import secrets
import unittest

import psycopg
from psycopg import sql
import pymysql

from migrate import TABLE_KEYS, migrate, mysql_identifier


@unittest.skipUnless(os.getenv("PSL_TEST_POSTGRES_HOST") and os.getenv("PSL_TEST_MYSQL_HOST"),
                     "Set explicit PSL_TEST_POSTGRES_HOST and PSL_TEST_MYSQL_HOST for isolated fixtures")
class MigrationIntegrationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pg_options = dict(host=os.environ["PSL_TEST_POSTGRES_HOST"],
                              port=int(os.getenv("PSL_TEST_POSTGRES_PORT", "5432")),
                              user=os.getenv("PSL_TEST_POSTGRES_USER", "postgres"),
                              password=os.environ["PSL_TEST_POSTGRES_PASSWORD"])
        cls.mysql_options = dict(host=os.environ["PSL_TEST_MYSQL_HOST"],
                                 port=int(os.getenv("PSL_TEST_MYSQL_PORT", "3306")),
                                 user=os.getenv("PSL_TEST_MYSQL_USER", "root"),
                                 password=os.environ["PSL_TEST_MYSQL_PASSWORD"], charset="utf8mb4")
        cls.pg_admin = psycopg.connect(**cls.pg_options, dbname="postgres", autocommit=True)
        cls.addClassCleanup(cls.pg_admin.close)
        cls.mysql_admin = pymysql.connect(**cls.mysql_options, autocommit=True)
        cls.addClassCleanup(cls.mysql_admin.close)

    def setUp(self):
        self.database = "psl_migration_test_" + secrets.token_hex(6)
        self.pg_admin.execute(sql.SQL("CREATE DATABASE {}").format(sql.Identifier(self.database)))
        self.addCleanup(lambda: self.pg_admin.execute(sql.SQL("DROP DATABASE {}").format(sql.Identifier(self.database))))
        self.destination = psycopg.connect(**self.pg_options, dbname=self.database)
        self.addCleanup(self.destination.close)
        with self.mysql_admin.cursor() as cursor:
            cursor.execute("CREATE DATABASE " + mysql_identifier(self.database) + " CHARACTER SET utf8mb4")
        self.addCleanup(self.drop_source)
        self.source = pymysql.connect(**self.mysql_options, database=self.database, autocommit=True)
        self.addCleanup(self.source.close)

        with self.destination.cursor() as cursor:
            cursor.execute(Path(__file__).with_name("schema.sql").read_text())
            cursor.execute("SELECT table_name, column_name, data_type, character_maximum_length, "
                           "numeric_precision, numeric_scale FROM information_schema.columns "
                           "WHERE table_schema = 'public' ORDER BY ordinal_position")
            definitions = {table: [] for table in TABLE_KEYS}
            for table, column, kind, length, precision, scale in cursor:
                if table not in definitions:
                    continue
                mysql_type = {"integer": "INT", "bigint": "BIGINT", "boolean": "BIT(1)", "date": "DATE"}.get(kind)
                if kind == "character varying":
                    mysql_type = f"VARCHAR({length})"
                elif kind == "numeric":
                    mysql_type = f"DECIMAL({precision},{scale})"
                self.assertIsNotNone(mysql_type, kind)
                definitions[table].append(mysql_identifier(column) + " " + mysql_type)
        self.destination.commit()
        with self.source.cursor() as cursor:
            for table, fields in definitions.items():
                fields.append("PRIMARY KEY (" + ", ".join(map(mysql_identifier, TABLE_KEYS[table])) + ")")
                cursor.execute("CREATE TABLE " + mysql_identifier(table) + " (" + ", ".join(fields) + ") ENGINE=InnoDB")
            cursor.execute("ALTER TABLE player ADD cricsheet_id VARCHAR(100)")
        self.seed()

    def drop_source(self):
        with self.mysql_admin.cursor() as cursor:
            cursor.execute("DROP DATABASE " + mysql_identifier(self.database))

    def insert(self, table, **values):
        with self.source.cursor() as cursor:
            cursor.execute("INSERT INTO " + mysql_identifier(table) + " ("
                           + ", ".join(map(mysql_identifier, values)) + ") VALUES ("
                           + ", ".join(["%s"] * len(values)) + ")", list(values.values()))

    def seed(self):
        self.insert("teams", team_id=10, team_name="O'Brien \\ Falcons – کراچی\nA")
        self.insert("teams", team_id=20, team_name="Stars")
        self.insert("season", season_id=30, season_year=2026)
        for player_id, role in [(101, "Batsman"), (102, "Baller"), (103, "Keeper")]:
            self.insert("player", id=player_id, first_name="Player", last_name=str(player_id),
                        full_name="Player " + str(player_id), nationality="Pakistan", role=role,
                        playing_style="Right", bowling_style="FastMedium" if role == "Baller" else None,
                        cricsheet_id="original-id-" + str(player_id))
        self.insert("matches", match_id=40, season_id=30, match_date=date(2026, 10, 1),
                    venue="Ground One", format="T20", team_a_id=10, team_b_id=20)
        self.insert("match_squads", match_id=40, player_id=101, team_id=10,
                    is_captain=0, is_keeper=0, is_playing=1)
        self.insert("match_squads", match_id=40, player_id=102, team_id=20,
                    is_captain=1, is_keeper=None, is_playing=1)
        self.insert("match_squads", match_id=40, player_id=103, team_id=10,
                    is_captain=0, is_keeper=1, is_playing=1)
        for i in range(30):
            self.insert("deliveries", delivery_id=1000 + i, match_id=40, batter_id=101,
                        non_striker_id=103, bowler_id=102, innings=1, over_num=i // 6, ball_num=i % 6 + 1,
                        runs_batter=6 if i < 3 else 4 if i == 3 else 1 if i < 27 else 0, runs_extras=0,
                        wicket_type="caught" if i in (14, 29) else None,
                        player_dismissed_id=101 if i in (14, 29) else None)
        self.insert("partnerships", partnership_id=70, match_id=40, batter1_id=101, batter2_id=103,
                    innings=1, wicket_number=1, runs=45, balls_faced=30)
        self.insert("scorecard_batters", match_id=40, player_id=101, runs=45, balls_faced=30,
                    fours=1, sixes=3, is_out=1, batting_position=1)
        self.insert("scorecard_bowlers", match_id=40, player_id=102, overs_bowled=Decimal("5.0"),
                    maidens=0, runs_conceded=45, wickets=2, economy=Decimal("9.00"))

    def count(self, table):
        with self.destination.cursor() as cursor:
            cursor.execute(sql.SQL("SELECT COUNT(*) FROM {}").format(sql.Identifier("public", table)))
            result = cursor.fetchone()[0]
        self.destination.commit()
        return result

    def test_copies_all_tables_and_preserves_types_relationships_and_legacy_columns(self):
        counts = migrate(self.source, self.destination)
        self.assertEqual(counts["deliveries"], 30)
        self.assertEqual(counts["partnerships"], 1)
        self.assertEqual(self.count("deliveries"), 30)
        with self.destination.cursor() as cursor:
            cursor.execute("SELECT team_name FROM teams WHERE team_id=10")
            self.assertEqual(cursor.fetchone()[0], "O'Brien \\ Falcons – کراچی\nA")
            cursor.execute("SELECT is_captain, is_keeper, is_playing FROM match_squads WHERE player_id=101")
            self.assertEqual(cursor.fetchone(), (False, False, True))
            cursor.execute("SELECT is_keeper FROM match_squads WHERE player_id=102")
            self.assertIsNone(cursor.fetchone()[0])
            cursor.execute("SELECT overs_bowled, economy FROM scorecard_bowlers")
            self.assertEqual(cursor.fetchone(), (Decimal("5.0"), Decimal("9.00")))
            cursor.execute("SELECT batter1_id, batter2_id, match_id FROM partnerships")
            self.assertEqual(cursor.fetchone(), (101, 103, 40))
            cursor.execute("SELECT values_json FROM mysql_migration_legacy WHERE table_name='player' AND source_key=%s::jsonb",
                           ('{"id":101}',))
            self.assertEqual(cursor.fetchone()[0], {"cricsheet_id": "original-id-101"})
        self.destination.commit()
        with self.source.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) FROM deliveries")
            self.assertEqual(cursor.fetchone()[0], 30)

    def test_identity_sequences_continue_after_imported_ids(self):
        migrate(self.source, self.destination)
        with self.destination.cursor() as cursor:
            cursor.execute("INSERT INTO teams(team_name) VALUES ('New Team') RETURNING team_id")
            self.assertEqual(cursor.fetchone()[0], 21)
            cursor.execute("INSERT INTO partnerships(match_id, batter1_id, batter2_id, innings) VALUES(40,101,103,2) RETURNING partnership_id")
            self.assertEqual(cursor.fetchone()[0], 71)

    def test_refuses_populated_destination_without_replacing_records(self):
        self.destination.execute("INSERT INTO teams(team_id,team_name) VALUES(999,'Keep me')")
        self.destination.commit()
        with self.assertRaisesRegex(ValueError, "not empty"):
            migrate(self.source, self.destination)
        self.assertEqual(self.count("teams"), 1)
        self.assertEqual(self.count("deliveries"), 0)

    def test_foreign_key_failure_rolls_back_every_copied_table(self):
        with self.source.cursor() as cursor:
            cursor.execute("UPDATE deliveries SET bowler_id=999 WHERE delivery_id=1000")
        with self.assertRaises(psycopg.errors.ForeignKeyViolation):
            migrate(self.source, self.destination)
        for table in TABLE_KEYS:
            self.assertEqual(self.count(table), 0)

    def test_invalid_mysql_boolean_rolls_back_the_import(self):
        with self.source.cursor() as cursor:
            cursor.execute("ALTER TABLE match_squads MODIFY is_keeper INT")
            cursor.execute("UPDATE match_squads SET is_keeper=2 WHERE player_id=101")
        with self.assertRaisesRegex(ValueError, "boolean"):
            migrate(self.source, self.destination)
        self.assertEqual(self.count("teams"), 0)

    def test_fresh_database_preview_does_not_create_schema_and_copy_can_follow(self):
        with self.destination.cursor() as cursor:
            cursor.execute(sql.SQL("DROP TABLE {} CASCADE").format(
                sql.SQL(", ").join(sql.Identifier("public", table) for table in TABLE_KEYS)))
        self.destination.commit()
        migrate(self.source, self.destination, check_only=True)
        with self.destination.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public'")
            self.assertEqual(cursor.fetchone()[0], 0)
        self.destination.commit()
        migrate(self.source, self.destination)
        self.assertEqual(self.count("deliveries"), 30)

    def test_streams_a_full_history_sized_copy(self):
        with self.source.cursor() as cursor:
            for start in range(30, 73784, 2000):
                cursor.executemany("INSERT INTO deliveries(delivery_id,match_id,batter_id,bowler_id,non_striker_id,innings,runs_batter,runs_extras) "
                                   "VALUES(%s,%s,%s,%s,%s,%s,%s,%s)",
                                   [(1000+i,40,101,102,103,1,1,0) for i in range(start, min(start+2000,73784))])
        counts = migrate(self.source, self.destination)
        self.assertEqual(counts["deliveries"], 73784)
        self.assertEqual(self.count("deliveries"), 73784)


if __name__ == "__main__":
    unittest.main()
