package com.cricket.stats.repository;

import com.cricket.stats.entity.Match;
import com.cricket.stats.entity.MatchFormat;
import com.cricket.stats.entity.MatchSquad;
import com.cricket.stats.entity.ResultType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Match m WHERE m.matchId = :matchId")
    Optional<Match> findForUpdate(@Param("matchId") Integer matchId);

    // --- Derived Queries ---

    // Get all matches in a season
    List<Match> findBySeasonYear(Integer year);

    // Get all matches at a specific venue
    List<Match> findByVenueContainingIgnoreCase(String venue);

    // Get all matches by format
    List<Match> findByFormat(MatchFormat format);

    // Get all matches won by a specific result type e.g. runs, wickets
    List<Match> findByResultType(ResultType resultType);

    // Get matches between two dates
    List<Match> findByMatchDateBetween(LocalDate start, LocalDate end);

    // Get all matches sorted by date descending (latest first)
    List<Match> findAllByOrderByMatchDateDesc();


    // --- JPQL Queries ---

    // Get all matches a specific team played in
    @Query("SELECT m FROM Match m " +
            "WHERE m.teamA.teamId = :teamId OR m.teamB.teamId = :teamId " +
            "ORDER BY m.matchDate DESC")
    List<Match> findMatchesByTeamId(@Param("teamId") Integer teamId);

    // Get all matches a specific team won
    @Query("SELECT m FROM Match m " +
            "WHERE m.winner.teamId = :teamId " +
            "ORDER BY m.matchDate DESC")
    List<Match> findMatchesWonByTeam(@Param("teamId") Integer teamId);

    // Get head to head matches between two teams
    @Query("SELECT m FROM Match m " +
            "WHERE (m.teamA.teamId = :team1 AND m.teamB.teamId = :team2) " +
            "OR (m.teamA.teamId = :team2 AND m.teamB.teamId = :team1) " +
            "ORDER BY m.matchDate DESC")
    List<Match> findHeadToHead(@Param("team1") Integer team1,
                               @Param("team2") Integer team2);

    // Get matches where a specific player won player of the match
    @Query("SELECT m FROM Match m " +
            "WHERE m.playerOfMatch.id = :playerId " +
            "ORDER BY m.matchDate DESC")
    List<Match> findByPlayerOfMatch(@Param("playerId") Long playerId);

        @Query("SELECT ms FROM MatchSquad ms " +
                "JOIN FETCH ms.match m " +
                "JOIN FETCH ms.team " +
                "JOIN FETCH m.teamA " +
                "JOIN FETCH m.teamB " +
                "LEFT JOIN FETCH m.winner " +
                "LEFT JOIN FETCH m.season " +
                "WHERE ms.player.id = :playerId " +
                "ORDER BY m.matchDate DESC")
        List<MatchSquad> findMatchesByPlayerId(@Param("playerId") Long playerId);
}
