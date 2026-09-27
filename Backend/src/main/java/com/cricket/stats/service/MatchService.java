package com.cricket.stats.service;

import com.cricket.stats.entity.*;
import com.cricket.stats.repository.*;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import org.springframework.stereotype.Service;
import com.cricket.stats.dto.*;
import com.cricket.stats.mapper.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchSquadRepository matchSquadRepository;
    private final ScorecardBatterRepository batterRepository;
    private final ScorecardBowlerRepository bowlerRepository;
    private final DeliveryRepository deliveryRepository;

    public List<MatchDTO> getAllMatches() {
        return matchRepository.findAllByOrderByMatchDateDesc()
                .stream()
                .map(MatchMapper::toDTO)
                .toList();
    }

    public MatchDTO getMatchById(Integer id) {
        return matchRepository.findById(id)
                .map(MatchMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Match not found"));
    }

    public List<MatchDTO> getMatchesByFormat(MatchFormat format) {
        return matchRepository.findByFormat(format)
                .stream()
                .map(MatchMapper::toDTO)
                .toList();
    }

    public List<MatchDTO> getMatchesByTeam(Integer teamId) {
        return matchRepository.findMatchesByTeamId(teamId)
                .stream()
                .map(MatchMapper::toDTO)
                .toList();
    }

    public List<PlayerMatchDTO> getMatchesByPlayer(Long playerId) {
    return matchRepository.findMatchesByPlayerId(playerId)
            .stream()
            .map(PlayerMatchMapper::toDTO)
            .toList();
    }

    public List<MatchDTO> getMatchesWonByTeam(Integer teamId) {
        return matchRepository.findMatchesWonByTeam(teamId)
                .stream()
                .map(MatchMapper::toDTO)
                .toList();
    }

    public List<MatchDTO> getHeadToHead(Integer team1, Integer team2) {
        return matchRepository.findHeadToHead(team1, team2)
                .stream()
                .map(MatchMapper::toDTO)
                .toList();
    }

    public List<MatchSquadDTO> getMatchSquad(Integer matchId) {
        return matchSquadRepository.findSquadByMatchId(matchId)
                .stream()
                .map(MatchSquadMapper::toDTO)
                .toList();
    }

    public List<ScorecardBatterDTO> getBattingScorecard(Integer matchId) {
        return batterRepository.findByMatchIdWithDetails(matchId)
                .stream()
                .map(ScorecardBatterMapper::toDTO)
                .toList();
    }

    public List<DeliveryDTO> getBattingBallbyBall(Integer matchId, Long playerId) {
        return deliveryRepository.findByMatchMatchIdAndBatterIdOrderByInningsAscOverNumAscBallNumAsc(matchId, playerId)
                .stream()
                .map(DeliveryMapper::toDTO)
                .toList();
    }

    public List<ScorecardBowlerDTO> getBowlingScorecard(Integer matchId) {
        return bowlerRepository.findByMatchIdWithDetails(matchId)
                .stream()
                .map(ScorecardBowlerMapper::toDTO)
                .toList();
    }

    public List<DeliveryDTO> getBowlingBallbyBall(Integer matchId, Long playerId) {
        return deliveryRepository.findByMatchMatchIdAndBowlerIdOrderByInningsAscOverNumAscBallNumAsc(matchId, playerId)
                .stream()
                .map(DeliveryMapper::toDTO)
                .toList();
    }

    public List<DeliveryDTO> getDeliveries(Integer matchId) {
        return deliveryRepository
                .findByMatchMatchIdOrderByInningsAscOverNumAscBallNumAsc(matchId)
                .stream()
                .map(DeliveryMapper::toDTO)
                .toList();
    }

    public List<InningsExtrasDTO> getExtrasByInnings(Integer matchId) {
        List<DeliveryDTO> deliveries = getDeliveries(matchId);

        Map<Integer, List<DeliveryDTO>> byInnings = deliveries.stream()
                .collect(Collectors.groupingBy(DeliveryDTO::getInnings));

        return byInnings.entrySet().stream()
                .map(entry -> {
                    Integer innings = entry.getKey();
                    List<DeliveryDTO> balls = entry.getValue();

                    long totalExtras = balls.stream()
                            .mapToLong(d -> d.getRunsExtras() != null ? d.getRunsExtras() : 0)
                            .sum();

                    Map<String, Long> byType = balls.stream()
                            .filter(d -> d.getExtrasType() != null && !d.getExtrasType().isBlank()
                                    && d.getRunsExtras() != null && d.getRunsExtras() > 0)
                            .collect(Collectors.groupingBy(
                                    DeliveryDTO::getExtrasType,
                                    Collectors.summingLong(DeliveryDTO::getRunsExtras)
                            ));

                    return new InningsExtrasDTO(innings, totalExtras, byType);
                })
                .sorted(Comparator.comparing(InningsExtrasDTO::getInnings))
                .collect(Collectors.toList());
    }
}
