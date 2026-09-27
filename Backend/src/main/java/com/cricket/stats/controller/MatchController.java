package com.cricket.stats.controller;

import com.cricket.stats.entity.*;
import com.cricket.stats.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*; 
import com.cricket.stats.dto.*;

import java.util.List;

@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping
    public List<MatchDTO> getAllMatches() {
        return matchService.getAllMatches();
    }

    @GetMapping("/{id}")
    public MatchDTO getMatchById(@PathVariable Integer id) {
        return matchService.getMatchById(id);
    }

    @GetMapping("/format/{format}")
    public List<MatchDTO> getMatchesByFormat(
            @PathVariable MatchFormat format) {

        return matchService.getMatchesByFormat(format);
    }

    @GetMapping("/team/{teamId}")
    public List<MatchDTO> getMatchesByTeam(
            @PathVariable Integer teamId) {

        return matchService.getMatchesByTeam(teamId);
    }

    @GetMapping("/team/{teamId}/wins")
    public List<MatchDTO> getMatchesWonByTeam(
            @PathVariable Integer teamId) {

        return matchService.getMatchesWonByTeam(teamId);
    }

    @GetMapping("/headtohead/{team1}/{team2}")
    public List<MatchDTO> getHeadToHead(
            @PathVariable Integer team1,
            @PathVariable Integer team2) {

        return matchService.getHeadToHead(team1, team2);
    }

    @GetMapping("/{id}/squad")
    public List<MatchSquadDTO> getMatchSquad(
            @PathVariable Integer id) {

        return matchService.getMatchSquad(id);
    }

    @GetMapping("/{id}/batting-scorecard")
    public List<ScorecardBatterDTO> getBattingScorecard(
            @PathVariable Integer id) {

        return matchService.getBattingScorecard(id);
    }

    @GetMapping("/{id}/deliveries/batter/{playerId}")
    public List<DeliveryDTO> getPlayerDeliveries(
            @PathVariable Integer id, @PathVariable Long playerId) {

        return matchService.getBattingBallbyBall(id, playerId);
    }

    @GetMapping("/{id}/bowling-scorecard")
    public List<ScorecardBowlerDTO> getBowlingScorecard(
            @PathVariable Integer id) {

        return matchService.getBowlingScorecard(id);
    }

    @GetMapping("/{id}/deliveries/bowler/{playerId}")
    public List<DeliveryDTO> getBowlingDeliveries(
            @PathVariable Integer id, @PathVariable Long playerId) {
      return matchService.getBowlingBallbyBall(id, playerId);
    }
    

    @GetMapping("/{id}/deliveries")
    public List<DeliveryDTO> getDeliveries(
            @PathVariable Integer id) {

        return matchService.getDeliveries(id);
    }

    @GetMapping("/{id}/extras")
    public List<InningsExtrasDTO> getExtrasByInnings(@PathVariable Integer id) {
        return matchService.getExtrasByInnings(id);
    }
}