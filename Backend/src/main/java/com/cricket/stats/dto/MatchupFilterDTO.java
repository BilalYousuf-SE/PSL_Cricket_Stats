package com.cricket.stats.dto;

import com.cricket.stats.entity.BowlingStyle;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/** Filters encounters, independently of the teams represented by either player. */
@Data
public class MatchupFilterDTO {
    @Min(1)
    private Integer seasonYear;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;
    @Size(max = 100)
    private String venue;
    @Min(1)
    private Integer innings;
    private BowlingStyle bowlingStyle;
}
