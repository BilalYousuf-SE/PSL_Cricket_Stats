package com.cricket.stats.service;

import com.cricket.stats.entity.Delivery;
import com.cricket.stats.entity.Match;
import com.cricket.stats.entity.Player;
import com.cricket.stats.entity.ScorecardBatter;
import com.cricket.stats.entity.ScorecardBowler;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Calculates match/player aggregates without performing database writes. */
@Component
public class ScorecardCalculator {
    private static final Set<String> BOWLER_WICKETS = Set.of(
            "bowled", "caught", "caughtandbowled", "caught&bowled", "lbw", "stumped", "hitwicket");

    public List<ScorecardBatter> batting(Match match, List<Delivery> deliveries) {
        Map<Long, Batting> players = new LinkedHashMap<>();
        for (Delivery delivery : deliveries) {
            if (delivery.getBatter() != null) {
                Batting stats = players.computeIfAbsent(delivery.getBatter().getId(),
                        id -> new Batting(delivery.getBatter()));
                int runs = runs(delivery.getRunsBatter());
                stats.runs += runs;
                if (!isWide(delivery)) stats.balls++;
                if (runs == 4) stats.fours++;
                if (runs == 6) stats.sixes++;
            }
            // A run-out can dismiss the non-striker, even before they face a ball.
            if (!normalized(delivery.getWicketType()).isEmpty()
                    && !normalized(delivery.getWicketType()).equals("retiredhurt")
                    && delivery.getPlayerDismissed() != null) {
                players.computeIfAbsent(delivery.getPlayerDismissed().getId(),
                        id -> new Batting(delivery.getPlayerDismissed())).dismissal = delivery.getWicketType();
            }
        }
        int position = 1;
        for (Batting stats : players.values()) stats.position = position++;
        return players.values().stream().map(stats -> {
            ScorecardBatter row = new ScorecardBatter();
            row.setMatch(match);
            row.setPlayer(stats.player);
            row.setRuns(stats.runs);
            row.setBallsFaced(stats.balls);
            row.setFours(stats.fours);
            row.setSixes(stats.sixes);
            row.setBattingPosition(stats.position);
            row.setIsOut(stats.dismissal != null);
            row.setOutDescription(stats.dismissal);
            return row;
        }).toList();
    }

    public List<ScorecardBowler> bowling(Match match, List<Delivery> deliveries) {
        Map<Long, Bowling> players = new LinkedHashMap<>();
        for (Delivery delivery : deliveries) {
            if (delivery.getBowler() == null) continue;
            Bowling stats = players.computeIfAbsent(delivery.getBowler().getId(),
                    id -> new Bowling(delivery.getBowler()));
            String extra = normalized(delivery.getExtraType());
            boolean legal = !isWide(delivery) && !extra.contains("noball");
            int charged = runs(delivery.getRunsBatter())
                    + (extra.contains("bye") ? 0 : runs(delivery.getRunsExtras()));
            stats.runs += charged;
            if (legal) stats.balls++;
            if (BOWLER_WICKETS.contains(normalized(delivery.getWicketType()))) stats.wickets++;
            Over over = stats.overs.computeIfAbsent(
                    new OverKey(delivery.getInnings(), delivery.getOverNum()), key -> new Over());
            over.runs += charged;
            if (legal) over.balls++;
        }
        return players.values().stream().map(stats -> {
            ScorecardBowler row = new ScorecardBowler();
            row.setMatch(match);
            row.setPlayer(stats.player);
            row.setOversBowled(BigDecimal.valueOf(stats.balls / 6)
                    .add(BigDecimal.valueOf(stats.balls % 6, 1)));
            row.setMaidens((int) stats.overs.values().stream()
                    .filter(over -> over.balls == 6 && over.runs == 0).count());
            row.setRunsConceded(stats.runs);
            row.setWickets(stats.wickets);
            row.setEconomy(stats.balls == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(stats.runs).multiply(BigDecimal.valueOf(6))
                            .divide(BigDecimal.valueOf(stats.balls), 2, RoundingMode.HALF_UP));
            return row;
        }).toList();
    }

    private static boolean isWide(Delivery delivery) {
        return normalized(delivery.getExtraType()).contains("wide");
    }

    private static String normalized(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
    }

    private static int runs(Integer value) {
        return value == null ? 0 : value;
    }

    private static class Batting {
        final Player player;
        int runs, balls, fours, sixes, position;
        String dismissal;
        Batting(Player player) { this.player = player; }
    }

    private static class Bowling {
        final Player player;
        int runs, balls, wickets;
        final Map<OverKey, Over> overs = new LinkedHashMap<>();
        Bowling(Player player) { this.player = player; }
    }

    private record OverKey(Integer innings, Integer number) {}
    private static class Over { int runs, balls; }
}
