package com.cricket.stats.ID;

import lombok.Data;
import java.io.Serializable;

@Data
public class ScorecardBatterId implements Serializable {
    private Integer match;
    private Long player;
}