package com.valueplus.dotgo.service;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.dotgo.model.CallBackResponse;
import com.valueplus.dotgo.model.SmsResponse;


public interface OtpService {
    SmsResponse callDotgo();

    void callBack(CallBackResponse callBackResponse) throws ValuePlusException;

    void verifyOtp(String otp) throws ValuePlusException;
}
