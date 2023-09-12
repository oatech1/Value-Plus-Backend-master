package com.valueplus.domain.service.concretes;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.enums.PaymentPlatform;
import com.valueplus.domain.model.PaymentRequestDTO;
import com.valueplus.domain.model.PaymentResponseDTO;
import com.valueplus.domain.model.ProductOrderTransactionResponse;
import com.valueplus.flutterwave.model.CollectionDataFlw;
import com.valueplus.flutterwave.model.CollectionResponseFl;
import com.valueplus.flutterwave.model.ProductOrderTransactionResponseFlw;
import com.valueplus.flutterwave.service.CallbackResponse;
import com.valueplus.flutterwave.service.FlutterwaveService;
import com.valueplus.flutterwave.service.FlutterwaveServiceImpl;
import com.valueplus.paystack.model.*;
import com.valueplus.paystack.service.PaystackService;
import com.valueplus.persistence.entity.ProductOrderTransactions;
import com.valueplus.persistence.repository.ProductOrderPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Call;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Optional;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductOrderPaymentService {

    private final PaystackService paystackService;
    private final ProductOrderPaymentRepository productOrderPaymentRepository;
    private final FlutterwaveService flutterwaveService;

    public PaymentResponseDTO productOrdersPayoutService(PaymentRequestDTO paymentRequestDTO) throws ValuePlusException {
        CollectionData collectionData = CollectionData.builder()
                .amount(paymentRequestDTO.getAmount())
                .email(paymentRequestDTO.getEmail())
                .build();
        ProductOrderTransactions productOrderTransactions = null;
        PaymentResponseDTO paymentResponseDTO = new PaymentResponseDTO();
        switch(paymentRequestDTO.getPaymentPlatform()) {
            case PAYSTACK:
                CollectionResponse collectionResponse = paystackService.initiateCollection(collectionData);
               productOrderTransactions = ProductOrderTransactions.builder()
                        .email(collectionData.getEmail())
                        .authorization_url(collectionResponse.getAuthorization_url())
                        .access_code(collectionResponse.getAccess_code())
                        .reference(collectionResponse.getReference())
                        .amount(paymentRequestDTO.getAmount())
                        .status(collectionResponse.getStatus())
                        .orderSkuId(paymentRequestDTO.getOrderSkuId())
                        .build();
                productOrderPaymentRepository.save(productOrderTransactions);

                paymentResponseDTO.setPaymentLink(collectionResponse.getAuthorization_url());
                break;
            case FLUTTERWAVE:
              CollectionDataFlw collectionDataFlw = flutterwaveService.initiateCollection(collectionData);
                System.out.println("collectionResponseFl.getLink() = " + collectionDataFlw.getLink());
                System.out.println("collectionResponseFl.getRef() = " + collectionDataFlw.getRef());
                productOrderTransactions = ProductOrderTransactions.builder()
                        .email(collectionData.getEmail())
                        .authorization_url(collectionDataFlw.getLink())
                        .access_code(null)
                        .reference(collectionDataFlw.getRef())
                        .amount(paymentRequestDTO.getAmount())
                        .status("PENDING")
                        .orderSkuId(paymentRequestDTO.getOrderSkuId())
                        .build();
                productOrderPaymentRepository.save(productOrderTransactions);
                paymentResponseDTO.setPaymentLink(collectionDataFlw.getLink());
                break;
        }
        return paymentResponseDTO;
    }

    public ProductOrderTransactionResponse verify(String referenceNumber) throws ValuePlusException {
        Optional<ProductOrderTransactions> transaction = productOrderPaymentRepository.findByReference(referenceNumber);

        ProductOrderTransactions transactionEntity = transaction
                .orElseThrow(() -> new ValuePlusException("No transaction exists with this reference number", BAD_REQUEST));

        ProductOrderTransactionResponse result = paystackService.verify(referenceNumber);
        ProductOrderTransactionResponse.PaystackData data = result.getData();

        transactionEntity.setStatus(data.getStatus());

        productOrderPaymentRepository.save(transactionEntity);

        return result;
    }
    public ProductOrderTransactionResponseFlw verifyFlw(CallbackResponse callbackResponse) throws ValuePlusException {

        log.info(callbackResponse.getStatus());
        Optional<ProductOrderTransactions> transaction = productOrderPaymentRepository.findByReference(callbackResponse.getTrx_ref());

        ProductOrderTransactions transactionEntity = transaction
                .orElseThrow(() -> new ValuePlusException("No transaction exists with this reference number", BAD_REQUEST));

      //  ProductOrderTransactionResponse result = paystackService.verify(referenceNumber);
        ProductOrderTransactionResponseFlw result = flutterwaveService.verifyCallback(callbackResponse.getTransaction_id());
        ProductOrderTransactionResponseFlw.FlutterwaveData data = result.getData();

        transactionEntity.setStatus(data.getStatus());

        productOrderPaymentRepository.save(transactionEntity);

        return result;
    }

}
