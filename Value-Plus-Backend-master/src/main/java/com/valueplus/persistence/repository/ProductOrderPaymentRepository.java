package com.valueplus.persistence.repository;

import com.valueplus.persistence.entity.ProductOrderTransactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import javax.transaction.Transactional;
import java.util.Optional;

@Transactional
@Repository
public interface ProductOrderPaymentRepository extends JpaRepository<ProductOrderTransactions, Long> {
    Optional<ProductOrderTransactions> findByReference(String reference);
    Optional<ProductOrderTransactions> findByOrderSkuId(String orderSkuId);
}
