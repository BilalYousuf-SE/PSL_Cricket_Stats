package com.cricket.stats.service;

import com.cricket.stats.dto.TeamDTO;
import com.cricket.stats.entity.Team;
import com.cricket.stats.mapper.TeamMapper;
import com.cricket.stats.repository.TeamRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;

    public List<TeamDTO> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(TeamMapper::toDTO)
                .toList();
    }

    public TeamDTO getTeamById(Integer id) {
        return teamRepository.findById(id)
                .map(TeamMapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Team not found"));
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

    @Transactional
    public TeamDTO createTeam(TeamDTO dto) {
        Team team = new Team();
        team.setTeamName(dto.getTeamName());
        return TeamMapper.toDTO(teamRepository.save(team));
    }

    @Transactional
    public TeamDTO updateTeam(Integer id, TeamDTO dto) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Team not found: " + id));
        team.setTeamName(dto.getTeamName());
        return TeamMapper.toDTO(teamRepository.save(team));
    }

    @Transactional
    public void deleteTeam(Integer id) {
        if (!teamRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Team not found: " + id);
        }
        teamRepository.deleteById(id);
    }
}
