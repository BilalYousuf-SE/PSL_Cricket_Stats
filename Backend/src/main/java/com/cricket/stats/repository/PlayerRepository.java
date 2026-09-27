package com.cricket.stats.repository;

import com.cricket.stats.entity.Player;
import com.cricket.stats.entity.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.cricket.stats.dto.*;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    // --- Derived Queries ---

    // Search player by full name (case insensitive)
    List<Player> findByFullNameContainingIgnoreCase(String name);

    // Get all players of a specific role e.g. Batsman, AllRounder
    List<Player> findByRole(Roles role);

    // Get all players from a country
    List<Player> findByNationality(String nationality);

    // Get all players sorted by first name
    List<Player> findAllByOrderByFirstNameAsc();

    // Find by exact full name
    Optional<Player> findByFullName(String fullName);


    // --- JPQL Queries ---

    // Get all players who played in a specific season
    @Query("SELECT DISTINCT p FROM Player p " +
            "JOIN MatchSquad ms ON ms.player.id = p.id " +
            "JOIN ms.match m " +
            "WHERE m.season.year = :year " +
            "ORDER BY p.firstName ASC")
    List<Player> findBySeasonYear(@Param("year") Integer year);

    // Get all players who played for a specific team
    @Query("SELECT DISTINCT p FROM Player p " +
            "JOIN MatchSquad ms ON ms.player.id = p.id " +
            "WHERE ms.team.teamId = :teamId " +
            "ORDER BY p.firstName ASC")
    List<Player> findByTeamId(@Param("teamId") Integer teamId);

        // Get all players who played for a specific team in a specific season
        @Query("SELECT DISTINCT p FROM Player p " +
                "JOIN MatchSquad ms ON ms.player.id = p.id " +
                "JOIN ms.match m " +
                "WHERE ms.team.teamId = :teamId AND m.season.year = :seasonYear " +
                "ORDER BY p.firstName ASC")
        List<Player> findByTeamIdAndSeasonId(@Param("teamId") Integer teamId, @Param("seasonYear") Integer seasonYear);

    // Get top run scorers across all matches
        @Query("SELECT new com.cricket.stats.dto.RunScorerDTO(" +
                "p.id, p.fullName, SUM(sb.runs)) " +
                "FROM Player p " +
                "JOIN ScorecardBatter sb ON sb.player.id = p.id " +
                "GROUP BY p.id, p.fullName " +
                "ORDER BY SUM(sb.runs) DESC")
        List<RunScorerDTO> findTopRunScorers();

    // Get top wicket takers across all matches
        @Query("SELECT new com.cricket.stats.dto.WicketTakerDTO(" +
                "p.id, p.fullName, SUM(sb.wickets)) " +
                "FROM Player p " +
                "JOIN ScorecardBowler sb ON sb.player.id = p.id " +
                "GROUP BY p.id, p.fullName " +
                "ORDER BY SUM(sb.wickets) DESC")
        List<WicketTakerDTO> findTopWicketTakers();

    // Get player of the match awards count
        @Query("SELECT new com.cricket.stats.dto.PlayerOfMatchDTO(" +
                "p.id, p.fullName, COUNT(m)) " +
                "FROM Player p " +
                "JOIN Match m ON m.playerOfMatch.id = p.id " +
                "GROUP BY p.id, p.fullName " +
                "ORDER BY COUNT(m) DESC")
        List<PlayerOfMatchDTO> findPlayerOfMatchAwards();
}
