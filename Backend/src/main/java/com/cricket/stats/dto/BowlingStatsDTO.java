package com.cricket.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BowlingStatsDTO {
    private Long totalWickets;
    private Long totalRunsConceded;
    private Long totalMaidens;
    private Integer bestWickets;
    private Long innings;
}
