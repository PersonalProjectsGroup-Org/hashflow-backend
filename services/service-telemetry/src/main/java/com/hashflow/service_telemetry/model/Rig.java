package com.hashflow.service_telemetry.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rigs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String externalId;
    
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private MinerType minerType;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String model;
    
    private Double hashRate;
    
    private Double temperature;
    
    private Integer powerUsageWatts;

    @Enumerated(EnumType.STRING)
    private Coin activeCoin;

    private LocalDateTime lastSync;
}