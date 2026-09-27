package com.cricket.stats.dto;

import lombok.Data;

@Data
public class PlayerStatsDTO {
    private Long playerId;
    private String playerName;

    // Batting
    private Long totalRuns;
    private Long totalBallsFaced;
    private Long totalFours;
    private Long totalSixes;
    private Integer highestScore;
    private Long innings;
    private Double battingAverage;
    private Double strikeRate;

    // Bowling
    private Long totalWickets;
    private Long totalRunsConceded;
    private Long totalMaidens;
    private Integer bestWickets;
    private Double bowlingAverage;
    private Double economy;

    // General
    private Long matchesPlayed;
    private Long playerOfMatchAwards;
}
