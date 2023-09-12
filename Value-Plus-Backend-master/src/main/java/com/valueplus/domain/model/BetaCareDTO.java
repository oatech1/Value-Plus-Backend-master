package com.valueplus.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BetaCareDTO {
    private String referralCode;
    private String dateOfRegistration;
}
