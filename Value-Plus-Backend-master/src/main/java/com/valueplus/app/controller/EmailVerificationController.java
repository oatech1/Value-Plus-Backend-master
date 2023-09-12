package com.valueplus.app.controller;


import com.valueplus.app.exception.BadRequestException;
import com.valueplus.domain.model.MessageResponse;
import com.valueplus.domain.model.VerifyEmail;
import com.valueplus.domain.service.concretes.EmailVerificationService;
import com.valueplus.persistence.entity.EmailVerificationToken;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.repository.EmailVerificationTokenRepository;
import com.valueplus.persistence.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

@Validated
@Slf4j
@RestController
@RequestMapping(path = "v1/user", produces = MediaType.APPLICATION_JSON_VALUE)
public class EmailVerificationController {
    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;

    public EmailVerificationController(EmailVerificationService emailVerificationService, UserRepository userRepository, EmailVerificationTokenRepository verificationTokenRepository) {
        this.emailVerificationService = emailVerificationService;
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
    }

    @PostMapping("/current/send-verify-mail/{email}")
    public MessageResponse sendVerifyMail(@PathVariable String email) throws Exception {
        Optional<User> user = userRepository.findByEmail(email);
        MessageResponse messageResponse ;
        if(user.isPresent()){
            log.info("sendVerifyMail() received userId = {}", user.get().getId());
          messageResponse =  emailVerificationService.sendVerifyEmail(user.get());
        }else{
            throw new Exception("Could not process request");
        }
        return messageResponse;
    }

    @PostMapping("/current/send-verify-mails/{token}")
    public MessageResponse resendToken(@PathVariable String token) throws Exception {
        Optional<EmailVerificationToken> verificationToken = verificationTokenRepository.findByVerificationToken(token);
        User user = userRepository.findById(verificationToken.get().getUserId()).orElseThrow(() -> new BadRequestException("Invalid Agent Id"));
        MessageResponse messageResponse ;
        if(verificationToken.isPresent()){
            log.info("sendVerifyMail() received userId = {}", verificationToken.get());

            messageResponse =  emailVerificationService.sendVerifyEmail(user);
        }else{
            throw new Exception("Could not process request");
        }
        return messageResponse;
    }


    @PostMapping("/verify-mail")
    public MessageResponse verifyMail(@Valid @RequestBody VerifyEmail verifyEmail) throws Exception {
        log.info("verifyMail() received verifyMail = {}", verifyEmail);
      return   emailVerificationService.confirmEmail(verifyEmail.getVerificationToken());
    }
}
