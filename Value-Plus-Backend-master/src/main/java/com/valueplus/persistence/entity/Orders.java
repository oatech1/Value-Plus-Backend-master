package com.valueplus.persistence.entity;

import com.valueplus.domain.enums.OrderStatus;
import com.valueplus.domain.model.AgentDto;
import com.valueplus.domain.model.OrdersModel;
import com.valueplus.domain.model.ProductOrderModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "orders")
@Accessors(chain = true)
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime dateCreated;
    private String skuId;
    private BigDecimal totalCostPrice;
    private String customerName;
    private BigDecimal totalSellingPrice;
    private BigDecimal commission;
    private String customerAddress;
    private String customerPhone;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
