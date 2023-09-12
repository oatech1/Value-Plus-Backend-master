package com.valueplus.domain.model;

import com.valueplus.domain.enums.ProductProvider;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
public class DigitalProductCommissionResponse {
    private ProductProvider productProvider;
    private BigDecimal commission;
}
