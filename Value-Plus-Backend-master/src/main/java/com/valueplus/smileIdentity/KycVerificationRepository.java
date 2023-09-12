package com.valueplus.smileIdentity;

import com.valueplus.persistence.entity.KycVerification;
import com.valueplus.persistence.entity.KycVerificationPartnerParams;
import com.valueplus.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KycVerificationRepository extends JpaRepository<KycVerification,Long> {
   KycVerification findByPartnerParams(KycVerificationPartnerParams partnerParams);
   KycVerification findBySmileJobID(String jobId);
   Optional<KycVerification> findByUser(User user);
}
