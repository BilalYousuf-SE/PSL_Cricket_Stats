package com.cricket.stats.dto;

import java.util.List;

public record PlayerPerformanceOptionsDTO(
        List<Integer> seasons, List<String> venues, List<TeamOption> teams,
        List<String> stages, List<String> bowlingStyles) {
    public record TeamOption(Integer id, String name) {}
}
