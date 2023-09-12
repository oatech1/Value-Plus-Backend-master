package com.valueplus.smileIdentity;

import com.valueplus.persistence.entity.Account;
import com.valueplus.persistence.entity.KycVerificationPartnerParams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KycVerificationPartnerParamsRepository extends JpaRepository<com.valueplus.persistence.entity.KycVerificationPartnerParams, Long> {
KycVerificationPartnerParams findByUserId(String userId);
Optional<KycVerificationPartnerParams> findByJobId(String jobId);
}
