package com.cricket.stats.controller;

import com.cricket.stats.entity.Roles;
import com.cricket.stats.service.PlayerService;
import com.cricket.stats.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.cricket.stats.dto.BattingStatsDTO;
import com.cricket.stats.dto.BowlingStatsDTO;
import com.cricket.stats.dto.PlayerDTO;
import com.cricket.stats.dto.PlayerMatchDTO;

import java.util.List;

@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;
    private final MatchService matchService;

    @GetMapping
    public List<PlayerDTO> getAllPlayers() {
        return playerService.getAllPlayers();
    }

    @GetMapping("/{id}")
    public PlayerDTO getPlayerById(@PathVariable Long id) {
        return playerService.getPlayerById(id);
    }

    @GetMapping("/search")
    public List<PlayerDTO> searchPlayers(@RequestParam String name) {
        return playerService.searchPlayers(name);
    }

    @GetMapping("/role/{role}")
    public List<PlayerDTO> getPlayersByRole(@PathVariable Roles role) {
        return playerService.getPlayersByRole(role);
    }

    @GetMapping("/team/{teamId}")
    public List<PlayerDTO> getPlayersByTeam(@PathVariable Integer teamId) {
        return playerService.getPlayersByTeam(teamId);
    }

    @GetMapping("/team/{teamId}/{seasonYear}")
    public List<PlayerDTO> getPlayersByTeamAndSeason(@PathVariable Integer teamId, @PathVariable Integer seasonYear) {
        return playerService.getPlayersByTeamAndSeason(teamId, seasonYear);
    }

    @GetMapping("/{id}/batting")
    public BattingStatsDTO getBattingStats(@PathVariable Long id) {
        return playerService.getCareerBattingStats(id);
    }

    @GetMapping("/{id}/bowling")
    public BowlingStatsDTO getBowlingStats(@PathVariable Long id) {
        return playerService.getCareerBowlingStats(id);
    }

    @GetMapping("/{id}/matches-count")
    public Long getMatchesPlayed(@PathVariable Long id) {
        return playerService.getMatchesPlayed(id);
    }

    @GetMapping("/{id}/matches")
    public List<PlayerMatchDTO> getMatches(@PathVariable Long id) {
        return matchService.getMatchesByPlayer(id);
    }
}