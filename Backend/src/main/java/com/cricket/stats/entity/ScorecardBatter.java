package com.cricket.stats.entity;

import com.cricket.stats.ID.ScorecardBatterId;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "scorecard_batters")
@IdClass(ScorecardBatterId.class)
public class ScorecardBatter {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(name = "runs")
    private Integer runs;

    @Column(name = "balls_faced")
    private Integer ballsFaced;

    @Column(name = "fours")
    private Integer fours;

    @Column(name = "sixes")
    private Integer sixes;

    @Column(name = "out_description", length = 255)
    private String outDescription;

    @Column(name = "is_out")
    private Boolean isOut;

    @Column(name = "batting_position")
    private Integer battingPosition;
}
