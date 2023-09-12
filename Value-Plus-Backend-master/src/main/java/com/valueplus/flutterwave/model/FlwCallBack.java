package com.valueplus.flutterwave.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class FlwCallBack {
    private String event;
    @JsonProperty("event.type")
    public String eventType;
    private FlwData data;

    @Data
    @RequiredArgsConstructor
    public class FlwData {
        private String id;
        @JsonProperty("account_number")
        private String accountNumber;
        private String bank_name;
        private String bank_code;
        private String fullname;
        private String created_at;
        private String currency;
        @JsonProperty("debit_currency")
        private String debitCurrency;
        private String amount;
        private String fee;
        private String status;
        private String reference;
        private String meta;
        private String narration;
        private String approver;
        @JsonProperty("complete_message")
        private String completeMessage;
        @JsonProperty("requires_approval")
        private boolean requiresApproval;
        @JsonProperty("is_approved")
        private boolean isApproved;
    }
}
