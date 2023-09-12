package com.valueplus.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.valueplus.domain.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrdersModel {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    private LocalDateTime dateCreated;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String skuId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal totalCostPrice;
    private String customerName;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal totalSellingPrice;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal commission;
    private OrderStatus status;
}
