package com.cricket.stats.service;

import com.cricket.stats.dto.*;
import com.cricket.stats.entity.Player;
import com.cricket.stats.repository.MatchupRepository;
import com.cricket.stats.repository.PlayerRepository;
import com.cricket.stats.repository.TeamRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchupService {
    private final MatchupRepository matchups;
    private final PlayerRepository players;
    private final TeamRepository teams;

    public MatchupDTO pair(Long batterId, Long bowlerId, MatchupFilterDTO filter) {
        validate(filter);
        requireDifferent(batterId, bowlerId, "players");
        Player batter = player(batterId);
        Player bowler = player(bowlerId);
        return matchups.pairs(filter, List.of(batterId), List.of(bowlerId)).stream().findFirst()
                .orElseGet(() -> new MatchupDTO(describe(batter), describe(bowler), MatchupStatsDTO.empty()));
    }

    public Page<MatchupDTO> opponents(Long playerId, String perspective, MatchupFilterDTO filter, int page, int size) {
        validate(filter);
        var pageable = page(page, size, 100);
        player(playerId);
        if (!List.of("batting", "bowling").contains(perspective)) {
            throw new ResponseStatusException(BAD_REQUEST, "perspective must be batting or bowling");
        }
        return matchups.opponents(playerId, perspective.equals("batting"), filter, pageable);
    }

    public Page<MatchupMatchDTO> matches(Long batterId, Long bowlerId, MatchupFilterDTO filter, int page, int size) {
        validate(filter);
        var pageable = page(page, size, 100);
        requireDifferent(batterId, bowlerId, "players");
        player(batterId);
        player(bowlerId);
        return matchups.matches(batterId, bowlerId, filter, pageable);
    }

    public MatchupMatrixDTO matrix(Integer battingTeamId, Integer bowlingTeamId, MatchupFilterDTO filter) {
        validate(filter);
        requireDifferent(battingTeamId, bowlingTeamId, "teams");
        var battingTeam = teams.findById(battingTeamId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Batting team not found"));
        var bowlingTeam = teams.findById(bowlingTeamId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Bowling team not found"));
        var batters = matchups.roster(battingTeamId, filter.getSeasonYear(), false);
        var bowlers = matchups.roster(bowlingTeamId, filter.getSeasonYear(), true);
        var cells = matchups.pairs(filter, batters.stream().map(MatchupPlayerDTO::playerId).toList(),
                bowlers.stream().map(MatchupPlayerDTO::playerId).toList());
        return new MatchupMatrixDTO(battingTeamId, battingTeam.getTeamName(), bowlingTeamId,
                bowlingTeam.getTeamName(), batters, bowlers, cells);
    }

    public MatchupOptionsDTO options() {
        return matchups.options();
    }

    private Player player(Long id) {
        positive(id, "playerId");
        return players.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Player not found"));
    }

    private MatchupPlayerDTO describe(Player player) {
        String name = player.getFullName();
        if (name == null || name.isBlank()) name = (java.util.Objects.toString(player.getFirstName(), "")
                + " " + java.util.Objects.toString(player.getLastName(), "")).trim();
        if (name.isBlank()) name = "Player #" + player.getId();
        return new MatchupPlayerDTO(player.getId(), name,
                player.getBowlingStyle() == null ? null : player.getBowlingStyle().name());
    }

    private void validate(MatchupFilterDTO filter) {
        positive(filter.getSeasonYear(), "seasonYear");
        positive(filter.getInnings(), "innings");
        if (filter.getFromDate() != null && filter.getToDate() != null && filter.getFromDate().isAfter(filter.getToDate())) {
            throw new ResponseStatusException(BAD_REQUEST, "fromDate must be on or before toDate");
        }
        if (filter.getVenue() != null && filter.getVenue().length() > 100) {
            throw new ResponseStatusException(BAD_REQUEST, "venue must be at most 100 characters");
        }
    }

    private PageRequest page(int page, int size, int max) {
        if (page < 0 || size < 1 || size > max) {
            throw new ResponseStatusException(BAD_REQUEST, "page must be non-negative and size between 1 and " + max);
        }
        return PageRequest.of(page, size);
    }

    private void requireDifferent(Number first, Number second, String label) {
        positive(first, label);
        positive(second, label);
        if (first == null || second == null || first.longValue() == second.longValue()) {
            throw new ResponseStatusException(BAD_REQUEST, "Select two different " + label);
        }
    }

    private void positive(Number value, String field) {
        if (value != null && value.longValue() < 1) throw new ResponseStatusException(BAD_REQUEST, field + " must be positive");
    }
}
