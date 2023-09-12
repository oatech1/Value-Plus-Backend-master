package com.valueplus.persistence.repository;

import com.valueplus.persistence.entity.VerifyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import javax.transaction.Transactional;
import java.util.Optional;

@Repository
@Transactional
public interface VerifyTransactionRepository extends JpaRepository<VerifyTransaction, Long> {
    Optional<VerifyTransaction> findVerifyTransactionByReference(String ref);
}
