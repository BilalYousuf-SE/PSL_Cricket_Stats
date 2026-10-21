package com.cricket.stats.dto;

import java.util.List;

public record MatchupOptionsDTO(List<Integer> seasons, List<String> venues, List<String> bowlingStyles) {}
