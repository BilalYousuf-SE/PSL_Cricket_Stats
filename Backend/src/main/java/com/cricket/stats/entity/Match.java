package com.cricket.stats.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Integer matchId;

    @ManyToOne
    @JoinColumn(name = "seasonId")
    private Season season;

    @Column(name = "match_date")
    private LocalDate matchDate;

    @Column(name = "venue", length = 100)
    private String venue;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", length = 10)
    private MatchFormat format;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_a_id")
    private Team teamA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_b_id")
    private Team teamB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toss_winner_id")
    private Team tossWinner;

    @Enumerated(EnumType.STRING)
    @Column(name = "toss_decision")
    private TossDecision tossDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Team winner;

    @Column(name = "result_margin")
    private Integer resultMargin;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type")
    private ResultType resultType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_of_match")
    private Player playerOfMatch;
}
