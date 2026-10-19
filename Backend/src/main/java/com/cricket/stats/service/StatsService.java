package com.cricket.stats.service;

import com.cricket.stats.dto.PlayerOfMatchDTO;
import com.cricket.stats.dto.RunScorerDTO;
import com.cricket.stats.dto.ScorecardBatterDTO;
import com.cricket.stats.dto.ScorecardBowlerDTO;
import com.cricket.stats.dto.StrikeRateDTO;
import com.cricket.stats.dto.TeamLeaderboardDTO;
import com.cricket.stats.dto.WicketTakerDTO;
import com.cricket.stats.entity.ScorecardBatter;
import com.cricket.stats.entity.ScorecardBowler;
import com.cricket.stats.mapper.ScorecardBatterMapper;
import com.cricket.stats.mapper.ScorecardBowlerMapper;
import com.cricket.stats.repository.PlayerRepository;
import com.cricket.stats.repository.ScorecardBatterRepository;
import com.cricket.stats.repository.ScorecardBowlerRepository;
import com.cricket.stats.repository.TeamRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final Pageable TOP_RECORDS = PageRequest.of(0, 10);

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
        List<ScorecardBatter> result = batterRepository.findHighestScores(TOP_RECORDS);
        return result.stream()
                .map(ScorecardBatterMapper::toDTO)
                .toList();
    }

    public List<ScorecardBatterDTO> getHighestScores(Integer year) {
        List<ScorecardBatter> result = batterRepository.findHighestScoresBySeason(year, TOP_RECORDS);
        return result.stream()
                .map(ScorecardBatterMapper::toDTO)
                .toList();
    }

    public List<ScorecardBowlerDTO> getBestBowlingFigures() {
        List<ScorecardBowler> result = bowlerRepository.findBestBowlingFigures(TOP_RECORDS);
        return result.stream()
                .map(ScorecardBowlerMapper::toDTO)
                .toList();
    }

    public List<ScorecardBowlerDTO> getBestBowlingFigures(Integer year) {
        List<ScorecardBowler> result = bowlerRepository.findBestBowlingFiguresBySeason(year, TOP_RECORDS);
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
        return batterRepository.findTopStrikeRatesBySeason(year, TOP_RECORDS);
    }
}
