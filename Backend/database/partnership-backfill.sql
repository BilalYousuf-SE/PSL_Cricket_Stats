-- MySQL 8.0+ only. Requires CREATE ROUTINE and TEMPORARY TABLE privileges.
-- First run partnership-schema.sql. Stop backend/import writes and take a backup.
-- Defaults to PREVIEW: no partnership rows are changed.
-- SET @partnership_apply = 1; to apply, or 0 to preview.
-- SET @partnership_match_id = 123; to limit to one match; NULL selects all matches.
-- Apply replaces ALL stands for selected matches that have deliveries, including
-- manual records. Partnership IDs are regenerated; other matches are preserved.
SET NAMES utf8mb4;
SET @partnership_apply = COALESCE(@partnership_apply, 0);

DROP PROCEDURE IF EXISTS psl_backfill_partnerships_v1;
DELIMITER $$
CREATE PROCEDURE psl_backfill_partnerships_v1(IN p_apply INT, IN p_match_id INT)
BEGIN
    DECLARE v_invalid BIGINT DEFAULT 0;
    DECLARE v_inserted BIGINT DEFAULT 0;
    DECLARE v_deleted BIGINT DEFAULT 0;
    DECLARE v_safe_updates INT DEFAULT @@SQL_SAFE_UPDATES;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET SQL_SAFE_UPDATES = v_safe_updates;
        RESIGNAL;
    END;

    IF VERSION() LIKE '%MariaDB%' OR CAST(SUBSTRING_INDEX(VERSION(), '.', 1) AS UNSIGNED) < 8 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'This backfill requires MySQL 8.0 or newer.';
    END IF;
    IF p_apply NOT IN (0, 1) OR (p_match_id IS NOT NULL AND p_match_id < 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Use apply=0/1 and a positive match ID or NULL.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = DATABASE() AND table_name = 'partnerships' AND engine = 'InnoDB') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Run the schema file first; partnerships must use InnoDB.';
    END IF;
    IF p_match_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM matches WHERE match_id = p_match_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'The selected match does not exist.';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS psl_partnership_source;
    DROP TEMPORARY TABLE IF EXISTS psl_partnership_stands;
    SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
    START TRANSACTION WITH CONSISTENT SNAPSHOT;

    CREATE TEMPORARY TABLE psl_partnership_source ENGINE = InnoDB AS
    SELECT raw_source.*,
           CASE WHEN extra_kind LIKE '%wide%' OR extra_kind LIKE '%noball%'
                     OR extra_kind IN ('wd', 'w', 'nb') THEN 0 ELSE 1 END AS legal_ball,
           CASE WHEN wicket_kind IN ('', 'retiredhurt', 'retirednotout') THEN 0 ELSE 1 END AS is_wicket,
           CASE WHEN wicket_kind = '' THEN 0 ELSE 1 END AS ends_stand
    FROM (
    SELECT d.delivery_id, d.match_id, d.innings, d.over_num, d.ball_num,
           d.batter_id, d.non_striker_id, d.player_dismissed_id,
           LEAST(d.batter_id, d.non_striker_id) AS batter_low,
           GREATEST(d.batter_id, d.non_striker_id) AS batter_high,
           COALESCE(d.runs_batter, 0) AS runs_batter,
           COALESCE(d.runs_extras, 0) AS runs_extras,
           REGEXP_REPLACE(LOWER(COALESCE(d.extra_type, '')), '[[:space:]_-]+', '') AS extra_kind,
           CASE WHEN TRIM(LOWER(COALESCE(d.wicket_type, ''))) IN ('none', 'null', 'n/a', 'na', '—')
                THEN '' ELSE REGEXP_REPLACE(LOWER(COALESCE(d.wicket_type, '')), '[[:space:]_-]+', '') END AS wicket_kind,
           m.match_id AS valid_match_id, m.team_a_id, m.team_b_id,
           b1.team_id AS batter_team, b2.team_id AS non_striker_team,
           b1.is_playing AS batter_playing, b2.is_playing AS non_striker_playing
    FROM deliveries d
    LEFT JOIN matches m ON m.match_id = d.match_id
    LEFT JOIN match_squads b1 ON b1.match_id = d.match_id AND b1.player_id = d.batter_id
    LEFT JOIN match_squads b2 ON b2.match_id = d.match_id AND b2.player_id = d.non_striker_id
    WHERE p_match_id IS NULL OR d.match_id = p_match_id
    ) AS raw_source;

    -- Never invent a non-striker, innings, dismissal, or batting team.
    SELECT COUNT(*) INTO v_invalid FROM psl_partnership_source
    WHERE valid_match_id IS NULL OR innings IS NULL OR innings < 1
       OR over_num IS NULL OR over_num < 0 OR ball_num IS NULL OR ball_num < 1
       OR batter_id IS NULL OR non_striker_id IS NULL OR batter_id = non_striker_id
       OR runs_batter < 0 OR runs_extras < 0
       OR batter_team IS NULL OR non_striker_team IS NULL OR batter_team <> non_striker_team
       OR team_a_id IS NULL OR team_b_id IS NULL OR batter_team NOT IN (team_a_id, team_b_id)
       OR batter_playing = 0 OR non_striker_playing = 0
       OR (player_dismissed_id IS NOT NULL AND player_dismissed_id NOT IN (batter_id, non_striker_id))
       OR (wicket_kind = '' AND player_dismissed_id IS NOT NULL)
       OR (wicket_kind <> '' AND player_dismissed_id IS NULL)
       OR wicket_kind NOT IN ('', 'bowled', 'caught', 'caughtandbowled', 'caught&bowled', 'lbw',
              'stumped', 'hitwicket', 'runout', 'retiredhurt', 'retirednotout', 'retiredout',
              'obstructingthefield', 'obstructingfield', 'hitballtwice', 'handledtheball', 'timedout');
    IF v_invalid > 0 THEN
        SELECT v_invalid AS invalid_deliveries,
               'Check both batters, squads, innings, ball order, runs, and dismissal details.' AS action_required;
        SELECT delivery_id, match_id, innings, over_num, ball_num, batter_id, non_striker_id,
               batter_team, non_striker_team, wicket_kind, player_dismissed_id
        FROM psl_partnership_source
        WHERE valid_match_id IS NULL OR innings IS NULL OR innings < 1
           OR over_num IS NULL OR over_num < 0 OR ball_num IS NULL OR ball_num < 1
           OR batter_id IS NULL OR non_striker_id IS NULL OR batter_id = non_striker_id
           OR runs_batter < 0 OR runs_extras < 0
           OR batter_team IS NULL OR non_striker_team IS NULL OR batter_team <> non_striker_team
           OR team_a_id IS NULL OR team_b_id IS NULL OR batter_team NOT IN (team_a_id, team_b_id)
           OR batter_playing = 0 OR non_striker_playing = 0
           OR (player_dismissed_id IS NOT NULL AND player_dismissed_id NOT IN (batter_id, non_striker_id))
           OR (wicket_kind = '' AND player_dismissed_id IS NOT NULL)
           OR (wicket_kind <> '' AND player_dismissed_id IS NULL)
           OR wicket_kind NOT IN ('', 'bowled', 'caught', 'caughtandbowled', 'caught&bowled', 'lbw',
                  'stumped', 'hitwicket', 'runout', 'retiredhurt', 'retirednotout', 'retiredout',
                  'obstructingthefield', 'obstructingfield', 'hitballtwice', 'handledtheball', 'timedout')
        ORDER BY match_id, innings, over_num, ball_num, delivery_id LIMIT 50;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid source data; no partnerships were changed. See diagnostic rows.';
    END IF;

    SELECT COUNT(*) INTO v_invalid FROM (
        SELECT match_id, innings FROM psl_partnership_source
        GROUP BY match_id, innings HAVING COUNT(DISTINCT batter_team) <> 1
    ) AS mixed_teams;
    IF v_invalid > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'An innings contains batters from different teams; no partnerships changed.';
    END IF;

    -- Illegal deliveries may share a ball label with a legal delivery. Two legal
    -- deliveries with the same label cannot be ordered reliably and need correction.
    SELECT COUNT(*) INTO v_invalid FROM (
        SELECT match_id, innings, over_num, ball_num FROM psl_partnership_source
        GROUP BY match_id, innings, over_num, ball_num HAVING SUM(legal_ball) > 1
    ) AS duplicate_balls;
    IF v_invalid > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate legal ball labels found; fix delivery ordering before backfilling.';
    END IF;

    CREATE TEMPORARY TABLE psl_partnership_stands ENGINE = InnoDB AS
    WITH history AS (
        SELECT s.*,
               LAG(batter_low) OVER delivery_order AS previous_low,
               LAG(batter_high) OVER delivery_order AS previous_high,
               LAG(ends_stand) OVER delivery_order AS previous_end,
               LAG(is_wicket) OVER delivery_order AS previous_wicket,
               LAG(player_dismissed_id) OVER delivery_order AS previous_dismissed,
               COALESCE(SUM(is_wicket) OVER (
                   PARTITION BY match_id, innings ORDER BY over_num, ball_num, delivery_id
                   ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) AS wickets_before
        FROM psl_partnership_source s
        WINDOW delivery_order AS (PARTITION BY match_id, innings ORDER BY over_num, ball_num, delivery_id)
    ), numbered AS (
        SELECT h.*,
               SUM(CASE WHEN previous_low IS NULL OR batter_low <> previous_low
                              OR batter_high <> previous_high OR previous_end = 1 THEN 1 ELSE 0 END)
                   OVER (PARTITION BY match_id, innings ORDER BY over_num, ball_num, delivery_id
                         ROWS UNBOUNDED PRECEDING) AS stand_number,
               CASE WHEN previous_wicket = 1
                          AND previous_dismissed IN (batter_id, non_striker_id) THEN 1 ELSE 0 END AS dismissed_still_batting
        FROM history h
    )
    SELECT match_id, innings, stand_number, MIN(batter_low) AS batter1_id,
           MIN(batter_high) AS batter2_id, SUM(runs_batter + runs_extras) AS runs,
           SUM(legal_ball) AS balls_faced, MIN(wickets_before) + 1 AS wicket_number,
           MAX(dismissed_still_batting) AS invalid_continuation
    FROM numbered GROUP BY match_id, innings, stand_number;

    SELECT COUNT(*) INTO v_invalid FROM psl_partnership_stands
    WHERE wicket_number > 10 OR invalid_continuation = 1
       OR runs > 2147483647 OR balls_faced > 2147483647;
    IF v_invalid > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid stand: batting after dismissal/ten wickets or totals outside INT range.';
    END IF;

    SELECT IF(p_apply = 1, 'APPLY', 'PREVIEW - no partnership rows changed') AS mode,
           COUNT(*) AS calculated_stands, COUNT(DISTINCT match_id) AS selected_matches
    FROM psl_partnership_stands;
    SELECT match_id, innings, COUNT(*) AS stands, SUM(runs) AS innings_runs,
           SUM(balls_faced) AS legal_balls
    FROM psl_partnership_stands GROUP BY match_id, innings ORDER BY match_id, innings;
    SELECT match_id, innings, stand_number, batter1_id, batter2_id, wicket_number, runs, balls_faced
    FROM psl_partnership_stands ORDER BY match_id, innings, stand_number LIMIT 100;

    IF p_apply = 1 THEN
        SET SQL_SAFE_UPDATES = 0;
        DELETE p FROM partnerships p
        JOIN (SELECT DISTINCT match_id FROM psl_partnership_stands) targets ON targets.match_id = p.match_id;
        SET v_deleted = ROW_COUNT();
        INSERT INTO partnerships (match_id, innings, batter1_id, batter2_id, runs, balls_faced, wicket_number)
        SELECT match_id, innings, batter1_id, batter2_id, runs, balls_faced, wicket_number
        FROM psl_partnership_stands ORDER BY match_id, innings, stand_number;
        SET v_inserted = ROW_COUNT();
        SET SQL_SAFE_UPDATES = v_safe_updates;
        COMMIT;
        SELECT v_deleted AS replaced_rows, v_inserted AS inserted_rows, 'Committed' AS result;
    ELSE
        ROLLBACK;
    END IF;
    DROP TEMPORARY TABLE psl_partnership_stands;
    DROP TEMPORARY TABLE psl_partnership_source;
END$$
DELIMITER ;

CALL psl_backfill_partnerships_v1(@partnership_apply, @partnership_match_id);
DROP PROCEDURE psl_backfill_partnerships_v1;
-- An additional SOURCE defaults back to preview, even after an apply.
SET @partnership_apply = 0;
