package com.valueplus.flutterwave.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferPayload {
    String accountBank;
    String accountNumber;
    BigDecimal amount;
    String narration;
}
