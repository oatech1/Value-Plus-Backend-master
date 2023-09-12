package com.valueplus.persistence.repository;

import com.valueplus.persistence.entity.BusinessCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessCategoryRepository extends JpaRepository<BusinessCategory,Long> {

    Optional<BusinessCategory>findByName(String name);
}
