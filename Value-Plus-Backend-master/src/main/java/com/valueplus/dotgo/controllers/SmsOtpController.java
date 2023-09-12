package com.valueplus.dotgo.controllers;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.dotgo.model.CallBackResponse;
import com.valueplus.dotgo.model.SmsResponse;
import com.valueplus.dotgo.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("v1/otp")
public class SmsOtpController {
    private final OtpService otpService;

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.OK)
    public SmsResponse sendOtp(){
        return otpService.callDotgo();
    }

    @PostMapping("/callback")
    @ResponseStatus(HttpStatus.OK)
    public void sendOtp(@RequestBody CallBackResponse callBackResponse) throws ValuePlusException {
         otpService.callBack(callBackResponse);
    }


}
