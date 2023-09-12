package com.valueplus.persistence.repository;

import com.valueplus.persistence.entity.BusinessSubcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessSubCategoryRepository extends JpaRepository<BusinessSubcategory,Long> {

    Optional<BusinessSubcategory> findByName(String name);

    List<BusinessSubcategory> findByDeletedFalse();
}
