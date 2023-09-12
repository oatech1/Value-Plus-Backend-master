package com.valueplus.domain.service.concretes;

import com.valueplus.app.config.audit.AuditEventPublisher;
import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.AccountModel;
import com.valueplus.domain.model.AccountRequest;
import com.valueplus.domain.model.MessageResponse;
import com.valueplus.domain.service.abstracts.AccountService;
import com.valueplus.domain.service.abstracts.PaymentService;
import com.valueplus.paystack.model.AccountNumberModel;
import com.valueplus.persistence.entity.Account;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.valueplus.domain.enums.ActionType.ACCOUNT_CREATE;
import static com.valueplus.domain.enums.EntityType.ACCOUNT;
import static com.valueplus.domain.util.MapperUtil.copy;
import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class DefaultAccountService implements AccountService {
    private final PaymentService paymentService;
    private final AccountRepository repository;
    private final AuditEventPublisher auditEvent;

    @Override
    public AccountNumberModel validateBankAccount(AccountRequest request) throws ValuePlusException {
        return paymentService.resolveAccountNumber(request.getAccountNumber().trim(), request.getBankCode());
    }

    @Override
    public AccountModel create(User user, AccountRequest request) throws ValuePlusException {
        var existingAccount = repository.findAccountByAccountNumberAndBankCode(request.getAccountNumber(),request.getBankCode());
        if(existingAccount.isPresent()){
            throw new ValuePlusException("This account exist already for this user", BAD_REQUEST);
        }

        try {
            Account account = Account.builder()
                    .accountName(request.getAccountName().trim())
                    .accountNumber(request.getAccountNumber().trim())
                    .bankCode(request.getBankCode().trim())
                    .bankName(request.getBankName().trim())
                    .user(user)
                    .build();

            var savedAccount = repository.save(account).toModel();

            auditEvent.publish(new Object(), savedAccount, ACCOUNT_CREATE, ACCOUNT);
            return savedAccount;
        } catch (Exception e) {
            throw new ValuePlusException("Error adding account to profile", e);
        }
    }


    @Override
    public AccountModel update(Long id, User user, AccountRequest request) throws ValuePlusException {
        Account account = repository.findById(id)
                .orElseThrow(() -> new ValuePlusException("Invalid account id", BAD_REQUEST));

//        if (account.getAccountNumber().equals(request.getAccountNumber())) {
//            return account.toModel();
//        }

        if (!account.getUser().getId().equals(user.getId())) {
            throw new ValuePlusException("Invalid account update request", UNAUTHORIZED);
        }

        var oldObject = copy(account, Account.class);

        try {
            account.setAccountName(request.getAccountName());
            account.setAccountNumber(request.getAccountNumber());
            account.setBankCode(request.getBankCode());
            account.setBankName(request.getBankName());

            var savedAccount = repository.save(account).toModel();

            auditEvent.publish(oldObject, savedAccount, ACCOUNT_CREATE, ACCOUNT);
            return savedAccount;
        } catch (Exception e) {
            throw new ValuePlusException("Error updating account details", e);
        }
    }

    @Override
    public List<AccountModel> getAccount(User user) throws ValuePlusException {
        return repository.findAccountsByUser_Id(user.getId())
                .stream()
                .map(Account::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public MessageResponse deleteBankAccount(Long accountId) throws ValuePlusException {
       MessageResponse messageResponse = new MessageResponse();


            var account = repository.findById(accountId)
                    .orElseThrow(() -> new ValuePlusException("Account Not Found", NOT_FOUND));

            repository.delete(account);
            messageResponse.setMessage("Account Deleted Successfully");

        return  messageResponse;
    }
}
