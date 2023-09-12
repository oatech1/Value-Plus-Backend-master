package com.valueplus.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
@Accessors(chain = true)
public class ProductModel {

    private Long id;
    @NotBlank
    private String name;
    @NotBlank
    private String description;
    @NotBlank
    private String image;
    @NotBlank
    private String categoryName;
    @NotBlank
    private String subcategoryName;
    @NotNull
    @Min(1)
    private BigDecimal price;
    private String discountPercentage;
    private boolean discounted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean disabled;
    private BigDecimal discountPrice;
}
