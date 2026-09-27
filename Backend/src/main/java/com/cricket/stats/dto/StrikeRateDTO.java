package com.cricket.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StrikeRateDTO {
    private Long playerId;
    private String playerName;
    private Long runs;
    private Long ballsFaced;
    private Double strikeRate;
}