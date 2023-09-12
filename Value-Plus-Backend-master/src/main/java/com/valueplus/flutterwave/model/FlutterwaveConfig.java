package com.valueplus.flutterwave.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

@AllArgsConstructor
@Getter
@Builder
public class FlutterwaveConfig {
    @Value("${flutterwave.base.url}")
    private final String baseUrl;
    @Value("${flutterwave.api.domain}")
    private final Domain domain;
    @Value("${flutterwave.api.test.key}")
    private final String testApiKey;
    @Value("${flutterwave.api.live.key}")
    private final String liveApiKey;
    @Value("${flutterwave.api.test.transfer.callback}")
    private final String testTransferCallBackUrl;
    @Value("${flutterwave.api.live.transfer.callback}")
    private final String liveTransferCallBackUrl;
    private final String paymentReason;

    public enum Domain {
        TEST("test"), LIVE("live");

        private final String domain;

        Domain(String domain) {
            this.domain = domain;
        }

        public static Domain fromString(String domain) {
            for (Domain b : Domain.values()) {
                if (b.domain.equalsIgnoreCase(domain)) {
                    return b;
                }
            }
            return TEST;
        }
    }
}
