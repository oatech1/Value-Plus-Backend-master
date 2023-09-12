package com.valueplus.app.controller;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.app.model.PaymentRequestModel;
import com.valueplus.domain.model.AccountModel;
import com.valueplus.domain.model.TransactionModel;
import com.valueplus.domain.service.abstracts.TransferService;
import com.valueplus.domain.util.UserUtils;
import com.valueplus.flutterwave.model.CollectionDataFlw;
import com.valueplus.flutterwave.model.FlwCallBack;
import com.valueplus.flutterwave.model.FlwTransferResponse;
import com.valueplus.flutterwave.model.TransferPayload;
import com.valueplus.flutterwave.service.FlutterwaveService;
import com.valueplus.flutterwave.service.FlutterwaveServiceImpl;
import com.valueplus.paystack.model.CollectionData;
import com.valueplus.paystack.model.TransferResponse;
import com.valueplus.persistence.entity.VerifyTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Validated
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "v1/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
public class TransactionController {
    private final TransferService transferService;
    private final FlutterwaveService flutterwaveService;


    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TransactionModel initiate(@Valid @RequestBody PaymentRequestModel request) throws ValuePlusException {
        return transferService.transfer(UserUtils.getLoggedInUser(), request);
    }

    @PreAuthorize("hasAuthority('VIEW_ALL_TRANSACTIONS')")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<TransactionModel> getAllTransfers(@PageableDefault(sort = "id", direction = DESC) Pageable pageable) throws ValuePlusException {
        return transferService.getAllTransactions(pageable);
    }

    @PreAuthorize("hasAnyRole('AGENT')")
    @GetMapping("/user")
    @ResponseStatus(HttpStatus.OK)
    public Page<TransactionModel> getUserTransfers(@PageableDefault(sort = "id", direction = DESC) Pageable pageable) throws ValuePlusException {
        return transferService.getAllUserTransactions(UserUtils.getLoggedInUser(), pageable);
    }

    @GetMapping("/reference/{reference}")
    @ResponseStatus(HttpStatus.OK)
    public Optional<TransactionModel> getUserTransfers(@PathVariable("reference") String reference) throws ValuePlusException {
        return transferService.getTransactionByReference(UserUtils.getLoggedInUser(), reference);
    }

    @GetMapping("/verify/{reference}")
    @ResponseStatus(HttpStatus.OK)
    public TransactionModel verifyTransaction(@PathVariable("reference") String reference) throws ValuePlusException {
        return transferService.verify(UserUtils.getLoggedInUser(), reference);
    }

    @GetMapping("/flw/callback")
    @ResponseStatus(HttpStatus.OK)
    public TransactionModel TransferCallback(@RequestBody FlwCallBack callBack) throws ValuePlusException {
        return transferService.verifyFlw(callBack);
    }

    @GetMapping("/commissions/verify/{reference}")
    @ResponseStatus(HttpStatus.OK)
    public VerifyTransaction verifyCommission(@PathVariable("reference") String reference) throws ValuePlusException {
        return transferService.findCommissionByTransactionReference(reference);
    }

    @PreAuthorize("hasAuthority('VERIFY_PENDING_TRANSACTIONS')")
    @GetMapping("/verify")
    @ResponseStatus(HttpStatus.OK)
    public CompletableFuture<Void> verifyPendingTransaction() {
        return transferService.verifyPendingTransactions();
    }

    @GetMapping("/filter")
    @ResponseStatus(HttpStatus.OK)
    public Page<TransactionModel> getTransferByDate(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "status", required = false) String status,
            @PageableDefault(sort = "id", direction = DESC) Pageable pageable) throws ValuePlusException {
        return transferService.filter(UserUtils.getLoggedInUser(), status, startDate, endDate, pageable);
    }

    @GetMapping("/user/filter")
    @ResponseStatus(HttpStatus.OK)
    public Page<TransactionModel> getUserTransferByDate(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(sort = "id", direction = DESC) Pageable pageable) throws ValuePlusException {
        return transferService.getTransactionBetween(UserUtils.getLoggedInUser(), startDate, endDate, pageable);
    }



    @PostMapping("/test-flw")
    @ResponseStatus(HttpStatus.OK)
    public FlwTransferResponse initiateTf(@RequestBody AccountModel accountModel) throws ValuePlusException {
        System.out.println("here = ");
        return flutterwaveService.initiateTransfer(accountModel, new BigDecimal(500));
    }

//    @PostMapping("/coll-flw")
//    @ResponseStatus(HttpStatus.OK)
//    public CollectionDataFlw initiateCollection(@RequestBody CollectionData collectionData) throws ValuePlusException {
//        return flutterwaveServiceImpl.initiateCollection(collectionData);
//    }
}
