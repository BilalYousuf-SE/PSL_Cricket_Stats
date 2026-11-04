package com.cricket.stats.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "partnerships")
public class Partnership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "partnership_id")
    private Long partnershipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    // Nullable for existing rows until their innings are backfilled through SQL.
    @Column(name = "innings")
    private Integer innings;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batter1_id")
    private Player batter1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batter2_id")
    private Player batter2;

    @Column(name = "runs")
    private Integer runs;

    @Column(name = "balls_faced")
    private Integer ballsFaced;

    @Column(name = "wicket_number")
    private Integer wicketNumber;
}
