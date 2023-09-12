package com.valueplus.domain.service.concretes;

import com.valueplus.app.config.audit.AuditEventPublisher;
import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.app.model.PaymentRequestModel;
import com.valueplus.domain.model.AccountModel;
import com.valueplus.domain.model.TransactionModel;
import com.valueplus.domain.service.abstracts.PaymentService;
import com.valueplus.domain.service.abstracts.SettingsService;
import com.valueplus.domain.service.abstracts.TransferService;
import com.valueplus.domain.service.abstracts.WalletService;
import com.valueplus.dotgo.service.OtpService;
import com.valueplus.flutterwave.model.FlwCallBack;
import com.valueplus.flutterwave.model.FlwTransferResponse;
import com.valueplus.flutterwave.service.FlutterwaveService;
import com.valueplus.paystack.model.TransferResponse;
import com.valueplus.paystack.model.TransferVerificationResponse;
import com.valueplus.persistence.entity.Account;
import com.valueplus.persistence.entity.Transaction;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.entity.VerifyTransaction;
import com.valueplus.persistence.repository.AccountRepository;
import com.valueplus.persistence.repository.TransactionRepository;
import com.valueplus.persistence.repository.VerifyTransactionRepository;
import com.valueplus.persistence.specs.SearchCriteria;
import com.valueplus.persistence.specs.SearchOperation;
import com.valueplus.persistence.specs.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.valueplus.domain.enums.ActionType.TRANSACTION_INITIATE;
import static com.valueplus.domain.enums.ActionType.TRANSACTION_STATUS_CHANGE;
import static com.valueplus.domain.enums.EntityType.TRANSACTION;
import static com.valueplus.domain.util.FunctionUtil.convertToNaira;
import static com.valueplus.domain.util.FunctionUtil.setScale;
import static com.valueplus.domain.util.MapperUtil.copy;
import static com.valueplus.domain.util.UserUtils.isAgent;
import static com.valueplus.domain.util.UserUtils.isSuperAgent;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.springframework.http.HttpStatus.BAD_REQUEST;


@RequiredArgsConstructor
@Slf4j
@Service
public class DefaultTransferService implements TransferService {

    private static final String TRANSACTION_FETCH_ERROR_MSG = "Unable to fetch transaction";
    private final PaymentService paymentService;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final WalletService walletService;
    private final PasswordEncoder passwordEncoder;
    private final SettingsService settingsService;
    private final AuditEventPublisher auditEvent;
    private final VerifyTransactionRepository verifyTransactionRepository;
    private final UserService userService;
    private final OtpService otpService;
    private final FlutterwaveService flutterwaveService;

    @Override
    public TransactionModel transfer(User user, PaymentRequestModel requestModel) throws ValuePlusException {
        otpService.verifyOtp(requestModel.getOtp());
        ensureUserIsKycVerified(user);
        ensureUserHasPinSet(user);
        ensureUserPinIsMatching(user, requestModel);

        AccountModel accountModel = accountRepository.findById(requestModel.getAccountId())
                .map(Account::toModel)
                .orElseThrow(() -> new ValuePlusException("User has no existing account", BAD_REQUEST));

//        AccountModel accountModel = accountRepository.findByUser_Id(user.getId())
//                .map(Account::toModel)
//                .orElseThrow(() -> new ValuePlusException("User has no existing account", BAD_REQUEST));
        var settings = settingsService.getCurrentSetting()
                .orElseThrow(() -> new ValuePlusException("Unable to retrieve commission"));

        BigDecimal percentageCommission = settings.getCommissionPercentage().divide(BigDecimal.valueOf(100.00));
        BigDecimal transactionAmount = setScale(requestModel.getAmount());
        BigDecimal commission = setScale(transactionAmount.multiply(percentageCommission));
        BigDecimal actualTransferFee = setScale(transactionAmount.subtract(commission));
        Transaction transaction = new Transaction();
        User admin = new User();
        switch (requestModel.getPaymentPlatform()){
            case ("paystack"):
                TransferResponse response = paymentService.transfer(accountModel, actualTransferFee);

      //  requestModel.getPaymentPlatform().equals("paystack") ? response = paymentService.transfer(accountModel, actualTransferFee) : config.getTestApiKey();

        walletService.debitWallet(user, transactionAmount, "Debit via Withdrawal from transfer module");
        walletService.creditAdminWallet(commission, "Credit via Withdrawal from transfer module");

        // track both debited and credited account
       admin = userService.getAdminUserAccount()
                .orElseThrow(() -> new ValuePlusException("Unable to retrieve admin adhoc account"));

        saveTransactionForVerification(admin, user, response.getReference(), convertToNaira(response.getAmount()));

        transaction = Transaction.builder()
                .accountNumber(accountModel.getAccountNumber())
                .amount(convertToNaira(response.getAmount()))
                .bankCode(accountModel.getBankCode())
                .reference(response.getReference())
                .transferId(response.getId())
                .currency(response.getCurrency())
                .user(user)
                .status(response.getStatus().toLowerCase())
                .build();

        break;

            case ("flutterwave"):
                FlwTransferResponse flresponse = flutterwaveService.initiateTransfer(accountModel, actualTransferFee);

                walletService.debitWallet(user, transactionAmount, "Debit via Withdrawal from transfer module");
                walletService.creditAdminWallet(commission, "Credit via Withdrawal from transfer module");

                // track both debited and credited account
                admin = userService.getAdminUserAccount()
                        .orElseThrow(() -> new ValuePlusException("Unable to retrieve admin adhoc account"));

                saveTransactionForVerification(admin, user, flresponse.getReference(), convertToNaira(flresponse.getAmount()));

                transaction = Transaction.builder()
                        .accountNumber(accountModel.getAccountNumber())
                        .amount(flresponse.getAmount())
                        .bankCode(accountModel.getBankCode())
                        .reference(flresponse.getReference())
                        .transferId(flresponse.getId())
                        .currency(flresponse.getCurrency())
                        .user(user)
                        .status(flresponse.getRequestStatus().toLowerCase())
                        .build();
                    break;

    }

        var savedTransaction = transactionRepository.save(transaction).toModel();

        auditEvent.publish(new Transaction(), savedTransaction, TRANSACTION_INITIATE, TRANSACTION);
        return savedTransaction;
    }

    private void saveTransactionForVerification(User admin, User user, String ref, BigDecimal amount) throws ValuePlusException {

        VerifyTransaction verifyTransaction = VerifyTransaction.builder()
                .creditedUser(admin)
                .debitedUser(user)
                .reference(ref)
                .amount(convertToNaira(amount))
                .build();
        try{
            verifyTransactionRepository.save(verifyTransaction);
        }catch (Exception e){
            log.error("error saving transaction between admin and user");
        }
    }

    @Override
    public VerifyTransaction findCommissionByTransactionReference(String ref) throws ValuePlusException {
        return verifyTransactionRepository.findVerifyTransactionByReference(ref)
                .orElseThrow(() -> new ValuePlusException("No transaction of such made", BAD_REQUEST));
    }

    @Override
    public TransactionModel verify(User user, String referenceNumber) throws ValuePlusException {
        Optional<Transaction> transaction;
        if (isAgent(user) || isSuperAgent(user)) {
            transaction = transactionRepository.findByUser_IdAndReference(user.getId(), referenceNumber);
        } else {
            transaction = transactionRepository.findByReference(referenceNumber);
        }

        var transactionEntity = transaction
                .orElseThrow(() -> new ValuePlusException("No transaction exists with this reference number", BAD_REQUEST));

        return verify(transactionEntity);
    }
    @Override
    public TransactionModel verifyFlw(FlwCallBack callBack) throws ValuePlusException {
        Optional<Transaction> transaction;
            transaction = transactionRepository.findByReference(callBack.getData().getReference());
        var transactionEntity = transaction
                .orElseThrow(() -> new ValuePlusException("No transaction exists with this reference number", BAD_REQUEST));
        transactionEntity.setStatus(callBack.getData().getStatus());

        var savedTransaction = transactionRepository.save(transactionEntity);
        auditEvent.publish(transaction.get(), savedTransaction, TRANSACTION_STATUS_CHANGE, TRANSACTION);
        return savedTransaction.toModel();
    }


    @Override
    public CompletableFuture<Void> verifyPendingTransactions() {
        return runAsync(() -> {
            for (Transaction transaction : transactionRepository.findPendingTransactions()) {
                try {
                    verify(transaction);
                    Thread.sleep(10000);
                } catch (ValuePlusException | InterruptedException e) {
                    log.error("Error verifying transaction status", e);
                }
            }
        });
    }

    @Override
    public Page<TransactionModel> getAllUserTransactions(User user, Pageable pageable) throws ValuePlusException {
        try {
            return transactionRepository.findByUser_IdOrderByIdDesc(user.getId(), pageable)
                    .map(Transaction::toModel);
        } catch (Exception e) {
            log.error(TRANSACTION_FETCH_ERROR_MSG, e);
            throw new ValuePlusException(TRANSACTION_FETCH_ERROR_MSG, e);
        }
    }

    @Override
    public Page<TransactionModel> getAllTransactions(Pageable pageable) throws ValuePlusException {
        try {
            return transactionRepository.findAllByOrderByIdDesc(pageable)
                    .map(Transaction::toModel);
        } catch (Exception e) {
            log.error(TRANSACTION_FETCH_ERROR_MSG, e);
            throw new ValuePlusException(TRANSACTION_FETCH_ERROR_MSG, e);
        }
    }

    @Override
    public Optional<TransactionModel> getTransactionByReference(User user, String reference) throws ValuePlusException {
        try {
            Optional<Transaction> transaction;
            if (isAgent(user) || isSuperAgent(user)) {
                transaction = transactionRepository.findByUser_IdAndReference(user.getId(), reference);
            } else {
                transaction = transactionRepository.findByReference(reference);
            }
            return transaction.map(Transaction::toModel);
        } catch (Exception e) {
            log.error(TRANSACTION_FETCH_ERROR_MSG, e);
            throw new ValuePlusException(TRANSACTION_FETCH_ERROR_MSG, e);
        }
    }

    @Override
    public Page<TransactionModel> getTransactionBetween(User user,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        Pageable pageable) throws ValuePlusException {
        try {
            LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.MIN);
            LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.MAX);
            Page<Transaction> transactions;
            if (isAgent(user) || isSuperAgent(user)) {
                transactions = transactionRepository.findByUser_IdAndCreatedAtIsBetweenOrderByIdDesc(
                        user.getId(),
                        startDateTime,
                        endDateTime,
                        pageable);
            } else {
                transactions = transactionRepository.findByCreatedAtIsBetweenOrderByIdDesc(
                        startDateTime,
                        endDateTime,
                        pageable);
            }

            return transactions.map(Transaction::toModel);
        } catch (Exception e) {
            log.error(TRANSACTION_FETCH_ERROR_MSG, e);
            throw new ValuePlusException(TRANSACTION_FETCH_ERROR_MSG, e);
        }
    }

    @Override
    public Page<TransactionModel> filter(User user, String status, LocalDate startDate, LocalDate endDate, Pageable pageable) throws ValuePlusException {
        TransactionSpecification specification = buildSpecification(status, startDate, endDate, user);
        return transactionRepository.findAll(Specification.where(specification), pageable)
                .map(Transaction::toModel);
    }

    private TransactionSpecification buildSpecification(String status,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        User user) {
        TransactionSpecification specification = new TransactionSpecification();
        if (status != null) {
            specification.add(new SearchCriteria<>("status", status.toLowerCase(), SearchOperation.MATCH));
        }

        if (isAgent(user) || isSuperAgent(user)) {
            specification.add(new SearchCriteria<>("user", user, SearchOperation.EQUAL));
        }

        if (startDate != null) {
            specification.add(new SearchCriteria<>(
                    "createdAt",
                    startDate.atTime(LocalTime.MIN),
                    SearchOperation.GREATER_THAN_EQUAL));
        }

        if (endDate != null) {
            specification.add(new SearchCriteria<>("createdAt",
                    endDate.atTime(LocalTime.MAX),
                    SearchOperation.LESS_THAN_EQUAL));
        }
        return specification;
    }

    private TransactionModel verify(Transaction transaction) throws ValuePlusException {
        var oldObject = copy(transaction, Transaction.class);
        TransferVerificationResponse response = paymentService.verifyTransfer(transaction.getReference());
        transaction.setStatus(response.getStatus());

        var savedTransaction = transactionRepository.save(transaction);
        auditEvent.publish(oldObject, savedTransaction, TRANSACTION_STATUS_CHANGE, TRANSACTION);
        return savedTransaction.toModel();
    }

    private void ensureUserPinIsMatching(User user, PaymentRequestModel requestModel) throws ValuePlusException {
        if (!passwordEncoder.matches(requestModel.getPin(), user.getTransactionPin()))
            throw new ValuePlusException("Incorrect pin", UNAUTHORIZED);
    }

    private void ensureUserHasPinSet(User user) throws ValuePlusException {
        if (!user.isTransactionTokenSet())
            throw new ValuePlusException("You need to set your transaction pin", BAD_REQUEST);
    }

    private void ensureUserIsKycVerified(User user) throws ValuePlusException{
        if (user.isKycVerification() == false){
            throw new ValuePlusException("Kyc verification not passed",UNAUTHORIZED);
        }

    }
}
