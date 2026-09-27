package com.cricket.stats.controller;

import com.cricket.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.cricket.stats.dto.*;

import java.util.List;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/top-run-scorers")
    public List<RunScorerDTO> getTopRunScorers() {
        return statsService.getTopRunScorers();
    }

    @GetMapping("/top-wicket-takers")
    public List<WicketTakerDTO> getTopWicketTakers() {
        return statsService.getTopWicketTakers();
    }

    @GetMapping("/player-of-match-awards")
    public List<PlayerOfMatchDTO> getPlayerOfMatchAwards() {
        return statsService.getPlayerOfMatchLeaders();
    }

    @GetMapping("/team-leaderboard")
    public List<TeamLeaderboardDTO> getTeamLeaderboard() {
        return statsService.getTeamWinLeaderboard();
    }

    @GetMapping("/highest-scores")
    public List<ScorecardBatterDTO> getHighestScores() {
        return statsService.getHighestScores();
    }

    @GetMapping("/best-bowling")
    public List<ScorecardBowlerDTO> getBestBowlingFigures() {
        return statsService.getBestBowlingFigures();
    }
}