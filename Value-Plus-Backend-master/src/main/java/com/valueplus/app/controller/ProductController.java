package com.valueplus.app.controller;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.BusinessCategoryModel;
import com.valueplus.domain.model.BusinessSubCategoryModel;
import com.valueplus.domain.model.ProductCommissionDTO;
import com.valueplus.domain.model.ProductModel;
import com.valueplus.domain.service.abstracts.ProductService;
import com.valueplus.domain.util.UserUtils;
import com.valueplus.persistence.entity.BusinessCategory;
import com.valueplus.persistence.entity.BusinessSubcategory;
import com.valueplus.persistence.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import java.util.List;

import static org.springframework.data.domain.Sort.Direction.DESC;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "v1/products", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductController {

    private final ProductService productService;

    @PreAuthorize("hasAuthority('CREATE_PRODUCT')")
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ProductModel create(@Valid @RequestBody ProductModel productModel) throws ValuePlusException {
        return productService.create(productModel);
    }

    @PreAuthorize("hasAuthority('UPDATE_PRODUCT')")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductModel update(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductModel productModel) throws ValuePlusException {
        return productService.update(id, productModel);
    }

    @PreAuthorize("hasAuthority('DISABLE_PRODUCT')")
    @PostMapping("/{id}/disable")
    @ResponseStatus(HttpStatus.OK)
    public ProductModel disable(@PathVariable("id") Long id) throws ValuePlusException {
        return productService.disable(id);
    }

    @PreAuthorize("hasAuthority('ENABLE_PRODUCT')")
    @PostMapping("/{id}/enable")
    @ResponseStatus(HttpStatus.OK)
    public ProductModel enable(@PathVariable("id") Long id) throws ValuePlusException {
        return productService.enable(id);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductModel get(@PathVariable("id") Long id) throws ValuePlusException {
        return productService.get(id);
    }

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public Page<ProductModel> getAll(@PageableDefault(sort = "id", direction = DESC) Pageable pageable) throws ValuePlusException {
        return productService.get(pageable, UserUtils.getLoggedInUser());
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public Page<ProductModel> search(@PageableDefault(sort = "id", direction = DESC) Pageable pageable, @Param("keyword") String keyword) throws ValuePlusException {
        return (keyword == "" || keyword == null)? productService.get(pageable, UserUtils.getLoggedInUser()): productService.searchProduct(keyword, pageable);
    }

    @GetMapping("/commissions/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductCommissionDTO.Commissions getProductCommissionById(@PageableDefault(sort = "id", direction = DESC) Pageable pageable, @PathVariable String id){
        return productService.getCommissionMadeOnProduct(pageable, Long.parseLong(id));
    }

    @GetMapping("/category/{categoryName}")
    @ResponseStatus(HttpStatus.OK)
    public Page<Product> findProductByCateogry (@PageableDefault(sort = "id", direction = DESC) Pageable pageable, @PathVariable("categoryName") String categoryName){
        return productService.getProductByCategory(categoryName,pageable);
    }

    @GetMapping("/subcategory/{subCategoryName}")
    @ResponseStatus(HttpStatus.OK)
    public Page<Product> findProductBySubCateogry (@PageableDefault(sort = "id", direction = DESC) Pageable pageable, @PathVariable("subCategoryName") String categoryName){
        return productService.getProductBySubCategory(categoryName,pageable);
    }

    @GetMapping("/category")
    @ResponseStatus(HttpStatus.OK)
    public List<BusinessCategory> getProductCategory (){
        return productService.getBusinessCategory();
    }

    @GetMapping("/subcategory")
    @ResponseStatus(HttpStatus.OK)
    public List<BusinessSubcategory> getProductSubCategory (){
        return productService.getBusinessSubCategory();
    }

    @PreAuthorize("hasAuthority('CREATE_PRODUCT')")
    @PostMapping("/category")
    @ResponseStatus(HttpStatus.OK)
    public BusinessCategoryModel createCategory(@Valid @RequestBody BusinessCategoryModel categoryModel) throws ValuePlusException {
        return productService.create(categoryModel);
    }

    @PreAuthorize("hasAuthority('CREATE_PRODUCT')")
    @PostMapping("/subcategory")
    @ResponseStatus(HttpStatus.OK)
    public BusinessSubCategoryModel createSubCategory(@Valid @RequestBody BusinessSubCategoryModel subCategoryModel) throws ValuePlusException {
        return productService.create(subCategoryModel);
    }

    @PreAuthorize("hasAuthority('CREATE_PRODUCT')")
    @PutMapping("/category/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BusinessCategoryModel editCategory(@Valid @RequestBody BusinessCategoryModel categoryModel, @PathVariable("id") Long id) throws ValuePlusException{
        return productService.editCategory(categoryModel, id);
    }

    @PreAuthorize("hasAuthority('CREATE_PRODUCT')")
    @PutMapping("/subcategory/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BusinessSubCategoryModel editSubCategory(@Valid @RequestBody BusinessSubCategoryModel subCategoryModel, @PathVariable("id") long id) throws ValuePlusException{
        return productService.editSubCategory(subCategoryModel, id);
    }

}
