package com.cricket.stats.service;

import com.cricket.stats.entity.*;
import com.cricket.stats.ID.MatchSquadId;
import com.cricket.stats.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.cricket.stats.dto.*;
import com.cricket.stats.mapper.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MatchService {

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

    // ---------------- Match CRUD ----------------

    public MatchDTO createMatch(MatchRequestDTO request) {
        Match match = new Match();
        applyMatchRequest(match, request);
        return MatchMapper.toDTO(matchRepository.save(match));
    }

    public MatchDTO updateMatch(Integer id, MatchRequestDTO request) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        applyMatchRequest(match, request);
        return MatchMapper.toDTO(matchRepository.save(match));
    }

    public void deleteMatch(Integer id) {
        if (!matchRepository.existsById(id)) {
            throw new RuntimeException("Match not found");
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

        if (request.getTeamAId() != null) {
            match.setTeamA(teamRepository.findById(request.getTeamAId())
                    .orElseThrow(() -> new RuntimeException("Team A not found")));
        }
        if (request.getTeamBId() != null) {
            match.setTeamB(teamRepository.findById(request.getTeamBId())
                    .orElseThrow(() -> new RuntimeException("Team B not found")));
        }
        if (request.getTossWinnerId() != null) {
            match.setTossWinner(teamRepository.findById(request.getTossWinnerId())
                    .orElseThrow(() -> new RuntimeException("Toss-winning team not found")));
        }
        if (request.getWinnerId() != null) {
            match.setWinner(teamRepository.findById(request.getWinnerId())
                    .orElseThrow(() -> new RuntimeException("Winning team not found")));
        }
        if (request.getSeasonId() != null) {
            match.setSeason(seasonRepository.findById(request.getSeasonId())
                    .orElseThrow(() -> new RuntimeException("Season not found")));
        }
        if (request.getPlayerOfMatchId() != null) {
            match.setPlayerOfMatch(playerRepository.findById(request.getPlayerOfMatchId())
                    .orElseThrow(() -> new RuntimeException("Player of the match not found")));
        }
    }

    // ---------------- Match Squad CRUD ----------------

    public MatchSquadDTO addSquadMember(Integer matchId, MatchSquadRequestDTO request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        Player player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new RuntimeException("Player not found"));
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("Team not found"));

        MatchSquad squad = new MatchSquad();
        squad.setMatch(match);
        squad.setPlayer(player);
        squad.setTeam(team);
        squad.setIsCaptain(Boolean.TRUE.equals(request.getIsCaptain()));
        squad.setIsKeeper(Boolean.TRUE.equals(request.getIsKeeper()));
        squad.setIsPlaying(request.getIsPlaying() == null || request.getIsPlaying());

        return MatchSquadMapper.toDTO(matchSquadRepository.save(squad));
    }

    public MatchSquadDTO updateSquadMember(Integer matchId, Long playerId, MatchSquadRequestDTO request) {
        MatchSquadId id = new MatchSquadId();
        id.setMatch(matchId);
        id.setPlayer(playerId);
        MatchSquad squad = matchSquadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Squad entry not found"));

        if (request.getTeamId() != null) {
            squad.setTeam(teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new RuntimeException("Team not found")));
        }
        if (request.getIsCaptain() != null) squad.setIsCaptain(request.getIsCaptain());
        if (request.getIsKeeper() != null) squad.setIsKeeper(request.getIsKeeper());
        if (request.getIsPlaying() != null) squad.setIsPlaying(request.getIsPlaying());

        return MatchSquadMapper.toDTO(matchSquadRepository.save(squad));
    }

    public void removeSquadMember(Integer matchId, Long playerId) {
        MatchSquadId id = new MatchSquadId();
        id.setMatch(matchId);
        id.setPlayer(playerId);
        if (!matchSquadRepository.existsById(id)) {
            throw new RuntimeException("Squad entry not found");
        }
        matchSquadRepository.deleteById(id);
    }

    // ---------------- Delivery CRUD (scorecards auto-rebuild after each) ----------------

    public DeliveryDTO addDelivery(Integer matchId, DeliveryRequestDTO request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        Delivery delivery = new Delivery();
        delivery.setMatch(match);
        applyDeliveryRequest(delivery, request);
        DeliveryDTO saved = DeliveryMapper.toDTO(deliveryRepository.save(delivery));

        recalculateScorecardsForMatch(matchId);
        return saved;
    }

    public DeliveryDTO updateDelivery(Integer matchId, Long deliveryId, DeliveryRequestDTO request) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
        if (!delivery.getMatch().getMatchId().equals(matchId)) {
            throw new RuntimeException("Delivery does not belong to this match");
        }
        applyDeliveryRequest(delivery, request);
        DeliveryDTO saved = DeliveryMapper.toDTO(deliveryRepository.save(delivery));

        recalculateScorecardsForMatch(matchId);
        return saved;
    }

    public void deleteDelivery(Integer matchId, Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
        if (!delivery.getMatch().getMatchId().equals(matchId)) {
            throw new RuntimeException("Delivery does not belong to this match");
        }
        deliveryRepository.deleteById(deliveryId);

        recalculateScorecardsForMatch(matchId);
    }

    private void applyDeliveryRequest(Delivery delivery, DeliveryRequestDTO request) {
        delivery.setInnings(request.getInnings());
        delivery.setOverNum(request.getOverNum());
        delivery.setBallNum(request.getBallNum());
        delivery.setRunsBatter(request.getRunsBatter());
        delivery.setRunsExtras(request.getRunsExtras());
        delivery.setExtraType(request.getExtraType());
        delivery.setWicketType(request.getWicketType());

        if (request.getBatterId() != null) {
            delivery.setBatter(playerRepository.findById(request.getBatterId())
                    .orElseThrow(() -> new RuntimeException("Batter not found")));
        } else {
            delivery.setBatter(null);
        }
        if (request.getNonStrikerId() != null) {
            delivery.setNonStriker(playerRepository.findById(request.getNonStrikerId())
                    .orElseThrow(() -> new RuntimeException("Non-striker not found")));
        } else {
            delivery.setNonStriker(null);
        }
        if (request.getBowlerId() != null) {
            delivery.setBowler(playerRepository.findById(request.getBowlerId())
                    .orElseThrow(() -> new RuntimeException("Bowler not found")));
        } else {
            delivery.setBowler(null);
        }
        if (request.getPlayerDismissedId() != null) {
            delivery.setPlayerDismissed(playerRepository.findById(request.getPlayerDismissedId())
                    .orElseThrow(() -> new RuntimeException("Dismissed player not found")));
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

        batterRepository.deleteAll(batterRepository.findByMatchIdWithDetails(matchId));
        bowlerRepository.deleteAll(bowlerRepository.findByMatchIdWithDetails(matchId));

        if (deliveries.isEmpty()) return;

        Match match = deliveries.get(0).getMatch();
        rebuildBattingScorecard(match, deliveries);
        rebuildBowlingScorecard(match, deliveries);
    }

    private void rebuildBattingScorecard(Match match, List<Delivery> deliveries) {
        LinkedHashMap<Long, Player> order = new LinkedHashMap<>();
        Map<Long, int[]> stats = new HashMap<>(); // playerId -> [runs, ballsFaced, fours, sixes]
        Map<Long, Delivery> dismissalBall = new HashMap<>();

        for (Delivery d : deliveries) {
            Player batter = d.getBatter();
            if (batter == null) continue;
            Long pid = batter.getId();
            order.putIfAbsent(pid, batter);
            int[] s = stats.computeIfAbsent(pid, k -> new int[4]);

            boolean isWide = containsIgnoreCase(d.getExtraType(), "wide");
            int runsBatter = nz(d.getRunsBatter());

            if (!isWide) s[1]++; // balls faced (wides aren't faced by the batter)
            s[0] += runsBatter;
            if (runsBatter == 4) s[2]++;
            if (runsBatter == 6) s[3]++;

            if (d.getWicketType() != null && d.getPlayerDismissed() != null) {
                dismissalBall.put(d.getPlayerDismissed().getId(), d);
            }
        }

        int position = 1;
        for (Map.Entry<Long, Player> entry : order.entrySet()) {
            Long pid = entry.getKey();
            Player player = entry.getValue();
            int[] s = stats.get(pid);

            ScorecardBatter row = new ScorecardBatter();
            row.setMatch(match);
            row.setPlayer(player);
            row.setRuns(s[0]);
            row.setBallsFaced(s[1]);
            row.setFours(s[2]);
            row.setSixes(s[3]);
            row.setBattingPosition(position++);

            Delivery dismissal = dismissalBall.get(pid);
            row.setIsOut(dismissal != null);
            row.setOutDescription(dismissal != null ? dismissal.getWicketType() : null);

            batterRepository.save(row);
        }
    }

    private void rebuildBowlingScorecard(Match match, List<Delivery> deliveries) {
        LinkedHashMap<Long, Player> order = new LinkedHashMap<>();
        Map<Long, int[]> legalBallsAndWickets = new HashMap<>(); // [legalBalls, wickets]
        Map<Long, Integer> runsConceded = new HashMap<>();
        Map<Long, Map<String, int[]>> overRuns = new HashMap<>(); // bowlerId -> "innings:over" -> [runsInOver]

        for (Delivery d : deliveries) {
            Player bowler = d.getBowler();
            if (bowler == null) continue;
            Long pid = bowler.getId();
            order.putIfAbsent(pid, bowler);

            boolean isWide = containsIgnoreCase(d.getExtraType(), "wide");
            boolean isNoBall = containsIgnoreCase(d.getExtraType(), "no ball")
                    || containsIgnoreCase(d.getExtraType(), "noball");
            boolean isBye = containsIgnoreCase(d.getExtraType(), "bye"); // covers bye + leg bye

            int[] lw = legalBallsAndWickets.computeIfAbsent(pid, k -> new int[2]);
            if (!isWide && !isNoBall) lw[0]++;

            int chargedToBowler = nz(d.getRunsBatter()) + (isBye ? 0 : nz(d.getRunsExtras()));
            runsConceded.merge(pid, chargedToBowler, Integer::sum);

            if (d.getWicketType() != null && !containsIgnoreCase(d.getWicketType(), "run out")) {
                lw[1]++;
            }

            String overKey = d.getInnings() + ":" + d.getOverNum();
            Map<String, int[]> overs = overRuns.computeIfAbsent(pid, k -> new HashMap<>());
            overs.computeIfAbsent(overKey, k -> new int[1])[0] += chargedToBowler;
        }

        for (Map.Entry<Long, Player> entry : order.entrySet()) {
            Long pid = entry.getKey();
            Player player = entry.getValue();
            int[] lw = legalBallsAndWickets.get(pid);
            int legalBalls = lw[0];
            int wickets = lw[1];
            int runs = runsConceded.getOrDefault(pid, 0);

            int completedOvers = legalBalls / 6;
            int remainingBalls = legalBalls % 6;
            BigDecimal oversBowled = new BigDecimal(completedOvers + "." + remainingBalls);

            long maidens = overRuns.getOrDefault(pid, Map.of()).values().stream()
                    .filter(o -> o[0] == 0)
                    .count();

            BigDecimal economy = legalBalls > 0
                    ? BigDecimal.valueOf(runs).divide(BigDecimal.valueOf(legalBalls / 6.0), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            ScorecardBowler row = new ScorecardBowler();
            row.setMatch(match);
            row.setPlayer(player);
            row.setOversBowled(oversBowled);
            row.setMaidens((int) maidens);
            row.setRunsConceded(runs);
            row.setWickets(wickets);
            row.setEconomy(economy);

            bowlerRepository.save(row);
        }
    }

    private static boolean containsIgnoreCase(String s, String needle) {
        return s != null && s.toLowerCase().contains(needle.toLowerCase());
    }

    private static int nz(Integer i) {
        return i == null ? 0 : i;
    }
}