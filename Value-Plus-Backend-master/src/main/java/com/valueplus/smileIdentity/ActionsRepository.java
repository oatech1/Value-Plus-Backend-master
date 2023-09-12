package com.valueplus.smileIdentity;

import com.valueplus.persistence.entity.KycVerification;
import com.valueplus.persistence.entity.KycVerificationActions;
import com.valueplus.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActionsRepository extends JpaRepository<KycVerificationActions,Long> {

    KycVerificationActions findByUser(User user);
    KycVerificationActions findByJobId(String jobId);
    List<KycVerificationActions> findKycVerificationActionsByUser(User user);
}
