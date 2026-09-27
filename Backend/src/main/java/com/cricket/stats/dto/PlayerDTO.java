package com.cricket.stats.dto;

import com.cricket.stats.entity.PlayingStyle;
import com.cricket.stats.entity.Roles;
import lombok.Data;

@Data
public class PlayerDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private Integer age;
    private Roles role;
    private String nationality;
    private PlayingStyle battingStyle;
    private PlayingStyle bowlingStyle;
}
