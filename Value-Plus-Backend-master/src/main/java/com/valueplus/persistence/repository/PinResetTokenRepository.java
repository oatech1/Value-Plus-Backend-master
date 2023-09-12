package com.valueplus.persistence.repository;

import com.valueplus.persistence.entity.PinResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PinResetTokenRepository extends JpaRepository<PinResetToken,Long> {
    Optional<PinResetToken> findByResetToken(String token);
    Optional<PinResetToken> findByUserId(Long userId);
}