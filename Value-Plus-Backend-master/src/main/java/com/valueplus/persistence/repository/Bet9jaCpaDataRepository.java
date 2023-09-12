package com.valueplus.persistence.repository;

import com.valueplus.persistence.entity.Bet9jaAgentData;
import com.valueplus.persistence.entity.Bet9jaCpaData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Bet9jaCpaDataRepository  extends JpaRepository<Bet9jaCpaData, Long> {
    Optional<Bet9jaCpaData> findByUserId(Long userId);
    Optional<Bet9jaCpaData>findByAgentReferralCode(String code);
}
