package com.cricket.stats.dto;

import lombok.Data;

@Data
public class ScorecardBatterRequestDTO {
    private Long playerId;      // required on POST; ignored on PUT (playerId comes from the path)
    private Integer runs;
    private Integer ballsFaced;
    private Integer fours;
    private Integer sixes;
    private String outDescription;
    private Boolean isOut;
    private Integer battingPosition;
}