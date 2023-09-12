package com.valueplus.domain.service.concretes;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.mail.EmailService;
import com.valueplus.domain.model.MessageResponse;
import com.valueplus.domain.util.GeneratorUtils;
import com.valueplus.persistence.entity.EmailVerificationToken;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.repository.EmailVerificationTokenRepository;
import com.valueplus.persistence.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class EmailVerificationService {
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final String verifyEmailLink;

    public EmailVerificationService(EmailVerificationTokenRepository verificationTokenRepository,
                                    EmailService emailService,
                                    UserRepository userRepository,
                                    @Value("${valueplus.verify-email}") String verifyEmailLink) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.verifyEmailLink = verifyEmailLink;
    }

    public MessageResponse sendVerifyEmail(User user) throws Exception {
        Optional<User> userOptional = userRepository.findById(user.getId());
        if (!(userOptional.isPresent() && userOptional.get().isEmailVerified())){
        String token = GeneratorUtils.generateRandomString(16);
        EmailVerificationToken emailVerificationToken = new EmailVerificationToken(user.getId(), token,false);
        verificationTokenRepository.save(emailVerificationToken);
        emailService.sendEmailVerification(user, verifyEmailLink.concat(emailVerificationToken.getVerificationToken()));}
        else {throw new ValuePlusException("User already verified",HttpStatus.CONFLICT);}
        return new MessageResponse("Verify Email Sent Successfully");
    }

    public MessageResponse sendVerifyEmailForAgentBySuperAgent(User user,String password) throws Exception {
        Optional<User> userOptional = userRepository.findById(user.getId());
        if (!(userOptional.isPresent() && userOptional.get().isEmailVerified())){
            String token = GeneratorUtils.generateRandomString(16);
            EmailVerificationToken emailVerificationToken = new EmailVerificationToken(user.getId(), token,false);
            verificationTokenRepository.save(emailVerificationToken);
            emailService.sendEmailVerificationToAgentsCreatedBySuperAgents(user, verifyEmailLink.concat(emailVerificationToken.getVerificationToken()),password);}
        else {throw new ValuePlusException("User already verified",HttpStatus.CONFLICT);}
        return new MessageResponse("Verify Email Sent Successfully");
    }



    public void sendAdminAccountCreationNotification(User user, String password) throws Exception {
        emailService.sendAdminUserCreationEmail(user, password);
    }

    public void sendSubAdminAccountCreationNotification(User user, String password) throws Exception {
        emailService.sendSubAdminUserCreationEmail(user,password);
    }

    public void sendSuperAgentAccountCreationNotification(User user, String password) throws Exception {
        emailService.sendSuperAgentUserCreationEmail(user, password);
    }

    public MessageResponse confirmEmail(String token) throws Exception {

        Optional<EmailVerificationToken> emailVerificationToken = verificationTokenRepository.findByVerificationToken(token);
        if (emailVerificationToken.isEmpty()) {
            log.error("email verification token not found, token = {}", token);

            throw new ValuePlusException("TOKEN NOT FOUND", HttpStatus.NOT_FOUND);
        }

        if (emailVerificationToken.get().getUsed()){

            throw new ValuePlusException("ACCOUNT ALREADY VERIFIED",HttpStatus.BAD_REQUEST);
        }

        Long userId = emailVerificationToken.get().getUserId();
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            log.error("user not found, userId = {}", userId);
            throw new ValuePlusException("USER NOT FOUND", HttpStatus.NOT_FOUND);
        }

        User user = userOptional.get();
        user.setEmailVerified(true);
        user.setEnabled(true);
        userRepository.save(user);

       EmailVerificationToken verificationToken = emailVerificationToken.get();
       verificationToken.setUsed(true);
       verificationTokenRepository.save(verificationToken);

        return new MessageResponse("Token Verified Successfully");
    }
}
