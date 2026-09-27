package com.cricket.stats.ID;

import lombok.Data;
import java.io.Serializable;

@Data
public class MatchSquadId implements Serializable {
    private Integer match;   // Must match field name in MatchSquad entity
    private Long player;  // Must match field name in MatchSquad entity
}