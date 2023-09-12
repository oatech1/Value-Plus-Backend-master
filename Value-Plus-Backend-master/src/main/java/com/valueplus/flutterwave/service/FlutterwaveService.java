package com.valueplus.flutterwave.service;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.AccountModel;
import com.valueplus.domain.model.ProductOrderTransactionResponse;
import com.valueplus.flutterwave.model.CollectionDataFlw;
import com.valueplus.flutterwave.model.CollectionResponseFl;
import com.valueplus.flutterwave.model.FlwTransferResponse;
import com.valueplus.flutterwave.model.ProductOrderTransactionResponseFlw;
import com.valueplus.paystack.model.AccountNumberModel;
import com.valueplus.paystack.model.BankModel;
import com.valueplus.paystack.model.CollectionData;
import com.valueplus.paystack.model.TransferResponse;

import java.math.BigDecimal;
import java.util.List;

public interface FlutterwaveService {

    List<BankModel> getBanks() throws ValuePlusException;

    AccountNumberModel resolveAccountNumber(String accountNumber, String bankCode) throws ValuePlusException;

    ProductOrderTransactionResponseFlw verifyCallback(String reference) throws ValuePlusException;

    FlwTransferResponse initiateTransfer(AccountModel accountModel, BigDecimal amount) throws ValuePlusException;

    CollectionDataFlw initiateCollection(CollectionData collectionData)throws ValuePlusException;
}
