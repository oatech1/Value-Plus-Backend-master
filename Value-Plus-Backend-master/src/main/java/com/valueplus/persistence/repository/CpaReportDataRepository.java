package com.valueplus.persistence.repository;

import com.valueplus.persistence.entity.CpaReportData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CpaReportDataRepository extends JpaRepository<CpaReportData, Long> {
    Optional<CpaReportData> findByPlayerId(String playerId);
}
