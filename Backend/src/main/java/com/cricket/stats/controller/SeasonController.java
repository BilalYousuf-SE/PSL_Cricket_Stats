package com.cricket.stats.controller;

import com.cricket.stats.service.SeasonService;
import com.cricket.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.cricket.stats.dto.*;

import java.util.List;

@RestController
@RequestMapping("/season")
@RequiredArgsConstructor
public class SeasonController {

    private final SeasonService seasonService;
    private final StatsService statsService;

    @GetMapping
    public List<SeasonDTO> getAllSeasons() {
        return seasonService.getAllSeasons();
    }

    @GetMapping("/{seasonYear}/matches")
    public List<MatchDTO> getMatchesBySeasonYear(@PathVariable Integer seasonYear) {
        return seasonService.getMatchesBySeasonYear(seasonYear);
    }

    @GetMapping("/{seasonYear}/winner")
    public TeamDTO getSeasonWinner(@PathVariable Integer seasonYear) {
        return seasonService.getSeasonWinner(seasonYear);
    }

    @GetMapping("/{year}/top-runs")
    public List<RunScorerDTO> getSeasonTopRunScorers(
            @PathVariable Integer year) {

        return statsService.getSeasonTopRunScorers(year);
    }

    @GetMapping("/{year}/top-wickets")
    public List<WicketTakerDTO> getSeasonTopWicketTakers(
            @PathVariable Integer year) {

        return statsService.getSeasonTopWicketTakers(year);
    }

    @GetMapping("/{year}/leaderboard")
    public List<TeamLeaderboardDTO> getSeasonTeamLeaderboard(@PathVariable Integer year) {
        return statsService.getSeasonTeamLeaderboard(year);
    }

    @GetMapping("/{year}/highest-scores")
    public List<ScorecardBatterDTO> getHighestScores(@PathVariable Integer year) {
        return statsService.getHighestScores(year);
    }

    @GetMapping("/{year}/best-bowling")
    public List<ScorecardBowlerDTO> getBestBowlingFigures(@PathVariable Integer year) {
        return statsService.getBestBowlingFigures(year);
    }

    @GetMapping("/{year}/top-strike-rates")
    public List<StrikeRateDTO> getTopStrikeRates(@PathVariable Integer year) {
        return statsService.getTopStrikeRates(year);
    }

    @PostMapping
    public SeasonDTO createSeason(@RequestBody SeasonDTO dto) {
        return seasonService.createSeason(dto);
    }

    @PutMapping("/{id}")
    public SeasonDTO updateSeason(@PathVariable Integer id, @RequestBody SeasonDTO dto) {
        return seasonService.updateSeason(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteSeason(@PathVariable Integer id) {
        seasonService.deleteSeason(id);
    }
}