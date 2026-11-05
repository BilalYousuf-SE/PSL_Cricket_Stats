package com.cricket.stats.repository;

import com.cricket.stats.dto.PlayerPerformanceFilterDTO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Reads existing delivery and match metadata; does not rebuild or write scorecards. */
@Repository
@RequiredArgsConstructor
public class PlayerPerformanceRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public record Ball(long id, int matchId, Integer innings, Integer seasonYear, String venue, String stage,
            Integer teamA, String teamAName, Integer teamB, String teamBName,
            Long batter, Long nonStriker, Long bowler, Long dismissed, Long fielder,
            long runs, long extras, String extraType, String wicketType, String bowlingStyle,
            String batterHand, String dismissedHand, Integer ownPosition, Integer batterPosition,
            Integer dismissedPosition, Integer battingTeam, Integer bowlingTeam, Integer playerTeam,
            Integer playerCaptain, Integer playerKeeper, Integer playerPlaying,
            int battingCaptains, int bowlingCaptains, int keepers, Long soleKeeper) {}

    public record MatchMetadata(Integer seasonYear, String venue, Integer teamA, String teamAName,
                                Integer teamB, String teamBName) {}

    private static final String PARTICIPATION = """
        (EXISTS (SELECT 1 FROM match_squads ps WHERE ps.match_id = m.match_id
                 AND ps.player_id = :playerId AND COALESCE(ps.is_playing, TRUE) = TRUE)
         OR EXISTS (SELECT 1 FROM deliveries pd WHERE pd.match_id = m.match_id
                    AND (pd.batter_id = :playerId OR pd.non_striker_id = :playerId
                         OR pd.bowler_id = :playerId OR pd.player_dismissed_id = :playerId
                         OR pd.fielder_id = :playerId)))
        """;

    public List<MatchMetadata> options(Long playerId) {
        return jdbc.query("""
            SELECT s.season_year, m.venue, m.team_a_id, a.team_name AS a_name,
                   m.team_b_id, b.team_name AS b_name
            FROM matches m LEFT JOIN season s ON s.season_id = m.season_id
            LEFT JOIN teams a ON a.team_id = m.team_a_id
            LEFT JOIN teams b ON b.team_id = m.team_b_id WHERE
            """ + PARTICIPATION, new MapSqlParameterSource("playerId", playerId),
            (rs, i) -> new MatchMetadata(integer(rs, "season_year"), rs.getString("venue"),
                integer(rs, "team_a_id"), rs.getString("a_name"), integer(rs, "team_b_id"), rs.getString("b_name")));
    }

    public List<Ball> deliveries(Long playerId, PlayerPerformanceFilterDTO filter) {
        var params = new MapSqlParameterSource("playerId", playerId);
        StringBuilder where = new StringBuilder(" WHERE " + PARTICIPATION);
        if (filter.getSeasonYear() != null) {
            where.append(" AND s.season_year = :seasonYear"); params.addValue("seasonYear", filter.getSeasonYear());
        }
        if (filter.getFromDate() != null) {
            where.append(" AND m.match_date >= :fromDate"); params.addValue("fromDate", filter.getFromDate());
        }
        if (filter.getToDate() != null) {
            where.append(" AND m.match_date <= :toDate"); params.addValue("toDate", filter.getToDate());
        }
        if (filter.getVenue() != null && !filter.getVenue().isBlank()) {
            where.append(" AND m.venue = :venue"); params.addValue("venue", filter.getVenue().trim());
        }
        String battingTeam = "COALESCE(bs.team_id, ns.team_id, CASE WHEN ws.team_id = m.team_a_id THEN m.team_b_id WHEN ws.team_id = m.team_b_id THEN m.team_a_id END)";
        String bowlingTeam = "COALESCE(ws.team_id, CASE WHEN COALESCE(bs.team_id, ns.team_id) = m.team_a_id THEN m.team_b_id WHEN COALESCE(bs.team_id, ns.team_id) = m.team_b_id THEN m.team_a_id END)";
        String wicket = "LOWER(COALESCE(wicket_type, ''))";
        String[] separators = {" ", "_", "-", "\t", "\n", "\r", "\f", "\u000b"};
        for (int i = 0; i < separators.length; i++) {
            params.addValue("performanceSeparator" + i, separators[i]);
            wicket = "REPLACE(" + wicket + ", :performanceSeparator" + i + ", '')";
        }
        String sql = """
            WITH selected_matches AS (
              SELECT m.match_id, m.venue, m.stage, m.team_a_id, m.team_b_id,
                     a.team_name AS a_name, b.team_name AS b_name, s.season_year
              FROM matches m LEFT JOIN season s ON s.season_id = m.season_id
              LEFT JOIN teams a ON a.team_id = m.team_a_id
              LEFT JOIN teams b ON b.team_id = m.team_b_id
            """ + where + """
            ), appearances AS (
              SELECT d.match_id, d.innings, d.batter_id AS player_id, d.over_num, d.ball_num,
                     d.delivery_id, 0 AS arrival_order
              FROM deliveries d JOIN selected_matches m ON m.match_id = d.match_id
              WHERE d.batter_id IS NOT NULL AND d.innings IS NOT NULL
                AND d.over_num IS NOT NULL AND d.ball_num IS NOT NULL
              UNION ALL
              SELECT d.match_id, d.innings, d.non_striker_id, d.over_num, d.ball_num, d.delivery_id, 1
              FROM deliveries d JOIN selected_matches m ON m.match_id = d.match_id
              WHERE d.non_striker_id IS NOT NULL AND d.innings IS NOT NULL
                AND d.over_num IS NOT NULL AND d.ball_num IS NOT NULL
            ), first_appearance AS (
              SELECT appearances.*, ROW_NUMBER() OVER (
                PARTITION BY match_id, innings, player_id
                ORDER BY over_num, ball_num, delivery_id, arrival_order) AS appearance_number
              FROM appearances
            ), positions AS (
              SELECT match_id, innings, player_id, ROW_NUMBER() OVER (
                PARTITION BY match_id, innings
                ORDER BY over_num, ball_num, delivery_id, arrival_order) AS batting_position
              FROM first_appearance WHERE appearance_number = 1
            ), team_roles AS (
              SELECT match_id, team_id,
                SUM(CASE WHEN is_captain = TRUE THEN 1 ELSE 0 END) AS captains,
                SUM(CASE WHEN is_keeper = TRUE THEN 1 ELSE 0 END) AS keepers,
                MIN(CASE WHEN is_keeper = TRUE THEN player_id END) AS keeper_id
              FROM match_squads WHERE COALESCE(is_playing, TRUE) = TRUE
              GROUP BY match_id, team_id
            ), ball_context AS (
              SELECT d.delivery_id, d.match_id, d.innings, m.season_year, m.venue, m.stage,
                m.team_a_id, m.a_name, m.team_b_id, m.b_name,
                d.batter_id, d.non_striker_id, d.bowler_id, d.player_dismissed_id, d.fielder_id,
                COALESCE(d.runs_batter, 0) AS runs_batter, COALESCE(d.runs_extras, 0) AS runs_extras,
                d.extra_type, d.wicket_type, bow.bowling_style, bat.playing_style AS batter_hand,
                out_player.playing_style AS dismissed_hand, own_pos.batting_position AS own_position,
                bat_pos.batting_position AS batter_position, out_pos.batting_position AS dismissed_position,
            """ + battingTeam + " AS batting_team, " + bowlingTeam + " AS bowling_team, " + """
                ps.team_id AS player_team,
                CASE WHEN ps.is_captain = TRUE THEN 1 WHEN ps.is_captain = FALSE THEN 0 END AS player_captain,
                CASE WHEN ps.is_keeper = TRUE THEN 1 WHEN ps.is_keeper = FALSE THEN 0 END AS player_keeper,
                CASE WHEN ps.is_playing = TRUE THEN 1 WHEN ps.is_playing = FALSE THEN 0 END AS player_playing,
                COALESCE(br.captains, 0) AS batting_captains, COALESCE(wr.captains, 0) AS bowling_captains,
                COALESCE(wr.keepers, 0) AS keepers, wr.keeper_id
              FROM deliveries d JOIN selected_matches m ON m.match_id = d.match_id
              LEFT JOIN player bat ON bat.id = d.batter_id
              LEFT JOIN player bow ON bow.id = d.bowler_id
              LEFT JOIN player out_player ON out_player.id = d.player_dismissed_id
              LEFT JOIN match_squads bs ON bs.match_id = d.match_id AND bs.player_id = d.batter_id
              LEFT JOIN match_squads ns ON ns.match_id = d.match_id AND ns.player_id = d.non_striker_id
              LEFT JOIN match_squads ws ON ws.match_id = d.match_id AND ws.player_id = d.bowler_id
              LEFT JOIN match_squads ps ON ps.match_id = d.match_id AND ps.player_id = :playerId
              LEFT JOIN positions own_pos ON own_pos.match_id = d.match_id AND own_pos.innings = d.innings AND own_pos.player_id = :playerId
              LEFT JOIN positions bat_pos ON bat_pos.match_id = d.match_id AND bat_pos.innings = d.innings AND bat_pos.player_id = d.batter_id
              LEFT JOIN positions out_pos ON out_pos.match_id = d.match_id AND out_pos.innings = d.innings AND out_pos.player_id = d.player_dismissed_id
            """ + " LEFT JOIN team_roles br ON br.match_id = d.match_id AND br.team_id = " + battingTeam
            + " LEFT JOIN team_roles wr ON wr.match_id = d.match_id AND wr.team_id = " + bowlingTeam + """
            )
            SELECT * FROM ball_context
            WHERE batter_id = :playerId OR non_striker_id = :playerId OR bowler_id = :playerId
               OR player_dismissed_id = :playerId OR fielder_id = :playerId
               OR (fielder_id IS NULL AND player_team = bowling_team AND COALESCE(player_playing, 1) = 1
            """ + " AND " + wicket + " IN ('caught', 'caughtandbowled', 'caught&bowled', 'stumped', 'runout'))";
        return jdbc.query(sql, params, (rs, i) -> new Ball(rs.getLong("delivery_id"), rs.getInt("match_id"),
            integer(rs, "innings"), integer(rs, "season_year"), rs.getString("venue"), rs.getString("stage"),
            integer(rs, "team_a_id"), rs.getString("a_name"), integer(rs, "team_b_id"), rs.getString("b_name"),
            number(rs, "batter_id"), number(rs, "non_striker_id"), number(rs, "bowler_id"), number(rs, "player_dismissed_id"), number(rs, "fielder_id"),
            rs.getLong("runs_batter"), rs.getLong("runs_extras"), rs.getString("extra_type"), rs.getString("wicket_type"),
            rs.getString("bowling_style"), rs.getString("batter_hand"), rs.getString("dismissed_hand"),
            integer(rs, "own_position"), integer(rs, "batter_position"), integer(rs, "dismissed_position"),
            integer(rs, "batting_team"), integer(rs, "bowling_team"), integer(rs, "player_team"),
            integer(rs, "player_captain"), integer(rs, "player_keeper"), integer(rs, "player_playing"),
            rs.getInt("batting_captains"), rs.getInt("bowling_captains"), rs.getInt("keepers"), number(rs, "keeper_id")));
    }

    private static Integer integer(ResultSet rs, String key) throws SQLException {
        Number value = (Number) rs.getObject(key); return value == null ? null : value.intValue();
    }
    private static Long number(ResultSet rs, String key) throws SQLException {
        Number value = (Number) rs.getObject(key); return value == null ? null : value.longValue();
    }
}
