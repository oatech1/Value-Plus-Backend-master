package com.valueplus.domain.service.concretes;

import com.valueplus.app.config.audit.AuditEventPublisher;
import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.enums.OrderStatus;
import com.valueplus.domain.model.BusinessCategoryModel;
import com.valueplus.domain.model.BusinessSubCategoryModel;
import com.valueplus.domain.model.ProductCommissionDTO;
import com.valueplus.domain.model.ProductModel;
import com.valueplus.domain.service.abstracts.ProductService;
import com.valueplus.domain.util.UserUtils;
import com.valueplus.persistence.entity.*;
import com.valueplus.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

import static com.valueplus.domain.enums.ActionType.*;
import static com.valueplus.domain.enums.EntityType.*;
import static com.valueplus.domain.util.MapperUtil.copy;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultProductService implements ProductService {

    private final ProductRepository repository;
    private final AuditEventPublisher auditEvent;
    private final WalletHistoryRepository walletHistoryRepository;
    private final ProductOrderRepository productOrderRepository;
    private final ProductRepository productRepository;
    private final BusinessSubCategoryRepository businessSubCategoryRepository;
    private final BusinessCategoryRepository businessCategoryRepository;

    @Override
    public ProductModel create(ProductModel product) throws ValuePlusException {
        Optional<BusinessCategory> optionalBusinessCategory =
                businessCategoryRepository.findByName(product.getCategoryName());
        if(optionalBusinessCategory.isEmpty()){
            throw new ValuePlusException("No such Category");
        }
        Optional<BusinessSubcategory> optionalBusinessSubcategory =
                businessSubCategoryRepository.findByName(product.getSubcategoryName());
        if(optionalBusinessSubcategory.isEmpty()){
            throw new ValuePlusException("No such Subcategory");
        }
        if(!optionalBusinessCategory.get().getBusinessSubcategories().contains(optionalBusinessSubcategory.get())){
           throw new ValuePlusException("The subcategory does not belong to the specified category");
        }
        if (repository.findByName(product.getName()).isPresent()) {
            throw new ValuePlusException("Product with name exists");
        }
        var entity = Product.fromModel(product);
        BigDecimal percentage = BigDecimal.valueOf(Double.valueOf(product.getDiscountPercentage()));
        entity.setBusinessCategory(optionalBusinessCategory.get());
        entity.setBusinessSubcategory(optionalBusinessSubcategory.get());
        entity.setDiscountPrice(createDiscountPrice(entity,percentage));
        var savedEntity = repository.save(entity);

        BusinessSubcategory subcategory = optionalBusinessSubcategory.get();
        subcategory.getProducts().add(savedEntity);
        businessSubCategoryRepository.save(subcategory);

        auditEvent.publish(new Object(), savedEntity, PRODUCT_CREATE, PRODUCT);
        return savedEntity.toModel();
    }

    @Override
    public ProductModel update(Long id, ProductModel product) throws ValuePlusException {
        if (!id.equals(product.getId())) {
            throw new ValuePlusException("Id not matching with Product id", HttpStatus.BAD_REQUEST);
        }

        Product entity = getProduct(product.getId());
        var oldObject = copy(entity, Product.class);

        Optional<Product> productWithSameName = repository.findByNameAndIdIsNot(
                product.getName(),
                product.getId());

        if (productWithSameName.isPresent()) {
            throw new ValuePlusException("Product name exists", HttpStatus.BAD_REQUEST);
        }

        BigDecimal percentage = BigDecimal.valueOf(Double.valueOf(product.getDiscountPercentage()));
        entity.setDescription(product.getDescription());
        entity.setName(product.getName());
        entity.setPrice(product.getPrice());
        entity.setImage(product.getImage());
        entity.setDiscounted(product.isDiscounted());
        entity.setDiscountPrice(createDiscountPrice(entity,percentage));
        log.info(entity.getDiscountPrice().toString());
        entity.setDiscountPercentage(product.getDiscountPercentage());

        var savedEntity = repository.save(entity);
        auditEvent.publish(oldObject, savedEntity, PRODUCT_UPDATE, PRODUCT);

        return savedEntity.toModel();
    }

    @Override
    public ProductModel disable(Long id) throws ValuePlusException {
        Product product = getProduct(id);
        var oldObject = copy(product, Product.class);

        product.setDisabled(true);

        var savedEntity = repository.save(product);
        auditEvent.publish(oldObject, savedEntity, PRODUCT_STATUS_DISABLE, PRODUCT);
        return savedEntity.toModel();

    }

    @Override
    public ProductModel enable(Long id) throws ValuePlusException {
        Product product = getProduct(id);
        var oldObject = copy(product, Product.class);

        product.setDisabled(false);

        var savedEntity = repository.save(product);
        auditEvent.publish(oldObject, savedEntity, PRODUCT_STATUS_ENABLE, PRODUCT);
        return savedEntity.toModel();
    }

    @Override
    public boolean delete(Long id) throws ValuePlusException {
        Product entity = getProduct(id);
        var oldObject = copy(entity, Product.class);

        entity.setDeleted(true);

        var savedEntity = repository.save(entity);
        auditEvent.publish(oldObject, savedEntity, PRODUCT_DELETE, PRODUCT);
        return true;
    }

    @Override
    public ProductModel get(Long id) throws ValuePlusException {
        return getProduct(id).toModel();
    }

    @Override
    public Page<ProductModel> get(Pageable pageable, User user) throws ValuePlusException {
        try {
            log.info("Before DB CALL");
            Page<Product> products = UserUtils.isAgent(user) || UserUtils.isSuperAgent(user)
                    ? repository.findProductsByDisabledFalse(pageable)
                    : repository.findAll(pageable);
                log.info("after db call ");
            return products.map(Product::toModel);
        } catch (Exception e) {
            throw new ValuePlusException("Error getting all products", e);
        }
    }

    public Product getProduct(Long id) throws ValuePlusException {
        return repository.findById(id)
                .orElseThrow(() -> new ValuePlusException("Product does not exist", NOT_FOUND));
    }

    @Override
    public Page<ProductModel> searchProduct(String keyword, Pageable pageable){
        Page<Product> products = repository.search(keyword, pageable);

        return products.map(Product::toModel);
    }

    @Override
    public ProductCommissionDTO.Commissions getCommissionMadeOnProduct(Pageable pageable, Long productId){

        List<ProductOrder> productCompletedOrders = productOrderRepository.findAllByProductIdAndStatus(productId, OrderStatus.COMPLETED);

        List<ProductCommissionDTO.WalletHistory> walletData = new ArrayList<>();

        productCompletedOrders.stream()
            .forEach((productOrder) -> {
                Optional<WalletHistory> walletHistory = walletHistoryRepository.findCommissionOnProduct(String.format("Credit from ProductOrder completion (id: %d)", productOrder.getId()));

                if(walletHistory.isPresent()){

                    ProductCommissionDTO.WalletHistory wallet = ProductCommissionDTO.WalletHistory
                        .builder()
                        .amount(walletHistory.get().getAmount())
                        .type(walletHistory.get().getType())
                        .walletId(walletHistory.get().getWallet().getId())
                        .description(walletHistory.get().getDescription())
                        .createdAt(walletHistory.get().getCreatedAt())
                        .updatedAt(walletHistory.get().getUpdatedAt())
                        .build();

                    walletData.add(wallet);
                }
            });

        var commissions = ProductCommissionDTO.Commissions.builder()
            .productId(productId)
            .commissions(walletData)
            .build();

        return commissions;
    }

    @Override
    public Page<Product> getProductByCategory(String categoryName, Pageable pageable){
            Optional<BusinessCategory> businessCategory = businessCategoryRepository.findByName(categoryName);
            BusinessCategory category = new BusinessCategory();
            if (businessCategory.isPresent())
            {
                category = businessCategory.get();
            }
            Page<Product> productList = this.productRepository.findByBusinessCategoryAndDisabledFalseAndDeletedFalse(category,pageable);
            return productList;
    }

   @Override
    public Page<Product> getProductBySubCategory(String subCategoryName, Pageable pageable){
        Optional<BusinessSubcategory> businessSubCategory = businessSubCategoryRepository.findByName(subCategoryName);
        BusinessSubcategory category = new BusinessSubcategory();
        if (businessSubCategory.isPresent())
        {
            category = businessSubCategory.get();
        }
        Page<Product> productsList = this.productRepository.findByBusinessSubcategoryAndDisabledIsFalseAndDeletedIsFalse(category,pageable);
        return productsList;
    }

    @Override
    public List<BusinessSubcategory> getBusinessSubCategory(){
        List<BusinessSubcategory> businessSubcategories = businessSubCategoryRepository.findByDeletedFalse();
        return businessSubcategories;
    }

    @Override
    public List<BusinessCategory> getBusinessCategory(){
        List<BusinessCategory> businessCategories = businessCategoryRepository.findAll();
        return businessCategories;
    }

    @Override
    public BusinessCategoryModel create(BusinessCategoryModel categoryModel) throws ValuePlusException {
        if (businessCategoryRepository.findByName(categoryModel.getName()).isPresent()) {
            throw new ValuePlusException("Category with name exists");
        }

        List<BusinessSubcategory> subcategories = new ArrayList<>();
        BusinessCategory businessCategory = BusinessCategory.builder()
                .name(categoryModel.getName())
                .photo(categoryModel.getPhoto())
                .businessSubcategories(subcategories)
                .deleted(false)
                .build();

        BusinessCategory savedCategory = businessCategoryRepository.save(businessCategory);

        auditEvent.publish(new Object(), savedCategory, CATEGORY_CREATE, CATEGORY);

        return BusinessCategoryModel.builder()
                .id(savedCategory.getId())
                .name(savedCategory.getName())
                .photo(savedCategory.getName())
                .build();
    }


    @Override
    public BusinessSubCategoryModel create(BusinessSubCategoryModel subCategoryModel) throws ValuePlusException {
        Optional<BusinessCategory> optionalBusinessCategory = businessCategoryRepository.findByName(subCategoryModel.getBusinessCategoryName());
        if(optionalBusinessCategory.isEmpty()){
            throw new ValuePlusException("No such Category");
        }
        if(businessSubCategoryRepository.findByName(subCategoryModel.getName()).isPresent()) {
            throw new ValuePlusException("SubCategory with name exists");
        }
        BusinessCategory existingCategory = optionalBusinessCategory.get();
        BusinessSubcategory businessSubCategory = BusinessSubcategory.builder()
                .name(subCategoryModel.getName())
                .businessCategory(existingCategory)
                .categoryId(existingCategory.getId())
                .deleted(false)
                .build();

        BusinessSubcategory savedSubCategory = businessSubCategoryRepository.save(businessSubCategory);
        existingCategory.getBusinessSubcategories().add(businessSubCategory);
        businessCategoryRepository.save(existingCategory);

        auditEvent.publish(new Object(), savedSubCategory, SUBCATEGORY_CREATE, SUBCATEGORY);

        return BusinessSubCategoryModel.builder()
                .id(savedSubCategory.getId())
                .name(savedSubCategory.getName())
                .categoryId(savedSubCategory.getCategoryId())
                .businessCategoryName(savedSubCategory.getBusinessCategory().getName())
                .build();
    }


    @Override
    public BusinessCategoryModel editCategory(BusinessCategoryModel categoryModel, Long id) throws ValuePlusException {
    BusinessCategory category = businessCategoryRepository.findById(id).get();
    if(Objects.nonNull(categoryModel.getName()) && !"".equalsIgnoreCase(categoryModel.getName())){
        category.setName(categoryModel.getName());
    }
    if(Objects.nonNull(categoryModel.getPhoto()) && !"".equalsIgnoreCase(categoryModel.getPhoto())){
        category.setPhoto(categoryModel.getPhoto());
    }
    BusinessCategory savedCategory = businessCategoryRepository.save(category);

        return BusinessCategoryModel.builder()
                .id(savedCategory.getId())
                .name(savedCategory.getName())
                .photo(savedCategory.getName())
                .build();
    }

    @Override
    public BusinessSubCategoryModel editSubCategory(BusinessSubCategoryModel subCategoryModel, Long id) throws ValuePlusException {
        BusinessSubcategory subcategory = businessSubCategoryRepository.findById(id).get();
        if (Objects.nonNull(subCategoryModel.getName()) && !"".equalsIgnoreCase(subCategoryModel.getName())) {
            subcategory.setName(subCategoryModel.getName());
        }
        if (Objects.nonNull(subCategoryModel.getBusinessCategoryName()) && !"".equalsIgnoreCase(subCategoryModel.getBusinessCategoryName())) {
            Optional<BusinessCategory> optionalBusinessCategory = businessCategoryRepository.findByName(subCategoryModel.getBusinessCategoryName());
            if (optionalBusinessCategory.isPresent()) {
                BusinessCategory category = optionalBusinessCategory.get();
                subcategory.setBusinessCategory(category);
                subcategory.setCategoryId(category.getId());
            } else {
                throw new ValuePlusException("No such Category");
            }
        }
        BusinessSubcategory savedSubCategory = businessSubCategoryRepository.save(subcategory);

        return BusinessSubCategoryModel.builder()
                .id(savedSubCategory.getId())
                .name(savedSubCategory.getName())
                .categoryId(savedSubCategory.getCategoryId())
                .businessCategoryName(savedSubCategory.getBusinessCategory().getName())
                .build();
    }

    private BigDecimal createDiscountPrice(Product product, BigDecimal percentage){
      BigDecimal cen =  percentage.divide(BigDecimal.valueOf(100.00));
       BigDecimal cut= cen.multiply(product.getPrice());
       return product.getPrice().subtract(cut);
    }

}