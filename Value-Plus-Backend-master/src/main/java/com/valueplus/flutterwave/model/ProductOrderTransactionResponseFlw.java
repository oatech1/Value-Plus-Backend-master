package com.valueplus.flutterwave.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.valueplus.domain.model.ProductOrderTransactionResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductOrderTransactionResponseFlw implements Serializable {

        private String status;
        private String message;
        private FlutterwaveData data;

        @Data
        public static class FlutterwaveData{
            private int id;
            private String domain;
            private String status;
            private String reference;
            private String amount;
            private String message;
            private String gateway_response;
            private String paid_at;
            private String created_at;
            private String channel;
            private String currency;
            private com.valueplus.domain.model.ProductOrderTransactionResponse.Customer customer;
            private String createdAt;
            private String requested_amount;
            private String transaction_date;
        }

        @Data
        public static class Customer implements Serializable{
            private int id;
            private String email;
            private String customer_code;
        }


    }


