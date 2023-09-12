package com.valueplus.domain.service.abstracts;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.paystack.model.AccountNumberModel;
import com.valueplus.paystack.model.BankModel;
import com.valueplus.paystack.model.TransferVerificationResponse;

import java.util.List;

public interface BankService {
    List<BankModel> getBanks() throws ValuePlusException;

    AccountNumberModel resolveAccountNumber(String accountNumber, String bankCode) throws ValuePlusException;

    TransferVerificationResponse verifyTransfer(String reference) throws ValuePlusException;
}
