-- MySQL 8.0+. Select the PSL database, stop backend writes, and take a backup first.
-- Repeatable schema update; this file does not populate or delete partnership rows.
-- DDL commits implicitly. Run in a fresh mysql session, not inside a transaction.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS partnerships (
    partnership_id BIGINT NOT NULL AUTO_INCREMENT,
    match_id INT NULL,
    innings INT NULL,
    batter1_id BIGINT NULL,
    batter2_id BIGINT NULL,
    runs INT NULL,
    balls_faced INT NULL,
    wicket_number INT NULL,
    PRIMARY KEY (partnership_id),
    CONSTRAINT fk_partnership_match FOREIGN KEY (match_id) REFERENCES matches (match_id),
    CONSTRAINT fk_partnership_batter1 FOREIGN KEY (batter1_id) REFERENCES player (id),
    CONSTRAINT fk_partnership_batter2 FOREIGN KEY (batter2_id) REFERENCES player (id)
) ENGINE = InnoDB;

-- CREATE TABLE IF NOT EXISTS does not repair a legacy ID column. Preserve its
-- integer type (including UNSIGNED) and existing IDs while enabling generation.
SET @partnership_schema_ddl = (
    SELECT IF(extra LIKE '%auto_increment%', 'DO 0',
        CONCAT('ALTER TABLE partnerships MODIFY COLUMN partnership_id ',
               column_type, ' NOT NULL AUTO_INCREMENT'))
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'partnerships' AND column_name = 'partnership_id'
);
PREPARE partnership_schema_statement FROM @partnership_schema_ddl;
EXECUTE partnership_schema_statement;
DEALLOCATE PREPARE partnership_schema_statement;

SET @partnership_schema_ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE partnerships ADD COLUMN innings INT NULL AFTER match_id', 'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'partnerships' AND column_name = 'innings'
);
PREPARE partnership_schema_statement FROM @partnership_schema_ddl;
EXECUTE partnership_schema_statement;
DEALLOCATE PREPARE partnership_schema_statement;

SET @partnership_schema_ddl = (
    SELECT IF(COUNT(*) = 0,
        'CREATE INDEX idx_partnership_match_innings ON partnerships (match_id, innings, wicket_number)', 'DO 0')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'partnerships'
      AND index_name = 'idx_partnership_match_innings'
);
PREPARE partnership_schema_statement FROM @partnership_schema_ddl;
EXECUTE partnership_schema_statement;
DEALLOCATE PREPARE partnership_schema_statement;

SELECT DATABASE() AS database_name, COUNT(*) AS existing_partnerships,
       SUM(innings IS NULL) AS rows_awaiting_innings_backfill
FROM partnerships;
