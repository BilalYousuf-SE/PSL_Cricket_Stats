package com.cricket.stats.dto;

import java.time.LocalDate;

public record MatchupMatchDTO(Integer matchId, LocalDate matchDate, Integer seasonYear,
        String venue, Integer innings, String teamAName, String teamBName, MatchupStatsDTO stats) {}
