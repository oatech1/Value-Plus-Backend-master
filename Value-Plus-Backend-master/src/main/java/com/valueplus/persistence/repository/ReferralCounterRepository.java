package com.valueplus.persistence.repository;

import com.valueplus.persistence.entity.ReferralCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReferralCounterRepository extends JpaRepository<ReferralCounter, Long> {
    Optional<ReferralCounter> findByReferralCode(String referralCode);
    boolean existsByReferralCode(String referralCode);

    @Query(value = "SELECT r.count from ReferralCounter r where r.referralCode =?1 ")
    Integer countCodeAndCount(String code );

}
