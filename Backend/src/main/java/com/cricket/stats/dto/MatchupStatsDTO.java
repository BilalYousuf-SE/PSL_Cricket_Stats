package com.cricket.stats.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record MatchupStatsDTO(
        long deliveries, long matches, long innings, long runs, long ballsFaced,
        long legalBalls, long fours, long sixes, long dotBalls, long dismissals,
        long runOuts, long runsConceded, BigDecimal strikeRate, BigDecimal average,
        BigDecimal economy, BigDecimal bowlingAverage) {

    public static MatchupStatsDTO of(long deliveries, long matches, long innings, long runs,
            long ballsFaced, long legalBalls, long fours, long sixes, long dotBalls,
            long dismissals, long runOuts, long runsConceded) {
        return new MatchupStatsDTO(deliveries, matches, innings, runs, ballsFaced, legalBalls,
                fours, sixes, dotBalls, dismissals, runOuts, runsConceded,
                rate(runs, ballsFaced, 100), rate(runs, dismissals, 1),
                rate(runsConceded, legalBalls, 6), rate(runsConceded, dismissals, 1));
    }

    public static MatchupStatsDTO empty() {
        return of(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private static BigDecimal rate(long numerator, long denominator, int multiplier) {
        return denominator == 0 ? null : BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(multiplier))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
