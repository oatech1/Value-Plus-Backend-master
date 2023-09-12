package com.valueplus.domain.model;


import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data@Builder
public class BusinessSubCategoryModel {
    private int id;
    private int categoryId;
    @NotBlank(message = "Name field must not be blank")
    private String name;
    @NotBlank(message = "Category Name field must not be blank")
    private String businessCategoryName;

}
