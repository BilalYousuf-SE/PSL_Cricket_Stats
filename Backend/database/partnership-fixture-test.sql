-- Regression test only. Select a NEW EMPTY database named psl_partnership_test_*
-- and run from the repository root. Never run against your PSL database.
-- This script leaves the disposable database in place for inspection.
DELIMITER $$
CREATE PROCEDURE psl_test_assert(IN passed BOOLEAN, IN failure_message VARCHAR(128))
BEGIN
    IF passed IS NULL OR passed = FALSE THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = failure_message;
    END IF;
END$$
DELIMITER ;
CALL psl_test_assert(DATABASE() LIKE 'psl\\_partnership\\_test\\_%', 'Select a disposable psl_partnership_test_* database.');
CALL psl_test_assert((SELECT COUNT(*) = 0 FROM information_schema.tables
    WHERE table_schema = DATABASE()), 'Test database must be empty.');

CREATE TABLE player (id BIGINT PRIMARY KEY) ENGINE = InnoDB;
CREATE TABLE matches (match_id INT PRIMARY KEY, team_a_id INT, team_b_id INT) ENGINE = InnoDB;
CREATE TABLE match_squads (match_id INT, player_id BIGINT, team_id INT, is_playing BOOLEAN,
    PRIMARY KEY (match_id, player_id)) ENGINE = InnoDB;
CREATE TABLE deliveries (
    delivery_id BIGINT PRIMARY KEY, match_id INT, innings INT, over_num INT, ball_num INT,
    batter_id BIGINT, non_striker_id BIGINT, runs_batter INT, runs_extras INT,
    extra_type VARCHAR(50), wicket_type VARCHAR(50), player_dismissed_id BIGINT
) ENGINE = InnoDB;
INSERT INTO player VALUES (1), (2), (3), (4), (5), (6), (7);
INSERT INTO matches VALUES (1, 10, 20), (2, 10, 20), (3, 10, 20);
INSERT INTO match_squads VALUES
    (1, 1, 10, 1), (1, 2, 10, 1), (1, 3, 10, 1), (1, 7, 10, 1),
    (1, 4, 20, 1), (1, 5, 20, 1), (1, 6, 20, 1), (2, 1, 10, 1), (2, 2, 10, 1);
INSERT INTO deliveries VALUES
    (1, 1, 1, 0, 1, 1, 2, 1, 0, NULL, NULL, NULL),
    (2, 1, 1, 0, 2, 2, 1, 0, 1, 'WIDE', NULL, NULL),
    (3, 1, 1, 0, 2, 2, 1, 4, 0, NULL, NULL, NULL),
    (4, 1, 1, 0, 3, 2, 1, 0, 0, NULL, 'caught', 2),
    (5, 1, 1, 0, 4, 3, 1, 2, 0, NULL, NULL, NULL),
    (6, 1, 1, 0, 5, 1, 3, 0, 1, 'NO_BALL', NULL, NULL),
    (7, 1, 1, 0, 5, 1, 3, 0, 0, NULL, 'run out', 3),
    (8, 1, 1, 0, 6, 7, 1, 6, 0, NULL, NULL, NULL),
    (9, 1, 2, 0, 1, 4, 5, 4, 0, NULL, NULL, NULL),
    (10, 1, 2, 0, 2, 4, 5, 0, 0, NULL, 'retired hurt', 4),
    (11, 1, 2, 0, 3, 5, 6, 2, 0, NULL, NULL, NULL),
    (12, 1, 2, 0, 4, 5, 6, 0, 0, NULL, 'retired_hurt', 6),
    (13, 1, 2, 0, 5, 4, 5, 1, 0, NULL, NULL, NULL),
    (14, 1, 2, 0, 6, 5, 4, 0, 0, NULL, 'bowled', 5),
    (15, 2, 1, 0, 1, 1, 2, 3, 0, NULL, NULL, NULL);

SOURCE Backend/database/partnership-schema.sql;
-- Exercise migration of a pre-existing table without an innings column too.
CREATE INDEX idx_fixture_match ON partnerships (match_id);
ALTER TABLE partnerships DROP INDEX idx_partnership_match_innings;
ALTER TABLE partnerships DROP COLUMN innings;
INSERT INTO partnerships (partnership_id, match_id, batter1_id, batter2_id, runs, balls_faced, wicket_number)
VALUES (900, 1, 1, 2, 999, 99, 1);
SOURCE Backend/database/partnership-schema.sql;
SOURCE Backend/database/partnership-schema.sql;
INSERT INTO partnerships (partnership_id, match_id, innings, batter1_id, batter2_id, runs, balls_faced, wicket_number)
VALUES (901, 3, 1, 1, 2, 99, 20, 1), (902, 2, 1, 1, 2, 99, 20, 1);

SET SQL_SAFE_UPDATES = 1;
SET @partnership_match_id = 1;
SET @partnership_apply = 0;
SOURCE Backend/database/partnership-backfill.sql;
CALL psl_test_assert((SELECT COUNT(*) = 3 FROM partnerships), 'Preview modified partnership rows.');
CALL psl_test_assert((SELECT runs = 999 FROM partnerships WHERE partnership_id = 900), 'Preview changed existing values.');

SET @partnership_apply = 1;
SOURCE Backend/database/partnership-backfill.sql;
CALL psl_test_assert((SELECT COUNT(*) = 6 FROM partnerships WHERE match_id = 1), 'Expected six distinct stands in match one.');
CALL psl_test_assert((SELECT runs = 6 AND balls_faced = 3 AND wicket_number = 1 FROM partnerships
    WHERE match_id = 1 AND innings = 1 AND batter1_id = 1 AND batter2_id = 2), 'Strike rotation or wide counting failed.');
CALL psl_test_assert((SELECT runs = 3 AND balls_faced = 2 AND wicket_number = 2 FROM partnerships
    WHERE match_id = 1 AND innings = 1 AND batter1_id = 1 AND batter2_id = 3), 'No-ball or non-striker run-out counting failed.');
CALL psl_test_assert((SELECT runs = 6 AND balls_faced = 1 AND wicket_number = 3 FROM partnerships
    WHERE match_id = 1 AND innings = 1 AND batter1_id = 1 AND batter2_id = 7), 'Final unbeaten stand was lost.');
CALL psl_test_assert((SELECT COUNT(*) = 2 FROM partnerships
    WHERE match_id = 1 AND innings = 2 AND batter1_id = 4 AND batter2_id = 5), 'Resumed pair was merged.');
CALL psl_test_assert((SELECT MIN(wicket_number) = 1 AND MAX(wicket_number) = 1 FROM partnerships
    WHERE match_id = 1 AND innings = 2), 'Retired hurt was counted as a wicket.');
CALL psl_test_assert((SELECT runs = 99 FROM partnerships WHERE partnership_id = 902), 'Single-match apply changed another match.');
CALL psl_test_assert(@@SQL_SAFE_UPDATES = 1 AND @partnership_apply = 0, 'Session settings were not restored.');

SET @partnership_match_id = NULL;
SET @partnership_apply = 1;
SOURCE Backend/database/partnership-backfill.sql;
SET @partnership_apply = 1;
SOURCE Backend/database/partnership-backfill.sql;
CALL psl_test_assert((SELECT COUNT(*) = 8 FROM partnerships), 'Repeated apply duplicated or lost stands.');
CALL psl_test_assert((SELECT runs = 3 FROM partnerships WHERE match_id = 2), 'All-match apply did not update match two.');
CALL psl_test_assert((SELECT runs = 99 FROM partnerships WHERE partnership_id = 901), 'Match without deliveries was changed.');
CALL psl_test_assert((SELECT SUM(runs) = 15 AND SUM(balls_faced) = 6 FROM partnerships
    WHERE match_id = 1 AND innings = 1), 'Innings one totals do not reconcile.');
CALL psl_test_assert((SELECT SUM(runs) = 7 AND SUM(balls_faced) = 6 FROM partnerships
    WHERE match_id = 1 AND innings = 2), 'Innings two totals do not reconcile.');
SOURCE Backend/database/partnership-verify.sql;
DROP PROCEDURE psl_test_assert;
SELECT 'Partnership SQL fixture checks passed' AS result;
