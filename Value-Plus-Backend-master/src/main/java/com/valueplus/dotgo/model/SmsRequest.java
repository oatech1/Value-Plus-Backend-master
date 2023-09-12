package com.valueplus.dotgo.model;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class SmsRequest {

    private String id;
    private String to;
    private String from;
    private String direction;
    private String sender_mask;
    private String body;
    private int expiry;
    private String priority;
    private String callback_url;

}
