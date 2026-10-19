package com.cricket.stats.dto;

import com.cricket.stats.entity.MatchFormat;
import com.cricket.stats.entity.ResultType;
import com.cricket.stats.entity.TossDecision;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MatchRequestDTO {
    private Integer seasonId;
    private LocalDate matchDate;
    private String venue;
    private MatchFormat format;
    private Integer teamAId;
    private Integer teamBId;
    private Integer tossWinnerId;
    private TossDecision tossDecision;
    private Integer winnerId;       // nullable — set once the match is decided
    private Integer resultMargin;   // nullable
    private ResultType resultType;  // nullable
    private Long playerOfMatchId;   // nullable
}