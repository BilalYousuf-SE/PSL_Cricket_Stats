package com.cricket.stats.controller;

import com.cricket.stats.dto.*;
import com.cricket.stats.service.MatchupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/matchups")
@RequiredArgsConstructor
public class MatchupController {
    private final MatchupService service;

    @GetMapping("/options")
    public MatchupOptionsDTO options() {
        return service.options();
    }

    @GetMapping("/players/{batterId}/{bowlerId}")
    public MatchupDTO pair(@PathVariable Long batterId, @PathVariable Long bowlerId,
                          @Valid @ModelAttribute MatchupFilterDTO filter) {
        return service.pair(batterId, bowlerId, filter);
    }

    @GetMapping("/players/{batterId}/{bowlerId}/matches")
    public Page<MatchupMatchDTO> matches(@PathVariable Long batterId, @PathVariable Long bowlerId,
            @Valid @ModelAttribute MatchupFilterDTO filter,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return service.matches(batterId, bowlerId, filter, page, size);
    }

    @GetMapping("/players/{playerId}")
    public Page<MatchupDTO> opponents(@PathVariable Long playerId,
            @RequestParam(defaultValue = "batting") String perspective,
            @Valid @ModelAttribute MatchupFilterDTO filter,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return service.opponents(playerId, perspective, filter, page, size);
    }

    @GetMapping("/teams")
    public MatchupMatrixDTO matrix(@RequestParam Integer battingTeamId, @RequestParam Integer bowlingTeamId,
            @Valid @ModelAttribute MatchupFilterDTO filter) {
        return service.matrix(battingTeamId, bowlingTeamId, filter);
    }
}
