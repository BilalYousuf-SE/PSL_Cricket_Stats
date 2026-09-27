package com.cricket.stats.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "season")
public class Season {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "season_id")
    private Integer seasonId;

    @Column(name = "season_year")
    private Integer year;
}
