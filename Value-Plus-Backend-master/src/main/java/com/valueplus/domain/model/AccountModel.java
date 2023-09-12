package com.valueplus.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountModel {
    private Long id;
    private Long userId;
    private String accountNumber;
    private String accountName;
    private String bankCode;
    private String bankName;
}
