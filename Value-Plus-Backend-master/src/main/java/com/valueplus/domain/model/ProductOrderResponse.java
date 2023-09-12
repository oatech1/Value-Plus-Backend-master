package com.valueplus.domain.model;

import com.valueplus.domain.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Data
public class ProductOrderResponse {
    private LocalDateTime dateCreated;
    private String skuId;
    private Long orderId;
    private List<ProductOrderModel> productOrderModelList;
    private BigDecimal totalCostPrice;
    private String customerName;
    private BigDecimal totalSellingPrice;
    private BigDecimal commission;
    private OrderStatus status;
}
