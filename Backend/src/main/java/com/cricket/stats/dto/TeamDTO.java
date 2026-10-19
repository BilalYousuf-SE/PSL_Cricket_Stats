package com.cricket.stats.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class TeamDTO {
    private Integer teamId;
    @NotBlank
    private String teamName;
}
