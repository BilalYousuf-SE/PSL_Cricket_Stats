package com.cricket.stats.mapper;

import com.cricket.stats.dto.SeasonDTO;
import com.cricket.stats.entity.Season;

public class SeasonMapper {
    
    public static SeasonDTO toDTO(Season season) {
         SeasonDTO dto = new SeasonDTO();
         dto.setSeasonId(season.getSeasonId());
         dto.setYear(season.getYear());

         return dto;
    }
}
