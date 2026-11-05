package com.cricket.stats.controller;

import com.cricket.stats.dto.*;
import com.cricket.stats.service.PlayerPerformanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/players/{playerId}/performance")
@RequiredArgsConstructor
public class PlayerPerformanceController {
    private final PlayerPerformanceService service;

    @GetMapping
    public PlayerPerformanceDTO performance(@PathVariable Long playerId,
            @Valid @ModelAttribute PlayerPerformanceFilterDTO filter) {
        return service.performance(playerId, filter);
    }

    @GetMapping("/options")
    public PlayerPerformanceOptionsDTO options(@PathVariable Long playerId) {
        return service.options(playerId);
    }
}
