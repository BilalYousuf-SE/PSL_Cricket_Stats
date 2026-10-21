package com.cricket.stats.repository;

import com.cricket.stats.dto.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** One aggregation for individual comparisons, opponent lists, and matrix cells. MySQL 8+. */
@Repository
@RequiredArgsConstructor
public class MatchupRepository {
    private final NamedParameterJdbcTemplate jdbc;

    private static String playerName(String alias) {
        return "COALESCE(NULLIF(TRIM(" + alias + ".full_name), ''), NULLIF(TRIM(CONCAT_WS(' ', "
                + alias + ".first_name, " + alias + ".last_name)), ''), CONCAT('Player #', " + alias + ".id))";
    }

    private static final String AGGREGATES = """
            COUNT(*) AS deliveries, COUNT(DISTINCT match_id) AS matches,
            COUNT(DISTINCT match_id, innings) AS innings_count,
            SUM(runs_batter) AS runs, SUM(ball_faced) AS balls_faced,
            SUM(legal_ball) AS legal_balls, SUM(runs_batter = 4) AS fours,
            SUM(runs_batter = 6) AS sixes,
            SUM(legal_ball = 1 AND runs_batter + runs_extras = 0) AS dot_balls,
            SUM(bowler_wicket) AS dismissals,
            SUM(wicket_kind = 'runout' AND player_dismissed_id = batter_id) AS run_outs,
            SUM(runs_batter + CASE WHEN extra_kind LIKE '%wide%' OR extra_kind LIKE '%noball%'
                                  THEN runs_extras ELSE 0 END) AS runs_conceded
            """;

    private String source(MatchupFilterDTO filter, MapSqlParameterSource params,
                          List<Long> batterIds, List<Long> bowlerIds) {
        StringBuilder where = new StringBuilder(" WHERE d.batter_id <> d.bowler_id");
        if (batterIds != null) {
            where.append(" AND d.batter_id IN (:batterIds)");
            params.addValue("batterIds", batterIds);
        }
        if (bowlerIds != null) {
            where.append(" AND d.bowler_id IN (:bowlerIds)");
            params.addValue("bowlerIds", bowlerIds);
        }
        if (filter.getSeasonYear() != null) {
            where.append(" AND s.season_year = :seasonYear");
            params.addValue("seasonYear", filter.getSeasonYear());
        }
        if (filter.getFromDate() != null) {
            where.append(" AND m.match_date >= :fromDate");
            params.addValue("fromDate", filter.getFromDate());
        }
        if (filter.getToDate() != null) {
            where.append(" AND m.match_date <= :toDate");
            params.addValue("toDate", filter.getToDate());
        }
        if (filter.getVenue() != null && !filter.getVenue().isBlank()) {
            where.append(" AND m.venue = :venue");
            params.addValue("venue", filter.getVenue().trim());
        }
        if (filter.getInnings() != null) {
            where.append(" AND d.innings = :innings");
            params.addValue("innings", filter.getInnings());
        }
        if (filter.getBowlingStyle() != null) {
            where.append(" AND bp.bowling_style = :bowlingStyle");
            params.addValue("bowlingStyle", filter.getBowlingStyle().name());
        }
        // Team IDs intentionally do not appear here: a pair's encounters follow the players.
        return """
                WITH normalized AS (
                  SELECT d.match_id, d.batter_id, d.bowler_id, d.innings, d.player_dismissed_id,
                         COALESCE(d.runs_batter, 0) AS runs_batter,
                         COALESCE(d.runs_extras, 0) AS runs_extras,
                         REGEXP_REPLACE(LOWER(COALESCE(d.extra_type, '')), '[[:space:]_-]+', '') AS extra_kind,
                         REGEXP_REPLACE(LOWER(COALESCE(d.wicket_type, '')), '[[:space:]_-]+', '') AS wicket_kind,
                         m.match_date, s.season_year, m.venue, m.team_a_id, m.team_b_id
                  FROM deliveries d JOIN matches m ON m.match_id = d.match_id
                  LEFT JOIN season s ON s.season_id = m.season_id
                  JOIN player bp ON bp.id = d.bowler_id
                """ + where + """
                ), encounters AS (
                  SELECT normalized.*,
                         CASE WHEN extra_kind LIKE '%wide%' THEN 0 ELSE 1 END AS ball_faced,
                         CASE WHEN extra_kind LIKE '%wide%' OR extra_kind LIKE '%noball%'
                              THEN 0 ELSE 1 END AS legal_ball,
                         CASE WHEN player_dismissed_id = batter_id AND extra_kind NOT LIKE '%noball%'
                              AND (wicket_kind IN ('stumped', 'hitwicket') OR
                                   (wicket_kind IN ('bowled', 'caught', 'caughtandbowled', 'caught&bowled', 'lbw')
                                    AND extra_kind NOT LIKE '%wide%'))
                              THEN 1 ELSE 0 END AS bowler_wicket
                  FROM normalized
                )
                """;
    }

    public List<MatchupDTO> pairs(MatchupFilterDTO filter, List<Long> batterIds, List<Long> bowlerIds) {
        if ((batterIds != null && batterIds.isEmpty()) || (bowlerIds != null && bowlerIds.isEmpty())) return List.of();
        var params = new MapSqlParameterSource();
        return jdbc.query(source(filter, params, batterIds, bowlerIds) + pairSelect(), params,
                (rs, row) -> pair(rs));
    }

    public Page<MatchupDTO> opponents(Long playerId, boolean batting, MatchupFilterDTO filter, Pageable page) {
        var params = new MapSqlParameterSource();
        String cte = source(filter, params, batting ? List.of(playerId) : null, batting ? null : List.of(playerId));
        Long total = jdbc.queryForObject(cte + "SELECT COUNT(DISTINCT "
                + (batting ? "bowler_id" : "batter_id") + ") FROM encounters", params, Long.class);
        params.addValue("limit", page.getPageSize()).addValue("offset", page.getOffset());
        var rows = jdbc.query(cte + pairSelect() + " ORDER BY balls_faced DESC, runs DESC, e.batter_id, e.bowler_id LIMIT :limit OFFSET :offset",
                params, (rs, row) -> pair(rs));
        return new PageImpl<>(rows, page, total == null ? 0 : total);
    }

    private String pairSelect() {
        return "SELECT e.batter_id, e.bowler_id, MAX(" + playerName("b") + ") AS batter_name, MAX("
                + playerName("p") + ") AS bowler_name, MAX(b.bowling_style) AS batter_style, MAX(p.bowling_style) AS bowler_style, "
                + AGGREGATES + " FROM encounters e JOIN player b ON b.id = e.batter_id JOIN player p ON p.id = e.bowler_id"
                + " GROUP BY e.batter_id, e.bowler_id";
    }

    public Page<MatchupMatchDTO> matches(Long batterId, Long bowlerId, MatchupFilterDTO filter, Pageable page) {
        var params = new MapSqlParameterSource();
        String cte = source(filter, params, List.of(batterId), List.of(bowlerId));
        Long total = jdbc.queryForObject(cte + "SELECT COUNT(*) FROM (SELECT match_id, innings FROM encounters GROUP BY match_id, innings) groups_found",
                params, Long.class);
        params.addValue("limit", page.getPageSize()).addValue("offset", page.getOffset());
        String sql = cte + "SELECT match_id, innings, MAX(match_date) AS match_date, MAX(season_year) AS season_year,"
                + " MAX(venue) AS venue, MAX(a.team_name) AS team_a_name, MAX(b.team_name) AS team_b_name, " + AGGREGATES
                + " FROM encounters LEFT JOIN teams a ON a.team_id = team_a_id LEFT JOIN teams b ON b.team_id = team_b_id"
                + " GROUP BY match_id, innings ORDER BY match_date DESC, match_id DESC, innings ASC LIMIT :limit OFFSET :offset";
        var rows = jdbc.query(sql, params, (rs, row) -> new MatchupMatchDTO(rs.getInt("match_id"),
                rs.getObject("match_date", java.time.LocalDate.class), rs.getObject("season_year", Integer.class),
                rs.getString("venue"), rs.getObject("innings", Integer.class), rs.getString("team_a_name"),
                rs.getString("team_b_name"), stats(rs)));
        return new PageImpl<>(rows, page, total == null ? 0 : total);
    }

    /** Season selects squad membership. Date, venue, and innings filter encounters only. */
    public Page<MatchupPlayerDTO> roster(Integer teamId, Integer seasonYear, boolean bowlers, Pageable page) {
        var params = new MapSqlParameterSource("teamId", teamId);
        String from = " FROM match_squads ms JOIN player p ON p.id = ms.player_id JOIN matches m ON m.match_id = ms.match_id"
                + " LEFT JOIN season s ON s.season_id = m.season_id WHERE ms.team_id = :teamId AND COALESCE(ms.is_playing, TRUE) = TRUE";
        if (seasonYear != null) {
            from += " AND s.season_year = :seasonYear";
            params.addValue("seasonYear", seasonYear);
        }
        if (bowlers) {
            // Roles do not exclude occasional bowlers; actual recorded bowling also qualifies.
            from += " AND (p.bowling_style IS NOT NULL OR p.role IN ('Baller', 'Allrounder')"
                    + " OR EXISTS (SELECT 1 FROM deliveries d WHERE d.bowler_id = p.id))";
        }
        Long total = jdbc.queryForObject("SELECT COUNT(DISTINCT p.id)" + from, params, Long.class);
        params.addValue("limit", page.getPageSize()).addValue("offset", page.getOffset());
        var rows = jdbc.query("SELECT DISTINCT p.id, " + playerName("p") + " AS player_name, p.bowling_style" + from
                + " ORDER BY player_name, p.id LIMIT :limit OFFSET :offset", params,
                (rs, row) -> new MatchupPlayerDTO(rs.getLong("id"), rs.getString("player_name"), rs.getString("bowling_style")));
        return new PageImpl<>(rows, page, total == null ? 0 : total);
    }

    public MatchupOptionsDTO options() {
        return new MatchupOptionsDTO(
                jdbc.getJdbcTemplate().queryForList("SELECT DISTINCT season_year FROM season WHERE season_year > 0 ORDER BY season_year DESC", Integer.class),
                jdbc.getJdbcTemplate().queryForList("SELECT DISTINCT venue FROM matches WHERE venue IS NOT NULL AND TRIM(venue) <> '' ORDER BY venue", String.class),
                java.util.Arrays.stream(com.cricket.stats.entity.BowlingStyle.values()).map(Enum::name).toList());
    }

    private MatchupDTO pair(ResultSet rs) throws SQLException {
        return new MatchupDTO(new MatchupPlayerDTO(rs.getLong("batter_id"), rs.getString("batter_name"), rs.getString("batter_style")),
                new MatchupPlayerDTO(rs.getLong("bowler_id"), rs.getString("bowler_name"), rs.getString("bowler_style")), stats(rs));
    }

    private MatchupStatsDTO stats(ResultSet rs) throws SQLException {
        return MatchupStatsDTO.of(rs.getLong("deliveries"), rs.getLong("matches"), rs.getLong("innings_count"),
                rs.getLong("runs"), rs.getLong("balls_faced"), rs.getLong("legal_balls"), rs.getLong("fours"),
                rs.getLong("sixes"), rs.getLong("dot_balls"), rs.getLong("dismissals"), rs.getLong("run_outs"), rs.getLong("runs_conceded"));
    }
}
