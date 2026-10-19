package com.cricket.stats.dto;

import com.cricket.stats.entity.MatchFormat;
import com.cricket.stats.entity.ResultType;
import com.cricket.stats.entity.TossDecision;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MatchDTO {
    private Integer seasonId;
    private Integer tossWinnerId;
    private Integer winnerId;
    private Integer matchId;
    private LocalDate matchDate;
    private String venue;
    private MatchFormat format;

    // Instead of full Team object, just names and IDs
    private Integer teamAId;
    private String teamAName;
    private Integer teamBId;
    private String teamBName;

    // Toss info
    private String tossWinnerName;
    private TossDecision tossDecision;

    // Result
    private String winnerName;
    private Integer resultMargin;
    private ResultType resultType;

    // Player of match — just name, not full object
    private Long playerOfMatchId;
    private String playerOfMatchName;

    // Season
    private Integer seasonYear;
}
