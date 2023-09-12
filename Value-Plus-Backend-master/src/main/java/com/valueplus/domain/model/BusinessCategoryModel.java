package com.valueplus.domain.model;


import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Builder
@Data
public class BusinessCategoryModel {

    private int id;
    @NotBlank(message = "Category name must not be blank")
    private String name;
    private String photo;
}
