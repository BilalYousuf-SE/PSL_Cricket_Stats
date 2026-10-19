package com.cricket.stats.service;

import com.cricket.stats.dto.MatchDTO;
import com.cricket.stats.dto.SeasonDTO;
import com.cricket.stats.dto.TeamDTO;
import com.cricket.stats.entity.Season;
import com.cricket.stats.mapper.MatchMapper;
import com.cricket.stats.mapper.SeasonMapper;
import com.cricket.stats.mapper.TeamMapper;
import com.cricket.stats.repository.MatchRepository;
import com.cricket.stats.repository.SeasonRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
        return seasonRepository.findSeasonWinner(seasonYear)
                .map(TeamMapper::toDTO)
                .orElse(null);
    }

    @Transactional
    public SeasonDTO createSeason(SeasonDTO dto) {
        if (seasonRepository.existsByYear(dto.getYear())) {
            throw new ResponseStatusException(CONFLICT, "A season for " + dto.getYear() + " already exists");
        }
        Season season = new Season();
        season.setYear(dto.getYear());
        return SeasonMapper.toDTO(seasonRepository.save(season));
    }

    @Transactional
    public SeasonDTO updateSeason(Integer id, SeasonDTO dto) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Season not found: " + id));
        if (!season.getYear().equals(dto.getYear()) && seasonRepository.existsByYear(dto.getYear())) {
            throw new ResponseStatusException(CONFLICT, "A season for " + dto.getYear() + " already exists");
        }
        season.setYear(dto.getYear());
        return SeasonMapper.toDTO(seasonRepository.save(season));
    }

    @Transactional
    public void deleteSeason(Integer id) {
        if (!seasonRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Season not found: " + id);
        }
        seasonRepository.deleteById(id);
    }
}
