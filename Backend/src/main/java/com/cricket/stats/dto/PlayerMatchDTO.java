package com.cricket.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerMatchDTO {
    private Integer matchId;
    private LocalDate matchDate;
    private String venue;
    private String format;
    private String teamAName;
    private String teamBName;
    private String winnerName;
    private String playerTeam;
    private Integer seasonYear;
}