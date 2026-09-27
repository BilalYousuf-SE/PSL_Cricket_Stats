package com.cricket.stats.mapper;

import com.cricket.stats.dto.TeamDTO;
import com.cricket.stats.entity.Team;

public class TeamMapper {

    public static TeamDTO toDTO(Team team) {
        if (team == null) return null;

        TeamDTO dto = new TeamDTO();
        dto.setTeamId(team.getTeamId());
        dto.setTeamName(team.getTeamName());
        return dto;
    }
}
