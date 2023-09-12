package com.valueplus.dotgo.model;

import lombok.Data;

@Data
public class SmsResponse {

    private String status;
    private String error_code;
    private String error_reason;
}
