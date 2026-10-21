package com.cricket.stats.repository;

import com.cricket.stats.entity.MatchSquad;
import com.cricket.stats.ID.MatchSquadId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchSquadRepository extends JpaRepository<MatchSquad, MatchSquadId> {

    // --- Derived Queries ---

    // Keep every squad retrieval in batting-role order, independent of bowling style.
    String SQUAD_ORDER = " ORDER BY CASE ms.player.role " +
            "WHEN com.cricket.stats.entity.Roles.Batsman THEN 0 " +
            "WHEN com.cricket.stats.entity.Roles.Keeper THEN 1 " +
            "WHEN com.cricket.stats.entity.Roles.Allrounder THEN 2 " +
            "WHEN com.cricket.stats.entity.Roles.Baller THEN 3 ELSE 4 END, " +
            "ms.player.fullName ASC, ms.player.id ASC";

    // Get full squad for a match
    @Query("SELECT ms FROM MatchSquad ms JOIN FETCH ms.player JOIN FETCH ms.team " +
            "WHERE ms.match.matchId = :matchId" + SQUAD_ORDER)
    List<MatchSquad> findByMatchMatchId(@Param("matchId") Integer matchId);

    // Get all matches a player was part of
    List<MatchSquad> findByPlayerId(Long playerId);

    // Get squad for a specific team in a match
    @Query("SELECT ms FROM MatchSquad ms JOIN FETCH ms.player JOIN FETCH ms.team " +
            "WHERE ms.match.matchId = :matchId AND ms.team.teamId = :teamId" + SQUAD_ORDER)
    List<MatchSquad> findByMatchMatchIdAndTeamTeamId(@Param("matchId") Integer matchId,
                                                  @Param("teamId") Integer teamId);

    // Get all captains across all matches
    List<MatchSquad> findByIsCaptainTrue();

    // Get all wicket keepers across all matches
    List<MatchSquad> findByIsKeeperTrue();


    // --- JPQL Queries ---

    // Get full squad for a match with player details
    @Query("SELECT ms FROM MatchSquad ms " +
            "JOIN FETCH ms.player " +
            "JOIN FETCH ms.team " +
            "WHERE ms.match.matchId = :matchId" + SQUAD_ORDER)
    List<MatchSquad> findSquadByMatchId(@Param("matchId") Integer matchId);

    // Get all matches a player played for a specific team
    @Query("SELECT ms FROM MatchSquad ms " +
            "WHERE ms.player.id = :playerId " +
            "AND ms.team.teamId = :teamId")
    List<MatchSquad> findByPlayerAndTeam(@Param("playerId") Long playerId,
                                          @Param("teamId") Integer teamId);

    // Count total matches played by a player
    @Query("SELECT COUNT(ms) FROM MatchSquad ms " +
            "WHERE ms.player.id = :playerId AND ms.isPlaying = true")
    Long countMatchesPlayedByPlayer(@Param("playerId") Long playerId);
}
