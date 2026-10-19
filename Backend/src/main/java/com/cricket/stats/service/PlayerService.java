package com.cricket.stats.service;

import com.cricket.stats.dto.BattingStatsDTO;
import com.cricket.stats.dto.BowlingStatsDTO;
import com.cricket.stats.dto.PlayerDTO;
import com.cricket.stats.entity.Player;
import com.cricket.stats.entity.Roles;
import com.cricket.stats.mapper.PlayerMapper;
import com.cricket.stats.repository.MatchSquadRepository;
import com.cricket.stats.repository.PlayerRepository;
import com.cricket.stats.repository.ScorecardBatterRepository;
import com.cricket.stats.repository.ScorecardBowlerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Player not found"));
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

    @Transactional
    public PlayerDTO createPlayer(PlayerDTO dto) {
        Player player = new Player();
        applyDtoToEntity(dto, player);
        return PlayerMapper.toDTO(playerRepository.save(player));
    }

    @Transactional
    public PlayerDTO updatePlayer(Long id, PlayerDTO dto) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Player not found: " + id));
        applyDtoToEntity(dto, player);
        return PlayerMapper.toDTO(playerRepository.save(player));
    }

    @Transactional
    public void deletePlayer(Long id) {
        if (!playerRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Player not found: " + id);
        }
        playerRepository.deleteById(id);
    }

    // fullName is computed here rather than trusted from the client, since it's
    // a real stored column with no automatic derivation elsewhere in the code.
    private void applyDtoToEntity(PlayerDTO dto, Player player) {
        player.setFirstName(dto.getFirstName().strip());
        player.setLastName(dto.getLastName().strip());
        player.setFullName(player.getFirstName() + " " + player.getLastName());
        player.setAge(dto.getAge());
        player.setRole(dto.getRole());
        player.setNationality(dto.getNationality());
        player.setBattingStyle(dto.getBattingStyle());
        player.setBowlingStyle(dto.getBowlingStyle());
    }
}
