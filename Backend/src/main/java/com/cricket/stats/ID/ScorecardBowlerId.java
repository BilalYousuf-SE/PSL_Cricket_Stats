package com.cricket.stats.ID;

import lombok.Data;
import java.io.Serializable;

@Data
public class ScorecardBowlerId implements Serializable {
    private Integer match;
    private Long player;
}