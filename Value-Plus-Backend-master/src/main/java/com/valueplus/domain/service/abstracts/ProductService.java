package com.valueplus.domain.service.abstracts;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.BusinessCategoryModel;
import com.valueplus.domain.model.BusinessSubCategoryModel;
import com.valueplus.domain.model.ProductCommissionDTO;
import com.valueplus.domain.model.ProductModel;
import com.valueplus.persistence.entity.BusinessCategory;
import com.valueplus.persistence.entity.BusinessSubcategory;
import com.valueplus.persistence.entity.Product;
import com.valueplus.persistence.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    ProductModel create(ProductModel product) throws ValuePlusException;

    ProductModel update(Long id, ProductModel product) throws ValuePlusException;

    ProductModel disable(Long id) throws ValuePlusException;

    ProductModel enable(Long id) throws ValuePlusException;


    boolean delete(Long id) throws ValuePlusException;

    ProductModel get(Long id) throws ValuePlusException;

    Page<ProductModel> get(Pageable pageable, User user) throws ValuePlusException;

    Page<ProductModel> searchProduct(String keyword, Pageable pageable);

    ProductCommissionDTO.Commissions getCommissionMadeOnProduct(Pageable pageable, Long productId);



    Page<Product> getProductByCategory(String categoryName, Pageable pageable);

    Page<Product> getProductBySubCategory(String subCategoryName, Pageable pageable);

    List<BusinessSubcategory> getBusinessSubCategory();

    List<BusinessCategory> getBusinessCategory();
    BusinessCategoryModel create(BusinessCategoryModel categoryModel) throws ValuePlusException;
    BusinessSubCategoryModel create(BusinessSubCategoryModel subCategoryModel) throws ValuePlusException;

    BusinessSubCategoryModel editSubCategory(BusinessSubCategoryModel subCategoryModel,Long id) throws ValuePlusException;

    BusinessCategoryModel editCategory(BusinessCategoryModel categoryModel, Long id) throws ValuePlusException;
}
