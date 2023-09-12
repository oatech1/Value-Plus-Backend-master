package com.valueplus.persistence.repository;

import com.valueplus.persistence.entity.Bet9jaReferrals;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Bet9jaReferralsRepository extends JpaRepository<Bet9jaReferrals,Long> {
    Bet9jaReferrals findFirstByUsedFalse();
}
