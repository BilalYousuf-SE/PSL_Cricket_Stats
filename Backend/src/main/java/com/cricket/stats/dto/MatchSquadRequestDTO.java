package com.cricket.stats.dto;

import lombok.Data;

@Data
public class MatchSquadRequestDTO {
    private Long playerId;
    private Integer teamId;
    private Boolean isCaptain;
    private Boolean isKeeper;
    private Boolean isPlaying;
}