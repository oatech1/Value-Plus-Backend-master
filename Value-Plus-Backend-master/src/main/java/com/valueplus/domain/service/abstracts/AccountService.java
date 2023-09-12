package com.valueplus.domain.service.abstracts;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.AccountModel;
import com.valueplus.domain.model.AccountRequest;
import com.valueplus.domain.model.MessageResponse;
import com.valueplus.paystack.model.AccountNumberModel;
import com.valueplus.persistence.entity.User;

import java.util.List;

public interface AccountService {
    AccountNumberModel validateBankAccount(AccountRequest request) throws ValuePlusException;

    AccountModel create(User user, AccountRequest request) throws ValuePlusException;

    AccountModel update(Long id,User user, AccountRequest request) throws ValuePlusException;

    List<AccountModel> getAccount(User user) throws ValuePlusException;

    MessageResponse deleteBankAccount(Long accountId) throws ValuePlusException;
}
