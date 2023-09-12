package com.valueplus.persistence.repository;

import com.valueplus.domain.enums.OrderStatus;
import com.valueplus.persistence.entity.Orders;
import com.valueplus.persistence.entity.ProductOrder;
import com.valueplus.persistence.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Orders,Long>, JpaSpecificationExecutor<Orders> {
    Page<Orders> findByUser_id(Long id, Pageable pageable);
    Optional<Orders> findByIdAndUser_id(Long id, Long userId);
    Optional<Orders> findBySkuId(String skuId);
    List<Orders> findByUserAndStatus(User user, OrderStatus orderStatus);
}
