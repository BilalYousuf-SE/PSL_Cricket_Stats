package com.cricket.stats.mapper;

import com.cricket.stats.dto.ScorecardBatterDTO;
import com.cricket.stats.entity.ScorecardBatter;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ScorecardBatterMapper {

    public static ScorecardBatterDTO toDTO(ScorecardBatter batter) {
        if (batter == null) return null;

        ScorecardBatterDTO dto = new ScorecardBatterDTO();
        dto.setRuns(batter.getRuns());
        dto.setBallsFaced(batter.getBallsFaced());
        dto.setFours(batter.getFours());
        dto.setSixes(batter.getSixes());
        dto.setOutDescription(batter.getOutDescription());
        dto.setIsOut(batter.getIsOut());
        dto.setStrikeRate(calculateStrikeRate(batter.getRuns(), batter.getBallsFaced()));

        if (batter.getMatch() != null) {
            dto.setMatchId(batter.getMatch().getMatchId());
        }

        if (batter.getPlayer() != null) {
            dto.setPlayerId(batter.getPlayer().getId());
            dto.setPlayerName(batter.getPlayer().getFullName());
        }

        return dto;
    }

    private static BigDecimal calculateStrikeRate(Integer runs, Integer ballsFaced) {
        if (runs == null || ballsFaced == null || ballsFaced == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(runs)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(ballsFaced), 2, RoundingMode.HALF_UP);
    }
}
