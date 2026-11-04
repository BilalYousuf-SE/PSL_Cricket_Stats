package com.cricket.stats.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class PartnershipDTO {
    private Long partnershipId;
    private Integer matchId;
    private LocalDate matchDate;
    private Integer seasonYear;
    private String venue;
    private Integer innings;
    private Long batter1Id;
    private String batter1Name;
    private Long batter2Id;
    private String batter2Name;
    private Integer runs;
    private Integer ballsFaced;
    private Integer wicketNumber;
}
