package com.cricket.stats.service;

import com.cricket.stats.dto.*;
import com.cricket.stats.dto.PlayerPerformanceDTO.*;
import com.cricket.stats.entity.*;
import com.cricket.stats.repository.PlayerPerformanceRepository;
import com.cricket.stats.repository.PlayerPerformanceRepository.Ball;
import com.cricket.stats.repository.PlayerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerPerformanceService {
    private final PlayerPerformanceRepository repository;
    private final PlayerRepository players;
    private static final Set<String> BOWLER_OUTS = Set.of("bowled", "caught", "caughtandbowled", "caught&bowled", "lbw", "stumped", "hitwicket");
    private static final Set<String> OTHER_OUTS = Set.of("runout", "obstructingthefield", "retiredout", "timedout", "hittheballtwice", "handledtheball");

    public PlayerPerformanceDTO performance(Long id, PlayerPerformanceFilterDTO filter) {
        requirePlayer(id);
        if (!Set.of("batting", "bowling", "keeping", "fielding").contains(filter.getDiscipline())) {
            throw new ResponseStatusException(BAD_REQUEST, "Unknown performance discipline");
        }
        if (filter.getFromDate() != null && filter.getToDate() != null && filter.getFromDate().isAfter(filter.getToDate())) {
            throw new ResponseStatusException(BAD_REQUEST, "fromDate must be on or before toDate");
        }
        String mode = filter.getDiscipline();
        var totals = new Accumulator();
        Map<String, Map<Bucket, Accumulator>> grouped = new LinkedHashMap<>();
        Map<String, Set<Long>> coverage = new LinkedHashMap<>();
        List<String> dimensions = new ArrayList<>(List.of("venue", "team", "opposition", "season", "battingPosition", "captaincy", "innings", "stage"));
        if (!mode.equals("bowling")) dimensions.add(0, "bowlingStyle");
        if (!mode.equals("batting")) dimensions.add(0, "batterHand");
        dimensions.forEach(dimension -> grouped.put(dimension, new LinkedHashMap<>()));

        for (Ball ball : repository.deliveries(id, filter)) {
            Context context = new Context(ball, id, mode);
            if (!context.candidate || !context.matches(filter)) continue;
            if (blank(ball.stage())) missing(coverage, "missingStageMatches", ball.matchId());
            if (context.captaincy.equals("unknown")) missing(coverage, "missingCaptainMatches", ball.matchId());
            if (context.position == null) missing(coverage, "missingBattingPositions", ball.id());
            if (!mode.equals("bowling") && blank(ball.bowlingStyle())) missing(coverage, "missingBowlingStyles", ball.id());
            if (!mode.equals("batting") && blank(context.hand)) missing(coverage, "missingBattingHands", ball.id());
            if (context.unknownKeeperCatch) missing(coverage, "unclassifiedKeeperCatches", ball.id());
            if (context.unidentified) missing(coverage, "unidentifiedFieldingDismissals", ball.id());
            if (!context.credited) continue;
            totals.add(context);
            for (String dimension : dimensions) {
                grouped.get(dimension).computeIfAbsent(context.bucket(dimension), key -> new Accumulator()).add(context);
            }
        }
        Map<String, List<BreakdownRow>> breakdowns = new LinkedHashMap<>();
        grouped.forEach((dimension, rows) -> breakdowns.put(dimension, rows.entrySet().stream()
            .sorted((a, b) -> compareBuckets(a.getKey(), b.getKey(), dimension))
            .map(entry -> new BreakdownRow(entry.getKey().key(), entry.getKey().label(), entry.getValue().build(mode))).toList()));
        Map<String, Long> missing = new LinkedHashMap<>();
        coverage.forEach((key, values) -> missing.put(key, (long) values.size()));
        return new PlayerPerformanceDTO(id, mode, totals.build(mode), breakdowns, missing);
    }

    public PlayerPerformanceOptionsDTO options(Long id) {
        requirePlayer(id);
        var seasons = new TreeSet<Integer>(Comparator.reverseOrder());
        var venues = new TreeSet<String>();
        Map<Integer, String> teams = new HashMap<>();
        for (var match : repository.options(id)) {
            if (match.seasonYear() != null) seasons.add(match.seasonYear());
            if (!blank(match.venue())) venues.add(match.venue());
            if (match.teamA() != null) teams.put(match.teamA(), match.teamAName());
            if (match.teamB() != null) teams.put(match.teamB(), match.teamBName());
        }
        return new PlayerPerformanceOptionsDTO(List.copyOf(seasons), List.copyOf(venues),
            teams.entrySet().stream().sorted(Comparator.comparing(e -> Objects.toString(e.getValue(), "")))
                .map(e -> new PlayerPerformanceOptionsDTO.TeamOption(e.getKey(), e.getValue())).toList(),
            Arrays.stream(MatchStage.values()).map(Enum::name).toList(),
            Arrays.stream(BowlingStyle.values()).map(Enum::name).toList());
    }

    private void requirePlayer(Long id) {
        if (id == null || id <= 0) throw new ResponseStatusException(BAD_REQUEST, "playerId must be positive");
        if (!players.existsById(id)) throw new ResponseStatusException(NOT_FOUND, "Player not found");
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", ""); }
    private static void missing(Map<String, Set<Long>> coverage, String key, long id) {
        coverage.computeIfAbsent(key, ignored -> new HashSet<>()).add(id);
    }
    private static BigDecimal rate(long numerator, long denominator, int multiplier) {
        return denominator == 0 ? null : BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(multiplier))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
    private static int compareBuckets(Bucket a, Bucket b, String dimension) {
        if (a.key().equals("unknown")) return b.key().equals("unknown") ? 0 : 1;
        if (b.key().equals("unknown")) return -1;
        if (Set.of("season", "innings", "battingPosition").contains(dimension)) {
            int result = Integer.compare(Integer.parseInt(a.key()), Integer.parseInt(b.key()));
            return dimension.equals("season") ? -result : result;
        }
        return a.label().compareToIgnoreCase(b.label());
    }
    private record Bucket(String key, String label) {}

    private static final class Context {
        final Ball ball;
        final String mode, kind, extra, captaincy, hand;
        final Integer team, position;
        final boolean wide, legal, bowlerWicket, catchEvent, stumpEvent, runOutEvent,
            keeperCatch, unknownKeeperCatch, candidate, credited, unidentified, striker, dismissed;

        Context(Ball ball, Long id, String mode) {
            this.ball = ball; this.mode = mode;
            kind = normalize(ball.wicketType()); extra = normalize(ball.extraType());
            wide = extra.contains("wide"); legal = !wide && !extra.contains("noball");
            striker = Objects.equals(ball.batter(), id);
            bowlerWicket = Objects.equals(ball.dismissed(), ball.batter()) && ball.batter() != null
                && BOWLER_OUTS.contains(kind) && !extra.contains("noball")
                && (!wide || Set.of("stumped", "hitwicket").contains(kind));
            dismissed = Objects.equals(ball.dismissed(), id) && (bowlerWicket || OTHER_OUTS.contains(kind));
            boolean caughtAndBowled = Set.of("caughtandbowled", "caught&bowled").contains(kind);
            catchEvent = (kind.equals("caught") || caughtAndBowled) && legal;
            stumpEvent = kind.equals("stumped") && !extra.contains("noball"); runOutEvent = kind.equals("runout");
            Long owner = caughtAndBowled ? ball.bowler() : ball.fielder();
            if (stumpEvent && owner == null && ball.keepers() == 1) owner = ball.soleKeeper();
            boolean named = Objects.equals(owner, id);
            boolean sameTeam = ball.playerTeam() != null && Objects.equals(ball.playerTeam(), ball.bowlingTeam());
            keeperCatch = catchEvent && !caughtAndBowled && named && sameTeam && Objects.equals(ball.playerKeeper(), 1);
            unknownKeeperCatch = catchEvent && !caughtAndBowled && named && !keeperCatch && ball.keepers() == 0;
            unidentified = (catchEvent || stumpEvent || runOutEvent) && owner == null && sameTeam
                && !Objects.equals(ball.playerPlaying(), 0) && Set.of("keeping", "fielding").contains(mode);
            credited = switch (mode) {
                case "batting" -> striker || Objects.equals(ball.nonStriker(), id) || Objects.equals(ball.dismissed(), id);
                case "bowling" -> Objects.equals(ball.bowler(), id);
                case "keeping" -> (stumpEvent && named) || keeperCatch;
                default -> (catchEvent || stumpEvent || runOutEvent) && named;
            };
            candidate = credited || unidentified || (mode.equals("keeping") && unknownKeeperCatch);
            team = mode.equals("batting") ? ball.battingTeam() : ball.bowlingTeam();
            int captainAssignments = mode.equals("batting") ? ball.battingCaptains() : ball.bowlingCaptains();
            captaincy = team != null && Objects.equals(team, ball.playerTeam()) && Objects.equals(ball.playerCaptain(), 1)
                ? "captain" : team != null && Objects.equals(team, ball.playerTeam())
                    && Objects.equals(ball.playerCaptain(), 0) && captainAssignments > 0 ? "not-captain" : "unknown";
            boolean fielding = mode.equals("fielding") || mode.equals("keeping");
            position = mode.equals("batting") ? ball.ownPosition()
                : fielding && ball.dismissed() != null ? ball.dismissedPosition() : ball.batterPosition();
            hand = fielding && ball.dismissed() != null ? ball.dismissedHand() : ball.batterHand();
        }

        boolean matches(PlayerPerformanceFilterDTO filter) {
            return (filter.getTeamId() == null || Objects.equals(filter.getTeamId(), team))
                && (filter.getOppositionId() == null || team != null && Objects.equals(filter.getOppositionId(), Objects.equals(team, ball.teamA()) ? ball.teamB() : ball.teamA()))
                && (filter.getInnings() == null || Objects.equals(filter.getInnings(), ball.innings()))
                && (filter.getBattingPosition() == null || Objects.equals(filter.getBattingPosition(), position))
                && (filter.getCaptaincy() == null || filter.getCaptaincy().equals(captaincy))
                && (filter.getStage() == null || filter.getStage().name().equals(ball.stage()))
                && (filter.getBowlingStyle() == null || filter.getBowlingStyle().name().equals(ball.bowlingStyle()))
                && (filter.getBatterHand() == null || filter.getBatterHand().name().equals(hand));
        }

        Bucket bucket(String dimension) {
            return switch (dimension) {
                case "venue" -> value(ball.venue(), "Venue unrecorded");
                case "team" -> team(team);
                case "opposition" -> team(team == null ? null : Objects.equals(team, ball.teamA()) ? ball.teamB() : ball.teamA());
                case "season" -> value(ball.seasonYear(), "Season unrecorded");
                case "battingPosition" -> position == null ? value(null, "Position unrecorded") : new Bucket(position.toString(), "Position " + position);
                case "captaincy" -> new Bucket(captaincy, captaincy.equals("captain") ? "Captain" : captaincy.equals("not-captain") ? "Not captain" : "Captaincy unrecorded");
                case "stage" -> value(ball.stage(), "Stage unrecorded");
                case "batterHand" -> hand == null ? value(null, "Batting hand unrecorded") : new Bucket(hand, hand + "-handed batters");
                case "bowlingStyle" -> blank(ball.bowlingStyle()) ? value(null, "Bowling type unrecorded") : new Bucket(ball.bowlingStyle(), ball.bowlingStyle().replaceAll("([a-z])([A-Z])", "$1 $2"));
                default -> ball.innings() == null ? value(null, "Innings unrecorded")
                    : new Bucket(ball.innings().toString(), ball.innings() == 1
                        ? (mode.equals("batting") ? "Batting first" : "Bowling first")
                        : ball.innings() == 2 ? (mode.equals("batting") ? "Chasing" : "Defending") : "Innings " + ball.innings() + " (extra)");
            };
        }
        private Bucket team(Integer id) {
            return id == null ? value(null, "Team unrecorded") : new Bucket(id.toString(),
                Objects.toString(Objects.equals(id, ball.teamA()) ? ball.teamAName() : ball.teamBName(), "Team #" + id));
        }
        private static Bucket value(Object key, String unknown) {
            return key == null || key.toString().isBlank() ? new Bucket("unknown", unknown) : new Bucket(key.toString(), key.toString());
        }
    }

    private static final class Accumulator {
        final Set<Integer> matches = new HashSet<>();
        final Set<String> innings = new HashSet<>();
        long deliveries, runs, balls, legalBalls, dismissals, fours, sixes, dots, conceded,
            catches, keeperCatches, outfieldCatches, unclassifiedCatches, stumpings, runOuts;

        void add(Context context) {
            Ball b = context.ball;
            matches.add(b.matchId()); innings.add(b.matchId() + ":" + b.innings()); deliveries++;
            switch (context.mode) {
                case "batting" -> {
                    if (context.striker) {
                        runs += b.runs(); if (!context.wide) balls++;
                        if (b.runs() == 4) fours++; if (b.runs() == 6) sixes++;
                        if (context.legal && b.runs() + b.extras() == 0) dots++;
                    }
                    if (context.dismissed) dismissals++;
                }
                case "bowling" -> {
                    runs += b.runs(); if (!context.wide) balls++;
                    if (context.legal) legalBalls++;
                    conceded += b.runs() + (context.extra.contains("wide") || context.extra.contains("noball") ? b.extras() : 0);
                    if (context.bowlerWicket) dismissals++;
                    if (b.runs() == 4) fours++; if (b.runs() == 6) sixes++;
                    if (context.legal && b.runs() + b.extras() == 0) dots++;
                }
                default -> {
                    dismissals++;
                    if (context.catchEvent) {
                        catches++;
                        if (context.keeperCatch) keeperCatches++;
                        else if (context.unknownKeeperCatch) unclassifiedCatches++;
                        else outfieldCatches++;
                    }
                    if (context.stumpEvent) stumpings++;
                    if (context.runOutEvent) runOuts++;
                }
            }
        }
        Stats build(String mode) {
            boolean bat = mode.equals("batting"), bowl = mode.equals("bowling");
            return new Stats(matches.size(), innings.size(), deliveries, runs, balls, legalBalls, dismissals,
                fours, sixes, dots, conceded, catches, keeperCatches, outfieldCatches, unclassifiedCatches,
                stumpings, runOuts, bat ? Math.max(0, innings.size() - dismissals) : 0,
                (legalBalls / 6) + "." + (legalBalls % 6),
                bat ? rate(runs, dismissals, 1) : null, bat ? rate(runs, balls, 100) : null,
                bowl ? rate(conceded, legalBalls, 6) : null, bowl ? rate(conceded, dismissals, 1) : null,
                bowl ? rate(legalBalls, dismissals, 1) : null);
        }
    }
}
