package com.valueplus.app.controller;

import com.valueplus.domain.FirebaseTokenRequest;
import com.valueplus.domain.model.PinUpdate;
import com.valueplus.domain.model.data4Me.ResetToken;
import com.valueplus.domain.service.concretes.ResetPinService;
import com.valueplus.domain.service.concretes.TokenService;
import com.valueplus.domain.util.UserUtils;
import com.valueplus.persistence.entity.User;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static com.valueplus.domain.util.UserUtils.getLoggedInUser;

@RestController
@RequestMapping(path = "v1/user", produces = MediaType.APPLICATION_JSON_VALUE)
public class PinController {
    private final ResetPinService resetPinService;

    private final TokenService tokenService;

    public PinController(ResetPinService resetPinService, TokenService tokenService) {
        this.resetPinService = resetPinService;
        this.tokenService = tokenService;
    }
//
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/send-reset-pin")
    public void sendResetPin() throws Exception {
        User userId = UserUtils.getLoggedInUser();
        resetPinService.sendResetPin(userId);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/verify-reset-pin-token")
    public ResponseEntity validateResetPinToken(@Valid @RequestBody ResetToken token) throws Exception {
        long userId = getLoggedInUser().getId();
        return resetPinService.validateResetPin(userId,token);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/reset-pin")
    public void resetPin(@Valid @RequestBody PinUpdate pinUpdate) throws Exception {
        long userId = getLoggedInUser().getId();
        resetPinService.resetPin(userId,pinUpdate);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/firebase")
    public ResponseEntity saveFirebaseToken(@Valid @RequestBody FirebaseTokenRequest token) throws Exception {
        User user = getLoggedInUser();
        return resetPinService.saveFirebaseToken(user,token);
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/delete-firebase-token")
    public ResponseEntity deleteFirebaseToken() throws Exception {
        long userId = getLoggedInUser().getId();
        return resetPinService.deleteFirebaseToken(userId);
    }
}
