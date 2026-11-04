package com.cricket.stats.dto;

import java.util.List;

public record MatchupMatrixDTO(Integer battingTeamId, String battingTeamName,
        Integer bowlingTeamId, String bowlingTeamName, List<MatchupPlayerDTO> batters,
        List<MatchupPlayerDTO> bowlers, List<MatchupDTO> cells) {}
