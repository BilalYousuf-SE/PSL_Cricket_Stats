package com.cricket.stats.repository;

import com.cricket.stats.dto.TeamLeaderboardDTO;
import com.cricket.stats.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Integer> {

    // --- Derived Queries ---

    // Find team by exact name
    Optional<Team> findByTeamName(String teamName);

    // Search team by partial name
    List<Team> findByTeamNameContainingIgnoreCase(String name);

    // Get all teams sorted alphabetically
    List<Team> findAllByOrderByTeamNameAsc();


    // --- JPQL Queries ---

    // Get total matches played by a team (as team_a or team_b)
    @Query("SELECT COUNT(m) FROM Match m " +
            "WHERE m.teamA.teamId = :teamId OR m.teamB.teamId = :teamId")
    Long countMatchesPlayedByTeam(@Param("teamId") Integer teamId);

    // Get total matches won by a team
    @Query("SELECT COUNT(m) FROM Match m " +
            "WHERE m.winner.teamId = :teamId")
    Long countMatchesWonByTeam(@Param("teamId") Integer teamId);

    // Get all teams that played in a specific season
    @Query("SELECT DISTINCT t FROM Team t " +
            "JOIN Match m ON (m.teamA.teamId = t.teamId OR m.teamB.teamId = t.teamId) " +
            "WHERE m.season.year = :year " +
            "ORDER BY t.teamName ASC")
    List<Team> findTeamsBySeasonYear(@Param("year") Integer year);

    // All-time leaderboard: wins, losses, matches played and points (2 per
    // win) for every team that has played at least one match, sorted by
    // points descending.
    // NOTE: net run rate is not tracked anywhere in the schema (would need
    // per-innings runs/overs aggregation), so it is left as 0.0 here as a
    // placeholder.
    @Query("SELECT new com.cricket.stats.dto.TeamLeaderboardDTO(" +
        "t.teamId, t.teamName, " +
        "(SELECT COUNT(m1) FROM Match m1 WHERE m1.winner.teamId = t.teamId), " +
        "(SELECT COUNT(m2) FROM Match m2 WHERE m2.winner IS NOT NULL " +
        "   AND m2.winner.teamId <> t.teamId AND (m2.teamA.teamId = t.teamId OR m2.teamB.teamId = t.teamId)), " +
        "(SELECT COUNT(m3) FROM Match m3 WHERE (m3.teamA.teamId = t.teamId OR m3.teamB.teamId = t.teamId)), " +
        "(SELECT COUNT(m4) FROM Match m4 WHERE m4.winner.teamId = t.teamId) * 2L, " +
        "0.0) " +
        "FROM Team t " +
        "WHERE EXISTS (SELECT 1 FROM Match m5 WHERE (m5.teamA.teamId = t.teamId OR m5.teamB.teamId = t.teamId)) " +
        "ORDER BY (SELECT COUNT(m6) FROM Match m6 WHERE m6.winner.teamId = t.teamId) DESC")
    List<TeamLeaderboardDTO> findAllTimeLeaderboard();

    // Full leaderboard (wins/losses/matches/points) for a specific season.
    // NOTE: net run rate is not tracked anywhere in the schema (would need
    // per-innings runs/overs aggregation), so it is left as 0.0 here as a
    // placeholder.
    @Query("SELECT new com.cricket.stats.dto.TeamLeaderboardDTO(" +
        "t.teamId, t.teamName, " +
        "(SELECT COUNT(m1) FROM Match m1 WHERE m1.season.year = :year AND m1.winner.teamId = t.teamId), " +
        "(SELECT COUNT(m2) FROM Match m2 WHERE m2.season.year = :year AND m2.winner IS NOT NULL " +
        "   AND m2.winner.teamId <> t.teamId AND (m2.teamA.teamId = t.teamId OR m2.teamB.teamId = t.teamId)), " +
        "(SELECT COUNT(m3) FROM Match m3 WHERE m3.season.year = :year " +
        "   AND (m3.teamA.teamId = t.teamId OR m3.teamB.teamId = t.teamId)), " +
        "(SELECT COUNT(m4) FROM Match m4 WHERE m4.season.year = :year AND m4.winner.teamId = t.teamId) * 2L, " +
        "0.0) " +
        "FROM Team t " +
        "WHERE EXISTS (SELECT 1 FROM Match m5 WHERE m5.season.year = :year " +
        "   AND (m5.teamA.teamId = t.teamId OR m5.teamB.teamId = t.teamId)) " +
        "ORDER BY (SELECT COUNT(m6) FROM Match m6 WHERE m6.season.year = :year AND m6.winner.teamId = t.teamId) * 2L DESC")
    List<TeamLeaderboardDTO> findTeamLeaderboardBySeason(@Param("year") Integer year);

}