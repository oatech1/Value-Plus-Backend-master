package com.valueplus.app.controller;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.AccountModel;
import com.valueplus.domain.model.AccountRequest;
import com.valueplus.domain.service.abstracts.AccountService;
import com.valueplus.paystack.model.AccountNumberModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import static com.valueplus.domain.util.UserUtils.getLoggedInUser;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "v1/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/validate")
    @ResponseStatus(HttpStatus.OK)
    public AccountNumberModel validateBankAccount(@Valid @RequestBody AccountRequest accountRequest) throws ValuePlusException {
        return accountService.validateBankAccount(accountRequest);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public AccountModel create(@Valid @RequestBody AccountRequest accountRequest) throws ValuePlusException {
        return accountService.create(getLoggedInUser(), accountRequest);
    }

    @PostMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AccountModel update(
            @PathVariable("id") Long id,
            @Valid @RequestBody AccountRequest accountRequest) throws ValuePlusException {
        return accountService.update(id, getLoggedInUser(), accountRequest);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<AccountModel> getAccount() throws ValuePlusException {
        return accountService.getAccount(getLoggedInUser());
    }

    @DeleteMapping("/delete/{accountId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> deleteBankAccount(@PathVariable Long accountId) throws ValuePlusException {
        return ResponseEntity.ok(accountService.deleteBankAccount(accountId));
    }

}
