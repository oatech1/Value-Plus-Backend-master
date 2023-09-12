package com.valueplus.domain.service.concretes;

import com.valueplus.app.config.audit.AuditEventPublisher;
import com.valueplus.app.exception.NotFoundException;
import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.enums.ProductProvider;
import com.valueplus.domain.mail.EmailService;
import com.valueplus.domain.model.NewPassword;
import com.valueplus.domain.model.PasswordChange;
import com.valueplus.domain.model.PasswordReset;
import com.valueplus.domain.products.ProductProviderUrlService;
import com.valueplus.domain.util.GeneratorUtils;
import com.valueplus.persistence.entity.PasswordResetToken;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.repository.PasswordResetTokenRepository;
import com.valueplus.persistence.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

import static com.valueplus.domain.enums.ActionType.USER_PASSWORD_RESET;
import static com.valueplus.domain.enums.ActionType.USER_PASSWORD_UPDATE;
import static com.valueplus.domain.enums.EntityType.USER;
import static com.valueplus.domain.util.MapperUtil.copy;
import static com.valueplus.domain.util.UserUtils.isAgent;
import static com.valueplus.domain.util.UserUtils.isSuperAgent;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Service
public class PasswordService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final String adminPasswordResetLink;
    private final String userPasswordResetLink;
    private final AuditEventPublisher auditEvent;
    private final UserUtilService userUtilService;

    public PasswordService(PasswordEncoder passwordEncoder, UserRepository userRepository, EmailService emailService,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           @Value("${valueplus.admin.reset-password}") String adminPasswordResetLink,
                           @Value("${valueplus.user.reset-password}") String userPasswordResetLink,
                           AuditEventPublisher auditEvent,
                           UserUtilService userUtilService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.adminPasswordResetLink = adminPasswordResetLink;
        this.userPasswordResetLink = userPasswordResetLink;
        this.auditEvent = auditEvent;
        this.userUtilService = userUtilService;
    }

    public User changePassword(Long userId, PasswordChange passwordChange) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found"));
        if (!passwordEncoder.matches(passwordChange.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("wrong password");
        }

        var oldObject = copy(user, User.class);

        String hashedPassword = passwordEncoder.encode(passwordChange.getNewPassword());
        user.setPassword(hashedPassword);

        var savedEntity = userRepository.save(user);

        auditEvent.publish(oldObject, savedEntity, USER_PASSWORD_UPDATE, USER);
        return savedEntity;
    }

    public void sendResetPassword(PasswordReset passwordReset) throws Exception {
        User user = userRepository.findByEmailAndDeletedFalse(passwordReset.getEmail())
                .orElseThrow(() -> new NotFoundException("Invalid Credentials"));

        String token = GeneratorUtils.generateRandomString(16);

        PasswordResetToken resetToken = new PasswordResetToken(user.getId(), token);
        passwordResetTokenRepository.save(resetToken);

        String resetLink = isAgent(user) || isSuperAgent(user)
                ? userPasswordResetLink
                : adminPasswordResetLink;

        emailService.sendPasswordReset(user, resetLink.concat(token));
    }


    public User resetPassword(NewPassword newPassword) throws Exception {
        Optional<PasswordResetToken> resetToken = passwordResetTokenRepository.findByResetToken(newPassword.getResetToken());
        if (resetToken.isEmpty()) {
            throw new ValuePlusException("expired link", BAD_REQUEST);
        }

        Long userId = resetToken.get().getUserId();
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new ValuePlusException("expired link", BAD_REQUEST);
        }

        String hashedPassword = passwordEncoder.encode(newPassword.getNewPassword());

        User user = userOptional.get();
        var oldObject = copy(user, User.class);
        user.setPassword(hashedPassword);
        if (user.getRetries()>=5 && !user.isEnabled()){
            user.setEnabled(true);
            user.setRetries(0);
            userRepository.save(user);
        }
        userRepository.save(user);
        passwordResetTokenRepository.deleteById(userId);

        auditEvent.publish(oldObject, user, USER_PASSWORD_RESET, USER);
        return user;
    }

    public Map<ProductProvider, ProductProviderUrlService> productUrlProvider() {
        return userUtilService.productUrlProvider();
    }
}
