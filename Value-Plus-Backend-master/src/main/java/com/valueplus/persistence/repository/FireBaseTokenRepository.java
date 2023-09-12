package com.valueplus.persistence.repository;


import com.valueplus.persistence.entity.FirebaseToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FireBaseTokenRepository extends JpaRepository<FirebaseToken,Long> {
    Optional<FirebaseToken> findByUserId(Long userId);
}
