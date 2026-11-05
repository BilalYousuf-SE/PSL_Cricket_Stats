package com.cricket.stats.dto;

import com.cricket.stats.entity.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class PlayerPerformanceFilterDTO {
    @Pattern(regexp = "batting|bowling|keeping|fielding")
    private String discipline = "batting";
    @Min(1) private Integer seasonYear;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;
    @Min(1) private Integer teamId;
    @Min(1) private Integer oppositionId;
    @Size(max = 100) private String venue;
    @Min(1) private Integer innings;
    @Min(1) @Max(11) private Integer battingPosition;
    @Pattern(regexp = "captain|not-captain|unknown")
    private String captaincy;
    private MatchStage stage;
    private BowlingStyle bowlingStyle;
    private PlayingStyle batterHand;
}
