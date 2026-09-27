package com.cricket.stats.entity;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.validation.constraints.*;

@Data
@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Integer teamId;

    @NotBlank(message = "Team name is required")
    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;
}
