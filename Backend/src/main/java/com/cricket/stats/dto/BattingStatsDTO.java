package com.cricket.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BattingStatsDTO {
    private Long totalRuns;
    private Long totalBallsFaced;
    private Long totalFours;
    private Long totalSixes;
    private Integer highestScore;
    private Long innings;
}
