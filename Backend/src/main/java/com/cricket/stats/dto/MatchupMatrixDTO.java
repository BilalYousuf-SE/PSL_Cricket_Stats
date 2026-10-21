package com.cricket.stats.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record MatchupMatrixDTO(Integer battingTeamId, String battingTeamName,
        Integer bowlingTeamId, String bowlingTeamName, Page<MatchupPlayerDTO> batters,
        Page<MatchupPlayerDTO> bowlers, List<MatchupDTO> cells) {}
