package com.cricket.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamLeaderboardDTO {
    private Integer teamId;
    private String teamName;
    private Long wins;
    private Long losses;
    private Long matchesPlayed;
    private Long points;
    private Double netRunRate;
}