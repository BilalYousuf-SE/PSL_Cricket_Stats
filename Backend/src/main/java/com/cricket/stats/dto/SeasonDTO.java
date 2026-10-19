package com.cricket.stats.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class SeasonDTO {
    private Integer seasonId;
    @NotNull @Min(1)
    private Integer year;
}
