package com.valueplus.persistence.repository;

import com.valueplus.domain.enums.ProductProvider;
import com.valueplus.persistence.entity.DigitalProductCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import javax.transaction.Transactional;
import java.util.Optional;

@Transactional
@Repository
public interface DigitalProductCommissionRepository extends JpaRepository<DigitalProductCommission, Long> {

    @Query(value = "SELECT d FROM DigitalProductCommission d WHERE d.productProvider = ?1")
    Optional<DigitalProductCommission> findByProductProvider(ProductProvider productProvider);
}
