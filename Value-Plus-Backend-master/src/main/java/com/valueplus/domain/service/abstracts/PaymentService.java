package com.valueplus.domain.service.abstracts;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.AccountModel;
import com.valueplus.paystack.model.*;
import java.math.BigDecimal;

public interface PaymentService {
    TransferResponse transfer(String accountNumber,
                              String bankCode,
                              BigDecimal amount) throws ValuePlusException;

    TransferResponse transfer(AccountModel accountModel,
                              BigDecimal amount) throws ValuePlusException;

    AccountNumberModel resolveAccountNumber(String accountNumber,
                                            String bankCode) throws ValuePlusException;

    TransferVerificationResponse verifyTransfer(String reference) throws ValuePlusException;
}
