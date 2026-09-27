package com.cricket.stats.service;

import com.cricket.stats.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.cricket.stats.dto.*;
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

}
