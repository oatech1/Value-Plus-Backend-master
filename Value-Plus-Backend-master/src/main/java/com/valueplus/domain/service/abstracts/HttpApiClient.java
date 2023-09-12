package com.valueplus.domain.service.abstracts;

import com.valueplus.domain.model.ProductOrderTransactionResponse;
import com.valueplus.dotgo.model.SmsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@RequiredArgsConstructor
public abstract class HttpApiClient {
    private final String consumer;
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public  <T> T sendRequest(HttpMethod method,
                                    String urlPath,
                                    Object requestEntity,
                                    Map<String, String> headers,
                                    ParameterizedTypeReference<T> clazz) {
        String url = baseUrl.concat(urlPath);

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

        log.info("{} API {} request to {} is successful", consumer, method.name(), urlPath);
        System.out.println(responseEntity.getBody());
        return responseEntity.getBody();
    }


    public <T> T sendRequest(HttpMethod method,
                             String urlPath,
                             Object requestEntity,
                             Map<String, String> headers) {
        ParameterizedTypeReference<T> typeReference = new ParameterizedTypeReference<>() {};

        return sendRequest(method, urlPath, requestEntity, headers, typeReference);
    }

    public ProductOrderTransactionResponse sendRequestWebClient(String urlPath, String token){
        WebClient.Builder builder = WebClient.builder();

        String url = baseUrl.concat(urlPath);

        ProductOrderTransactionResponse response = builder.build()
                .get()
                .uri(url)
                .header("Authorization", format("Bearer %s", token))
                .retrieve()
                .bodyToMono(ProductOrderTransactionResponse.class)
                .block();

        return response;
    }

    public  <T> SmsResponse getRequest(
                                       String urlPath) {
        String url = baseUrl.concat(urlPath);

//        HttpHeaders httpHeaders = new HttpHeaders();
//        httpHeaders.setContentType(APPLICATION_JSON);
//        httpHeaders.setAccept(singletonList(APPLICATION_JSON));
//        httpHeaders.setAll(headers);

//        HttpEntity<?> httpEntity = new HttpEntity<>(requestEntity, httpHeaders);
        System.out.println(url);
        ResponseEntity<SmsResponse> responseEntity = this.restTemplate.getForEntity(url, SmsResponse.class);



//        log.info("{} API {} request to {} is successful", consumer, method.name(), urlPath);
        System.out.println(responseEntity.getBody());
        return responseEntity.getBody();
    }

}
