package com.cricket.stats.dto;

import lombok.Data;

@Data
public class DeliveryRequestDTO {
    private Integer innings;
    private Integer overNum;
    private Integer ballNum;
    private Long batterId;
    private Long nonStrikerId;
    private Long bowlerId;
    private Integer runsBatter;
    private Integer runsExtras;
    private String extraType;
    private String wicketType;
    private Long playerDismissedId; // nullable
}