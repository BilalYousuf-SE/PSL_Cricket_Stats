package com.cricket.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InningsExtrasDTO {
    private Integer innings;
    private Long totalExtras;
    private Map<String, Long> extrasByType; // e.g. {"WIDE": 4, "NO_BALL": 2, "BYE": 1}
}