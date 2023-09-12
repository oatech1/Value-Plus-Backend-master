package com.valueplus.persistence.repository;

import com.valueplus.persistence.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findById(Long accountId);
    List<Account> findAccountsByUser_Id(Long userId);
    Optional<Account> findAccountByAccountNumberAndBankCode(String accountNumber, String bankCode);
    Optional<Account>findByUserId(Long id);

}
