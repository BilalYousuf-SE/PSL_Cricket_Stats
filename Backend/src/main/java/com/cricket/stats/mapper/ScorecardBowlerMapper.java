package com.cricket.stats.mapper;

import com.cricket.stats.dto.ScorecardBowlerDTO;
import com.cricket.stats.entity.ScorecardBowler;

public class ScorecardBowlerMapper {

    public static ScorecardBowlerDTO toDTO(ScorecardBowler bowler) {
        if (bowler == null) return null;

        ScorecardBowlerDTO dto = new ScorecardBowlerDTO();
        dto.setOversBowled(bowler.getOversBowled());
        dto.setMaidens(bowler.getMaidens());
        dto.setRunsConceded(bowler.getRunsConceded());
        dto.setWickets(bowler.getWickets());
        dto.setEconomy(bowler.getEconomy());

        if (bowler.getMatch() != null) {
            dto.setMatchId(bowler.getMatch().getMatchId());
        }

        if (bowler.getPlayer() != null) {
            dto.setPlayerId(bowler.getPlayer().getId());
            dto.setPlayerName(bowler.getPlayer().getFullName());
        }

        return dto;
    }
}
