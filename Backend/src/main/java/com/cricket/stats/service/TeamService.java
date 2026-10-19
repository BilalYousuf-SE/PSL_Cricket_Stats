package com.cricket.stats.service;

import com.cricket.stats.repository.TeamRepository;
import org.springframework.stereotype.Service;
import com.cricket.stats.dto.*;
import com.cricket.stats.entity.Team;
import com.cricket.stats.mapper.*;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public List<TeamDTO> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(TeamMapper::toDTO)
                .toList();
    }

    public TeamDTO getTeamById(Integer id) {
        return teamRepository.findById(id)
                .map(TeamMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Team not found"));
    }

    public Long getMatchesPlayed(Integer teamId) {
        return teamRepository.countMatchesPlayedByTeam(teamId);
    }

    public Long getMatchesWon(Integer teamId) {
        return teamRepository.countMatchesWonByTeam(teamId);
    }

    public List<TeamDTO> getTeamsBySeason(Integer year) {
        return teamRepository.findTeamsBySeasonYear(year).stream()
                .map(TeamMapper::toDTO)
                .toList();
    }

    public List<TeamLeaderboardDTO> getTeamLeaderboard() {
        return teamRepository.findAllTimeLeaderboard();
    }

    public TeamDTO createTeam(TeamDTO dto) {
        Team team = new Team();
        team.setTeamName(dto.getTeamName());
        return TeamMapper.toDTO(teamRepository.save(team));
    }

    public TeamDTO updateTeam(Integer id, TeamDTO dto) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found: " + id));
        team.setTeamName(dto.getTeamName());
        return TeamMapper.toDTO(teamRepository.save(team));
    }

    public void deleteTeam(Integer id) {
        if (!teamRepository.existsById(id)) {
            throw new RuntimeException("Team not found: " + id);
        }
        teamRepository.deleteById(id);
    }
}