package com.cricket.stats.entity;

import com.cricket.stats.ID.MatchSquadId;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "match_squads")
@IdClass(MatchSquadId.class)
public class MatchSquad {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "is_captain")
    private Boolean isCaptain = false;

    @Column(name = "is_keeper")
    private Boolean isKeeper = false;

    @Column(name = "is_playing")
    private Boolean isPlaying = true;
}
