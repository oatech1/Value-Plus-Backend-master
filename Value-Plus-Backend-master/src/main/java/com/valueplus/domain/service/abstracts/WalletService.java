package com.valueplus.domain.service.abstracts;

import com.google.gson.JsonObject;
import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.WalletHistoryModel;
import com.valueplus.domain.model.WalletModel;
import com.valueplus.persistence.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    WalletModel createWallet(User user);

    WalletModel getWallet(User user) throws ValuePlusException;

    WalletModel getWallet() throws ValuePlusException;

    Page<WalletModel> getAllWallet(Pageable pageable) throws ValuePlusException;

    List<WalletModel> createWalletForAllUsers();

    WalletModel creditWallet(User user, BigDecimal amount, String description);

    WalletModel creditAdminWallet(BigDecimal amount, String description) throws ValuePlusException;

    WalletModel debitWallet(User user, BigDecimal amount, String description) throws ValuePlusException;

    ResponseEntity<?> getAgentWalletBalance() throws ValuePlusException;

    Page<WalletHistoryModel> getWalletHistory(Pageable pageable) throws ValuePlusException;
}
