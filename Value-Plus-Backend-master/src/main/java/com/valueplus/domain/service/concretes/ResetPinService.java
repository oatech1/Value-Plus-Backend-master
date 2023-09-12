package com.valueplus.domain.service.concretes;

import com.valueplus.app.config.audit.AuditEventPublisher;
import com.valueplus.app.exception.NotFoundException;
import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.FirebaseTokenRequest;
import com.valueplus.domain.mail.EmailService;
import com.valueplus.domain.model.PinUpdate;
import com.valueplus.domain.model.data4Me.ResetToken;
import com.valueplus.persistence.entity.FirebaseToken;
import com.valueplus.persistence.entity.PinResetToken;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.repository.FireBaseTokenRepository;
import com.valueplus.persistence.repository.PinResetTokenRepository;
import com.valueplus.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.valueplus.domain.enums.ActionType.USER_PIN_UPDATE;
import static com.valueplus.domain.enums.EntityType.USER;
import static com.valueplus.domain.util.MapperUtil.copy;
import static org.bitbucket.dollar.Dollar.$;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ResetPinService {

    private final PinResetTokenRepository pinResetTokenRepository;
    private final EmailService emailService;
    private final String tokenGenerationPattern = $('0','9').join();
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuditEventPublisher auditEvent;
    private final FireBaseTokenRepository fireBaseTokenRepository;



    public void sendResetPin(User user) throws Exception {
        Optional<PinResetToken> oldResetToken = pinResetTokenRepository.findByUserId(user.getId());
        if(oldResetToken.isPresent()){
            pinResetTokenRepository.delete(oldResetToken.get());
        };

       String token =  $(tokenGenerationPattern).shuffle().slice(6).toString();

        PinResetToken resetToken = new PinResetToken(user.getId(),token);

        pinResetTokenRepository.save(resetToken);

        emailService.sendPinReset(user, token);
    }


    public User resetPin(Long userId, PinUpdate pinUpdate) throws Exception {

        User user = getUserById(userId);
        var oldObject = copy(user, User.class);

        String hashPin = passwordEncoder.encode(pinUpdate.getNewPin());
        user.setTransactionPin(hashPin);

        var savedEntity = userRepository.save(user);
        auditEvent.publish(oldObject, savedEntity, USER_PIN_UPDATE, USER);

        emailService.sendPinNotification(user);
        return user;
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found"));
    }

    public ResponseEntity validateResetPin(Long userId, ResetToken token) throws ValuePlusException {
        Optional<PinResetToken> resetToken = pinResetTokenRepository.findByResetToken(token.getResetToken());
        if (resetToken.isEmpty()) {
            throw new ValuePlusException("invalid token supplied", BAD_REQUEST);
        }
        Long resetTokenUserId = resetToken.get().getUserId();
        if(!resetTokenUserId.equals(userId)){
            throw new ValuePlusException("user not currently logged in", BAD_REQUEST);
        }
        return new ResponseEntity("Validation Successful", HttpStatus.OK);
    }

    public ResponseEntity saveFirebaseToken(User user, FirebaseTokenRequest token) throws ValuePlusException {
        FirebaseToken firebaseToken = new FirebaseToken();
        firebaseToken.setUser(user);
        firebaseToken.setToken(token.getToken());
        fireBaseTokenRepository.save(firebaseToken);

        return new ResponseEntity("Token saved successfully",HttpStatus.OK);
    }

    public ResponseEntity deleteFirebaseToken (Long userId) throws ValuePlusException{
        Optional<FirebaseToken> optionalFirebaseToken = fireBaseTokenRepository.findByUserId(userId);
        if(optionalFirebaseToken.isEmpty()) {
            throw new ValuePlusException("token not found", NOT_FOUND);
        }
        FirebaseToken firebaseToken = optionalFirebaseToken.get();
        fireBaseTokenRepository.delete(firebaseToken);
        return new ResponseEntity(" Token deleted successfully", HttpStatus.OK);
    }

}
