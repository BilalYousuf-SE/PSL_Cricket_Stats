package com.cricket.stats.controller;

import com.cricket.stats.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.cricket.stats.dto.*;

import java.util.List;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public List<TeamDTO> getAllTeams() {
        return teamService.getAllTeams();
    }

    @GetMapping("/{id}")
    public TeamDTO getTeamById(@PathVariable Integer id) {
        return teamService.getTeamById(id);
    }

    @GetMapping("/{id}/matches-played")
    public Long getMatchesPlayed(@PathVariable Integer id) {
        return teamService.getMatchesPlayed(id);
    }

    @GetMapping("/{id}/wins")
    public Long getMatchesWon(@PathVariable Integer id) {
        return teamService.getMatchesWon(id);
    }

    @GetMapping("/season/{year}")
    public List<TeamDTO> getTeamsBySeason(@PathVariable Integer year) {
        return teamService.getTeamsBySeason(year);
    }

    @PostMapping
    public TeamDTO createTeam(@RequestBody TeamDTO dto) {
        return teamService.createTeam(dto);
    }

    @PutMapping("/{id}")
    public TeamDTO updateTeam(@PathVariable Integer id, @RequestBody TeamDTO dto) {
        return teamService.updateTeam(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteTeam(@PathVariable Integer id) {
        teamService.deleteTeam(id);
    }
}