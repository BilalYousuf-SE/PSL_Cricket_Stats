package com.cricket.stats.mapper;

import com.cricket.stats.dto.MatchDTO;
import com.cricket.stats.entity.Match;

public class MatchMapper {

    public static MatchDTO toDTO(Match match) {
        if (match == null) return null;

        MatchDTO dto = new MatchDTO();
        dto.setMatchId(match.getMatchId());
        dto.setMatchDate(match.getMatchDate());
        dto.setVenue(match.getVenue());
        dto.setFormat(match.getFormat());
        dto.setResultMargin(match.getResultMargin());
        dto.setResultType(match.getResultType());
        dto.setTossDecision(match.getTossDecision());

        // Team A
        if (match.getTeamA() != null) {
            dto.setTeamAId(match.getTeamA().getTeamId());
            dto.setTeamAName(match.getTeamA().getTeamName());
        }

        // Team B
        if (match.getTeamB() != null) {
            dto.setTeamBId(match.getTeamB().getTeamId());
            dto.setTeamBName(match.getTeamB().getTeamName());
        }

        // Toss winner
        if (match.getTossWinner() != null) {
            dto.setTossWinnerId(match.getTossWinner().getTeamId());
            dto.setTossWinnerName(match.getTossWinner().getTeamName());
        }

        // Winner
        if (match.getWinner() != null) {
            dto.setWinnerId(match.getWinner().getTeamId());
            dto.setWinnerName(match.getWinner().getTeamName());
        }

        // Player of match
        if (match.getPlayerOfMatch() != null) {
            dto.setPlayerOfMatchId(match.getPlayerOfMatch().getId());
            dto.setPlayerOfMatchName(match.getPlayerOfMatch().getFullName());
        }

        // Season
        if (match.getSeason() != null) {
            dto.setSeasonId(match.getSeason().getSeasonId());
            dto.setSeasonYear(match.getSeason().getYear());
        }

        return dto;
    }
}
