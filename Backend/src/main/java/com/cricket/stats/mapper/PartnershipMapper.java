package com.cricket.stats.mapper;

import com.cricket.stats.dto.PartnershipDTO;
import com.cricket.stats.entity.Partnership;

public class PartnershipMapper {
    public static PartnershipDTO toDTO(Partnership partnership) {
        if (partnership == null) return null;

        PartnershipDTO dto = new PartnershipDTO();
        dto.setPartnershipId(partnership.getPartnershipId());
        dto.setInnings(partnership.getInnings());
        dto.setRuns(partnership.getRuns());
        dto.setBallsFaced(partnership.getBallsFaced());
        dto.setWicketNumber(partnership.getWicketNumber());
        if (partnership.getMatch() != null) {
            var match = partnership.getMatch();
            dto.setMatchId(match.getMatchId());
            dto.setMatchDate(match.getMatchDate());
            dto.setVenue(match.getVenue());
            if (match.getSeason() != null) dto.setSeasonYear(match.getSeason().getYear());
        }
        if (partnership.getBatter1() != null) {
            dto.setBatter1Id(partnership.getBatter1().getId());
            dto.setBatter1Name(partnership.getBatter1().getFullName());
        }
        if (partnership.getBatter2() != null) {
            dto.setBatter2Id(partnership.getBatter2().getId());
            dto.setBatter2Name(partnership.getBatter2().getFullName());
        }
        return dto;
    }
}
