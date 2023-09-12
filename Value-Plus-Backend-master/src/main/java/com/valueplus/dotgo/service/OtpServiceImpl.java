package com.valueplus.dotgo.service;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.MessageResponse;
import com.valueplus.domain.service.abstracts.HttpApiClient;
import com.valueplus.domain.util.UserUtils;
import com.valueplus.dotgo.config.DotgoConfig;
import com.valueplus.dotgo.model.CallBackResponse;
import com.valueplus.dotgo.model.SmsResponse;

import com.valueplus.persistence.entity.OtpVerificationToken;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.repository.OtpVerificationRepository;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static java.lang.String.join;
import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
public class OtpServiceImpl extends HttpApiClient implements OtpService{
    private final DotgoConfig dotgoConfig;
    private final OtpVerificationRepository otpVerificationRepository;
    private final RestTemplate restTemplate;


    public OtpServiceImpl(RestTemplate restTemplate, DotgoConfig config, OtpVerificationRepository otpVerificationRepository, RestTemplateBuilder restTemplate1) {
        super("dotgo", restTemplate, config.getDotGoUrl());
        this.dotgoConfig = config;
        this.otpVerificationRepository = otpVerificationRepository;
        this.restTemplate = restTemplate1.build();
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
        messageConverters.add(converter);
        this.restTemplate.setMessageConverters(messageConverters);
    }

    private OtpVerificationToken otpGenerationForWithdrawal() {
        User user = UserUtils.getLoggedInUser();

        String generatedString = RandomStringUtils.randomAlphanumeric(6);

        OtpVerificationToken otpVerificationToken = new OtpVerificationToken();
        otpVerificationToken.setVerificationToken(generatedString);
        otpVerificationToken.setUserId(user.getId());
        otpVerificationToken.setReferenceID(user.getId().toString().concat(generatedString));
        otpVerificationToken.setExpireTime(LocalDateTime.now().plusMinutes(4));
        otpVerificationToken.setBody(" Your Valueplus token is  ".concat(generatedString).concat(". \n This expires after 5 minutes. \n Do not share this with anybody!"));

        otpVerificationRepository.save(otpVerificationToken);
        return otpVerificationToken;
    }


    private void otpVerification(String token) throws ValuePlusException {
        User user = UserUtils.getLoggedInUser();
        OtpVerificationToken verificationToken =  otpVerificationRepository.findByUserIdAndVerificationTokenAndExpireTimeLessThanEqual(user.getId(),token,LocalDateTime.now()).orElseThrow(() ->new ValuePlusException("Otp invalid or expired"));
//      verificationToken.
    }

    @Override
    public SmsResponse callDotgo(){
        User user = UserUtils.getLoggedInUser();
        String phoneNo = preparePhoneNumber(user.getPhone());
        Map<String, String> headers = prepareRequestHeader();
        System.out.println(headers);
        var type = new ParameterizedTypeReference<SmsResponse>() {};
        OtpVerificationToken verificationToken = new OtpVerificationToken();
        verificationToken = otpGenerationForWithdrawal();
        String generatedString = RandomStringUtils.randomNumeric(3);
        String id = user.getId().toString().concat(generatedString);
        String url = "/Messages?id=".concat(id+"&").concat("to=").concat(phoneNo +"&").concat("body=").concat(verificationToken.getBody()+"&").concat("sender_mask=").concat(dotgoConfig.getSenderMask()+"&").concat("expiry=").concat(dotgoConfig.getExpiry()+"&").concat("api_token=").concat(dotgoConfig.getToken()+"&").concat("callback_url=").concat(dotgoConfig.getCallBackUrl());
        SmsResponse response = sendRequest(HttpMethod.GET,url,null,headers,type);

        return response;
    }
    public  <T> T sendRequest(HttpMethod method,
                              String urlPath,
                              Object requestEntity,
                              Map<String, String> headers,
                              ParameterizedTypeReference<T> clazz) {
        String url = "https://konnect.dotgo.com/api/v1/Accounts/r9H0uB7j58eUtbnuYL_d2A==".concat(urlPath);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(APPLICATION_JSON);
        httpHeaders.setAccept(singletonList(APPLICATION_JSON));
        httpHeaders.setAll(headers);

        HttpEntity<?> httpEntity = new HttpEntity<>(requestEntity, httpHeaders);
        System.out.println(url);
        ResponseEntity<T> responseEntity = this.restTemplate.exchange(
                url,
                method,
                httpEntity,
                clazz);

        //log.info("{} API {} request to {} is successful", consumer, method.name(), urlPath);
        System.out.println(responseEntity.getBody());
        return responseEntity.getBody();
    }

    private String preparePhoneNumber(String phoneNo){
        StringBuilder sb = new StringBuilder(phoneNo);
        sb.deleteCharAt(0);
        String phone = "+234".concat(sb.toString());
        return phone;
    }
    private Map<String, String> prepareRequestHeader() {
        Map<String, String> header = new HashMap<>();
        header.put("Authorization",  dotgoConfig.getToken());
        return header;
    }
    @Override
    public void callBack(CallBackResponse callBackResponse) throws ValuePlusException {
        System.out.println("callBackResponse.getStatus() = " + callBackResponse.getStatus());

    OtpVerificationToken verificationToken = otpVerificationRepository.findByReferenceID(callBackResponse.getId()).orElseThrow(() ->new ValuePlusException("Otp invalid or expired"));
        System.out.println("verificationToken.getBody() = " + verificationToken.getBody());
        System.out.println("callBackResponse = " + callBackResponse.getStatus());
        verificationToken.setStatus(callBackResponse.getStatus());
        if (callBackResponse.getStatus() != "sent"|| (callBackResponse.getStatus()!="delivered"))
        { verificationToken.setErrorCode(callBackResponse.getError_code());
        verificationToken.setErrorReason(callBackResponse.getError_reason());}
        otpVerificationRepository.save(verificationToken);
}
    @Override
    public void verifyOtp(String otp) throws ValuePlusException {
        User user = UserUtils.getLoggedInUser();
        System.out.println(user.getId());
        System.out.println(otp);
     OtpVerificationToken verificationToken = otpVerificationRepository.findByUserIdAndVerificationToken(user.getId(),otp).orElseThrow(() ->new ValuePlusException("Otp not found"));
     if (verificationToken.getExpireTime().compareTo(LocalDateTime.now())<0){
         System.out.println("verificationToken.getExpireTime() = " + verificationToken.getExpireTime());
         System.out.println("LocalDateTime.now() = " + LocalDateTime.now());
         throw  new ValuePlusException("Otp expired");
     }
     verificationToken.setVerified(true);
    }
}
