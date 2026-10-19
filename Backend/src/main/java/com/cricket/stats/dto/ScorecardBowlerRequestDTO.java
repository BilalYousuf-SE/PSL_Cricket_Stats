package com.cricket.stats.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ScorecardBowlerRequestDTO {
    private Long playerId;      // required on POST; ignored on PUT
    private BigDecimal oversBowled;
    private Integer maidens;
    private Integer runsConceded;
    private Integer wickets;
    private BigDecimal economy;
}