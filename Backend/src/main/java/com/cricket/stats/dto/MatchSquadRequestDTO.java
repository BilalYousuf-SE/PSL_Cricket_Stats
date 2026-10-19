package com.cricket.stats.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class MatchSquadRequestDTO {
    private Long playerId;
    @NotNull
    private Integer teamId;
    private Boolean isCaptain;
    private Boolean isKeeper;
    private Boolean isPlaying;
}