-- Read-only checks. An empty first result means stored innings totals agree with
-- deliveries. An empty second result means no malformed/orphan partnership rows.
-- Optional: SET @partnership_match_id = 123; NULL checks all matches.
SET NAMES utf8mb4;

WITH delivery_totals AS (
    SELECT match_id, innings,
           SUM(COALESCE(runs_batter, 0) + COALESCE(runs_extras, 0)) AS runs,
           SUM(CASE WHEN REGEXP_REPLACE(LOWER(COALESCE(extra_type, '')), '[[:space:]_-]+', '') LIKE '%wide%'
                          OR REGEXP_REPLACE(LOWER(COALESCE(extra_type, '')), '[[:space:]_-]+', '') LIKE '%noball%'
                          OR REGEXP_REPLACE(LOWER(COALESCE(extra_type, '')), '[[:space:]_-]+', '') IN ('wd', 'w', 'nb')
                    THEN 0 ELSE 1 END) AS legal_balls
    FROM deliveries WHERE @partnership_match_id IS NULL OR match_id = @partnership_match_id
    GROUP BY match_id, innings
), stored_totals AS (
    SELECT match_id, innings, SUM(runs) AS runs, SUM(balls_faced) AS legal_balls
    FROM partnerships WHERE @partnership_match_id IS NULL OR match_id = @partnership_match_id
    GROUP BY match_id, innings
)
SELECT d.match_id, d.innings, d.runs AS delivery_runs, p.runs AS partnership_runs,
       d.legal_balls AS delivery_balls, p.legal_balls AS partnership_balls
FROM delivery_totals d LEFT JOIN stored_totals p ON p.match_id = d.match_id AND p.innings = d.innings
WHERE p.match_id IS NULL OR NOT (d.runs <=> p.runs) OR NOT (d.legal_balls <=> p.legal_balls)
UNION ALL
SELECT p.match_id, p.innings, NULL, p.runs, NULL, p.legal_balls
FROM stored_totals p LEFT JOIN delivery_totals d ON p.match_id = d.match_id AND p.innings = d.innings
WHERE d.match_id IS NULL
  AND EXISTS (SELECT 1 FROM deliveries source WHERE source.match_id = p.match_id)
ORDER BY match_id, innings;

SELECT p.partnership_id, p.match_id, p.innings, p.batter1_id, p.batter2_id,
       p.wicket_number, p.runs, p.balls_faced
FROM partnerships p
LEFT JOIN matches m ON m.match_id = p.match_id
LEFT JOIN player b1 ON b1.id = p.batter1_id
LEFT JOIN player b2 ON b2.id = p.batter2_id
WHERE (@partnership_match_id IS NULL OR p.match_id = @partnership_match_id)
  AND (m.match_id IS NULL OR b1.id IS NULL OR b2.id IS NULL OR p.batter1_id = p.batter2_id
       OR p.innings IS NULL OR p.innings < 1 OR p.wicket_number IS NULL OR p.wicket_number NOT BETWEEN 1 AND 10
       OR p.runs IS NULL OR p.runs < 0 OR p.balls_faced IS NULL OR p.balls_faced < 0)
ORDER BY p.match_id, p.innings, p.partnership_id;

SELECT p.match_id, COUNT(*) AS preserved_rows_without_deliveries
FROM partnerships p
WHERE (@partnership_match_id IS NULL OR p.match_id = @partnership_match_id)
  AND NOT EXISTS (SELECT 1 FROM deliveries d WHERE d.match_id = p.match_id)
GROUP BY p.match_id;
