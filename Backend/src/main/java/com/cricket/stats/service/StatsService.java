package com.cricket.stats.service;
import org.springframework.data.domain.PageRequest;

import com.cricket.stats.entity.ScorecardBatter;
import com.cricket.stats.entity.ScorecardBowler;
import com.cricket.stats.repository.PlayerRepository;
import com.cricket.stats.repository.ScorecardBatterRepository;
import com.cricket.stats.repository.ScorecardBowlerRepository;
import com.cricket.stats.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.cricket.stats.dto.*;
import com.cricket.stats.mapper.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final ScorecardBatterRepository batterRepository;
    private final ScorecardBowlerRepository bowlerRepository;

    public List<RunScorerDTO> getTopRunScorers() {
        return playerRepository.findTopRunScorers();
    }

    public List<WicketTakerDTO> getTopWicketTakers() {
        return playerRepository.findTopWicketTakers();
    }

    public List<PlayerOfMatchDTO> getPlayerOfMatchLeaders() {
        return playerRepository.findPlayerOfMatchAwards();
    }

    public List<TeamLeaderboardDTO> getTeamWinLeaderboard() {
        return teamRepository.findAllTimeLeaderboard();
    }

    public List<RunScorerDTO> getSeasonTopRunScorers(Integer year) {
        return batterRepository.findTopRunScorersBySeason(year);
    }

    public List<WicketTakerDTO> getSeasonTopWicketTakers(Integer year) {
        return bowlerRepository.findTopWicketTakersBySeason(year);
    }

    public List<ScorecardBatterDTO> getHighestScores() {
        List<ScorecardBatter> result = batterRepository.findHighestScores(PageRequest.of(0, 10));
        return result.stream()
                .map(ScorecardBatterMapper::toDTO)
                .toList();
    }

    public List<ScorecardBatterDTO> getHighestScores(Integer year) {
        List<ScorecardBatter> result = batterRepository.findHighestScoresBySeason(year, PageRequest.of(0, 10));
        return result.stream()
                .map(ScorecardBatterMapper::toDTO)
                .toList();
    }

    public List<ScorecardBowlerDTO> getBestBowlingFigures() {
        List<ScorecardBowler> result = bowlerRepository.findBestBowlingFigures(PageRequest.of(0, 10));
        return result.stream()
                .map(ScorecardBowlerMapper::toDTO)
                .toList();
    }

    public List<ScorecardBowlerDTO> getBestBowlingFigures(Integer year) {
        List<ScorecardBowler> result = bowlerRepository.findBestBowlingFiguresBySeason(year, PageRequest.of(0, 10));
        return result.stream()
                .map(ScorecardBowlerMapper::toDTO)
                .toList();
    }

    public List<TeamLeaderboardDTO> getSeasonTeamLeaderboard(Integer year) {
        return teamRepository.findTeamLeaderboardBySeason(year);
    }

    // Now returns season-aggregated strike rates (one row per player, summed
    // across every innings they played that season) rather than a single
    // best innings.
    public List<StrikeRateDTO> getTopStrikeRates(Integer year) {
        return batterRepository.findTopStrikeRatesBySeason(year, PageRequest.of(0, 10));
    }
}