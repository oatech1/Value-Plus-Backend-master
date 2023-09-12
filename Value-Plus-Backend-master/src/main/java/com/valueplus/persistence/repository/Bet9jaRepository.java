package com.valueplus.persistence.repository;

import com.valueplus.persistence.entity.Authority;
import com.valueplus.persistence.entity.Bet9jaAgentData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Bet9jaRepository extends JpaRepository<Bet9jaAgentData, Long> {
    Optional<Bet9jaAgentData>findByUserId(Long userId);
    Optional<Bet9jaAgentData>findByAgentReferralCode(String code);
}
