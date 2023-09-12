package com.valueplus.dotgo.config;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

@AllArgsConstructor
@Getter
@Builder
public class DotgoConfig {

    @Value("${dotgo.sms.api.url}")
    private final String dotGoUrl;

    @Value("${dotgo.sms.api.token}")
    private final String token;

    @Value("${dotgo.sms.otp.expiry}")
    private final String expiry;

    @Value("${dotgo.sms.sender.mask}")
    private final String senderMask;

    @Value("${dotgo.sms.test.callback.url}")
    private final String callBackUrl;

    @Value("${dotgo.domain}")
    private final Domain domain;

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
