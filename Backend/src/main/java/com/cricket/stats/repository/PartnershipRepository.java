package com.cricket.stats.repository;

import com.cricket.stats.entity.Partnership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartnershipRepository extends JpaRepository<Partnership, Long> {
    @Override
    @EntityGraph(attributePaths = {"match", "match.season", "batter1", "batter2"})
    Optional<Partnership> findById(Long id);

    @EntityGraph(attributePaths = {"match", "match.season", "batter1", "batter2"})
    @Query("""
            SELECT p FROM Partnership p
            WHERE p.match.matchId = :matchId AND (:innings IS NULL OR p.innings = :innings)
            ORDER BY p.innings ASC, p.wicketNumber ASC, p.partnershipId ASC
            """)
    List<Partnership> findForMatch(@Param("matchId") Integer matchId,
                                   @Param("innings") Integer innings);

    @EntityGraph(attributePaths = {"match", "match.season", "batter1", "batter2"})
    @Query("""
            SELECT p FROM Partnership p LEFT JOIN p.match.season s
            WHERE (:matchId IS NULL OR p.match.matchId = :matchId)
              AND (:innings IS NULL OR p.innings = :innings)
              AND (:seasonYear IS NULL OR s.year = :seasonYear)
              AND (:playerId IS NULL OR p.batter1.id = :playerId OR p.batter2.id = :playerId)
              AND (:partnerId IS NULL
                   OR (p.batter1.id = :playerId AND p.batter2.id = :partnerId)
                   OR (p.batter2.id = :playerId AND p.batter1.id = :partnerId))
            ORDER BY p.match.matchDate DESC, p.match.matchId DESC,
                     p.innings ASC, p.wicketNumber ASC, p.partnershipId ASC
            """)
    Page<Partnership> search(@Param("matchId") Integer matchId, @Param("innings") Integer innings,
                             @Param("seasonYear") Integer seasonYear, @Param("playerId") Long playerId,
                             @Param("partnerId") Long partnerId, Pageable pageable);
}
