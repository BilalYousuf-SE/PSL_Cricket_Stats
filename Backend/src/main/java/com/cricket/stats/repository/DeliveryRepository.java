package com.cricket.stats.repository;

import com.cricket.stats.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // --- Derived Queries ---

    // Get all deliveries in a match ordered by innings, over, ball
    List<Delivery> findByMatchMatchIdOrderByInningsAscOverNumAscBallNumAsc(Integer matchId);

    List<Delivery> findByMatchMatchIdAndBowlerIdOrderByInningsAscOverNumAscBallNumAsc(Integer matchId, Long bowlerId);

    // Get all deliveries by a specific batter in a match
    List<Delivery> findByMatchMatchIdAndBatterId(Integer matchId, Long batterId);

    // Get all deliveries bowled by a specific bowler in a match
    List<Delivery> findByMatchMatchIdAndBowlerId(Integer matchId, Long bowlerId);

    // Get all wicket deliveries in a match
    List<Delivery> findByMatchMatchIdAndWicketTypeIsNotNull(Integer matchId);

    //Get a ball by ball detail of a specific batsman's batting inning in a match
    List<Delivery> findByMatchMatchIdAndBatterIdOrderByInningsAscOverNumAscBallNumAsc(Integer matchId, Long batterId);


    // --- JPQL Queries ---

    // Get all sixes hit by a player
    @Query("SELECT d FROM Delivery d " +
            "WHERE d.batter.id = :playerId AND d.runsBatter = 6")
    List<Delivery> findSixesByPlayer(@Param("playerId") Long playerId);

    // Get all fours hit by a player
    @Query("SELECT d FROM Delivery d " +
            "WHERE d.batter.id = :playerId AND d.runsBatter = 4")
    List<Delivery> findFoursByPlayer(@Param("playerId") Long playerId);

    // Get all deliveries in a specific over of a match
    @Query("SELECT d FROM Delivery d " +
            "WHERE d.match.matchId = :matchId " +
            "AND d.innings = :innings " +
            "AND d.overNum = :overNum " +
            "ORDER BY d.ballNum ASC")
    List<Delivery> findByMatchInningsAndOver(@Param("matchId") Integer matchId,
                                              @Param("innings") Integer innings,
                                              @Param("overNum") Integer overNum);

    // Count total sixes by a player across all matches
    @Query("SELECT COUNT(d) FROM Delivery d " +
            "WHERE d.batter.id = :playerId AND d.runsBatter = 6")
    Long countSixesByPlayer(@Param("playerId") Long playerId);

    // Count total fours by a player across all matches
    @Query("SELECT COUNT(d) FROM Delivery d " +
            "WHERE d.batter.id = :playerId AND d.runsBatter = 4")
    Long countFoursByPlayer(@Param("playerId") Long playerId);

    // Get all dismissals by a bowler
    @Query("SELECT d FROM Delivery d " +
            "WHERE d.bowler.id = :bowlerId AND d.wicketType IS NOT NULL")
    List<Delivery> findWicketsByBowler(@Param("bowlerId") Long bowlerId);
}
