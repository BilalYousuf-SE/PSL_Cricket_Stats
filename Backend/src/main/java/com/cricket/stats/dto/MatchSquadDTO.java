package com.cricket.stats.dto;

import lombok.Data;

@Data
public class MatchSquadDTO {
    private Integer matchId;
    private Long playerId;
    private String playerName;
    private Integer teamId;
    private String teamName;
    private Boolean isCaptain;
    private Boolean isKeeper;
    private Boolean isPlaying;
}
