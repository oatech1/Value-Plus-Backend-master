package com.valueplus.persistence.repository;

import com.valueplus.persistence.entity.OtpVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerificationToken,Long> {

    Optional<OtpVerificationToken>findByUserIdAndVerificationTokenAndExpireTimeLessThanEqual(Long userId, String token, LocalDateTime time);

    Optional<OtpVerificationToken>findByUserIdAndVerificationToken(Long userId, String token);

    Optional<OtpVerificationToken>findByReferenceID(String referenceID);

    Optional<OtpVerificationToken>findByVerificationToken(String token);
}
