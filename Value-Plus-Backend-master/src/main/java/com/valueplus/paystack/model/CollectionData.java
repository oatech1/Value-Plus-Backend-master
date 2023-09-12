package com.valueplus.paystack.model;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CollectionData {
    private String email;
    private BigDecimal amount;
}
