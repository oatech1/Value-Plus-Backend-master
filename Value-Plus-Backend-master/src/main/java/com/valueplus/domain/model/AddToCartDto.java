package com.valueplus.domain.model;


import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.persistence.Column;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
@Data
@RequiredArgsConstructor
public class AddToCartDto {
    private Long id;
    private @NotNull Long productId;
    private @NotNull Integer quantity;
    private @NotNull BigDecimal price;
    private @NotNull boolean discounted;

}