package com.valueplus.dotgo.model;

import lombok.Data;

@Data
public class CallBackResponse {
    private String ref_id;
    private String account_balance;
    private String price;
    private String from;
    private String id;
    private String to;
    private String event_timestamp;
    private String status;
    private String timestamp;
    private String error_reason;
    private String error_code;

}
