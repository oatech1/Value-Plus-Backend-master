package com.valueplus.persistence.repository;

import com.valueplus.domain.enums.OrderStatus;
import com.valueplus.persistence.entity.ProductOrder;
import com.valueplus.persistence.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Transactional
@Repository
public interface ProductOrderRepository extends JpaRepository<ProductOrder, Long>, JpaSpecificationExecutor<ProductOrder> {

    Page<ProductOrder> findByProduct_id(Long productId, Pageable pageable);

    Page<ProductOrder> findAllByProductSkuId(String skuId, Pageable pageable);

    Page<ProductOrder> findByUser_idAndProduct_id(Long userId, Long productId, Pageable pageable);

    Page<ProductOrder> findByUser_id(Long userid, Pageable pageable);

    List<ProductOrder> findByUser_idAndStatus(Long userid, OrderStatus status);

    List<ProductOrder> findByStatus(OrderStatus status);

    Optional<ProductOrder> findByIdAndUser_id(Long id, Long userId);

    List<ProductOrder> findAllByProductIdAndStatus(Long productId, OrderStatus orderStatus);

    List<ProductOrder>findByProductSkuIdAndUser_id(String skuId, Long id);

    List<ProductOrder>findByProductSkuId(String skuId);

    int countByUserAndStatus(User user, OrderStatus status );

    int countByUserAndStatusAndCreatedAtBetween(User user, OrderStatus orderStatus, LocalDateTime startTime,LocalDateTime endTime);

}
