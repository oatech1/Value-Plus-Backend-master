package com.valueplus.domain.service.concretes;

import com.valueplus.app.config.audit.AuditEventPublisher;
import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.app.exception.ValuePlusRuntimeException;
import com.valueplus.app.model.LoginResponseModel;
import com.valueplus.domain.model.LoginForm;
import com.valueplus.domain.service.abstracts.AuthenticationService;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

import static com.valueplus.domain.enums.ActionType.USER_LOGIN;
import static com.valueplus.domain.enums.EntityType.*;
import static com.valueplus.domain.util.UserUtils.*;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAuthenticationService implements AuthenticationService {

    private final static String ERROR_MSG = "Invalid credentials";
    private final static String ACCOUNT_LOCKOUT = "Account has been Locked";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenAuthenticationService tokenService;
    private final AuditEventPublisher auditEvent;

    @Override
    public LoginResponseModel agentLogin(LoginForm loginForm) throws ValuePlusException {
        try {
            System.out.println(loginForm);
            return loginAgentUser(loginForm, this::ensureUserIsAgent);
        } catch (Exception e) {
            throw new ValuePlusException(e.getMessage(), UNAUTHORIZED);
        }
    }

    @Override
    public LoginResponseModel superAgentLogin(LoginForm loginForm) throws ValuePlusException {
        try {
            System.out.println(loginForm);
            return loginSuperAgentUser(loginForm, this::ensureUserIsSuperAgent);
        } catch (Exception e) {
            throw new ValuePlusException(e.getMessage(), UNAUTHORIZED);
        }
    }

    @Override
    public LoginResponseModel adminLogin(LoginForm loginForm) throws ValuePlusException {
        try {
            return loginAdminUser(loginForm, this::ensureUserIsAdmin);
        } catch (Exception e) {
            throw new ValuePlusException(e.getMessage(), UNAUTHORIZED);
        }
    }

    @Override
    public LoginResponseModel subAdminLogin(LoginForm loginForm) throws ValuePlusException {
        try {
            return loginSubAdminUser(loginForm, this::ensureUserIsSubAdmin);
        } catch (Exception e) {
            throw new ValuePlusException(e.getMessage(), UNAUTHORIZED);
        }
    }

//    private LoginResponseModel loginUser(LoginForm loginForm, Consumer<User> validateUserTypeFunction) {
//        User user = getUser(loginForm.getEmail());
//        checkRetries(user);
//
//        validateUserTypeFunction.accept(user);
//
//        matchPassword(loginForm.getPassword(), user.getPassword(),user);
//        user.setRetries(0);
//        userRepository.save(user);
//        auditEvent.publish(user, user, USER_LOGIN, AGENT);
//        return new LoginResponseModel(tokenService.generatorToken(user));
//    }

    private LoginResponseModel loginAgentUser(LoginForm loginForm, Consumer<User> validateUserTypeFunction) throws ValuePlusException {
        User user = getUser(loginForm.getEmail());
        checkRetries(user);

        validateUserTypeFunction.accept(user);

        matchPassword(loginForm.getPassword(), user.getPassword(),user);
        user.setRetries(0);
        userRepository.save(user);
        auditEvent.publish(user, user, USER_LOGIN, AGENT);
        return new LoginResponseModel(tokenService.generatorToken(user));
    }
    private LoginResponseModel loginSuperAgentUser(LoginForm loginForm, Consumer<User> validateUserTypeFunction) throws ValuePlusException {
        User user = getUser(loginForm.getEmail());
        checkRetries(user);

        validateUserTypeFunction.accept(user);

        matchPassword(loginForm.getPassword(), user.getPassword(),user);
        user.setRetries(0);
        userRepository.save(user);
        auditEvent.publish(user, user, USER_LOGIN, SUPER_AGENT);
        return new LoginResponseModel(tokenService.generatorToken(user));
    }
    private LoginResponseModel loginAdminUser(LoginForm loginForm, Consumer<User> validateUserTypeFunction) throws ValuePlusException {
        User user = getUser(loginForm.getEmail());
        checkRetries(user);

        validateUserTypeFunction.accept(user);

        matchPassword(loginForm.getPassword(), user.getPassword(),user);
        user.setRetries(0);
        userRepository.save(user);
        auditEvent.publish(user, user, USER_LOGIN, ADMIN);
        return new LoginResponseModel(tokenService.generatorToken(user));
    }
    private LoginResponseModel loginSubAdminUser(LoginForm loginForm, Consumer<User> validateUserTypeFunction) throws ValuePlusException {
        User user = getUser(loginForm.getEmail());
        checkRetries(user);

        validateUserTypeFunction.accept(user);

        matchPassword(loginForm.getPassword(), user.getPassword(),user);
        user.setRetries(0);
        userRepository.save(user);
        auditEvent.publish(user, user, USER_LOGIN, SUB_ADMIN);
        return new LoginResponseModel(tokenService.generatorToken(user));
    }



    private User getUser(String email) throws ValuePlusRuntimeException {
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(this::invalidCredentialException);

        if (!user.isEnabled()) {
            log.warn("User is currently disabled");
            throw invalidCredentialException();
        }

        return user;
    }

    private ValuePlusRuntimeException invalidCredentialException() {
        return new ValuePlusRuntimeException(ERROR_MSG);
    }

    private ValuePlusException accountLockedException(){
        return new ValuePlusException(ACCOUNT_LOCKOUT);
    }

    private void ensureUserIsAdmin(User user) throws ValuePlusRuntimeException {
        if (!isAdmin(user)) {
            throw invalidCredentialException();
        }
    }

    private void ensureUserIsSubAdmin(User user) throws ValuePlusRuntimeException {
        if (!isSubAdmin(user)) {
            throw invalidCredentialException();
        }
    }

    private void ensureUserIsAgent(User user) throws ValuePlusRuntimeException {
        if (!isAgent(user)) {
            throw invalidCredentialException();
        }
    }

    private void ensureUserIsSuperAgent(User user) throws ValuePlusRuntimeException {
        if (!isSuperAgent(user)) {
            throw invalidCredentialException();
        }
    }

    private void matchPassword(String plainPassword, String encryptedPassword,User user) {
        if (!passwordEncoder.matches(plainPassword, encryptedPassword)) {
            user.setRetries(user.getRetries()+1);
            userRepository.save(user);
            System.out.println(user);
            throw invalidCredentialException();
        }
    }
    private void checkRetries(User user) throws ValuePlusException {
        try {
        if (user.getRetries()>=5){
            if (user.isEnabled())
            {
            user.setEnabled(false);
            userRepository.save(user);
            }
            System.out.println("accoutlockexception");
            throw accountLockedException();
        }}
        catch (ValuePlusException e){
            log.info(e.getMessage());
            throw new ValuePlusException(e.getMessage(), UNAUTHORIZED);
        }
    }
}
