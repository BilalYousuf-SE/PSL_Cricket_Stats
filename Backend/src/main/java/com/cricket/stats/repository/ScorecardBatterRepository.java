package com.cricket.stats.repository;

import com.cricket.stats.entity.ScorecardBatter;
import com.cricket.stats.ID.ScorecardBatterId;
import com.cricket.stats.dto.BattingStatsDTO;
import com.cricket.stats.dto.RunScorerDTO;
import com.cricket.stats.dto.StrikeRateDTO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface ScorecardBatterRepository extends JpaRepository<ScorecardBatter, ScorecardBatterId> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ScorecardBatter row WHERE row.match.matchId = :matchId")
    void deleteByMatchId(@Param("matchId") Integer matchId);

    // --- Derived Queries ---

    // Get all batting scorecards for a player
    List<ScorecardBatter> findByPlayerId(Long playerId);

    // Get batting scorecard for a specific match
    List<ScorecardBatter> findByMatchMatchIdOrderByRunsDesc(Integer matchId);

    // Get all innings where a player scored more than X runs
    List<ScorecardBatter> findByPlayerIdAndRunsGreaterThan(Long playerId, Integer runs);

    // Get all centuries (100+ runs)
    List<ScorecardBatter> findByRunsGreaterThanEqual(Integer runs);


    // --- JPQL Queries ---

    // Get batting scorecard for a match with player details
    @Query("SELECT sb FROM ScorecardBatter sb " +
            "JOIN FETCH sb.player " +
            "JOIN FETCH sb.match " +
            "WHERE sb.match.matchId = :matchId " +
            "ORDER BY sb.battingPosition ASC")
    List<ScorecardBatter> findByMatchIdWithDetails(@Param("matchId") Integer matchId);

    // Get career batting stats for a player
    @Query("SELECT new com.cricket.stats.dto.BattingStatsDTO(" +
            "SUM(sb.runs), SUM(sb.ballsFaced), SUM(sb.fours), " +
            "SUM(sb.sixes), MAX(sb.runs), COUNT(sb)) " +
            "FROM ScorecardBatter sb " +
            "WHERE sb.player.id = :playerId")
    BattingStatsDTO getCareerBattingStats(@Param("playerId") Long playerId);

    // Get batting stats for a player in a specific season
    @Query("SELECT new com.cricket.stats.dto.BattingStatsDTO(" +
            "SUM(sb.runs), SUM(sb.ballsFaced), SUM(sb.fours), " +
            "SUM(sb.sixes), MAX(sb.runs), COUNT(sb)) " +
            "FROM ScorecardBatter sb " +
            "JOIN sb.match m " +
            "WHERE sb.player.id = :playerId AND m.season.year = :year")
    BattingStatsDTO getBattingStatsBySeason(@Param("playerId") Long playerId,
                                            @Param("year") Integer year);

    // Get top run scorers in a season
    @Query("SELECT new com.cricket.stats.dto.RunScorerDTO(" +
            "sb.player.id, sb.player.fullName, SUM(sb.runs)) " +
            "FROM ScorecardBatter sb " +
            "JOIN sb.match m " +
            "WHERE m.season.year = :year " +
            "GROUP BY sb.player.id, sb.player.fullName " +
            "ORDER BY SUM(sb.runs) DESC")
    List<RunScorerDTO> findTopRunScorersBySeason(@Param("year") Integer year);

    // Get highest individual scores ever
    @Query("SELECT sb FROM ScorecardBatter sb " +
            "JOIN FETCH sb.player " +
            "JOIN FETCH sb.match " +
            "ORDER BY sb.runs DESC")
    List<ScorecardBatter> findHighestScores(Pageable pageable);

    // Get highest individual scores within a specific season
    @Query("SELECT sb FROM ScorecardBatter sb " +
            "JOIN FETCH sb.player " +
            "JOIN FETCH sb.match m " +
            "WHERE m.season.year = :year " +
            "ORDER BY sb.runs DESC")
    List<ScorecardBatter> findHighestScoresBySeason(@Param("year") Integer year, Pageable pageable);

    // Top strike rates ACROSS THE WHOLE SEASON per player: sums each player's
    // runs and balls faced over every innings they played that season, then
    // ranks by the combined (season-long) strike rate — not any single
    // innings' strike rate. HAVING clause requires at least 10 balls faced
    // across the season to qualify (tune as needed).
    @Query("""
            SELECT new com.cricket.stats.dto.StrikeRateDTO(
                sb.player.id, sb.player.fullName, SUM(sb.runs), SUM(sb.ballsFaced),
                (SUM(sb.runs) * 100.0 / SUM(sb.ballsFaced)))
            FROM ScorecardBatter sb
            JOIN sb.match m
            WHERE m.season.year = :year
            GROUP BY sb.player.id, sb.player.fullName
            HAVING SUM(sb.ballsFaced) >= 10
            ORDER BY (SUM(sb.runs) * 100.0 / SUM(sb.ballsFaced)) DESC
            """)
    List<StrikeRateDTO> findTopStrikeRatesBySeason(@Param("year") Integer year, Pageable pageable);

}