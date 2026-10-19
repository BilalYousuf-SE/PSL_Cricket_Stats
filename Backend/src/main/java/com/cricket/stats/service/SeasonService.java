package com.cricket.stats.service;

import com.cricket.stats.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.cricket.stats.dto.*;
import com.cricket.stats.entity.Season;
import com.cricket.stats.mapper.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeasonService {
    
    private final SeasonRepository seasonRepository;
    private final MatchRepository matchRepository;

    public List<SeasonDTO> getAllSeasons() {
        return seasonRepository.findAllByOrderByYearDesc()
                .stream()
                .map(SeasonMapper::toDTO)
                .toList();
    }

    public List<MatchDTO> getMatchesBySeasonYear(Integer seasonYear) {
        return matchRepository.findBySeasonYear(seasonYear)
                .stream()
                .map(MatchMapper::toDTO)
                .toList();
    }

    public TeamDTO getSeasonWinner(Integer seasonYear) {
        return seasonRepository.SeasonWinner(seasonYear)
                .map(TeamMapper::toDTO)
                .orElse(null);
    }

    public SeasonDTO createSeason(SeasonDTO dto) {
        if (seasonRepository.existsByYear(dto.getYear())) {
            throw new RuntimeException("A season for " + dto.getYear() + " already exists");
        }
        Season season = new Season();
        season.setYear(dto.getYear());
        return SeasonMapper.toDTO(seasonRepository.save(season));
    }

    public SeasonDTO updateSeason(Integer id, SeasonDTO dto) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Season not found: " + id));
        season.setYear(dto.getYear());
        return SeasonMapper.toDTO(seasonRepository.save(season));
    }

    public void deleteSeason(Integer id) {
        if (!seasonRepository.existsById(id)) {
            throw new RuntimeException("Season not found: " + id);
        }
        seasonRepository.deleteById(id);
    }
}
