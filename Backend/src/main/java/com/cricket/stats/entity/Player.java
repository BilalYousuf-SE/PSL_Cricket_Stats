package com.cricket.stats.entity;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.validation.constraints.*;

@Data
@Entity
@Table(name="player")
public class Player {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)

    private Long Id;

    @NotBlank (message = "First Name is required")
    @Column(name = "first_name")
    private String firstName;

    @NotBlank (message = "Last Name is required")
    @Column(name = "last_name")
    private String lastName;

    @Min(value = 15)
    @Max(value = 99)
    @Column(name = "age")
    private Integer Age;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Roles role;

    @NotBlank(message = "Country is required")
    @Column(name = "nationality")
    private String nationality;

    @Column(name = "batting_style")
    @Enumerated(EnumType.STRING)
    private PlayingStyle battingStyle;

    @Column(name = "bowling_style")
    @Enumerated(EnumType.STRING)
    private PlayingStyle bowlingStyle;

    @Column(name = "full_name")
    private String fullName;

}
