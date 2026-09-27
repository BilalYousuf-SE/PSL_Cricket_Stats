package com.cricket.stats.service;

import com.cricket.stats.entity.Roles;
import com.cricket.stats.repository.MatchSquadRepository;
import com.cricket.stats.repository.PlayerRepository;
import com.cricket.stats.repository.ScorecardBatterRepository;
import com.cricket.stats.repository.ScorecardBowlerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.cricket.stats.dto.BattingStatsDTO;
import com.cricket.stats.dto.BowlingStatsDTO;
import com.cricket.stats.dto.PlayerDTO;
import com.cricket.stats.mapper.PlayerMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final ScorecardBatterRepository batterRepository;
    private final ScorecardBowlerRepository bowlerRepository;
    private final MatchSquadRepository matchSquadRepository;

    public List<PlayerDTO> getAllPlayers() {
        return playerRepository.findAll()
                .stream()
                .map(PlayerMapper::toDTO)
                .toList();
    }

    public PlayerDTO getPlayerById(Long id) {
        return playerRepository.findById(id)
                .map(PlayerMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Player not found"));
    }

    public List<PlayerDTO> searchPlayers(String name) {
        return playerRepository.findByFullNameContainingIgnoreCase(name)
                .stream()
                .map(PlayerMapper::toDTO)
                .toList();
    }

    public List<PlayerDTO> getPlayersByRole(Roles role) {
        return playerRepository.findByRole(role)
                .stream()
                .map(PlayerMapper::toDTO)
                .toList();
    }

    public List<PlayerDTO> getPlayersByTeam(Integer teamId) {
        return playerRepository.findByTeamId(teamId)
                .stream()
                .map(PlayerMapper::toDTO)
                .toList();
    }

    public List<PlayerDTO> getPlayersByTeamAndSeason(Integer teamId, Integer seasonYear) {
        return playerRepository.findByTeamIdAndSeasonId(teamId, seasonYear)
                .stream()
                .map(PlayerMapper::toDTO)
                .toList();
    }

    public List<PlayerDTO> getPlayersBySeason(Integer year) {
        return playerRepository.findBySeasonYear(year)
                .stream()
                .map(PlayerMapper::toDTO)
                .toList();
    }

    public BattingStatsDTO getCareerBattingStats(Long playerId) {
        return batterRepository.getCareerBattingStats(playerId);
    }

    public BowlingStatsDTO getCareerBowlingStats(Long playerId) {
        return bowlerRepository.getCareerBowlingStats(playerId);
    }

    public Long getMatchesPlayed(Long playerId) {
        return matchSquadRepository.countMatchesPlayedByPlayer(playerId);
    }
}