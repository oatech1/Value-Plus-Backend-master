package com.valueplus.persistence.repository;

import com.valueplus.domain.enums.ProductProvider;
import com.valueplus.persistence.entity.ProductProviderUser;
import com.valueplus.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface ProductProviderUserRepository extends JpaRepository<ProductProviderUser, Long> {
    Optional<ProductProviderUser> findByAgentCodeAndProvider(String agentCode, ProductProvider provider);
    Optional<ProductProviderUser> findByUserIdAndProvider(Long userId, ProductProvider provider);
    List<ProductProviderUser>findByUserAndProvider(User user, ProductProvider productProvider);
    List<ProductProviderUser> findByProvider(ProductProvider productProvider);
    List<ProductProviderUser> findByUserId(Long userId);
}
