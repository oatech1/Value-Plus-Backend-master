package com.valueplus.app.exception;

public class ReferralCodeNotExistException extends IllegalArgumentException{
    public ReferralCodeNotExistException(String msg) {
        super(msg);
    }
}
