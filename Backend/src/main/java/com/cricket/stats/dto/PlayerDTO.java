package com.cricket.stats.dto;

import com.cricket.stats.entity.PlayingStyle;
import com.cricket.stats.entity.Roles;
import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class PlayerDTO {
    private Long id;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String fullName;
    @Min(15) @Max(99)
    private Integer age;
    @NotNull
    private Roles role;
    @NotBlank
    private String nationality;
    private PlayingStyle battingStyle;
    private PlayingStyle bowlingStyle;
}
