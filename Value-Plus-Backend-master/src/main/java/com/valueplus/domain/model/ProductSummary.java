package com.valueplus.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSummary {
    BigDecimal totalProductSales;
    BigDecimal totalProductAgentProfits;
}
