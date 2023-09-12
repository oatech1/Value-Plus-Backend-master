package com.valueplus.domain.model;

import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class PercentageCommissionRequest {
    @NotBlank
    private String digitalProduct;
    @Min(value = 1)
    private BigDecimal commission;
}
