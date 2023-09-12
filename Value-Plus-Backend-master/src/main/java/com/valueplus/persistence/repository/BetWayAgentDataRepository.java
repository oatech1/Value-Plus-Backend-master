package com.valueplus.persistence.repository;

import com.valueplus.betway.BetWayAgentData;
import com.valueplus.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BetWayAgentDataRepository extends JpaRepository<BetWayAgentData,Long> {
    Optional<BetWayAgentData> findByAgentReferralCode(String agentReferralCode);
    Optional<BetWayAgentData> findByUser(User user);

    @Query(value = "SELECT r.totalReferrals from BetWayAgentData r where r.user =?1 ")
    Integer countReferral(User userId);

}
