package com.hashflow.service_telemetry.repository;

import com.hashflow.service_telemetry.model.Rig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RigRepository extends JpaRepository<Rig, Long> {
    Optional<Rig> findByExternalId(String externalId);
}