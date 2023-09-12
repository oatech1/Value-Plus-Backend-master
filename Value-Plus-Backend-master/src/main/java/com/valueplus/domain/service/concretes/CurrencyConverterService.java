package com.valueplus.domain.service.concretes;

import com.valueplus.domain.model.CurrencyConverterResponse;
import com.valueplus.domain.service.abstracts.HttpApiClient;
import com.valueplus.smileIdentity.SmileIdentityResponse;
import io.micrometer.core.ipc.http.HttpSender;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class CurrencyConverterService extends HttpApiClient {
    @Autowired
    RestTemplate restTemplate;

    public CurrencyConverterService( RestTemplate restTemplate) {
        super("converter", restTemplate, "");
    }

    public String convertBetwayEarnings(String earnings){


        String url = "https://api.apilayer.com/exchangerates_data/convert?to=NGN&from=EUR&amount=";
        Map<String, String> header = new HashMap<>();
        header.put("apikey","sRTYPaPLPXEDEZKQ0sfruOPiX2RUI26l");

        var type = new ParameterizedTypeReference<CurrencyConverterResponse>() {};
        CurrencyConverterResponse response = sendRequest(HttpMethod.GET, url.concat("20"), null, header, type);
        if (response != null){
            System.out.println("response.getInfo().getRate() = " + response.getInfo().getRate());
           return response.getInfo().getRate();
        }
        return null;
}
}
