package com.cricket.stats.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

@Data
public class DeliveryRequestDTO {
    @NotNull @Min(1)
    private Integer innings;
    @NotNull @Min(0)
    private Integer overNum;
    @NotNull @Min(1)
    private Integer ballNum;
    @NotNull
    private Long batterId;
    private Long nonStrikerId;
    @NotNull
    private Long bowlerId;
    @Min(0)
    private Integer runsBatter;
    @Min(0)
    private Integer runsExtras;
    private String extraType;
    private String wicketType;
    private Long playerDismissedId; // nullable
    @Min(1)
    private Long fielderId;
    @JsonIgnore
    private boolean fielderIdProvided;

    @JsonSetter("fielderId")
    public void setFielderId(Long id) {
        fielderId = id;
        fielderIdProvided = true;
    }
}
