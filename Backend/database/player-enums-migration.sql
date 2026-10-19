-- MySQL 8: BACK UP the database and STOP the backend before running this file.
-- Only player metadata/schema is updated. IDs and all match links stay intact.
-- Repeatable. DDL commits implicitly; use the backup to roll back the migration.
-- Select the correct PSL database as your default schema before running.
SET @player_previous_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

SET @player_had_playing_style = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'player' AND column_name = 'playing_style');
SET @player_ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE player ADD COLUMN playing_style ENUM(''Right'',''Left'') NULL', 'DO 0')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'player' AND column_name = 'playing_style');
PREPARE player_statement FROM @player_ddl;
EXECUTE player_statement;
DEALLOCATE PREPARE player_statement;

-- Keep the original batting_style column for rollback/reference if it exists.
SET @player_ddl = (SELECT IF(COUNT(*) > 0,
  'UPDATE player SET playing_style = batting_style WHERE playing_style IS NULL AND batting_style IS NOT NULL AND @player_had_playing_style = 0', 'DO 0')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'player' AND column_name = 'batting_style');
PREPARE player_statement FROM @player_ddl;
EXECUTE player_statement;
DEALLOCATE PREPARE player_statement;

SET @player_ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE player ADD COLUMN legacy_bowling_arm ENUM(''Right'',''Left'') NULL', 'DO 0')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'player' AND column_name = 'legacy_bowling_arm');
PREPARE player_statement FROM @player_ddl;
EXECUTE player_statement;
DEALLOCATE PREPARE player_statement;

-- Widen before translating old values, so ENUM ordinals are never reinterpreted.
ALTER TABLE player MODIFY COLUMN role VARCHAR(32) NOT NULL;
ALTER TABLE player MODIFY COLUMN bowling_style VARCHAR(32) NULL;
UPDATE player SET legacy_bowling_arm = bowling_style
WHERE legacy_bowling_arm IS NULL AND bowling_style IN ('Right','Left');
UPDATE player SET bowling_style = NULL WHERE bowling_style IN ('Right','Left');

-- Old specialist roles provide known techniques, but never overwrite a newer style.
UPDATE player SET bowling_style = role
WHERE bowling_style IS NULL AND role IN ('FastBaller','OffSpinner','LegSpinner');
UPDATE player SET role = CASE role
  WHEN 'FastBaller' THEN 'Baller'
  WHEN 'OffSpinner' THEN 'Baller'
  WHEN 'LegSpinner' THEN 'Baller'
  WHEN 'WicketKeeper' THEN 'Keeper'
  WHEN 'AllRounder' THEN 'Allrounder'
  ELSE role END;

ALTER TABLE player MODIFY COLUMN role ENUM('Batsman','Baller','Keeper','Allrounder') NOT NULL;
ALTER TABLE player MODIFY COLUMN bowling_style ENUM(
  'FastBaller','FastMedium','MediumFast','MediumPacer','SlowMediumPacer',
  'OffSpinner','LegSpinner','LeftArmOrthodox','LeftArmWristSpin') NULL;
SET SQL_SAFE_UPDATES = @player_previous_safe_updates;
