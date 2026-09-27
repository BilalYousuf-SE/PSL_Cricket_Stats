package com.cricket.stats.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScorecardBowlerDTO {
    private Integer matchId;
    private Long playerId;
    private String playerName;
    private BigDecimal oversBowled;
    private Integer maidens;
    private Integer runsConceded;
    private Integer wickets;
    private BigDecimal economy;
}
