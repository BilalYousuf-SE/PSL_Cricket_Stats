package com.cricket.stats.mapper;

import com.cricket.stats.dto.PlayerStatsDTO;

public class PlayerStatsMapper {
    public static PlayerStatsDTO toDTO(Object[] row) {
        PlayerStatsDTO dto = new PlayerStatsDTO();
        dto.setPlayerId((Long) row[0]);
        dto.setPlayerName((String) row[1]);
        dto.setTotalRuns((Long) row[2]);
        dto.setTotalBallsFaced((Long) row[3]);
        dto.setTotalFours((Long) row[4]);
        dto.setTotalSixes((Long) row[5]);
        dto.setHighestScore((Integer) row[6]);
        dto.setInnings((Long) row[7]);
        dto.setBattingAverage((Double) row[8]);
        dto.setStrikeRate((Double) row[9]);
        dto.setTotalWickets((Long) row[10]);
        dto.setTotalRunsConceded((Long) row[11]);
        dto.setTotalMaidens((Long) row[12]);
        dto.setBestWickets((Integer) row[13]);
        dto.setBowlingAverage((Double) row[14]);
        dto.setEconomy((Double) row[15]);
        dto.setMatchesPlayed((Long) row[16]);
        dto.setPlayerOfMatchAwards((Long) row[17]);

        return dto;
    }
}
