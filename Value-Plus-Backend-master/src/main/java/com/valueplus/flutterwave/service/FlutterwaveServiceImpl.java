package com.valueplus.flutterwave.service;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.AccountModel;
import com.valueplus.domain.service.abstracts.HttpApiClient;
import com.valueplus.flutterwave.model.*;
import com.valueplus.paystack.model.*;
import com.valueplus.flutterwave.model.ResponseModel;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.valueplus.domain.util.FunctionUtil.convertToNaira;
import static java.lang.String.format;
import static java.lang.String.join;
@Service
public class FlutterwaveServiceImpl extends HttpApiClient implements FlutterwaveService {

    private static final String TRANSFER_TYPE = "nuban";
    private static final String CURRENCY = "NGN";
    private final FlutterwaveConfig config;

    public FlutterwaveServiceImpl(RestTemplate restTemplate, FlutterwaveConfig config ) {
        super("flutterwave", restTemplate,config.getBaseUrl());
        this.config = config;
    }

    @Override
    public List<BankModel> getBanks() throws ValuePlusException {
        Map<String, String> header = prepareRequestHeader();
        var type = new ParameterizedTypeReference<ResponseModel<List<BankModel>>>() {};

        ResponseModel<List<BankModel>> result = sendRequest(HttpMethod.GET, "/bank/NG", null, header, type);

        if (result.equals(false)) {
            throw new ValuePlusException(result.getMessage());
        }
        return result.getData();
    }

//    @Override
//    public TransferResponse transfer( String accountNumber, String bankCode, BigDecimal amount ) throws ValuePlusException {
////        AccountNumberModel accountModel = resolveAccountNumber(accountNumber, bankCode);
////
////        TransferRecipient recipient = createTransferRecipient(
////                accountModel.getAccountNumber(),
////                accountModel.getAccountName(),
////                bankCode);
////
////        String reference = getReference();
////        return initiateTransfer(recipient.getRecipientCode(), amount, config.getPaymentReason(), reference);
//        return null;
//    }
//
//    @Override
//    public TransferResponse transfer( AccountModel accountModel, BigDecimal amount ) throws ValuePlusException {
//        return null;
//    }

    @Override
    public AccountNumberModel resolveAccountNumber( String accountNumber, String bankCode ) throws ValuePlusException {
        Map<String, String> header = prepareRequestHeader();
        Map<Object, Object> requestEntity = new HashMap<>();
        requestEntity.put("account_number", accountNumber);
        requestEntity.put("account_bank", bankCode);

        var type = new ParameterizedTypeReference<ResponseModel<AccountNumberModel>>() {};
        ResponseModel<AccountNumberModel> result = sendRequest(HttpMethod.POST, "/accounts/resolve", requestEntity, header, type);

        if (result.getStatus().equals(false)) {
            throw new ValuePlusException(result.getMessage());
        }

        return result.getData();
    }

    @Override
    public ProductOrderTransactionResponseFlw verifyCallback(String reference) throws ValuePlusException {

        String requestUrl = format("transactions/"+reference+"/verify");
        Map<String, String> headers = prepareRequestHeader();
        var type = new ParameterizedTypeReference<ProductOrderTransactionResponseFlw>() {};
        ProductOrderTransactionResponseFlw result = sendRequest(HttpMethod.GET, requestUrl, null, headers, type);
        if (!result.getStatus().equalsIgnoreCase("success")) {
            throw new ValuePlusException(result.getMessage());
        }
        return result;
    }

    private Map<String, String> prepareRequestHeader() {
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", join(" ", "Bearer", getApiKey()));
        System.out.println("getApiKey() = " + getApiKey());
        return header;
    }

    private String getApiKey() {
        return FlutterwaveConfig.Domain.LIVE.equals(config.getDomain()) ? config.getLiveApiKey() : config.getTestApiKey();
    }
    private String getCallbackUrl() {
        return FlutterwaveConfig.Domain.LIVE.equals(config.getDomain()) ? config.getTestTransferCallBackUrl() : config.getLiveTransferCallBackUrl();
    }

//    public TransferRecipient createTransferRecipient(String accountNumber,
//                                                     BigDecimal amount,
//                                                     String bankCode) throws ValuePlusException {
//        Map<String, String> header = prepareRequestHeader();
//
//        Map<Object, Object> requestEntity = new HashMap<>();
//        requestEntity.put("account_number", accountNumber);
//        requestEntity.put("account_bank", bankCode);
//        requestEntity.put("currency", CURRENCY);
//        requestEntity.put("amount", amount);
//
//        var type = new ParameterizedTypeReference<ResponseModel<TransferRecipient>>() {};
//        ResponseModel<TransferRecipient> result = sendRequest(HttpMethod.POST, "/transferrecipient", requestEntity, header, type);
//
//        if (!result.isStatus()) {
//            throw new ValuePlusException(result.getMessage());
//        }
//        return result.getData();
//
//    }

    @Override
    public FlwTransferResponse initiateTransfer(AccountModel accountModel, BigDecimal amount) throws ValuePlusException {
        String reference = getReference();
        Map<String, String> header = prepareRequestHeader();

        Map<Object, Object> requestEntity = new HashMap<>();
        requestEntity.put("account_number", accountModel.getAccountNumber());
        requestEntity.put("account_bank", accountModel.getBankCode());
        requestEntity.put("currency", CURRENCY);
        requestEntity.put("amount", amount);
        requestEntity.put("narration", config.getPaymentReason());
        requestEntity.put("reference", reference);
        requestEntity.put("callback_url","https://webhook.site/8c2244c1-31c4-4b4b-bfcc-d49460d6c337");

        System.out.println("requestEntity = " + requestEntity);

        var type = new ParameterizedTypeReference<ResponseModel<FlwTransferResponse>>() {};
        ResponseModel<FlwTransferResponse> result = sendRequest(HttpMethod.POST, "/transfers", requestEntity, header, type);
        System.out.println("result.getMessage() = " + result.getMessage());
        System.out.println("result.getStatus() = " + result.getStatus());
        if (result.getStatus().equals(false)) {
            throw new ValuePlusException(result.getMessage());
        }
        result.getData().setRequestStatus(result.getStatus());
        result.getData().setMessage(result.getMessage());
        return result.getData();
    }

    private String getReference() {
        return UUID.randomUUID().toString();
    }

    @Override
    public CollectionDataFlw initiateCollection(CollectionData collectionData)throws ValuePlusException{
        Map<String, String> header = prepareRequestHeader();
      //  BigDecimal decimal = collectionData.getAmount().divide(BigDecimal.valueOf(100));
        var type = new ParameterizedTypeReference<com.valueplus.flutterwave.model.ResponseModel<CollectionResponseFl>>() {};
        System.out.println(convertToNaira(collectionData.getAmount()));
        Map<Object, Object> requestEntity = new HashMap<>();
        Map<Object, Object> customerEntity = new HashMap<>();

        customerEntity.put("email", collectionData.getEmail());
        String ref = getReference();
        requestEntity.put("tx_ref", ref);
        requestEntity.put("currency", CURRENCY);
        requestEntity.put("customer", customerEntity);
        requestEntity.put("amount", collectionData.getAmount());
        requestEntity.put("redirect_url",getCallbackUrl());

        com.valueplus.flutterwave.model.ResponseModel<CollectionResponseFl> result = sendRequest(HttpMethod.POST, "/payments", requestEntity, header, type);
        if (result == null) {
            throw new ValuePlusException(result.getMessage());
        }
        CollectionDataFlw data = new CollectionDataFlw();
        data.setLink(result.getData().getLink());
        data.setRef(ref);

        return data;
    }


}
