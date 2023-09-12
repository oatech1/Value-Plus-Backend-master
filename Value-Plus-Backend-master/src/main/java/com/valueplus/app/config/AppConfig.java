package com.valueplus.app.config;

import com.valueplus.app.exception.AsyncExceptionHandler;
import com.valueplus.dotgo.config.DotgoConfig;
import com.valueplus.flutterwave.model.FlutterwaveConfig;
import com.valueplus.paystack.model.PaystackConfig;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestTemplate;
import java.time.Clock;
import java.util.Properties;

@Configuration
public class AppConfig {

    @Value("${paystack.base.url}")
    private String paystackUrl;

    @Value("${paystack.api.live.key}")
    private String paystackLiveApiKey;

    @Value("${paystack.api.test.key}")
    private String paystackTestApiKey;

    @Value("${paystack.api.domain:test}")
    private String paystackDomain;
    @Value("${paystack.api.transfer.callback}")
    private String transferCallbackUrl;

    @Value("${flutterwave.base.url}")
    private String flutterwaveUrl;

    @Value("${flutterwave.api.live.key}")
    private String flutterwaveLiveApiKey;

    @Value("${flutterwave.api.test.key}")
    private String flutterwaveTestApiKey;

    @Value("${flutterwave.api.domain:test}")
    private String flutterwaveDomain;

    @Value("${flutterwave.api.test.transfer.callback}")
    private String flutterwaveTransferCallbackUrl;

    @Value("${dotgo.sms.api.url}")
    private  String dotGoUrl;

    @Value("${dotgo.sms.api.token}")
    private  String token;

    @Value("${dotgo.sms.otp.expiry}")
    private String expiry;

    @Value("${dotgo.sms.sender.mask}")
    private String senderMask;

    @Value("${dotgo.sms.test.callback.url}")
    private String callBackUrl;

    @Value("${dotgo.domain}")
    private DotgoConfig.Domain domain;


    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public PaystackConfig paystackConfig() {
        return PaystackConfig.builder()
                .baseUrl(paystackUrl)
                .domain(PaystackConfig.Domain.fromString(paystackDomain))
                .liveApiKey(paystackLiveApiKey)
                .testApiKey(paystackTestApiKey)
                .paymentReason("ValuePlus Payment")
                .transferCallBackUrl(transferCallbackUrl)
                .build();
    }

    @Bean
    public VelocityEngine velocityEngine() {
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("resource.loader", "class");
        velocityEngine.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        return velocityEngine;
    }

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost("in-v3.mailjet.com");
        mailSender.setPort(587);
        mailSender.setUsername("2a3d7ddea97ab7f824b4798d770d5f02");
        mailSender.setPassword("74ce25c6b7f3bec5802f1c5923f97751");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.debug.auth", "true");
        props.put("mail.debug", "true");
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.smtp.starttls.enable","true");

        return mailSender;
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }

    @Bean
    public FlutterwaveConfig flutterwaveConfig() {
        return FlutterwaveConfig.builder()
                .baseUrl(flutterwaveUrl)
                .domain(FlutterwaveConfig.Domain.fromString(flutterwaveDomain))
                .liveApiKey(flutterwaveLiveApiKey)
                .testApiKey(flutterwaveTestApiKey)
                .paymentReason("ValuePlus Payment")
                .testTransferCallBackUrl(flutterwaveTransferCallbackUrl)
                .build();
    }

    }
    @Bean
    public DotgoConfig dotgoConfig(){
        return DotgoConfig.builder()
                .expiry(expiry)
                .dotGoUrl(dotGoUrl)
                .token(token)
                .domain(domain)
                .senderMask(senderMask)
                .callBackUrl(callBackUrl)
                .build();
    }
}
