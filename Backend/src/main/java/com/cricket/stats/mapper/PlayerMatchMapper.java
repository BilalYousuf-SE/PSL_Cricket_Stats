package com.cricket.stats.mapper;

import com.cricket.stats.dto.PlayerMatchDTO;
import com.cricket.stats.entity.Match;
import com.cricket.stats.entity.MatchSquad;

public class PlayerMatchMapper {

    public static PlayerMatchDTO toDTO(MatchSquad ms) {
        if (ms == null) return null;

        Match m = ms.getMatch();
        PlayerMatchDTO dto = new PlayerMatchDTO();

        dto.setMatchId(m.getMatchId());
        dto.setMatchDate(m.getMatchDate());
        dto.setVenue(m.getVenue());
        dto.setFormat(m.getFormat() != null ? m.getFormat().name() : null);
        dto.setTeamAName(m.getTeamA() != null ? m.getTeamA().getTeamName() : null);
        dto.setTeamBName(m.getTeamB() != null ? m.getTeamB().getTeamName() : null);
        dto.setWinnerName(m.getWinner() != null ? m.getWinner().getTeamName() : "No Result");
        dto.setPlayerTeam(ms.getTeam() != null ? ms.getTeam().getTeamName() : null);
        dto.setSeasonYear(m.getSeason() != null ? m.getSeason().getYear() : null);

        return dto;
    }
}