package com.valueplus.domain.model;

import lombok.Data;
@Data
public class CurrencyConverterResponse {

    private String data;
    private Info info;
    private Query query;
    private String result;
    private String success;

    @Data
    public static class Info {
        private String rate;
        private String timeStamp;
    }
    @Data
    public static class Query {
        private String amount;
        private String from;
        private String to;
    }


}