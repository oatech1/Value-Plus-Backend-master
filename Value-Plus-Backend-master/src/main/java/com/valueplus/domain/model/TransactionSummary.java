package com.valueplus.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionSummary {
    BigDecimal totalApprovedWithdrawals;
    BigDecimal totalPendingWithdrawal;
    Integer pendingWithdrawalCount;
}
