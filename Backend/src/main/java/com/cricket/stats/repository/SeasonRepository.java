package com.cricket.stats.repository;

import com.cricket.stats.entity.Season;
import com.cricket.stats.entity.Team;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Integer> {

    // Find all seasons ordered by year descending
    List<Season> findAllByOrderByYearDesc();

    // Find season by year e.g. 2023
    Optional<Season> findByYear(Integer year);

    // Check if a season exists
    boolean existsByYear(Integer year);

    @Query("SELECT m.winner FROM " +
     "Match m WHERE m.season.year = :year " +
     "AND m.matchDate = (SELECT MAX(m2.matchDate) FROM Match m2 WHERE m2.season.year = :year)")
    Optional<Team> findSeasonWinner(@Param("year") Integer year);
}
