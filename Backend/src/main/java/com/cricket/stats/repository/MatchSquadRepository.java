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

    // Get full squad for a match
    List<MatchSquad> findByMatchMatchId(Integer matchId);

    // Get all matches a player was part of
    List<MatchSquad> findByPlayerId(Long playerId);

    // Get squad for a specific team in a match
    List<MatchSquad> findByMatchMatchIdAndTeamTeamId(Integer matchId, Integer teamId);

    // Get all captains across all matches
    List<MatchSquad> findByIsCaptainTrue();

    // Get all wicket keepers across all matches
    List<MatchSquad> findByIsKeeperTrue();


    // --- JPQL Queries ---

    // Get full squad for a match with player details
    @Query("SELECT ms FROM MatchSquad ms " +
            "JOIN FETCH ms.player " +
            "JOIN FETCH ms.team " +
            "WHERE ms.match.matchId = :matchId")
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
