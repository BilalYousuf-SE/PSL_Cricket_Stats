package com.cricket.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RunScorerDTO {
    private Long playerId;
    private String playerName;
    private Long totalRuns;
}
