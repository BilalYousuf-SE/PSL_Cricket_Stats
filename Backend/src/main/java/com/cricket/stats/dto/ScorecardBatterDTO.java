package com.cricket.stats.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScorecardBatterDTO {
    private Integer matchId;
    private Long playerId;
    private String playerName;
    private Integer runs;
    private Integer ballsFaced;
    private Integer fours;
    private Integer sixes;
    private BigDecimal strikeRate;
    private String outDescription;
    private Boolean isOut;
}
