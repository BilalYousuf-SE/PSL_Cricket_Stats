package com.cricket.stats.entity;

import com.cricket.stats.ID.ScorecardBowlerId;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "scorecard_bowlers")
@IdClass(ScorecardBowlerId.class)
public class ScorecardBowler {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(name = "overs_bowled", precision = 3, scale = 1)
    private BigDecimal oversBowled;

    @Column(name = "maidens")
    private Integer maidens = 0;

    @Column(name = "runs_conceded")
    private Integer runsConceded;

    @Column(name = "wickets")
    private Integer wickets;

    @Column(name = "economy", precision = 4, scale = 2)
    private BigDecimal economy;
}
