package com.valueplus.persistence.repository;

import com.valueplus.persistence.entity.CpaReportData;
import com.valueplus.persistence.entity.RevShareReportData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RevShareReportDataRepository extends JpaRepository<RevShareReportData, Long> {

    Optional<RevShareReportData> findByPlayerId(String playerId);
}
