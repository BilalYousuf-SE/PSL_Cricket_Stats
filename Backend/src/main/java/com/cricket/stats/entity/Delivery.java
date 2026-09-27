package com.cricket.stats.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Long deliveryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @Column(name = "innings")
    private Integer innings;

    @Column(name = "over_num")
    private Integer overNum;

    @Column(name = "ball_num")
    private Integer ballNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batter_id")
    private Player batter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "non_striker_id")
    private Player nonStriker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bowler_id")
    private Player bowler;

    @Column(name = "runs_batter")
    private Integer runsBatter = 0;

    @Column(name = "runs_extras")
    private Integer runsExtras = 0;

    @Column(name = "extra_type", length = 50)
    private String extraType;

    @Column(name = "wicket_type", length = 50)
    private String wicketType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_dismissed_id")
    private Player playerDismissed;
}
