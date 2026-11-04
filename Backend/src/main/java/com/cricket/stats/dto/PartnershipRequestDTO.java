package com.cricket.stats.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PartnershipRequestDTO {
    @NotNull @Min(1)
    private Integer innings;
    @NotNull @Min(1)
    private Long batter1Id;
    @NotNull @Min(1)
    private Long batter2Id;
    @NotNull @Min(0)
    private Integer runs;
    @NotNull @Min(0)
    private Integer ballsFaced;
    @NotNull @Min(1) @Max(10)
    private Integer wicketNumber;
}
