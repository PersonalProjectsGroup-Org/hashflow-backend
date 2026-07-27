package com.hashflow.service_telemetry.dto;

import com.hashflow.telemetry.model.Coin;
import com.hashflow.telemetry.model.MinerType;
import com.hashflow.telemetry.model.Status;

import java.time.LocalDateTime;

public record MinerInfoResponseDTO(
    MinerType minerType,
    Status status,
    String model,
    Double hashRate,
    Double temperature,
    Integer consumes,
    Double profitDay,
    LocalDateTime upTime,
    Coin coin
) {}