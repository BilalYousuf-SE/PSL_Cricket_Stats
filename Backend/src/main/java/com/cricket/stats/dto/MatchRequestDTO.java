package com.cricket.stats.dto;

import com.cricket.stats.entity.MatchFormat;
import com.cricket.stats.entity.ResultType;
import com.cricket.stats.entity.TossDecision;
import lombok.Data;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Data
public class MatchRequestDTO {
    @NotNull
    private Integer seasonId;
    @NotNull
    private LocalDate matchDate;
    @NotBlank
    private String venue;
    @NotNull
    private MatchFormat format;
    @NotNull
    private Integer teamAId;
    @NotNull
    private Integer teamBId;
    private Integer tossWinnerId;
    private TossDecision tossDecision;
    private Integer winnerId;       // nullable — set once the match is decided
    @Min(0)
    private Integer resultMargin;   // nullable
    private ResultType resultType;  // nullable
    private Long playerOfMatchId;   // nullable
}