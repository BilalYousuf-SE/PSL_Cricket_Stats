package com.cricket.stats.repository;

import com.cricket.stats.entity.ScorecardBowler;
import com.cricket.stats.ID.ScorecardBowlerId;
import com.cricket.stats.dto.BowlingStatsDTO;
import com.cricket.stats.dto.WicketTakerDTO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface ScorecardBowlerRepository extends JpaRepository<ScorecardBowler, ScorecardBowlerId> {

    // --- Derived Queries ---

    // Get all bowling scorecards for a player
    List<ScorecardBowler> findByPlayerId(Long playerId);

    // Get bowling scorecard for a specific match
    List<ScorecardBowler> findByMatchMatchIdOrderByWicketsDesc(Integer matchId);

    // Get all innings where a bowler took more than X wickets
    List<ScorecardBowler> findByPlayerIdAndWicketsGreaterThan(Long playerId, Integer wickets);

    // Get all 5 wicket hauls
    List<ScorecardBowler> findByWicketsGreaterThanEqual(Integer wickets);


    // --- JPQL Queries ---

    // Get bowling scorecard for a match with player details
    @Query("SELECT sb FROM ScorecardBowler sb " +
            "JOIN FETCH sb.player " +
            "JOIN FETCH sb.match " +
            "WHERE sb.match.matchId = :matchId " +
            "ORDER BY sb.wickets DESC")
    List<ScorecardBowler> findByMatchIdWithDetails(@Param("matchId") Integer matchId);

    // Get career bowling stats for a player
    @Query("SELECT new com.cricket.stats.dto.BowlingStatsDTO(" +
            "SUM(sb.wickets), SUM(sb.runsConceded), SUM(sb.maidens), " +
            "MAX(sb.wickets), COUNT(sb)) " +
            "FROM ScorecardBowler sb " +
            "WHERE sb.player.id = :playerId")
    BowlingStatsDTO getCareerBowlingStats(@Param("playerId") Long playerId);

    // Get bowling stats for a player in a specific season
    @Query("SELECT new com.cricket.stats.dto.BowlingStatsDTO(" +
            "SUM(sb.wickets), SUM(sb.runsConceded), SUM(sb.maidens), " +
            "MAX(sb.wickets), COUNT(sb)) " +
            "FROM ScorecardBowler sb " +
            "JOIN sb.match m " +
            "WHERE sb.player.id = :playerId AND m.season.year = :year")
    BowlingStatsDTO getBowlingStatsBySeason(@Param("playerId") Long playerId,
                                            @Param("year") Integer year);

    // Get top wicket takers in a season
    @Query("SELECT new com.cricket.stats.dto.WicketTakerDTO(" +
            "sb.player.id, sb.player.fullName, SUM(sb.wickets)) " +
            "FROM ScorecardBowler sb " +
            "JOIN sb.match m " +
            "WHERE m.season.year = :year " +
            "GROUP BY sb.player.id, sb.player.fullName " +
            "ORDER BY SUM(sb.wickets) DESC")
    List<WicketTakerDTO> findTopWicketTakersBySeason(@Param("year") Integer year);

    // Get best bowling figures ever (most wickets, least runs)
    @Query("SELECT sb FROM ScorecardBowler sb " +
            "JOIN FETCH sb.player " +
            "JOIN FETCH sb.match " +
            "ORDER BY sb.wickets DESC, sb.runsConceded ASC")
    List<ScorecardBowler> findBestBowlingFigures(Pageable pageable);

    // Get best bowling figures within a specific season
    @Query("SELECT sb FROM ScorecardBowler sb " +
            "JOIN FETCH sb.player " +
            "JOIN FETCH sb.match m " +
            "WHERE m.season.year = :year " +
            "ORDER BY sb.wickets DESC, sb.runsConceded ASC")
    List<ScorecardBowler> findBestBowlingFiguresBySeason(@Param("year") Integer year, Pageable pageable);
}
