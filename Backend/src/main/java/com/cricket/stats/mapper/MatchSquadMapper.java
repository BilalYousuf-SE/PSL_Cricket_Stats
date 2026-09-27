package com.cricket.stats.mapper;

import com.cricket.stats.dto.MatchSquadDTO;
import com.cricket.stats.entity.MatchSquad;

public class MatchSquadMapper {

    public static MatchSquadDTO toDTO(MatchSquad squad) {
        if (squad == null) return null;

        MatchSquadDTO dto = new MatchSquadDTO();
        dto.setIsCaptain(squad.getIsCaptain());
        dto.setIsKeeper(squad.getIsKeeper());
        dto.setIsPlaying(squad.getIsPlaying());

        if (squad.getMatch() != null) {
            dto.setMatchId(squad.getMatch().getMatchId());
        }

        if (squad.getPlayer() != null) {
            dto.setPlayerId(squad.getPlayer().getId());
            dto.setPlayerName(squad.getPlayer().getFullName());
        }

        if (squad.getTeam() != null) {
            dto.setTeamId(squad.getTeam().getTeamId());
            dto.setTeamName(squad.getTeam().getTeamName());
        }

        return dto;
    }
}
