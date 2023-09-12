package com.valueplus.domain.service.concretes;

import com.google.api.client.json.Json;
import com.google.gson.JsonObject;
import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.mail.EmailService;
import com.valueplus.domain.model.WalletHistoryModel;
import com.valueplus.domain.model.WalletModel;
import com.valueplus.domain.service.abstracts.WalletHistoryService;
import com.valueplus.domain.service.abstracts.WalletService;
import com.valueplus.domain.util.UserUtils;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.entity.Wallet;
import com.valueplus.persistence.entity.WalletHistory;
import com.valueplus.persistence.repository.UserRepository;
import com.valueplus.persistence.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.valueplus.domain.enums.TransactionType.CREDIT;
import static com.valueplus.domain.enums.TransactionType.DEBIT;
import static com.valueplus.domain.util.FunctionUtil.setScale;
import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RequiredArgsConstructor
@Service
public class DefaultWalletService implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletHistoryService walletHistoryService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final UserService userService;

    @Override
    public WalletModel createWallet(User user) {
        return getOrCreateWallet(user).toModel();
    }

    @Override
    public WalletModel getWallet(User user) {
        return getOrCreateWallet(user).toModel();
    }

    @Override
    public WalletModel getWallet() throws ValuePlusException {
        User user = getAdminUser();
        return getOrCreateWallet(user).toModel();
    }

    @Override
    public Page<WalletModel> getAllWallet(Pageable pageable) {
        Long adminUserId = userService.getAdminUserId();
        return walletRepository.findWalletByUser_IdNot(adminUserId, pageable)
                .map(Wallet::toModel);
    }

    @Override
    public List<WalletModel> createWalletForAllUsers() {
        Set<Long> userIdsWithWallet = walletRepository.findAll().stream()
                .map(Wallet::getUser)
                .map(User::getId)
                .collect(toSet());

        List<Wallet> newWallets = userRepository.findUsersByDeletedFalse()
                .stream()
                .filter(u -> !userIdsWithWallet.contains(u.getId()))
                .map(this::newWallet)
                .collect(toList());

        return walletRepository.saveAll(newWallets)
                .stream()
                .map(Wallet::toModel)
                .collect(toList());
    }

    @Override
    public WalletModel creditWallet(User user, BigDecimal amount, String description) {
        Wallet wallet = getOrCreateWallet(user);

        if (isAmountZero(amount)) {
            return wallet.toModel();
        }

        amount = setScale(amount);
        BigDecimal newTotal = setScale(wallet.getAmount()).add(amount);
        wallet.setAmount(setScale((newTotal)));

        wallet = walletRepository.save(wallet);
        walletHistoryService.createHistoryRecord(wallet, amount, CREDIT, description);
        sendCreditNotification(user, amount);

        return wallet.toModel();
    }

    private boolean isAmountZero(BigDecimal amount) {
        return setScale(amount).equals(setScale(ZERO));
    }

    @Override
    public WalletModel creditAdminWallet(BigDecimal amount, String description) throws ValuePlusException {
        return creditWallet(getAdminUser(), amount, description);
    }

    @Override
    public WalletModel debitWallet(User user, BigDecimal amount, String description) throws ValuePlusException {
        amount = setScale(amount);
        Wallet wallet = getOrCreateWallet(user);

        if (isWalletBalanceLessThanAmount(wallet, amount)) {
            throw new ValuePlusException("Amount to be debited more than the balance in user's wallet", BAD_REQUEST);
        }

        BigDecimal newTotal = setScale(wallet.getAmount()).subtract(amount);
        wallet.setAmount(setScale(newTotal));

        wallet = walletRepository.save(wallet);
        walletHistoryService.createHistoryRecord(wallet, amount, DEBIT, description);
        sendDebitNotification(user, amount);

        return wallet.toModel();
    }

    private User getAdminUser() throws ValuePlusException {
        return userService.getAdminUserAccount()
                .orElseThrow(() -> new ValuePlusException("Unable to retrieve admin adhoc account"));
    }

    private void sendCreditNotification(User user, BigDecimal amount) {
        try {
            emailService.sendCreditNotification(user, amount);
        } catch (Exception e) {
            log.info("Error sending CREDIT notification to user {} - {}", user.getId(), user.getFirstname());
        }
    }

    private void sendDebitNotification(User user, BigDecimal amount) {
        try {
            emailService.sendDebitNotification(user, amount);
        } catch (Exception e) {
            log.info("Error sending DEBIT notification to user {} - {}", user.getId(), user.getFirstname());
        }
    }

    private boolean isWalletBalanceLessThanAmount(Wallet wallet, BigDecimal amount) {
        return wallet.getAmount().compareTo(amount) < 0;
    }


    private Wallet getOrCreateWallet(User user) {
        Optional<Wallet> walletOptional = walletRepository.findWalletByUser_Id(user.getId());
        return walletOptional.orElseGet(() -> create(user));
    }

    private Wallet create(User user) {
        var wallet = newWallet(user);
        return walletRepository.save(wallet);
    }

    private Wallet newWallet(User user) {
        return Wallet.builder()
                .user(user)
                .amount(setScale(ZERO))
                .build();
    }

    public ResponseEntity<?> getAgentWalletBalance() throws ValuePlusException {
        User user = UserUtils.getLoggedInUser();
        Optional <Wallet> wallet = this.walletRepository.findByUser(user);
        if(wallet.isEmpty()) {
            throw new ValuePlusException("Incorrect parameter; user with id, " + user.getId() + " does not have wallet");
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("balance",wallet.get().getAmount());
        return ResponseEntity.status(HttpStatus.OK).body(jsonObject.toString());

    }

    public Page<WalletHistoryModel> getWalletHistory(Pageable pageable) throws ValuePlusException {

        User user = UserUtils.getLoggedInUser();
        Optional <Wallet> wallet = this.walletRepository.findByUser(user);

        if(wallet.isEmpty()) {
            throw new ValuePlusException("Incorrect parameter; user with id, " + user.getId() + " does not have wallet");
        }

        Page<WalletHistoryModel>  history = this.walletHistoryService.getHistory(user,wallet.get().getId(),pageable);

        return history ;
    }
}
