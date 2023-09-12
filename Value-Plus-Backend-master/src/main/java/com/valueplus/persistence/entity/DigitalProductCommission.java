package com.valueplus.persistence.entity;

import com.valueplus.domain.enums.ProductProvider;
import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "digital_product_commission")
public class DigitalProductCommission extends BasePersistentEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ProductProvider productProvider;

    private BigDecimal commissionPercentage;
}
