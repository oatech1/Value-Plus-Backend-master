package com.valueplus.flutterwave.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallbackResponse {

    private String status;
    private String trx_ref;
    private String transaction_id;
}
