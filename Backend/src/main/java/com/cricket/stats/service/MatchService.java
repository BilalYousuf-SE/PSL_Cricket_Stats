package com.cricket.stats.service;

import com.cricket.stats.ID.MatchSquadId;
import com.cricket.stats.dto.DeliveryDTO;
import com.cricket.stats.dto.InningsExtrasDTO;
import com.cricket.stats.dto.DeliveryRequestDTO;
import com.cricket.stats.dto.MatchDTO;
import com.cricket.stats.dto.MatchRequestDTO;
import com.cricket.stats.dto.MatchSquadDTO;
import com.cricket.stats.dto.MatchSquadRequestDTO;
import com.cricket.stats.dto.PlayerMatchDTO;
import com.cricket.stats.dto.ScorecardBatterDTO;
import com.cricket.stats.dto.ScorecardBowlerDTO;
import com.cricket.stats.entity.Delivery;
import com.cricket.stats.entity.Match;
import com.cricket.stats.entity.MatchFormat;
import com.cricket.stats.entity.MatchSquad;
import com.cricket.stats.entity.Player;
import com.cricket.stats.entity.Team;
import com.cricket.stats.mapper.DeliveryMapper;
import com.cricket.stats.mapper.MatchMapper;
import com.cricket.stats.mapper.MatchSquadMapper;
import com.cricket.stats.mapper.PlayerMatchMapper;
import com.cricket.stats.mapper.ScorecardBatterMapper;
import com.cricket.stats.mapper.ScorecardBowlerMapper;
import com.cricket.stats.repository.DeliveryRepository;
import com.cricket.stats.repository.MatchRepository;
import com.cricket.stats.repository.MatchSquadRepository;
import com.cricket.stats.repository.PlayerRepository;
import com.cricket.stats.repository.ScorecardBatterRepository;
import com.cricket.stats.repository.ScorecardBowlerRepository;
import com.cricket.stats.repository.SeasonRepository;
import com.cricket.stats.repository.TeamRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private final ScorecardCalculator scorecardCalculator;
    private final MatchRepository matchRepository;
    private final MatchSquadRepository matchSquadRepository;
    private final ScorecardBatterRepository batterRepository;
    private final ScorecardBowlerRepository bowlerRepository;
    private final DeliveryRepository deliveryRepository;
    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;
    private final PlayerRepository playerRepository;

    public List<MatchDTO> getAllMatches() {
        return matchRepository.findAllByOrderByMatchDateDesc()
                .stream()
                .map(MatchMapper::toDTO)
                .toList();
    }

    public MatchDTO getMatchById(Integer id) {
        return matchRepository.findById(id)
                .map(MatchMapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Match not found"));
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
        Map<Integer, List<DeliveryDTO>> innings = getDeliveries(matchId).stream()
                .collect(java.util.stream.Collectors.groupingBy(DeliveryDTO::getInnings));
        return innings.entrySet().stream().map(entry -> {
            long total = entry.getValue().stream().mapToLong(DeliveryDTO::getRunsExtras).sum();
            Map<String, Long> byType = entry.getValue().stream()
                    .filter(ball -> ball.getExtrasType() != null && !ball.getExtrasType().isBlank()
                            && ball.getRunsExtras() > 0)
                    .collect(java.util.stream.Collectors.groupingBy(DeliveryDTO::getExtrasType,
                            java.util.stream.Collectors.summingLong(DeliveryDTO::getRunsExtras)));
            return new InningsExtrasDTO(entry.getKey(), total, byType);
        }).sorted(java.util.Comparator.comparing(InningsExtrasDTO::getInnings)).toList();
    }

    // ---------------- Match CRUD ----------------

    @Transactional
    public MatchDTO createMatch(MatchRequestDTO request) {
        Match match = new Match();
        applyMatchRequest(match, request);
        return MatchMapper.toDTO(matchRepository.save(match));
    }

    @Transactional
    public MatchDTO updateMatch(Integer id, MatchRequestDTO request) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Match not found"));
        applyMatchRequest(match, request);
        return MatchMapper.toDTO(matchRepository.save(match));
    }

    @Transactional
    public void deleteMatch(Integer id) {
        if (!matchRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Match not found");
        }
        matchRepository.deleteById(id);
    }

    private void applyMatchRequest(Match match, MatchRequestDTO request) {
        match.setMatchDate(request.getMatchDate());
        match.setVenue(request.getVenue());
        match.setFormat(request.getFormat());
        match.setTossDecision(request.getTossDecision());
        match.setResultMargin(request.getResultMargin());
        match.setResultType(request.getResultType());

        if (request.getTeamAId().equals(request.getTeamBId())) {
            throw new ResponseStatusException(BAD_REQUEST, "A match requires two different teams");
        }
        match.setTeamA(teamRepository.findById(request.getTeamAId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Team A not found")));
        match.setTeamB(teamRepository.findById(request.getTeamBId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Team B not found")));
        match.setSeason(seasonRepository.findById(request.getSeasonId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Season not found")));
        validateMatchTeam(request.getWinnerId(), request, "Winner");
        validateMatchTeam(request.getTossWinnerId(), request, "Toss winner");
        match.setWinner(request.getWinnerId() == null ? null
                : teamRepository.findById(request.getWinnerId()).orElseThrow());
        match.setTossWinner(request.getTossWinnerId() == null ? null
                : teamRepository.findById(request.getTossWinnerId()).orElseThrow());
        match.setPlayerOfMatch(request.getPlayerOfMatchId() == null ? null
                : playerRepository.findById(request.getPlayerOfMatchId())
                        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Player of the match not found")));
    }

    private void validateMatchTeam(Integer teamId, MatchRequestDTO request, String label) {
        if (teamId != null && !teamId.equals(request.getTeamAId()) && !teamId.equals(request.getTeamBId())) {
            throw new ResponseStatusException(BAD_REQUEST, label + " must be one of the match teams");
        }
    }

    // ---------------- Match Squad CRUD ----------------

    @Transactional
    public MatchSquadDTO addSquadMember(Integer matchId, MatchSquadRequestDTO request) {
        Match match = lockMatch(matchId);
        if (request.getPlayerId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Player ID is required");
        }
        if (matchSquadRepository.existsById(squadId(matchId, request.getPlayerId()))) {
            throw new ResponseStatusException(CONFLICT, "This player is already in the match squad");
        }
        validateSquadTeam(match, request.getTeamId());
        Player player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Player not found"));
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Team not found"));

        MatchSquad squad = new MatchSquad();
        squad.setMatch(match);
        squad.setPlayer(player);
        squad.setTeam(team);
        squad.setIsCaptain(Boolean.TRUE.equals(request.getIsCaptain()));
        squad.setIsKeeper(Boolean.TRUE.equals(request.getIsKeeper()));
        squad.setIsPlaying(request.getIsPlaying() == null || request.getIsPlaying());

        return MatchSquadMapper.toDTO(matchSquadRepository.save(squad));
    }

    @Transactional
    public MatchSquadDTO updateSquadMember(Integer matchId, Long playerId, MatchSquadRequestDTO request) {
        MatchSquadId id = squadId(matchId, playerId);
        MatchSquad squad = matchSquadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Squad entry not found"));

        if (request.getTeamId() != null) {
            validateSquadTeam(squad.getMatch(), request.getTeamId());
            squad.setTeam(teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Team not found")));
        }
        if (request.getIsCaptain() != null) squad.setIsCaptain(request.getIsCaptain());
        if (request.getIsKeeper() != null) squad.setIsKeeper(request.getIsKeeper());
        if (request.getIsPlaying() != null) squad.setIsPlaying(request.getIsPlaying());

        return MatchSquadMapper.toDTO(matchSquadRepository.save(squad));
    }

    @Transactional
    public void removeSquadMember(Integer matchId, Long playerId) {
        MatchSquadId id = squadId(matchId, playerId);
        if (!matchSquadRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Squad entry not found");
        }
        matchSquadRepository.deleteById(id);
    }

    private MatchSquadId squadId(Integer matchId, Long playerId) {
        MatchSquadId id = new MatchSquadId();
        id.setMatch(matchId);
        id.setPlayer(playerId);
        return id;
    }

    private void validateSquadTeam(Match match, Integer teamId) {
        if (teamId == null || (!teamId.equals(match.getTeamA().getTeamId())
                && !teamId.equals(match.getTeamB().getTeamId()))) {
            throw new ResponseStatusException(BAD_REQUEST, "Squad team must be one of the match teams");
        }
    }

    // ---------------- Delivery CRUD (scorecards auto-rebuild after each) ----------------

    @Transactional
    public DeliveryDTO addDelivery(Integer matchId, DeliveryRequestDTO request) {
        Match match = lockMatch(matchId);

        Delivery delivery = new Delivery();
        delivery.setMatch(match);
        applyDeliveryRequest(delivery, request);
        DeliveryDTO saved = DeliveryMapper.toDTO(deliveryRepository.save(delivery));

        recalculateScorecardsForMatch(matchId);
        return saved;
    }

    @Transactional
    public DeliveryDTO updateDelivery(Integer matchId, Long deliveryId, DeliveryRequestDTO request) {
        lockMatch(matchId);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Delivery not found"));
        if (!delivery.getMatch().getMatchId().equals(matchId)) {
            throw new ResponseStatusException(BAD_REQUEST, "Delivery does not belong to this match");
        }
        applyDeliveryRequest(delivery, request);
        DeliveryDTO saved = DeliveryMapper.toDTO(deliveryRepository.save(delivery));

        recalculateScorecardsForMatch(matchId);
        return saved;
    }

    @Transactional
    public void deleteDelivery(Integer matchId, Long deliveryId) {
        lockMatch(matchId);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Delivery not found"));
        if (!delivery.getMatch().getMatchId().equals(matchId)) {
            throw new ResponseStatusException(BAD_REQUEST, "Delivery does not belong to this match");
        }
        deliveryRepository.deleteById(deliveryId);

        recalculateScorecardsForMatch(matchId);
    }

    private Match lockMatch(Integer matchId) {
        return matchRepository.findForUpdate(matchId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Match not found"));
    }

    private void applyDeliveryRequest(Delivery delivery, DeliveryRequestDTO request) {
        delivery.setInnings(request.getInnings());
        delivery.setOverNum(request.getOverNum());
        delivery.setBallNum(request.getBallNum());
        delivery.setRunsBatter(request.getRunsBatter() == null ? 0 : request.getRunsBatter());
        delivery.setRunsExtras(request.getRunsExtras() == null ? 0 : request.getRunsExtras());
        delivery.setExtraType(request.getExtraType());
        delivery.setWicketType(request.getWicketType());

        if (request.getBatterId() != null) {
            delivery.setBatter(playerRepository.findById(request.getBatterId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Batter not found")));
        } else {
            delivery.setBatter(null);
        }
        if (request.getNonStrikerId() != null) {
            delivery.setNonStriker(playerRepository.findById(request.getNonStrikerId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Non-striker not found")));
        } else {
            delivery.setNonStriker(null);
        }
        if (request.getBowlerId() != null) {
            delivery.setBowler(playerRepository.findById(request.getBowlerId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Bowler not found")));
        } else {
            delivery.setBowler(null);
        }
        if (request.getPlayerDismissedId() != null) {
            delivery.setPlayerDismissed(playerRepository.findById(request.getPlayerDismissedId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Dismissed player not found")));
        } else {
            delivery.setPlayerDismissed(null);
        }
    }

    // ---------------- Scorecard auto-rebuild from deliveries ----------------
    // Deliveries are the single source of truth. Every time one changes, the
    // batting and bowling scorecards for that match are wiped and rebuilt
    // from scratch off the current set of deliveries, so there's never a
    // chance for a manually-edited scorecard to drift out of sync.

    private void recalculateScorecardsForMatch(Integer matchId) {
        List<Delivery> deliveries = deliveryRepository
                .findByMatchMatchIdOrderByInningsAscOverNumAscBallNumAsc(matchId);

        batterRepository.deleteByMatchId(matchId);
        bowlerRepository.deleteByMatchId(matchId);

        if (deliveries.isEmpty()) return;

        Match match = deliveries.get(0).getMatch();
        batterRepository.saveAll(scorecardCalculator.batting(match, deliveries));
        bowlerRepository.saveAll(scorecardCalculator.bowling(match, deliveries));
    }

}
