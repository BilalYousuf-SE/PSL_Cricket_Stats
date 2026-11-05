package com.cricket.stats.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PlayerPerformanceDTO(
        Long playerId, String discipline, Stats totals,
        Map<String, List<BreakdownRow>> breakdowns, Map<String, Long> coverage) {
    public record BreakdownRow(String key, String label, Stats stats) {}

    public record Stats(
            long matches, long innings, long deliveries, long runs, long ballsFaced,
            long legalBalls, long dismissals, long fours, long sixes, long dotBalls,
            long runsConceded, long catches, long keeperCatches, long outfieldCatches,
            long unclassifiedCatches, long stumpings, long runOuts, long notOuts, String overs,
            BigDecimal average, BigDecimal strikeRate, BigDecimal economy,
            BigDecimal bowlingAverage, BigDecimal bowlingStrikeRate) {}
}
