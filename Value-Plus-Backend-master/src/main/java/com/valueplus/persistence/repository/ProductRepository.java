package com.valueplus.persistence.repository;

import com.valueplus.persistence.entity.BusinessCategory;
import com.valueplus.persistence.entity.BusinessSubcategory;
import com.valueplus.persistence.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findProductsByDisabledFalse(Pageable pageable);

    Optional<Product> findByNameAndIdIsNot(String name, Long id);

    Optional<Product> findByName(String name);

    Set<Product> findByIdIn(List<Long> productIds);

    @Query("SELECT p FROM Product p WHERE lower(p.name) LIKE lower(concat('%', ?1, '%'))"
            + " OR lower(p.description) LIKE lower(concat('%', ?1, '%'))"
            + " OR CONCAT(p.price, '') LIKE %?1%")
    Page<Product> search(String keyword, Pageable pageable);

    Page<Product>findByBusinessSubcategoryAndDisabledIsFalseAndDeletedIsFalse(BusinessSubcategory businessSubcategory, Pageable pageable);

    Page<Product>findByBusinessCategoryAndDisabledFalseAndDeletedFalse(BusinessCategory businessCategory,Pageable pageable);
}
