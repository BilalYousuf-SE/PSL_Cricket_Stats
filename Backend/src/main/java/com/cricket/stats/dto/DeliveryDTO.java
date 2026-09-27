package com.cricket.stats.dto;

import lombok.Data;

@Data
public class DeliveryDTO {
    private Long deliveryId;
    private Integer matchId;
    private Integer innings;
    private Integer overNum;
    private Integer ballNum;

    // Player names instead of full objects
    private String batterName;
    private String nonStrikerName;
    private String bowlerName;

    private Integer runsBatter;
    private Integer runsExtras;
    private Integer totalRuns;

    private String extrasType;
    private String wicketType;
    private String playerDismissedName;
}
