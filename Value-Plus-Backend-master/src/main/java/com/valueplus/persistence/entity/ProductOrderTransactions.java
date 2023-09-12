package com.valueplus.persistence.entity;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "product_order_transaction")
public class ProductOrderTransactions extends BasePersistentEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private BigDecimal amount;
    private String authorization_url;
    private String access_code;
    private String reference;
    private String status;
    private String orderSkuId;
}
