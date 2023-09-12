package com.valueplus.persistence.repository;

import com.valueplus.domain.enums.TransactionType;
import com.valueplus.persistence.entity.Wallet;
import com.valueplus.persistence.entity.WalletHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@Transactional
public interface WalletHistoryRepository extends JpaRepository<WalletHistory, Long> {

    Page<WalletHistory> findWalletHistoriesByWallet_Id(Long walletId, Pageable pageable);


    @Query("SELECT wh from WalletHistory wh WHERE " +
            "wh.wallet.user.id = :userId AND wh.createdAt >= :startDate AND wh.createdAt <= :endDate")
    Page<WalletHistory> findByUserIdAndDateBetween(@Param("userId") Long userId,
                                                   @Param("startDate") LocalDateTime starDate,
                                                   @Param("endDate") LocalDateTime endDate,
                                                   Pageable pageable);

    @Query("SELECT wh from WalletHistory wh WHERE wh.createdAt >= :startDate AND wh.createdAt <= :endDate")
    Page<WalletHistory> findByDateBetween(@Param("startDate") LocalDateTime starDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          Pageable pageable);

    @Query("SELECT cp FROM WalletHistory cp WHERE cp.description LIKE %?1%")
    Optional<WalletHistory> findCommissionOnProduct(String keyword);

    @Query("SELECT SUM(wh.amount) from WalletHistory wh where wh.wallet =?1 and wh.type =?2 and wh.description  LIKE 'ValuePlus Commission%'")
    Optional<BigDecimal> findCommissionForVpAgent(Wallet vpAgentWallet, TransactionType type);
}
